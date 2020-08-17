package cuong.dd.ble_scanner;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
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
    Set<SimpleDeviceInfo> setDeviceInfo;
    // BLE
    BluetoothManager bleManager;
    BluetoothAdapter bleAdapter;
    BluetoothLeScanner bleScanner;
    ScanSettings settings;
    boolean isScanning = false;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static UUID SERVICE_UUID = UUID.fromString("feed0001-c497-4476-a7ed-727de7648ab1");
    private Handler mHandler = new Handler();

    private static final long SCAN_PERIOD = 15000;

    String log_tag = "BLE_CUONG";

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
        bleManager =(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = bleManager.getAdapter();

        if(bleAdapter == null || !bleAdapter.isEnabled()) {
            Log.d(log_tag, "BLE adapter is not enable!");
            return;
        }
        else {
            Log.d(log_tag, "BLE adapter init successfully!");
        }

        bleScanner = bleAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(500).build();
    }

    private void InitView()
    {
        listDeviceInfo = new ArrayList<SimpleDeviceInfo>();
        adapterDeviceInfo = new SimpleDeviceInfoAdapter(this, R.layout.devices_scaned, listDeviceInfo);
        setDeviceInfo = new LinkedHashSet<SimpleDeviceInfo>();


        lvDeviceScaned = (ListView)findViewById(R.id.listViewDevives);
        lvDeviceScaned.setAdapter(adapterDeviceInfo);
        lvDeviceScaned.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setSelected(true);
                if(isScanning){
                    stopScanning();
                }
                lvDeviceScaned.setTag(listDeviceInfo.get(i));

            }
        });

        btConnect = (Button) findViewById(R.id.buttonConnect);
        btScan = (Button) findViewById(R.id.buttonScan);
        btScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isScanning)
                {
                    isScanning = true;
                    clearListView();
                    startScanning();
                }
            }
        });
    }

    private void startScanning(){
        Log.d(log_tag, "Start scanning!");
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

    private void stopScanning()
    {
        isScanning = false;
        Log.d(log_tag, "Stop scanning!");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bleScanner.stopScan(scanCallback);
            }
        });
    }
    private void clearListView()
    {
        listDeviceInfo.clear();
        adapterDeviceInfo.notifyDataSetChanged();
    }
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
            if(!results.isEmpty()){
                setDeviceInfo.clear();
                Log.d(log_tag, setDeviceInfo.size() + "");
                for (int i = 0; i < results.size(); i++)
                {
                    ScanResult result = results.get(i);
                    SimpleDeviceInfo spDeviceInfo = new SimpleDeviceInfo(result.getDevice().getName()+ "", result.getDevice().getAddress(), "rssi: " + result.getRssi());
                    Log.d(log_tag, spDeviceInfo.getDeviceMacAddress());
                    setDeviceInfo.add(spDeviceInfo);
                }
                listDeviceInfo.clear();
                for(SimpleDeviceInfo dv: setDeviceInfo)
                {
                    listDeviceInfo.add(dv);
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