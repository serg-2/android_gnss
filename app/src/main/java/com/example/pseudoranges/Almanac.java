package com.example.pseudoranges;

import static java.lang.Math.*;

import android.util.Log;
import android.widget.Toast;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.data.ZipJarCrawler;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.propagation.analytical.gnss.GNSSPropagator;
import org.orekit.propagation.analytical.gnss.data.GPSAlmanac;
import org.orekit.gnss.YUMAParser;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.Objects;

public class Almanac {
    // Other almanacs
    // http://celestrak.org/GPS/almanac/Yuma/2025/almanac.yuma.week0307.147456.txt
    // http://celestrak.org/GPS/almanac/Yuma/2025/

    private static final String TAG = "MyAlmanac";

    private static final String OREKIT_FILE = "orekit-data-master.zip";

    private final FileDownloader downloader;
    private final MainActivity activity;
    private YUMAParser yumaParser;

    // Calculation
    // https://gssc.esa.int/navipedia/index.php/Coordinates_Computation_from_Almanac_Data
    // https://gssc.esa.int/navipedia/index.php?title=GPS_and_Galileo_Satellite_Coordinates_Computation

    public Almanac(MainActivity mainActivity) {
        this.activity = mainActivity;
        this.downloader = new FileDownloader(mainActivity);
    }

    private void initOreKit() {
        // Init OreKit
        if (!getEphemeris()) {
            Log.e(TAG, "Can't download ephemeris");
            return;
        }
        DataContext.getDefault().getDataProvidersManager().addProvider(new ZipJarCrawler(new File(activity.getExternalFilesDir(null), OREKIT_FILE)));
        this.yumaParser = new YUMAParser(
            null,
            DataContext.getDefault().getDataProvidersManager(),
            DataContext.getDefault().getTimeScales()
        );
    }

    public void loadAlmanac() {
        initOreKit();

        String almanacFileName = getAlmanacName(1);
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(new File(activity.getExternalFilesDir(null), almanacFileName));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Last almanac file not found: " + almanacFileName);
            return;
        }

        // LONG TIME OPERATION!!! ~10 secs
        var ts = System.currentTimeMillis();
        try {
            yumaParser.loadData(inputStream, "almanac");
        } catch (IOException | ParseException e) {
            Log.e(TAG, "Exception during opening almanac: " + e);
            return;
        }
        Log.i(TAG, "Loaded almanac for: " + (System.currentTimeMillis() - ts) / 1000 + " sec.");
        activity.gpsAlmanac.postValue(yumaParser.getAlmanacs());
    }

    // Long operation. ~ 10 secs
    private GNSSPropagator getPropagatorForAlmanac(GPSAlmanac almanacSatellite) {
        var ts = System.currentTimeMillis();
        var result = almanacSatellite.getPropagator();
        Log.i(TAG, "Loaded propagator for " + (System.currentTimeMillis() - ts) / 1000 + " sec.");
        return result;
    }

    private GPSAlmanac getAlmanacByPRN(int prn) {
        return Objects.requireNonNull(activity.gpsAlmanac.getValue())
            .stream()
            .filter(gpsAlmanac -> gpsAlmanac.getPRN() == prn)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No this PRN in almanac: " + prn));
    }

    private boolean getEphemeris() {
        String orekitUrl = "https://gitlab.orekit.org/orekit/orekit-data/-/archive/master/orekit-data-master.zip";
        if (downloader.isFileAlreadyDownloaded(OREKIT_FILE)) {
            // activity.runOnUiThread(() -> Toast.makeText(activity, "Эфемериды уже загружены.", Toast.LENGTH_SHORT).show());
        } else {
            File file = downloader.downloadZipFile(orekitUrl, OREKIT_FILE);
            activity.runOnUiThread(() -> Toast.makeText(activity, "Скачиваем эфемериды...", Toast.LENGTH_LONG).show());
            if (file != null) {
                activity.runOnUiThread(() -> Toast.makeText(activity, "Эфемериды скачаны.", Toast.LENGTH_SHORT).show());
            } else {
                return false;
            }
        }
        return true;
    }

    private String getAlmanacName(int daysOfOld) {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        if (day < 3) {
            year--;
            day = 365;
        } else {
            day -= daysOfOld;
        }
        String almanacUrl = "https://www.navcen.uscg.gov/sites/default/files/gps/almanac/%d/yuma/%03d.alm".formatted(
            year,
            day
        );
        String almanacFilename = "%d-%03d.alm".formatted(year, day);

        if (downloader.isFileAlreadyDownloaded(almanacFilename)) {
            // activity.runOnUiThread(() -> Toast.makeText(activity, "Альманах уже загружен.", Toast.LENGTH_SHORT).show());
        } else {
            File file = downloader.downloadZipFile(almanacUrl, almanacFilename);
            // activity.runOnUiThread(() -> Toast.makeText(activity, "Скачиваем свежий альманах...", Toast.LENGTH_SHORT).show());
            if (file != null) {
                activity.runOnUiThread(() -> Toast.makeText(activity, "Альманах скачан.", Toast.LENGTH_SHORT).show());
            } else {
                return null;
            }
        }
        return almanacFilename;
    }

    // Get Distance to Satellite from Hardcoded Coordinates. For now ~ 200k, difference.
    // Old Almanac gives ~ 20 - 40km difference
    // Time of Calculation gives ~ 10km difference
    // Problem: 1) Almanac not so fine as ephemeris...
    //          2) Wrong calculation if biases in distance
    public void almanacFunction1(Instant time, int satellitePRN) {
        AbsoluteDate dateAt = new AbsoluteDate(time);
        GPSAlmanac almanacSatellite = getAlmanacByPRN(satellitePRN);

        // 10 sec operation
        GNSSPropagator propagator = getPropagatorForAlmanac(almanacSatellite);

        PVCoordinates fullCoordinates = propagator.propagateInEcef(dateAt);
        Vector3D positionOfSatellite = fullCoordinates.getPosition();
        Log.e("COORDINATES", "Position of Satellite %d: %s".formatted(
            satellitePRN,
            Arrays.toString(positionOfSatellite.toArray()))
        );

        /* Orbit to lat, long , alt
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            FramesFactory.getITRF(IERSConventions.IERS_2010, false)
        );
        GeodeticPoint point = earth.transform(orbit.getPosition(), orbit.getFrame(), orbit.getDate());
         */

        var earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(earthFrame);
        GeodeticPoint point = new GeodeticPoint(toRadians(55.67), toRadians(37.74), 10);
        Vector3D positionOnEarth = ellipsoid.transform(point);
        Log.e("COORDINATES", "Position onEarth: %s".formatted(
            Arrays.toString(positionOnEarth.toArray()))
        );

        // Calculate distance
        double result = positionOfSatellite.distance(positionOnEarth);
        Log.e("COORDINATES", "Distance: %.0f km".formatted(
                result / 1000d
            )
        );

        // For Now near 200km of difference
    }

}
