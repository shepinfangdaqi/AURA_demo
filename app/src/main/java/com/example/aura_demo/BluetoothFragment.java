package com.example.aura_demo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class BluetoothFragment extends Fragment {
    private BluetoothAdapter bluetoothAdapter;
    private final ArrayList<String> bluetoothDevices = new ArrayList<>();
    private ArrayAdapter<String> arrayAdapter;

    private static final int REQUEST_BLUETOOTH_SCAN = 1;
    private static final int REQUEST_BLUETOOTH_CONNECT = 2;

    private static final String TAG = "BLUETOOTH";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);
        Button buttonScan = view.findViewById(R.id.button_scan);
        ListView listViewDevices = view.findViewById(R.id.listview_devices);
        arrayAdapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, bluetoothDevices);
        listViewDevices.setAdapter(arrayAdapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        buttonScan.setOnClickListener(v -> checkPermissionsAndScan());

        // 设置ListView的项点击事件监听器
        listViewDevices.setOnItemClickListener((parent, view1, position, id) -> {
            String info = arrayAdapter.getItem(position);
            Log.d(TAG, "Device Info: " + info);
            if (info != null) {
                String[] parts = info.split("\n"); // 按换行符分割设备名称和地址
                if (parts.length > 1) { // 确保信息足够
                    String address = parts[1].trim(); // 提取并清理 MAC 地址
                    Log.d(TAG, "Extracted Address: " + address);
                    if (BluetoothAdapter.checkBluetoothAddress(address)) {
                        connectToDevice(address); // 尝试连接设备
                    } else {
                        Toast.makeText(getContext(), "Invalid Bluetooth address: " + address, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Device information is incomplete.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return view;
    }

    private void checkPermissionsAndScan() {
        Log.i(TAG, "checkPermissionsAndScan: ");
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(getContext(), "Bluetooth is not enabled.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "checkPermissionsAndScan: permission failed");
            ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_SCAN);
        } else {
            Log.i(TAG, "checkPermissionsAndScan: startScanning");
            startScanning();
        }
    }

    private void startScanning() {
        bluetoothDevices.clear();
        arrayAdapter.notifyDataSetChanged();
        if (ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.i(TAG, "startScanning: failed");
            return;
        }
        Log.i(TAG, "startScanning: in scanning");
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        boolean started = bluetoothAdapter.startDiscovery();
        Log.i(TAG, "startScanning: start discovery " + started);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        Log.i(TAG, "startScanning: filter " + filter);
        requireActivity().registerReceiver(bluetoothReceiver, filter);
        Log.i(TAG, "startScanning: Receiver registered");
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive: ");
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
                } else {
                    Log.i(TAG, "onReceive: add_device");
                    addDeviceToList(device, intent);
                }
            }
        }
    };

    private void addDeviceToList(BluetoothDevice device, Intent intent) {
        if (ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
            return;
        }
        String deviceName = device.getName();
        String deviceAddress = device.getAddress(); // 获取纯净的 MAC 地址
        int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

        if (deviceName != null) { // 过滤掉设备名为 null 的设备
            bluetoothDevices.add(deviceName + "\n" + deviceAddress); // 仅存储名称和地址
            arrayAdapter.notifyDataSetChanged();
            Log.d("BluetoothDevice", "Found device: Name: " + deviceName + ", Address: " + deviceAddress + ", RSSI: " + rssi);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter != null && ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.cancelDiscovery();
        }
        requireActivity().unregisterReceiver(bluetoothReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_SCAN && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else if (requestCode == REQUEST_BLUETOOTH_CONNECT && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Bluetooth connection permission granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToDevice(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            Toast.makeText(getContext(), "Invalid Bluetooth address: " + address, Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Toast.makeText(getContext(), "Device not found. Unable to connect.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 确保权限
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_SCAN);
            return;
        }

        // 取消扫描以确保不干扰连接
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        ParcelUuid[] uuids = device.getUuids();
        if (uuids != null && uuids.length > 0) {
            for (ParcelUuid uuid : uuids) {
                Log.i(TAG, "设备 UUID: " + uuid.toString());
            }
        } else {
            Log.e(TAG, "设备没有 UUID 信息");
        }
        UUID sppUUID = null;
        BluetoothSocket bluetoothSocket = null;

        for (ParcelUuid uuid : uuids) {
            sppUUID = uuid.getUuid();
            Log.i(TAG, "Trying UUID: " + sppUUID);

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(sppUUID);
                bluetoothSocket.connect(); // 尝试连接

                // 连接成功
                Toast.makeText(getContext(), getString(R.string.connected_to) + device.getName(), Toast.LENGTH_SHORT).show();

                // 成功后，进行数据交换等操作
                InputStream inputStream = bluetoothSocket.getInputStream();
                OutputStream outputStream = bluetoothSocket.getOutputStream();

                // 可以进行数据读写操作，接下来你可以进行蓝牙通信

                break; // 一旦连接成功，退出循环
            } catch (IOException e) {
                Log.e(TAG, "连接失败，UUID: " + sppUUID, e);
                // 如果连接失败，继续尝试下一个 UUID
                try {
                    if (bluetoothSocket != null) {
                        bluetoothSocket.close();
                    }
                } catch (IOException closeException) {
                    Log.e(TAG, "关闭套接字失败", closeException);
                }
            }
        }

        if (bluetoothSocket == null) {
            Toast.makeText(getContext(), R.string.all_fail_check_uuid, Toast.LENGTH_SHORT).show();
        }
    }





}
