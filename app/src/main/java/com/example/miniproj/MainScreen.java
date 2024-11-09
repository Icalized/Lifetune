package com.example.miniproj;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import Bluetooth.BluetoothLeService;

public class MainScreen extends AppCompatActivity implements BluetoothLeService.BluetoothLeScannerCallback {

    // Initialising variables
    BottomNavigationView btmNav;
    private HomeFragment homeFragment;
    BluetoothLeService bluetoothLeService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_screen);

        bluetoothLeService = new BluetoothLeService(this,this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Status bar icon color
        setStatusBarIconColor(true);

        //Navigation bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setNavigationBarColor(getResources().getColor(R.color.black));
        }

        // Bottom Navigation Bar implementation
        btmNav = findViewById(R.id.navigationBar);

        replaceFragment(new HomeFragment());

        btmNav.setOnItemSelectedListener(item -> {
            if(item.getItemId() == R.id.home){
                replaceFragment(new HomeFragment());
            } else if (item.getItemId() == R.id.report) {
                replaceFragment(new ReportFragment());
            }else{
                replaceFragment(new ProfileFragment());
            }
            return true;
        });

    }

    // Status bar dark mode
    public void setStatusBarIconColor(boolean isLightBackground) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Window window = getWindow();
            WindowInsetsController insetsController = window.getInsetsController();
            if (insetsController != null) {
                if (isLightBackground) {
                    insetsController.setSystemBarsAppearance(
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    );
                } else {
                    insetsController.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Fallback for older devices
            View decorView = getWindow().getDecorView();
            if (isLightBackground) {
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                decorView.setSystemUiVisibility(0);
            }
        }
    }

    // Function for replacing different fragments
    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame,fragment);
        fragmentTransaction.commit();
    }

    // Implementing the BluetoothLeScannerCallback interface in BluetoothLeService to update the Text in HomeFragment
    @Override
    public void onBpmDataReceived(int bpm) {
        runOnUiThread(() -> homeFragment.updateBpm(String.valueOf(bpm)));
    }

    //  // Implementing the BluetoothLeScannerCallback interface in BluetoothLeService to update the Text in HomeFragment
    @Override
    public void onSpo2DataReceived(int spo2) {
        runOnUiThread(() -> homeFragment.updateSpo2(String.valueOf(spo2)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BluetoothLeService.REQUEST_PERMISSION_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bluetoothLeService.startBluetoothLeScan();
            } else {
                Toast.makeText(this, "Bluetooth permission required to scan devices", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothLeService.stopBluetoothLeScan();
    }
}