package com.example.miniproj;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Bluetooth.BluetoothLeService;
import Database.DatabaseHandler;
import Model.Vitals;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class MainScreen extends AppCompatActivity {
    private static final String TAG = "MainScreen";
    private HomeFragment homeFragment;
    private final List<Runnable> pendingUpdates = new ArrayList<>();



    // UI Components
    private BottomNavigationView btmNav;

    // Service
    private BluetoothLeService bluetoothLeService;
    private static final int REQUEST_PERMISSIONS = 2;
    private void initializeHomeFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        homeFragment = (HomeFragment) fragmentManager.findFragmentById(R.id.frame);

        if (homeFragment == null) {
            homeFragment = new HomeFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.frame, homeFragment)
                    .commitNow(); // Use commitNow to ensure immediate initialization
            Log.d(TAG, "HomeFragment initialized and added.");
        } else {
            Log.d(TAG, "HomeFragment already exists.");
        }
    }
    void processPendingUpdates() {
        if (homeFragment != null && homeFragment.isAdded() && homeFragment.getView() != null) {
            for (Runnable update : pendingUpdates) {
                update.run();
            }
            pendingUpdates.clear();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_screen);
        initializeHomeFragment();

        bluetoothLeService = new BluetoothLeService(this, new BluetoothLeService.DataCallback() {
            @Override
            public void onBpmReceived(int bpm) {
                Log.d(TAG, "BPM callback triggered with value: " + bpm);
                runOnUiThread(() -> {
                    if (homeFragment != null && homeFragment.isAdded() && homeFragment.getView() != null) {
                        Log.d(TAG, "Updating BPM in HomeFragment.");
                        homeFragment.updateBpm(String.valueOf(bpm));
                    } else {
                        Log.w(TAG, "HomeFragment is not added yet or null during bpm update");
                        pendingUpdates.add(() -> homeFragment.updateBpm(String.valueOf(bpm)));
                    }
                });
            }

            @Override
            public void onSpo2Received(int spo2) {
                Log.d(TAG, "Spo2 callback triggered with value: " + spo2);
                runOnUiThread(() -> {
                    if (homeFragment != null && homeFragment.isAdded() && homeFragment.getView() != null){
                        Log.d(TAG, "Updating SpO2 in HomeFragment.");
                        homeFragment.updateSpo2(String.valueOf(spo2));
                    } else {
                        Log.w(TAG, "HomeFragment is not added yet or null during spo2 update");
                        pendingUpdates.add(() -> homeFragment.updateSpo2(String.valueOf(spo2)));
                    }
                });
            }
        });


        btmNav = findViewById(R.id.navigationBar);

        // Set up window insets
        setupWindowInsets();

        // Configure status and navigation bars
        configureSystemBars();

        replaceFragment(new HomeFragment());

        btmNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                replaceFragment(new HomeFragment());
            } else if (itemId == R.id.report) {
                replaceFragment(new ReportFragment());
            } else if (itemId == R.id.profile) {
                replaceFragment(new ProfileFragment());
            }
            return true;
        });

        checkPermissions();
    }


    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void configureSystemBars() {
        setStatusBarIconColor(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setNavigationBarColor(getResources().getColor(R.color.black));

            // Add system UI flags for edge-to-edge display
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void replaceFragment(Fragment fragment) {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();

            // Check if the fragment is already the current one
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.frame);
            if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
                Log.d(TAG, "Fragment already displayed: " + fragment.getClass().getSimpleName());
                return; // Avoid replacing the same fragment
            }

            // Proceed with replacing the fragment
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.frame, fragment);
            transaction.commitNow(); // Use commitNow for immediate execution

            Log.d(TAG, "Fragment replaced with: " + fragment.getClass().getSimpleName());
        } catch (Exception e) {
            Log.e(TAG, "Error replacing fragment: " + e.getMessage());
        }
    }

    // Status bar configuration
    public void setStatusBarIconColor(boolean isLightBackground) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Window window = getWindow();
            WindowInsetsController insetsController = window.getInsetsController();
            if (insetsController != null) {
                insetsController.setSystemBarsAppearance(
                        isLightBackground ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    isLightBackground ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0
            );
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 and above, request BLE permissions using string literals
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSIONS);
            } else {
                startScanning();
            }
        } else {
            // For Android 11 and below, request both fine and coarse location permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_PERMISSIONS);
            } else {
                startScanning();
            }
        }
    }
    private void startScanning() {
        Log.d(TAG,"Scanning started");
        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        ScanFilter filter = new ScanFilter.Builder()
//                .setDeviceAddress("CC:7B:5C:F0:12:42")
                .setDeviceName("ESP32_SPO2")
                .build();

        scanner.startScan(Collections.singletonList(filter), settings, scanCallback);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "Device found: " + " - " + result.getDevice().getAddress());

            // Stop scanning once the device is found
            BluetoothLeScannerCompat.getScanner().stopScan(this);
            try {
                // Connect to the device
                bluetoothLeService.connect(result.getDevice())
                        .useAutoConnect(false)
                        .retry(3, 100)
                        .enqueue();
                Log.d(TAG, "Connecting to the device");
            }catch (Exception e) {
                Log.e(TAG, "Error connecting to device: ");
                Toast.makeText(MainScreen.this, "Error connecting to device", Toast.LENGTH_SHORT).show();
            }
        }

    };


    public void onScanFailed(int errorCode) {
        Log.e(TAG, "Scan failed with error: " + errorCode);
        Toast.makeText(MainScreen.this, "Scan failed. Error code: " + errorCode, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Log.e(TAG, "Permissions not granted.");
                Toast.makeText(this, "Permissions are required to use Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Processing pending updates...");
        processPendingUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothLeService != null) {
            bluetoothLeService.disconnect().enqueue();
        }
    }
}