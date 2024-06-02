package com.example.ex1_permissions;

import android.Manifest;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;



public class MainActivity extends AppCompatActivity {
    private MaterialTextView main_TXT_enter_password;
    private AppCompatEditText main_EDT_password;
    private MaterialButton main_BTN_login;
    private boolean isReadContactsGranted;
    private ActivityResultLauncher<String> readContactsPermissionLauncher =
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
    private ActivityResultLauncher<Intent> readContactsManualPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            readContactsPermission();
                        }
                    }
            );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        initViews();
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            checkStatus();
    }

    private void checkStatus() {
        String permissionStatus = checkPermissionsStatus(this);
        if (!isLocationEnabled(this))
            state = STATE.LOCATION_DISABLED;
        else if (permissionStatus != null)
            if (permissionStatus.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
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
                location_LBL_title.setText("NA");
                location_LBL_content.setText("NA");
                location_LBL_Progress.setText("0/0");
                location_BTN_close.setVisibility(View.INVISIBLE);
                location_BTN_grant.setVisibility(View.INVISIBLE);
                break;
            case NO_REGULAR_PERMISSION:
                location_LBL_title.setText("Location Permission");
                location_LBL_content.setText("Location permission is needed for core functionality.\nPlease Enable the app permission to access your location data");
                location_LBL_Progress.setText("2/4");
                location_BTN_close.setVisibility(View.VISIBLE);
                location_BTN_grant.setVisibility(View.VISIBLE);
                location_BTN_grant.setText("Grant Permission");
                location_BTN_grant.setOnClickListener(v -> askForLocationPermissions(checkPermissionsStatus(this)));
                break;
            case NO_BACKGROUND_PERMISSION:
                location_LBL_title.setText("Background location permission");
                location_LBL_content.setText("This app collects location data even when the app is closed or not in use.\nTo protect your privacy, the app stores only calculated indicators, like distance from home and never exact location.\nA notification is always displayed in the notifications bar when service is running.");
                location_LBL_Progress.setText("3/4");
                location_BTN_close.setVisibility(View.VISIBLE);
                location_BTN_grant.setVisibility(View.VISIBLE);
                location_BTN_grant.setText("Grant Permission");
                location_BTN_grant.setOnClickListener(v ->
                        askForLocationPermissions(checkPermissionsStatus(this)));
                break;
            case LOCATION_DISABLED:
                location_LBL_title.setText("Enable Location Services");
                location_LBL_content.setText("The app samples your location.\nPlease enable location services (GPS).");
                location_LBL_Progress.setText("1/4");
                location_BTN_close.setVisibility(View.VISIBLE);
                location_BTN_grant.setVisibility(View.VISIBLE);
                location_BTN_grant.setText("Turn On Location");
                location_BTN_grant.setOnClickListener(v -> {
                    enableLocationServiceProgramatically();
                });
                break;
            case LOCATION_SETTING_PROCESS:
                location_LBL_title.setText("LOCATION_SETTINGS_PROCCESS");
                location_LBL_content.setText("LOCATION_SETTINGS_PROCCESS");
                location_LBL_Progress.setText("4/4");
                location_BTN_close.setVisibility(View.INVISIBLE);
                location_BTN_grant.setVisibility(View.INVISIBLE);
                break;
            case LOCATION_SETTINGS_OK:
                location_LBL_title.setText("All Good! 👍🏻");
                location_LBL_content.setText("Location services are running and all permissions have been granted.\nYou can now start recording.");
                location_LBL_Progress.setText("4/4");
                location_BTN_close.setVisibility(View.INVISIBLE);
                location_BTN_grant.setVisibility(View.INVISIBLE);
                break;
        }
    }





    private void readContactsPermission() {
        boolean isGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        if(isGranted){
            isReadContactsGranted = true;
        }else{
            boolean showDialog = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS);
            if(showDialog){
                openReadContactsPermissionDialog();
            }else{
                readContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
            }
        }
    }





    private String checkPermissionsStatus(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return Manifest.permission.ACCESS_FINE_LOCATION;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return Manifest.permission.ACCESS_COARSE_LOCATION;
        if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return Manifest.permission.ACCESS_BACKGROUND_LOCATION;
        return null;
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
        readContactsPermission();

        int batteryLevel = getBatteryLevel();
        String password = main_EDT_password.getText().toString();

        if(this.isReadContactsGranted && String.valueOf(batteryLevel).equals(password)){
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(intent);
        }
    }


    private void initViews() {
        main_BTN_login.setOnClickListener(v -> loginClicked());
    }


    private void findViews() {
        main_TXT_enter_password = findViewById(R.id.main_TXT_enter_password);
        main_EDT_password = findViewById(R.id.main_EDT_password);
        main_BTN_login = findViewById(R.id.main_BTN_login);
    }
}