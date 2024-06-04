package com.example.ex1_permissions;

import android.Manifest;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.snackbar.Snackbar;


public class MainActivity extends AppCompatActivity {
    private MaterialTextView main_TXT_enter_password;
    private AppCompatEditText main_EDT_password;
    private MaterialButton main_BTN_login;
    private MaterialButton main_BTN_grant;
    private MaterialTextView main_LBL_title;
    private MaterialTextView main_LBL_content;
    private MaterialTextView main_LBL_Progress;
    private MaterialButton main_BTN_close;
    private boolean isReadContactsGranted = false;
    private STATE state = STATE.NA;
    private enum STATE {
        NA,
        NO_REGULAR_PERMISSION,
        NO_BACKGROUND_PERMISSION,
        LOCATION_DISABLED,
        LOCATION_SETTING_PROCESS,
        LOCATION_SETTINGS_OK,
        ALL_OK
    }
    ActivityResultLauncher<String> readContactsPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(), isGranted -> {
                        if (isGranted) {
                            readContactsPermission();
                        } else {
                            boolean showDialog = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS);
                            if (showDialog){
                                openReadContactsPermissionDialog();
                            }else{
                                openAppInfo();
                            }
                        }
                    });
    ActivityResultLauncher<Intent> readContactsManualPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            readContactsPermission();
                        }
                    }
            );

    // this is the request for the permissions
    // we want to create a situation that we have location permissions, we will do it in 4 steps:
    // 1. we will check if the components that give us location is on, if not we will ask for turn on.
    // 2. we will ask for permission, is will give us approximate.
    // 3. we will ask for more accurate permission - fine location.
    // 4. we will ask for background service, so it will find location even when the application is closed.
    ActivityResultLauncher<String> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts
                            .RequestPermission(), result -> {
                        if (result) {
                            // location access granted.
                            checkStatus();
                        } else {
                            // No location access granted.
                            if (shouldShowRequestPermissionRationale(checkPermissionsStatus(this))){
                                Snackbar.make(findViewById(
                                                        android.R.id.content),
                                                R.string.permission_rationale,
                                                Snackbar.LENGTH_INDEFINITE)
                                        .setDuration(Snackbar.LENGTH_LONG)
                                        .setAction(R.string.settings, new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                locationPermissionRequest.launch(checkPermissionsStatus(MainActivity.this));
                                            }
                                        })
                                        .show();
                            }
                            else {
                                buildAlertMessageManuallyBackgroundPermission(
                                        checkPermissionsStatus(this)
                                );
                            }
                        }
                    }
            );
    ActivityResultLauncher<Intent> appSettingsResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
                checkStatus();
            });



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        initViews();
    }


    // i want that the view will be updated every time that i change the screen instead of restarting the activity
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            checkStatus();
    }


    private void checkStatus() {
        String permissionStatus = checkPermissionsStatus(this);// check if i have permissions

        if (!isLocationEnabled(this))// if the location component active
            state = STATE.LOCATION_DISABLED;

        else if (permissionStatus != null)
            if(permissionStatus.equals(Manifest.permission.READ_CONTACTS))
                state = STATE.LOCATION_SETTINGS_OK;

            else if (permissionStatus.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                state = STATE.NO_BACKGROUND_PERMISSION;
            else
                state = STATE.NO_REGULAR_PERMISSION;

        else {
            state = STATE.LOCATION_SETTING_PROCESS;
            validateLocationSensorsEnabled();
        }
        updateUI();
    }


    private void updateUI() {
        switch (state) {
            case NA:
                main_LBL_title.setText("NA");
                main_LBL_content.setText("NA");
                main_LBL_Progress.setText("0/5");
                main_BTN_close.setVisibility(View.INVISIBLE);
                main_BTN_grant.setVisibility(View.INVISIBLE);
                main_BTN_login.setVisibility(View.INVISIBLE);
                main_TXT_enter_password.setVisibility(View.INVISIBLE);
                main_EDT_password.setVisibility(View.INVISIBLE);
                break;

            case NO_REGULAR_PERMISSION:
                main_LBL_title.setText("Location Permission");
                main_LBL_content.setText("Location permission is needed for core functionality.\nPlease Enable the app permission to access your location data");
                main_LBL_Progress.setText("2/5");
                main_TXT_enter_password.setVisibility(View.INVISIBLE);
                main_EDT_password.setVisibility(View.INVISIBLE);
                main_BTN_login.setVisibility(View.INVISIBLE);
                main_BTN_close.setVisibility(View.VISIBLE);
                main_BTN_grant.setVisibility(View.VISIBLE);
                main_BTN_grant.setText("Grant Permission");
                main_BTN_grant.setOnClickListener(v -> askForLocationPermissions(checkPermissionsStatus(this)));
                break;

            case NO_BACKGROUND_PERMISSION:
                main_LBL_title.setText("Background location permission");
                main_LBL_content.setText("This app collects location data even when the app is closed or not in use.\nTo protect your privacy, the app stores only calculated indicators, like distance from home and never exact location.\nA notification is always displayed in the notifications bar when service is running.");
                main_LBL_Progress.setText("3/5");
                main_TXT_enter_password.setVisibility(View.INVISIBLE);
                main_EDT_password.setVisibility(View.INVISIBLE);
                main_BTN_close.setVisibility(View.VISIBLE);
                main_BTN_grant.setVisibility(View.VISIBLE);
                main_BTN_grant.setText("Grant Permission");
                main_BTN_grant.setOnClickListener(v ->
                        askForLocationPermissions(checkPermissionsStatus(this)));
                break;

            case LOCATION_DISABLED:
                main_LBL_title.setText("Enable Location Services");
                main_LBL_content.setText("The app samples your location.\nPlease enable location services (GPS).");
                main_LBL_Progress.setText("1/5");
                main_TXT_enter_password.setVisibility(View.INVISIBLE);
                main_EDT_password.setVisibility(View.INVISIBLE);
                main_BTN_close.setVisibility(View.VISIBLE);
                main_BTN_grant.setVisibility(View.VISIBLE);
                main_BTN_login.setVisibility(View.INVISIBLE);
                main_BTN_grant.setText("Turn On Location");
                main_BTN_grant.setOnClickListener(v -> {
                    enableLocationServiceProgramatically();
                });
                break;

            case LOCATION_SETTING_PROCESS:
                main_LBL_title.setText("LOCATION_SETTINGS_PROCCESS");
                main_LBL_content.setText("LOCATION_SETTINGS_PROCCESS");
                main_LBL_Progress.setText("4/5");
                main_TXT_enter_password.setVisibility(View.INVISIBLE);
                main_EDT_password.setVisibility(View.INVISIBLE);
                main_BTN_close.setVisibility(View.INVISIBLE);
                main_BTN_grant.setVisibility(View.INVISIBLE);
                break;

            case LOCATION_SETTINGS_OK:
                main_LBL_title.setText("Enable Read Contacts Permission");
                main_LBL_content.setText("The app need your permission to accsess phone contacts");
                main_LBL_Progress.setText("4/5");
                main_TXT_enter_password.setVisibility(View.INVISIBLE);
                main_EDT_password.setVisibility(View.INVISIBLE);
                main_BTN_close.setVisibility(View.VISIBLE);
                main_BTN_login.setVisibility(View.INVISIBLE);
                main_BTN_grant.setVisibility(View.VISIBLE);
                main_BTN_grant.setText("Grant");
                main_BTN_grant.setOnClickListener(v -> {
                    readContactsPermission();
                });
                break;

            case ALL_OK:
                main_LBL_title.setText("Almost There! 👍🏻");
                main_LBL_content.setText("Location services are running and all permissions have been granted.\nYou can now enter password.");
                main_LBL_Progress.setText("5/5");
                main_BTN_close.setVisibility(View.INVISIBLE);
                main_BTN_grant.setVisibility(View.INVISIBLE);
                main_BTN_login.setVisibility(View.VISIBLE);
                main_TXT_enter_password.setVisibility(View.VISIBLE);
                main_EDT_password.setVisibility(View.VISIBLE);
                break;
        }
    }


    private void askForLocationPermissions(String permission) {
        if (shouldShowRequestPermissionRationale(permission)) {
            if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                buildAlertMessageManuallyBackgroundPermission(permission);
            else
                locationPermissionRequest.launch(permission);
        }
        else {
            locationPermissionRequest.launch(permission);
        }
    }


    private void buildAlertMessageManuallyBackgroundPermission(String permission) {
        if (permission == null)
            return;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String allow_message_type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? "Allow all the time" : "Allow";

        builder.setMessage("You need to enable background location permission manually." +
                        "\nOn the page that opens - click on PERMISSIONS, then on LOCATION and then check '" + allow_message_type + "'")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> openAppSettings())
                .setNegativeButton("Exit",(dialog, which) -> finish());
        builder.create().show();
    }


    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(),null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appSettingsResultLauncher.launch(intent);
    }

    private void enableLocationServiceProgramatically() {
        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }


    private void validateLocationSensorsEnabled() {
        // check whether location setting are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        LocationRequest.Builder requestBuilder = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY);

        builder.addLocationRequest(requestBuilder.setPriority(Priority.PRIORITY_HIGH_ACCURACY).build())
                .addLocationRequest(requestBuilder.setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY).build());

        builder.setNeedBle(true); // check if the bluetooth in active

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);

        settingsClient.checkLocationSettings(builder.build())
                .addOnSuccessListener(locationSettingsResponse -> {
                    state = STATE.ALL_OK;
                    updateUI();
                })
                .addOnFailureListener(e -> Log.e("GPS", "Unable to execute request."))
                .addOnCanceledListener(() -> Log.e("GPS", "checkLocationSettings -> onCanceled"));
    }


    private boolean isLocationEnabled(Context context) { // check if i have access to location services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        } else {
            int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
            return mode != Settings.Secure.LOCATION_MODE_OFF;
        }
    }


    private String checkPermissionsStatus(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return Manifest.permission.ACCESS_FINE_LOCATION;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return Manifest.permission.ACCESS_COARSE_LOCATION;

        if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return Manifest.permission.ACCESS_BACKGROUND_LOCATION;

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            return Manifest.permission.READ_CONTACTS;

        return null;
    }


    private void readContactsPermission() {
        boolean showDialog = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS);
        if(showDialog){
            openReadContactsPermissionDialog();
        }else{
            readContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }


    private int getBatteryLevel() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } else {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            return (int) ((level / (float) scale) * 100);
        }
    }


    private void openReadContactsPermissionDialog() {
        new MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setTitle("READ CONTACTS Permission Required")
                .setMessage("This app requires Contacts permission.\n" +
                        "Please grant the permission in app settings.\n" +
                        "App info -> Permissions -> Contacts -> Allow")
                .setPositiveButton("Settings", (dialog, which) -> openAppInfo())
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void openAppInfo() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        readContactsManualPermissionLauncher.launch(intent);
    }


    private void loginClicked() {
        int batteryLevel = getBatteryLevel();
        String password = main_EDT_password.getText().toString();

        if(String.valueOf(batteryLevel).equals(password)){
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(intent);
        }
    }


    private void initViews() {
        main_BTN_login.setOnClickListener(v -> loginClicked());
        main_BTN_close.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }


    private void findViews() {
        main_TXT_enter_password = findViewById(R.id.main_TXT_enter_password);
        main_EDT_password = findViewById(R.id.main_EDT_password);
        main_BTN_login = findViewById(R.id.main_BTN_login);
        main_BTN_grant = findViewById(R.id.main_BTN_grant);
        main_LBL_title = findViewById(R.id.main_LBL_title);
        main_LBL_content = findViewById(R.id.main_LBL_content);
        main_LBL_Progress = findViewById(R.id.location_LBL_Progress);
        main_BTN_close = findViewById(R.id.main_BTN_close);
    }
}