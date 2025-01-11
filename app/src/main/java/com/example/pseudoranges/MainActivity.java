package com.example.pseudoranges;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.pseudoranges.models.MainViewModel;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, AdapterView.OnItemSelectedListener {
    private static final int LOCATION_REQUEST_ID = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private TextView mainTV1;
    private TextView mainTV2;
    private static final String TAG = "MainActivity";
    private Spinner constSpinner;
    private CheckBox cbFilter;

    private MeasurementProvider mMeasurementProvider;
    private MyListener myListener;
    public MainViewModel mainViewModel;

    public final MutableLiveData<String> tv1Text = new MutableLiveData<>();
    public final MutableLiveData<String> tv2Text = new MutableLiveData<>();

    public final MutableLiveData<String> filterConstellation = new MutableLiveData<>("GPS");
    public final MutableLiveData<Integer> filterTime = new MutableLiveData<>(9);

    private Timer timer;
    private boolean timerState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainViewModel = getVieModel(MainViewModel.class);

        mainTV1 = findViewById(R.id.mainTV1);
        mainTV2 = findViewById(R.id.mainTV2);
        mainTV1.setTypeface(Typeface.MONOSPACE);
        mainTV2.setTypeface(Typeface.MONOSPACE);

        cbFilter = findViewById(R.id.cbTime);

        constSpinner = findViewById(R.id.const_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.constellations_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        constSpinner.setAdapter(adapter);
        constSpinner.setOnItemSelectedListener(this);

        requestPermissionAndSetupFragments(this);

        // Create the observer which updates the UI.
        final Observer<String> tv1Observer = newName -> {
            // Update the UI, in this case, a TextView.
            mainTV1.setText(newName);
        };
        final Observer<String> tv2Observer = newName -> {
            // Update the UI, in this case, a TextView.
            mainTV2.setText(newName);
        };

        tv1Text.observe(this, tv1Observer);
        tv2Text.observe(this, tv2Observer);

    }

    private void requestPermissionAndSetupFragments(final Activity activity) {
        if (hasPermissions(activity)) {
            setupFragments();
        } else {
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, LOCATION_REQUEST_ID);
        }
    }

    private boolean hasPermissions(Activity activity) {
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void setupFragments() {
        myListener = new MyListener(this);
        mMeasurementProvider =
                new MeasurementProvider(
                        getApplicationContext(),
                        myListener);
        mMeasurementProvider.registerMeasurements();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "Connection failed: ErrorCode = " + result.getErrorCode());
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int cause) {

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (timerState) {
            timer.cancel();
            timerState = false;
        }
        if (hasPermissions(this)) {
            mMeasurementProvider.unregisterMeasurements();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasPermissions(this)) {
            mMeasurementProvider.registerMeasurements();
        }

        // Timer
        if (!timerState) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateScreen();
                }
            }, 0, 500);//put here time 1000 milliseconds=1 second
            timerState = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_REQUEST_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        setupFragments();
                    }
                } else {
                    Toast.makeText(this, "Can't run without permission.", Toast.LENGTH_LONG).show();
                    this.finish();
                }
                break;
            }
        }
    }

    private void updateScreen() {
        tv1Text.postValue(mainViewModel.toStringClockClass());
        // GPS, SBAS, GLONASS, QZSS, BEIDOU, GALILEO
        tv2Text.postValue(mainViewModel.toStringMeasurementMap(filterConstellation.getValue(), filterTime.getValue()));
    }

    public void onCBTimeClicked(View view) {
        if (cbFilter.isChecked()) {
            filterTime.postValue(9);
        } else {
            filterTime.postValue(0);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        filterConstellation.postValue((String) parent.getItemAtPosition(position));
        // Log.e("MAIN", "SELECTED: " + position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    // TECH PART ===================================================================
    protected <T extends ViewModel> T getVieModel(Class<T> clazz) {
        return getVieModel(this, clazz);
    }

    protected <T extends ViewModel> T getVieModel(ViewModelStoreOwner owner, Class<T> clazz) {
        return new ViewModelProvider(owner).get(clazz);
    }

}
