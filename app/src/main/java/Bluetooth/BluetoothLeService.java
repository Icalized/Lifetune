package Bluetooth;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.miniproj.HomeFragment;
import com.example.miniproj.MainActivity;
import com.example.miniproj.MainScreen;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import Database.DatabaseHandler;
import Model.Vitals;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.data.Data;

public class BluetoothLeService extends BleManager {

    // Standard BLE UUIDs for heart rate and SpO2 services/characteristics
    private static final UUID SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID BPM_CHARACTERISTIC_UUID = UUID.fromString("6d3f18a1-5e58-4cf6-9f41-61c6de4e43e6");
    private static final UUID SPO2_CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");
    //    private static final UUID DESCRIPTOR_CHARACTERISTIC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final String TAG = "BluetoothLeService";
    private BluetoothGattCharacteristic bpmCharacteristic;
    private BluetoothGattCharacteristic spo2Characteristic;

    // Database
    private DatabaseHandler db;
    private String lastBpm = null;
    private String lastSpo2 = null;
    private boolean updatedBpm = false;
    private boolean updatedSpo2 = false;

    private void storeInDatabase(String bpm,String spo2){
        long time = parseTime();
        if(updatedBpm && updatedSpo2 && lastBpm != null && lastSpo2 != null && time != -1){
            db.addData(new Vitals(bpm,spo2,time));
            updatedBpm = false;
            updatedSpo2 = false;
            Log.d(TAG, "Data added to database");
        }
    }

    private long parseTime(){
        // Get current date and time
        LocalDateTime now = LocalDateTime.now();

        // Format as yyyyMMddHHmmss
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        String formattedDateTime = now.format(formatter);
        Log.d(TAG, "Formatted DateTime: " + formattedDateTime);

        try {
            long dateTimeInt = Long.parseLong(formattedDateTime); // Use long for large numbers
            Log.d(TAG,"date converted");
            return dateTimeInt;
        } catch (NumberFormatException e) {
            Log.d(TAG,"Error converting date/time to integer: " + e.getMessage());
        }
        return -1;
    }

    public interface DataCallback {
        void onBpmReceived(int bpm);
        void onSpo2Received(int spo2);
    }

    private  DataCallback dataCallback = null;

    // Make the constructor public for external access
    public BluetoothLeService(Context context, DataCallback dataCallback) {
        super(context);
        this.dataCallback = dataCallback;
        db = new DatabaseHandler(context);
        Log.d(TAG,"BluetoothManager created");
    }

    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new BleManagerGattCallback() {

            @Override
            protected boolean isRequiredServiceSupported(BluetoothGatt gatt) {
                Log.d(TAG, "Checking if required services are supported");
                // Initialize characteristics using BluetoothGattCharacteristic
                bpmCharacteristic = gatt.getService(SERVICE_UUID)
                        .getCharacteristic(BPM_CHARACTERISTIC_UUID);
                spo2Characteristic = gatt.getService(SERVICE_UUID)
                        .getCharacteristic(SPO2_CHARACTERISTIC_UUID);

                // Ensure the characteristics were found
                boolean isSupported = bpmCharacteristic != null && spo2Characteristic != null;
                Log.d(TAG, "Services supported: " + isSupported);
                return isSupported;
            }

            @Override
            protected void initialize() {
                Log.d(TAG, "Initializing characteristics and setting up notifications");

                // Set up notifications for BPM
                if (bpmCharacteristic != null) {
                    setNotificationCallback(bpmCharacteristic).with(bpmCallback);
                    enableNotifications(bpmCharacteristic).enqueue();
                    Log.d(TAG, "BPM notifications enabled");
                } else {
                    Log.w(TAG, "BPM characteristic not found");
                }

                // Set up notifications for SpO₂
                if (spo2Characteristic != null) {
                    setNotificationCallback(spo2Characteristic).with(spo2Callback);
                    enableNotifications(spo2Characteristic).enqueue();
                    Log.d(TAG, "SpO₂ notifications enabled");
                } else {
                    Log.w(TAG, "SpO₂ characteristic not found");
                }
            }

            @Override
            protected void onDeviceDisconnected() {
                Log.d("BluetoothManager", "Device disconnected");
            }

            @Override
            protected void onServicesInvalidated() {
                // Clear the characteristics when services are invalidated
                bpmCharacteristic = null;
                spo2Characteristic = null;
            }
        };
    }


    // Callback for BPM characteristic
    private final DataReceivedCallback bpmCallback = (device, data) -> {
        int bpm = data.getIntValue(Data.FORMAT_UINT8, 0);
        Log.d(TAG, "Received BPM data: " + bpm);
        if (dataCallback != null) {
            dataCallback.onBpmReceived(bpm);
            lastBpm = String.valueOf(bpm);
            updatedBpm = true;
            storeInDatabase(lastBpm,lastSpo2);
        }
    };

    // Callback for SpO₂ characteristic
    private final DataReceivedCallback spo2Callback = (device, data) -> {
        int spo2 = data.getIntValue(Data.FORMAT_UINT8, 0);
        Log.d(TAG, "Received SpO₂ data: " + spo2);
        if (dataCallback != null) {
            dataCallback.onSpo2Received(spo2);
            lastSpo2 = String.valueOf(spo2);
            updatedSpo2 = true;
            storeInDatabase(lastBpm,lastSpo2);
        }
    };
}
