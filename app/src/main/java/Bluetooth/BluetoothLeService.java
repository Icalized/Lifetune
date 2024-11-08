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
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.miniproj.HomeFragment;
import com.example.miniproj.MainActivity;
import com.example.miniproj.MainScreen;

import java.util.UUID;

public class BluetoothLeService {

    // Services and Characteristics name
    public static final int REQUEST_PERMISSION_LOCATION = 1;
    private static final String TAG = "BluetoothLeService";
    private static final UUID SERVICE_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private static final UUID BPM_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private static final UUID SPO2_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");

    // Declaration for the various bluetooth services
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothLeScannerCallback callback;

    private Context context;

    // Constructor to initialise the context & call initializeBluetooth
    public BluetoothLeService(Context context, BluetoothLeScannerCallback callback){
        this.context = context;
        this.callback = callback;
        initializeBluetooth();
    }

    // Initialize Bluetooth and check permissions
    public void initializeBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            promptToEnableBluetooth();
        } else {
            checkPermissionsAndStartScan();
        }
    }

    // Prompt to enable Bluetooth if it's disabled
    private void promptToEnableBluetooth() {
        Toast.makeText(context, "Bluetooth is disabled. Please enable Bluetooth.", Toast.LENGTH_LONG).show();

        // Check if Bluetooth Admin permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(context)
                    .setTitle("Bluetooth Disabled")
                    .setMessage("Bluetooth is disabled. Would you like to turn it on?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        if (context instanceof Activity) {
                            try {
                                ((Activity) context).startActivityForResult(enableBtIntent, 1);  // Request code 1 for enabling Bluetooth
                            } catch (ActivityNotFoundException e) {
                                Log.e(TAG, "Activity to enable Bluetooth not found.", e);
                                Toast.makeText(context, "Error enabling Bluetooth.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                    1);  // Request code 1 for Bluetooth Admin permission
        }
    }

    // Check permissions and start Bluetooth scanning
    private void checkPermissionsAndStartScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((MainScreen) context,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSION_LOCATION);
            } else {
                startBluetoothLeScan();
            }
        } else {
            startBluetoothLeScan();
        }
    }

    // Start Bluetooth LE scan
    public void startBluetoothLeScan() {
        try {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            bluetoothLeScanner.startScan(scanCallback);
            Toast.makeText(context, "Bluetooth LE Scanning started", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(context, "Permission Denied: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Scan callback to handle devices found
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            stopBluetoothLeScan();
            connectToDevice(result.getDevice());
        }
    };

    // Connect to the Bluetooth device
    private void connectToDevice(BluetoothDevice bluetoothDevice) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback);
        } else {
            ActivityCompat.requestPermissions((MainScreen) context,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_PERMISSION_LOCATION);
            startBluetoothLeScan();  // Restart scan in case permissions were denied
        }
    }

    // GATT callback to handle connection events
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((MainScreen) context,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_PERMISSION_LOCATION);
                return;
            }

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((MainScreen) context,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_PERMISSION_LOCATION);
                return;
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService bpmService = gatt.getService(SERVICE_UUID);
                if (bpmService != null) {
                    BluetoothGattCharacteristic bpmCharacteristic = bpmService.getCharacteristic(BPM_UUID);
                    BluetoothGattCharacteristic spo2Characteristic = bpmService.getCharacteristic(SPO2_UUID);

                    if (bpmCharacteristic != null) {
                        gatt.setCharacteristicNotification(bpmCharacteristic, true);
                    }
                    if (spo2Characteristic != null) {
                        gatt.setCharacteristicNotification(spo2Characteristic, true);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((MainScreen) context,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_PERMISSION_LOCATION);
                return;
            }

            processCharacteristicData(characteristic);
        }

        private void processCharacteristicData(BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(BPM_UUID)) {
                int bpm = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                callback.onBpmDataReceived(bpm);
            } else if (characteristic.getUuid().equals(SPO2_UUID)) {
                int spo2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                callback.onSpo2DataReceived(spo2);
            }
        }
    };

    // Stop Bluetooth LE scan
    public void stopBluetoothLeScan() {
        try {
            if (bluetoothLeScanner != null) {
                bluetoothLeScanner.stopScan(scanCallback);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Bluetooth permission denied while stopping the scan.", e);
            Toast.makeText(context, "Permission denied for stopping scan.", Toast.LENGTH_SHORT).show();
        }
    }

    // Interface for callback to pass data to other components
    public interface BluetoothLeScannerCallback {
        void onBpmDataReceived(int bpm);
        void onSpo2DataReceived(int spo2);
    }
}
