package com.example.lydia.myapplication;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.ScannerResults;

import java.util.ArrayList;
import java.util.List;

import static com.symbol.emdk.barcode.BarcodeManager.ConnectionState.CONNECTED;
import static com.symbol.emdk.barcode.BarcodeManager.ConnectionState.DISCONNECTED;

public class MainActivity extends AppCompatActivity implements EMDKManager.EMDKListener, BarcodeManager.ScannerConnectionListener, Scanner.DataListener{

    // Text View to display status during pairing operation
    private TextView statusView = null;
    private EditText dataView = null;

    // Declare a variable to store EMDKManager object
    private EMDKManager emdkManager = null;

    // Declare a variable to store Barcode Manager object
    private BarcodeManager barcodeManager = null;

    // Declare a variable to hold scanner device to scan
    private Scanner scanner = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Reference to UI elements
        statusView = (TextView) findViewById(R.id.textViewStatus);
        dataView = (EditText) findViewById(R.id.editText1);

        // The EMDKManager object will be created and returned in the callback.
        EMDKResults results = EMDKManager.getEMDKManager(
                getApplicationContext(), this);

        // Check the return status of getEMDKManager and update the status Text
         // View accordingly
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            statusView.setText("Status: EMDKManager object request failed!");
        }
    }


    @Override
    public void onOpened(EMDKManager emdkManager) {
        // Update status view with EMDK Open Success message
        statusView.setText("Status: " + "EMDK open success!");

        this.emdkManager = emdkManager;
        // Get the Barcode Manager Instance
        barcodeManager = (BarcodeManager) emdkManager
                .getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
        // Add the Scanner Connection Listener to receive Connected/Disconnected events
        if (barcodeManager != null) {
            barcodeManager.addConnectionListener(this);
        }

        // Initialize Scanner
        initScanner();

    }

    private void initScanner() {
        if (scanner == null) {
            // Get a list of supported scanner devices
            List<ScannerInfo> deviceList = barcodeManager
                    .getSupportedDevicesInfo();

            // Iterate through Scanner devices and check if it supports Bluetooth Scanner
            for (ScannerInfo scannerInfo : deviceList){
                if(scannerInfo.getFriendlyName().equalsIgnoreCase("Bluetooth Scanner"))
                    scanner = barcodeManager.getDevice(scannerInfo);
            }
            // If null, then your device does not support Bluetooth Scanner
            if(scanner == null) {
                statusView.setText("Bluetooth Scanner not supported!!!");
                return;
            }else{
                // Supports Bluetooth Scanner
                try {
                    // Enable the Scanner
                    scanner.enable();
                } catch (ScannerException e) {
                    statusView.setText("Status: " + e.getMessage());
                }
            }
        }
    }

    // DeInitialize Scanner
    private void deInitScanner() {
        if (scanner != null) {
            try {
                // Cancel pending reads
                scanner.cancelRead();
                // Disable Scanner
                scanner.disable();
                // Release Scanner
                scanner.release();

            } catch (ScannerException e) {
                statusView.setText("Status: " + e.getMessage());
            }

            scanner = null;
        }
    }

    @Override
    public void onConnectionChange(ScannerInfo scannerInfo, BarcodeManager.ConnectionState connectionState) {

        String status = "";
        String scannerName = "";

        // Returns the Connection State for Bluetooth Scanner through callback
        String statusBT = connectionState.name();
        // Returns the Friendly Name of the Scanner through callback
        String scannerNameBT = scannerInfo.getFriendlyName();

        // Get the friendly name of our device's Scanner
        scannerName = scanner.getScannerInfo().getFriendlyName();

        // Check for the Bluetooth Scanner
        if (scannerName.equalsIgnoreCase(scannerNameBT)) {
            // If Bluetooth Scanner, update the status view
            status = scannerNameBT + ":" + statusBT;
            new AsyncStatusUpdate().execute(status);
            // Initialize or De-Initialize Bluetooth Scanner
            // device based on Connection State
            switch (connectionState) {
                case CONNECTED:
                    // Initialize Scanner
                    initScanner();
                    break;
                case DISCONNECTED:
                    // De-Initialize Scanner
                    deInitScanner();
                    break;
            }
        }
    }

    // AsyncTask for Updating Status in statusView during pairing operation
    private class AsyncStatusUpdate extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            return params[0];
        }

        @Override
        protected void onPostExecute(String result) {
            // Update Status View
            statusView.setText("Status: " + result);
        }


    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        new AsyncDataUpdate().execute(scanDataCollection);
    }

    // Update the scan data on UI
    int dataLength = 0;

    // AsyncTask that configures the scanned data on background
// thread and updated the result on UI thread with scanned data and type of
// label
    private class AsyncDataUpdate extends
            AsyncTask<ScanDataCollection, Void, String> {

        @Override
        protected String doInBackground(ScanDataCollection... params) {

            // Status string that contains both barcode data and type of barcode
            // that is being scanned
            String statusStr = "";

            try {

                // Starts an asynchronous Scan. The method will not turn ON the
                // scanner. It will, however, put the scanner in a state in
                // which
                // the scanner can be turned ON either by pressing a hardware
                // trigger or can be turned ON automatically.
                scanner.read();

                ScanDataCollection scanDataCollection = params[0];

                // The ScanDataCollection object gives scanning result and the
                // collection of ScanData. So check the data and its status
                if (scanDataCollection != null && scanDataCollection.getResult() == ScannerResults.SUCCESS) {

                    ArrayList<ScanDataCollection.ScanData> scanData = scanDataCollection
                            .getScanData();

                    // Iterate through scanned data and prepare the statusStr
                    for (ScanDataCollection.ScanData data : scanData) {
                        // Get the scanned data
                        String barcodeData = data.getData();
                        // Get the type of label being scanned
                        ScanDataCollection.LabelType labelType = data.getLabelType();
                        // Concatenate barcode data and label type
                        statusStr = barcodeData + " " + labelType;
                    }
                }

            } catch (ScannerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return result to populate on UI thread
            return statusStr;
        }

        @Override
        protected void onPostExecute(String result) {
            // Update the dataView EditText on UI thread with barcode data and
            // its label type
            if (dataLength++ >= 50) {
                // Clear the cache after 50 scans
                dataView.getText().clear();
                dataLength = 0;
            }
            dataView.append(result + "\n");
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }


    }


    @Override
    public void onClosed() {
        if (emdkManager != null) {
            // Remove the connection listener
            if (barcodeManager != null) {
                barcodeManager.removeConnectionListener(this);
            }
            // Release EMDK Manager
            emdkManager.release();
            emdkManager = null;
        }
        statusView
                .setText("Status: EMDK closed unexpectedly! Please close and restart the application.");
    }


}
