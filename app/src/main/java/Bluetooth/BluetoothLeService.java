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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothLeService {

    private static final String TAG = "BluetoothLeService";
    public static final int REQUEST_PERMISSION_LOCATION = 1;
    private static final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 2;

    // Standard BLE UUIDs for heart rate and SpO2 services/characteristics
    private static final UUID SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID BPM_CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");
    private static final UUID SPO2_CHARACTERISTIC_UUID = UUID.fromString("6d3f18a1-5e58-4cf6-9f41-61c6de4e43e6");

    // Connection timeouts and retry parameters
    private static final long SCAN_TIMEOUT = 10000; // 10 seconds
    private static final long CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int MAX_RETRY_ATTEMPTS = 3;

    // BLE components
    private final Context context;
    private final BluetoothLeService.BluetoothLeScannerCallback callback;
    public BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private boolean isScanning = false;
    private int retryCount = 0;
    private String lastConnectedDeviceAddress;

    // Handlers for timeouts
    private final Handler scanTimeoutHandler = new Handler(Looper.getMainLooper());
    private final Handler connectionTimeoutHandler = new Handler(Looper.getMainLooper());

    // Connection state
    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }
    private BluetoothLeService.ConnectionState currentState = BluetoothLeService.ConnectionState.DISCONNECTED;

    public BluetoothLeService(Context context, BluetoothLeService.BluetoothLeScannerCallback callback) {
        this.context = context;
        this.callback = callback;
        initializeBluetooth();
    }

    public void initializeBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            notifyError("Bluetooth service not available");
            return;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            notifyError("Bluetooth not supported on this device");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            promptToEnableBluetooth();
        } else {
            checkPermissionsAndStartScan();
        }
    }

    private void promptToEnableBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Check for Android 12 or higher
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                if (context instanceof Activity) {
                    ActivityCompat.requestPermissions((Activity) context,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                            REQUEST_BLUETOOTH_CONNECT_PERMISSION);
                }
                return;
            }

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
                    == PackageManager.PERMISSION_GRANTED) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (context instanceof Activity) {
                    try {
                        ((Activity) context).startActivityForResult(enableBtIntent, 1);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "Activity to enable Bluetooth not found.", e);
                        notifyError("Error enabling Bluetooth");
                    }
                }
            } else {
                requestBluetoothAdminPermission();
            }
        }
    }

    private void requestBluetoothAdminPermission() {
        if (context instanceof Activity) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                    REQUEST_PERMISSION_LOCATION);
        }
    }

    private void checkPermissionsAndStartScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

            boolean allPermissionsGranted = true;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (!allPermissionsGranted) {
                if (context instanceof Activity) {
                    ActivityCompat.requestPermissions((Activity) context,
                            permissions,
                            REQUEST_PERMISSION_LOCATION);
                }
                return;
            }
        }

        // Check if location services are enabled
        if (!isLocationEnabled()) {
            promptEnableLocation();
            return;
        }

        startBluetoothLeScan();
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null &&
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void promptEnableLocation() {
        new AlertDialog.Builder(context)
                .setTitle("Location Required")
                .setMessage("Location services are required for BLE scanning. Please enable location services.")
                .setPositiveButton("Open Settings", (dialogInterface, i) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void startBluetoothLeScan() {
        if (isScanning) {
            return;
        }

        try {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bluetoothLeScanner == null) {
                notifyError("Bluetooth LE Scanner not available");
                return;
            }

            // Set scan filters
            List<ScanFilter> filters = new ArrayList<>();
            filters.add(new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                    .build());

            // Configure scan settings
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            bluetoothLeScanner.startScan(filters, scanSettings, scanCallback);
            isScanning = true;
            notifyStatus("Scanning for devices...");

            // Set scan timeout
            scanTimeoutHandler.postDelayed(this::stopBluetoothLeScan, SCAN_TIMEOUT);

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception while starting scan", e);
            notifyError("Permission denied: " + e.getMessage());
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result.getDevice() != null &&
                    (lastConnectedDeviceAddress == null ||
                            result.getDevice().getAddress().equals(lastConnectedDeviceAddress))) {
                stopBluetoothLeScan();
                connectToDevice(result.getDevice());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            String errorMessage = "Scan failed with error code: " + errorCode;
            Log.e(TAG, errorMessage);
            notifyError(errorMessage);
            isScanning = false;
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        if (currentState != BluetoothLeService.ConnectionState.DISCONNECTED) {
            disconnectCurrentDevice();
        }

        try {
            currentState = BluetoothLeService.ConnectionState.CONNECTING;
            notifyStatus("Connecting to device: " + device.getName());

            if (bluetoothGatt != null) {
                bluetoothGatt.close();
            }

            bluetoothGatt = device.connectGatt(context, false, gattCallback,
                    BluetoothDevice.TRANSPORT_LE);
            lastConnectedDeviceAddress = device.getAddress();

            // Set connection timeout
            connectionTimeoutHandler.postDelayed(() -> {
                if (currentState == BluetoothLeService.ConnectionState.CONNECTING) {
                    notifyError("Connection timeout");
                    retryConnection();
                }
            }, CONNECTION_TIMEOUT);

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception while connecting", e);
            notifyError("Permission denied during connection");
            currentState = BluetoothLeService.ConnectionState.DISCONNECTED;
        }
    }

    private void retryConnection() {
        if (retryCount < MAX_RETRY_ATTEMPTS) {
            retryCount++;
            notifyStatus("Retrying connection (Attempt " + retryCount + "/" + MAX_RETRY_ATTEMPTS + ")");
            startBluetoothLeScan();
        } else {
            notifyError("Failed to connect after " + MAX_RETRY_ATTEMPTS + " attempts");
            retryCount = 0;
            currentState = BluetoothLeService.ConnectionState.DISCONNECTED;
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            connectionTimeoutHandler.removeCallbacksAndMessages(null);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    currentState = BluetoothLeService.ConnectionState.CONNECTED;
                    retryCount = 0;
                    notifyStatus("Connected to device");

                    // Add security checks before discovering services
                    try {
                        // Check if GATT is still valid
                        if (gatt == null) {
                            notifyError("GATT instance is null");
                            handleDisconnection();
                            return;
                        }

                        // Check if we still have necessary permissions
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ContextCompat.checkSelfPermission(context,
                                    Manifest.permission.BLUETOOTH_CONNECT)
                                    != PackageManager.PERMISSION_GRANTED) {
                                notifyError("Bluetooth connect permission not granted");
                                handleDisconnection();
                                return;
                            }
                        }

                        // Set connection priority to high before service discovery
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                        }

                        // Start service discovery with timeout handler
                        Handler serviceDiscoveryTimeoutHandler = new Handler(Looper.getMainLooper());
                        serviceDiscoveryTimeoutHandler.postDelayed(() -> {
                            if (currentState == BluetoothLeService.ConnectionState.CONNECTED &&
                                    !isServicesDiscovered) {
                                notifyError("Service discovery timeout");
                                handleDisconnection();
                            }
                        }, 5000); // 5 second timeout for service discovery

                        // Attempt to discover services
                        boolean success = gatt.discoverServices();
                        if (!success) {
                            serviceDiscoveryTimeoutHandler.removeCallbacksAndMessages(null);
                            notifyError("Failed to start service discovery");
                            handleDisconnection();
                        }

                    } catch (SecurityException e) {
                        Log.e(TAG, "Security exception during service discovery", e);
                        notifyError("Security error during service discovery: " + e.getMessage());
                        handleDisconnection();
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected error during service discovery", e);
                        notifyError("Error during service discovery: " + e.getMessage());
                        handleDisconnection();
                    }

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    handleDisconnection();
                }
            } else {
                handleConnectionError(status);
            }
        }

        // Add a flag to track service discovery status
        private boolean isServicesDiscovered = false;

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                isServicesDiscovered = true;

                // Verify if the required services and characteristics are available
                BluetoothGattService heartService = gatt.getService(SERVICE_UUID);
                if (heartService == null) {
                    notifyError("Required heart rate service not found");
                    disconnectCurrentDevice();
                    return;
                }

                // Verify required characteristics
                BluetoothGattCharacteristic bpmChar =
                        heartService.getCharacteristic(BPM_CHARACTERISTIC_UUID);
                BluetoothGattCharacteristic spo2Char =
                        heartService.getCharacteristic(SPO2_CHARACTERISTIC_UUID);

                if (bpmChar == null || spo2Char == null) {
                    notifyError("Required characteristics not found");
                    disconnectCurrentDevice();
                    return;
                }

                // If all verifications pass, enable notifications
                enableNotifications(gatt);
            } else {
                isServicesDiscovered = false;
                notifyError("Service discovery failed with status: " + status);
                disconnectCurrentDevice();
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt,
                                            @NonNull BluetoothGattCharacteristic characteristic,
                                            @NonNull byte[] value) {
            processCharacteristicData(characteristic, value);
        }
    };

    private void enableNotifications(BluetoothGatt gatt) {
        try {
            // Enable BPM notifications
            BluetoothGattService heartService = gatt.getService(SERVICE_UUID);
            if (heartService != null) {
                BluetoothGattCharacteristic bpmChar =
                        heartService.getCharacteristic(BPM_CHARACTERISTIC_UUID);
                if (bpmChar != null) {
                    gatt.setCharacteristicNotification(bpmChar, true);
                }
            }

            // Enable SpO2 notifications
            BluetoothGattService spo2Service = gatt.getService(SERVICE_UUID);
            if (spo2Service != null) {
                BluetoothGattCharacteristic spo2Char =
                        spo2Service.getCharacteristic(SPO2_CHARACTERISTIC_UUID);
                if (spo2Char != null) {
                    gatt.setCharacteristicNotification(spo2Char, true);
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception while enabling notifications", e);
            notifyError("Permission denied while enabling notifications");
        }
    }

    private void processCharacteristicData(BluetoothGattCharacteristic characteristic, byte[] value) {
        UUID uuid = characteristic.getUuid();
        if (uuid.equals(BPM_CHARACTERISTIC_UUID)) {
            int bpm = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            callback.onBpmDataReceived(bpm);
        } else if (uuid.equals(SPO2_CHARACTERISTIC_UUID)) {
            int spo2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            callback.onSpo2DataReceived(spo2);
        }
    }

    private void handleDisconnection() {
        currentState = BluetoothLeService.ConnectionState.DISCONNECTED;
        notifyStatus("Device disconnected");
        if (retryCount < MAX_RETRY_ATTEMPTS) {
            retryConnection();
        }
    }

    private void handleConnectionError(int status) {
        String errorMessage = "Connection error: " + status;
        Log.e(TAG, errorMessage);
        notifyError(errorMessage);
        currentState = BluetoothLeService.ConnectionState.DISCONNECTED;
        retryConnection();
    }

    public void disconnectCurrentDevice() {
        if (bluetoothGatt != null) {
            try {
                currentState = BluetoothLeService.ConnectionState.DISCONNECTING;
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
                bluetoothGatt = null;
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception while disconnecting", e);
            }
        }
        currentState = BluetoothLeService.ConnectionState.DISCONNECTED;
    }

    public void stopBluetoothLeScan() {
        try {
            if (bluetoothLeScanner != null && isScanning) {
                bluetoothLeScanner.stopScan(scanCallback);
                scanTimeoutHandler.removeCallbacksAndMessages(null);
                isScanning = false;
                notifyStatus("Scan stopped");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception while stopping scan", e);
            notifyError("Permission denied while stopping scan");
        }
    }

    private void notifyStatus(String message) {
        Log.d(TAG, message);
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() ->
                    Toast.makeText(context,message, Toast.LENGTH_SHORT).show());
        }
    }

    private void notifyError(String error) {
        Log.e(TAG, error);
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() ->
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show());
        }
    }

    public interface BluetoothLeScannerCallback {
        void onBpmDataReceived(int bpm);
        void onSpo2DataReceived(int spo2);
        default void onConnectionStateChanged(BluetoothLeService.ConnectionState state) {}
        default void onError(String error) {}
    }
}
