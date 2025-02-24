package com.example.pseudoranges;

import android.util.Log;
import android.widget.Toast;

import org.orekit.data.DataContext;
import org.orekit.data.ZipJarCrawler;
import org.orekit.propagation.analytical.gnss.data.GPSAlmanac;
import org.orekit.gnss.YUMAParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;

public class Almanac {
    // Other almanacs
    // http://celestrak.org/GPS/almanac/Yuma/2025/almanac.yuma.week0307.147456.txt
    // http://celestrak.org/GPS/almanac/Yuma/2025/

    private static final String TAG = "MyAlmanac";

    private static final String OREKIT_FILE = "orekit-data-master.zip";

    private final FileDownloader downloader;
    private final MainActivity activity;

    // Calculation
    // https://gssc.esa.int/navipedia/index.php/Coordinates_Computation_from_Almanac_Data
    // https://gssc.esa.int/navipedia/index.php?title=GPS_and_Galileo_Satellite_Coordinates_Computation

    public Almanac(MainActivity mainActivity) {
        this.activity = mainActivity;
        this.downloader = new FileDownloader(mainActivity);
    }

    public void loadAlmanac() {
        // To init OreKit we need file with ephemeris
        if (!getEphemeris()) {
            Log.e(TAG, "Can't download ephemeris");
            return;
        }
        String almanacFileName = getAlmanacName();

        DataContext.getDefault().getDataProvidersManager().addProvider(new ZipJarCrawler(new File(activity.getExternalFilesDir(null), OREKIT_FILE)));

        YUMAParser yumaParser = new YUMAParser(
            null,
            DataContext.getDefault().getDataProvidersManager(),
            DataContext.getDefault().getTimeScales()
        );

        var ts = System.currentTimeMillis();
        // LONG TIME OPERATION!!! ~10 secs
        try {
            FileInputStream inputStream = new FileInputStream(new File(activity.getExternalFilesDir(null), almanacFileName));
            yumaParser.loadData(
                inputStream,
                "almanac"
            );
        } catch (IOException | ParseException e) {
            Log.e(TAG, "Exception during opening almanac: " + e);
            return;
        }
        Log.e(TAG, "Loaded almanac for: " + ((System.currentTimeMillis() - ts) / 1000) + " sec.");

        activity.gpsAlmanac.postValue(yumaParser.getAlmanacs());
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

    private String getAlmanacName() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        if (day < 3) {
            year--;
            day = 365;
        } else {
            day -= 2;
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

}
