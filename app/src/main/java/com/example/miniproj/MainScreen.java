package com.example.miniproj;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import Bluetooth.BluetoothLeService;

public class MainScreen extends AppCompatActivity implements BluetoothLeService.BluetoothLeScannerCallback {
    private static final String TAG = "MainScreen";

    // UI Components
    private BottomNavigationView btmNav;
    private ProgressBar connectionProgress;
    private TextView connectionStatus;

    // Fragments
    private HomeFragment homeFragment;
    private ReportFragment reportFragment;
    private ProfileFragment profileFragment;

    // Service
    private BluetoothLeService bluetoothLeService;
    private boolean isServiceBound = false;

    // State
    private static final String KEY_CURRENT_FRAGMENT = "current_fragment";
    private static final String KEY_BPM_VALUE = "bpm_value";
    private static final String KEY_SPO2_VALUE = "spo2_value";
    private String currentFragmentTag = "HOME";
    private String lastBpmValue = "0";
    private String lastSpo2Value = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_screen);

        // Initialize UI components
        initializeViews();

        // Restore saved state if available
        if (savedInstanceState != null) {
            restoreSavedState(savedInstanceState);
        } else {
            initializeFragments();
        }

        // Initialize Bluetooth service
        initializeBluetoothService();

        // Set up window insets
        setupWindowInsets();

        // Configure status and navigation bars
        configureSystemBars();

        // Set up bottom navigation
        setupBottomNavigation();
    }

    private void initializeViews() {
        btmNav = findViewById(R.id.navigationBar);
        connectionProgress = findViewById(R.id.connectionProgress);
        connectionStatus = findViewById(R.id.connectionStatus);

        // Set initial visibility
        connectionProgress.setVisibility(View.GONE);
        connectionStatus.setVisibility(View.GONE);
    }

    private void initializeFragments() {
        homeFragment = new HomeFragment();
        reportFragment = new ReportFragment();
        profileFragment = new ProfileFragment();

        // Load initial fragment
        replaceFragment(homeFragment, "HOME");
    }

    private void initializeBluetoothService() {
        bluetoothLeService = new BluetoothLeService(this, this);
        showLoading("Initializing Bluetooth...");
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

    private void setupBottomNavigation() {
        btmNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                return switchFragment(homeFragment, "HOME");
            } else if (itemId == R.id.report) {
                return switchFragment(reportFragment, "REPORT");
            } else if (itemId == R.id.profile) {
                return switchFragment(profileFragment, "PROFILE");
            }
            return false;
        });

        // Disable bottom navigation during loading states
        btmNav.setEnabled(!isServiceBound);
    }

    private boolean switchFragment(Fragment fragment, String tag) {
        if (!tag.equals(currentFragmentTag)) {
            replaceFragment(fragment, tag);
            currentFragmentTag = tag;
            return true;
        }
        return false;
    }

    private void replaceFragment(Fragment fragment, String tag) {
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.frame, fragment);
            transaction.commit();

        } catch (Exception e) {
            Log.e(TAG, "Error replacing fragment: " + e.getMessage());
            showError("Error switching screens");
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

    // Bluetooth callback implementations
    @Override
    public void onBpmDataReceived(int bpm) {
        lastBpmValue = String.valueOf(bpm);
        runOnUiThread(() -> {
            hideLoading();
            if (homeFragment != null && homeFragment.isVisible()) {
                homeFragment.updateBpm(lastBpmValue);
            }
        });
    }

    @Override
    public void onSpo2DataReceived(int spo2) {
        lastSpo2Value = String.valueOf(spo2);
        runOnUiThread(() -> {
            hideLoading();
            if (homeFragment != null && homeFragment.isVisible()) {
                homeFragment.updateSpo2(lastSpo2Value);
            }
        });
    }

    @Override
    public void onConnectionStateChanged(BluetoothLeService.ConnectionState state) {
        runOnUiThread(() -> {
            switch (state) {
                case CONNECTED:
                    hideLoading();
                    showStatus("Connected");
                    btmNav.setEnabled(true);
                    break;
                case CONNECTING:
                    showLoading("Connecting...");
                    btmNav.setEnabled(false);
                    break;
                case DISCONNECTED:
                    hideLoading();
                    showStatus("Disconnected");
                    btmNav.setEnabled(true);
                    break;
                case DISCONNECTING:
                    showLoading("Disconnecting...");
                    btmNav.setEnabled(false);
                    break;
            }
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            hideLoading();
            showError(error);
            btmNav.setEnabled(true);
        });
    }

    // UI State management
    private void showLoading(String message) {
        connectionProgress.setVisibility(View.VISIBLE);
        connectionStatus.setVisibility(View.VISIBLE);
        connectionStatus.setText(message);
    }

    private void hideLoading() {
        connectionProgress.setVisibility(View.GONE);
        connectionStatus.setVisibility(View.GONE);
    }

    private void showStatus(String message) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_SHORT).show();
    }

    private void showError(String error) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.main), error, Snackbar.LENGTH_LONG);
        snackbar.setActionTextColor(getResources().getColor(R.color.black));
        snackbar.setAction("Retry", v -> {
            if (bluetoothLeService != null) {
                bluetoothLeService.initializeBluetooth();
            }
        });
        snackbar.show();
    }

    // Permission handling
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BluetoothLeService.REQUEST_PERMISSION_LOCATION) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                bluetoothLeService.startBluetoothLeScan();
            } else {
                showPermissionExplanationDialog();
            }
        }
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app requires Bluetooth and Location permissions to connect to your device. " +
                        "Please grant these permissions to continue.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // State saving
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_FRAGMENT, currentFragmentTag);
        outState.putString(KEY_BPM_VALUE, lastBpmValue);
        outState.putString(KEY_SPO2_VALUE, lastSpo2Value);
    }

    private void restoreSavedState(Bundle savedState) {
        currentFragmentTag = savedState.getString(KEY_CURRENT_FRAGMENT, "HOME");
        lastBpmValue = savedState.getString(KEY_BPM_VALUE, "0");
        lastSpo2Value = savedState.getString(KEY_SPO2_VALUE, "0");

        // Restore appropriate fragment
        switch (currentFragmentTag) {
            case "HOME":
                homeFragment = new HomeFragment();
                replaceFragment(homeFragment, currentFragmentTag);
                break;
            case "REPORT":
                reportFragment = new ReportFragment();
                replaceFragment(reportFragment, currentFragmentTag);
                break;
            case "PROFILE":
                profileFragment = new ProfileFragment();
                replaceFragment(profileFragment, currentFragmentTag);
                break;
        }
    }

    // Lifecycle methods
    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothLeService != null) {
            bluetoothLeService.initializeBluetooth();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bluetoothLeService != null) {
            bluetoothLeService.stopBluetoothLeScan();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothLeService != null) {
            bluetoothLeService.disconnectCurrentDevice();
            bluetoothLeService.stopBluetoothLeScan();
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Exit App")
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("Yes", (dialog, which) -> super.onBackPressed())
                    .setNegativeButton("No", null)
                    .show();
        }
    }
}