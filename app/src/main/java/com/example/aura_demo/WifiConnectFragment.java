package com.example.aura_demo;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.ArrayList;
import java.util.List;

public class WifiConnectFragment extends Fragment {

    private ListView wifiListView;
    private WifiManager wifiManager;
    private List<String> availableWifiList;
    private ArrayAdapter<String> wifiAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    public WifiConnectFragment() {
        super(R.layout.fragment_wifi_connect); // Reference the XML layout
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ListView and WifiManager
        wifiListView = view.findViewById(R.id.listView_wifi);
        wifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        // Initialize the list (ensure it's not null)
        availableWifiList = new ArrayList<>();

        // Set up the ArrayAdapter with available Wi-Fi networks
        wifiAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, availableWifiList);
        wifiListView.setAdapter(wifiAdapter);

        // Check if WiFi is enabled, if not, enable Wi-Fi
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        // Start Wi-Fi scan to get available networks
        startWifiScan();

//        send("AT_ID?\r\n");

        // Set up the SwipeRefreshLayout listener to refresh Wi-Fi list
        swipeRefreshLayout.setOnRefreshListener(this::startWifiScan);

        // Handle item click to show dialog to enter password
        wifiListView.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedWifi = availableWifiList.get(position);
            showPasswordDialog(selectedWifi);
        });
    }

    private void startWifiScan() {
//        send("AT_ID?\r\n");
        // Start Wi-Fi scan to get available networks
        boolean scanStarted = wifiManager.startScan();
        if (scanStarted) {
            // Wi-Fi scan started successfully, now get the results
            List<android.net.wifi.ScanResult> scanResults = wifiManager.getScanResults();
            availableWifiList.clear(); // Clear previous results

            for (android.net.wifi.ScanResult result : scanResults) {
                Log.i("WIFI_CONNECT", "startWifiScan: "+result.SSID);
                availableWifiList.add(result.SSID);  // Add SSID (Wi-Fi name)
            }

            // Notify the adapter that the data has changed
            wifiAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(getContext(), "Wi-Fi scan failed", Toast.LENGTH_SHORT).show();
        }

        // Stop the refreshing animation
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showPasswordDialog(String ssid) {
        // Create an EditText view to enter the password
        EditText passwordEditText = new EditText(getContext());
        passwordEditText.setHint("Enter Password");

        // Build the dialog to enter Wi-Fi password
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Connect to " + ssid)
                .setView(passwordEditText)
                .setPositiveButton("Connect", (dialog, which) -> {
                    String password = passwordEditText.getText().toString().trim();
                    if (password.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter a password", Toast.LENGTH_SHORT).show();
                    } else {
                        connectToWifi(ssid, password);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void connectToWifi(String ssid, String password) {
        // Here you should add the logic to connect to the Wi-Fi network using the provided password
        // You can use Android's WiFiManager or a third-party library to handle the connection

        // For now, we simulate a successful connection
        Toast.makeText(getContext(), "Connecting to " + ssid + " with password: " + password, Toast.LENGTH_SHORT).show();
        send("AT_WIFI_"+ssid+"_"+password+"\r\n");
        Navigation.findNavController(requireView()).navigate(R.id.action_wifi_connect_to_deviceList);

        // Example: Check if we are connected to the Wi-Fi (this is just a placeholder)
        WifiInfo currentConnection = wifiManager.getConnectionInfo();
        if (currentConnection.getSSID().equals("\"" + ssid + "\"")) {
            Toast.makeText(getContext(), "Connected to " + ssid, Toast.LENGTH_SHORT).show();
        } else {

            Toast.makeText(getContext(), "Failed to connect", Toast.LENGTH_SHORT).show();
        }
    }

    public void send(String data){
        Log.i("Wifi", "send: data"+data);
        ECBLE.writeBLECharacteristicValue(data, false);
    }

}
