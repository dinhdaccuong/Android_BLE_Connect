package cuong.dd.ble_scanner;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // View
    ListView lvDeviceScaned;
    Button btConnect;
    Button btScan;
    SimpleDeviceInfoAdapter adapterDeviceInfo;
    ArrayList<SimpleDeviceInfo> listDeviceInfo;
    ArrayList<BluetoothDevice> listDeviceDiscovered;
    Set<SimpleDeviceInfo> setDeviceInfo;
    // BLE
    BluetoothManager bleManager;
    BluetoothAdapter bleAdapter;
    BluetoothLeScanner bleScanner;
    BluetoothGatt bleGatt;
    BluetoothGattService myGattService = null;
    BluetoothGattCharacteristic myCharacteristic = null;
    BluetoothGattDescriptor     myDescriptor = null;
    ScanSettings settings;
    boolean isScanning = false;
    boolean isConnected = false;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static UUID SERVICE_UUID = UUID.fromString("feed0001-c497-4476-a7ed-727de7648ab1");
    private Handler mHandler = new Handler();

    // Server
    String myServiceUUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";

    private static final long SCAN_PERIOD = 15000;

    String log_tag = "BLE_CUONG";
    String log_tag_gatt = "GATT_CUONG";

    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitView();

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        // Init BLE
        bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = bleManager.getAdapter();

        if (bleAdapter == null || !bleAdapter.isEnabled()) {
            Log.d(log_tag, "BLE adapter is not enable!");
            return;
        } else {
            Log.d(log_tag, "BLE adapter init successfully!");
        }

        bleScanner = bleAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(500).build();
    }

    private void InitView() {
        listDeviceInfo = new ArrayList<SimpleDeviceInfo>();
        adapterDeviceInfo = new SimpleDeviceInfoAdapter(this, R.layout.devices_scaned, listDeviceInfo);
        setDeviceInfo = new LinkedHashSet<SimpleDeviceInfo>();
        listDeviceDiscovered = new ArrayList<BluetoothDevice>();

        lvDeviceScaned = (ListView) findViewById(R.id.listViewDevives);
        lvDeviceScaned.setAdapter(adapterDeviceInfo);
        lvDeviceScaned.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setSelected(true);
                if (isScanning) {
                    mHandler.removeCallbacksAndMessages(null);
                    stopScanning();
                }
                lvDeviceScaned.setTag(listDeviceInfo.get(i));
            }
        });

        btConnect = (Button) findViewById(R.id.buttonConnect);
        btConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isConnected){
                    connectToDeviceSelected();
                }
                else{
                    disconnectedDevice();
                }
            }
        });

        btScan = (Button) findViewById(R.id.buttonScan);
        btScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isScanning) {
                    if(isConnected) disconnectedDevice();
                    clearListView();
                    startScanning();
                }
                else{
                    mHandler.removeCallbacksAndMessages(null);
                    stopScanning();
                }
            }
        });
    }

    private void startScanning() {
        isScanning = true;
        Log.d(log_tag, "Start scanning!");
        btScan.setText("Stop Scanning");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bleScanner.startScan(null, settings, scanCallback);
            }
        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanning();
            }
        }, SCAN_PERIOD);
    }

    private void stopScanning() {
        isScanning = false;
        Log.d(log_tag, "Stop scanning!");
        btScan.setText("Star Scanning");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bleScanner.stopScan(scanCallback);
            }
        });
    }

    private void clearListView() {
        listDeviceInfo.clear();
        adapterDeviceInfo.notifyDataSetChanged();
    }

    private void connectToDeviceSelected(){
        Log.d(log_tag, "Trying to connect...");
        Object ob = lvDeviceScaned.getTag();
        if(ob == null)
            return;
        SimpleDeviceInfo spdvInfor = (SimpleDeviceInfo)ob;
        bleGatt = listDeviceDiscovered.get(spdvInfor.getDeviceIndex()).connectGatt(this, false, bleGattCallback);
    }

    private void disconnectedDevice(){
        Log.d(log_tag, "Trying to disconnect...");
        bleGatt.disconnect();
    }
    // Device scan callback
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            //Log.d(log_tag, result.getDevice().getName());
            Log.d(log_tag, "Search device!");
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            if (!results.isEmpty()) {
                setDeviceInfo.clear();  // Xóa set
                for (int i = 0; i < results.size(); i++) {      // Lặp lại địa chỉ mac
                    ScanResult result = results.get(i);
                    SimpleDeviceInfo spDeviceInfo = new SimpleDeviceInfo(i, result.getDevice().getName() + "", result.getDevice().getAddress(), "rssi: " + result.getRssi());
                    setDeviceInfo.add(spDeviceInfo);    // Thêm vào set
                }

                listDeviceInfo.clear();
                listDeviceDiscovered.clear();   // Xóa list cũ
                for (SimpleDeviceInfo dv : setDeviceInfo) {
                    listDeviceInfo.add(dv);
                    listDeviceDiscovered.add(results.get(dv.getDeviceIndex()).getDevice()); // indexDevice trong device infor cũn là chỉ số trong results
                }
                adapterDeviceInfo.notifyDataSetChanged();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(log_tag, "Scan failed");
        }
    };

    private final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(log_tag, "onConnectionStateChange");
            switch (newState){
                case 0:         // device disconnected
                    isConnected = false;
                    btConnect.setText("Connect");
                    Log.d(log_tag, "onConnectionStateChange: Device disconnected");
                    break;
                case 2:         // device connected
                    isConnected = true;
                    btConnect.setText("Disconnect");
                    Log.d(log_tag, "onConnectionStateChange: Device connected");
                    bleGatt.discoverServices();
                    break;
                default:        // another state
                    Log.d(log_tag, "onConnectionStateChange: Another state");
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            List<BluetoothGattService> listBLEGattService = bleGatt.getServices();
            if(listBLEGattService == null){
                Log.d(log_tag, "Could not find any services!");
                return;
            }
            Log.d(log_tag, "onServicesDiscovered");
            for(BluetoothGattService bleGatt: listBLEGattService){
                String sUUID = bleGatt.getUuid().toString();
                if(sUUID.equals(myServiceUUID)){
                    myGattService = bleGatt;
                    break;
                }
            }
            if(myGattService == null)
            {
                Log.d(log_tag, "Cound not find the Your GatService!");
                return;
            }
            Log.d(log_tag, "Found Your GattService!");
            List<BluetoothGattCharacteristic> listCharacteristic = myGattService.getCharacteristics();
            if(!listCharacteristic.isEmpty()){
                myCharacteristic = listCharacteristic.get(0);
                Log.d(log_tag, "My characteristic: " + myCharacteristic.getUuid().toString());
            }

            if(myCharacteristic == null)
                return;

            List<BluetoothGattDescriptor> listDescriptor = myCharacteristic.getDescriptors();
            if(!listDescriptor.isEmpty()){
                myDescriptor = listDescriptor.get(0);
                Log.d(log_tag, "My Descriptor: " + myDescriptor.getUuid().toString());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(log_tag, "onCharacteristicRead");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(log_tag, "onCharacteristicWrite");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(log_tag, "onCharacteristicChanged");
        }
    };

    @Override
    public void onStart() {
        super.onStart();

        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://cuong.dd.ble_scanner/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://cuong.dd.ble_scanner/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}