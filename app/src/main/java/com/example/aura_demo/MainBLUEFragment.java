package com.example.aura_demo;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import cn.leancloud.LCUser;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import cn.leancloud.LCObject;
import cn.leancloud.LCQuery;
import cn.leancloud.LCException;
import cn.leancloud.callback.LCCallback;
//import cn.leancloud.callback.LCFindCallback;
//import cn.leancloud.callback.LCBooleanResultCallback;

public class MainBLUEFragment extends Fragment implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "MainBLUEFragment";
    private static final Logger log = LoggerFactory.getLogger(MainBLUEFragment.class);

    static class DeviceInfo {
        String id;
        String name;
        String mac;
        int rssi;
        boolean isConnected;  // 添加连接状态字段


        DeviceInfo(String id, String name, String mac, int rssi,boolean isConnected) {
            this.id = id;
            this.name = name;
            this.mac = mac;
            this.rssi = rssi;
            this.isConnected = isConnected;  // 初始化连接状态
        }

        public boolean isConnected() {
            return isConnected;
        }

        public void setConnected(boolean connected) {
            isConnected = connected;
        }
    }

    static class Adapter extends ArrayAdapter<DeviceInfo> {
        private final int myResource;

        public Adapter(@NonNull Context context, int resource, List<DeviceInfo> deviceListData) {
            super(context, resource, deviceListData);
            myResource = resource;
        }

        @SuppressLint("DefaultLocale")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            DeviceInfo deviceInfo = getItem(position);
            String name = "";
            int rssi = 0;
            if (deviceInfo != null) {
                name = deviceInfo.name;
                rssi = deviceInfo.rssi;
            }
            @SuppressLint("ViewHolder") View view = LayoutInflater.from(getContext()).inflate(myResource, parent, false);
            ImageView headImg = view.findViewById(R.id.iv_type);
            if (name == null || name.isEmpty()) {
                headImg.setImageResource(R.drawable.ble);
            } else if (
                    (name.startsWith("@") && (name.length() == 11)) ||
                            (name.startsWith("BT_") && (name.length() == 15))
            ) {
                headImg.setImageResource(R.drawable.ecble);
            } else {
                headImg.setImageResource(R.drawable.ble);
            }
            ((TextView) view.findViewById(R.id.tv_name)).setText(name);
            ((TextView) view.findViewById(R.id.tv_rssi)).setText(String.format("%d", rssi));
            ImageView rssiImg = view.findViewById(R.id.iv_rssi);
            if (rssi >= -41) rssiImg.setImageResource(R.drawable.s5);
            else if (rssi >= -55) rssiImg.setImageResource(R.drawable.s4);
            else if (rssi >= -65) rssiImg.setImageResource(R.drawable.s3);
            else if (rssi >= -75) rssiImg.setImageResource(R.drawable.s2);
            else rssiImg.setImageResource(R.drawable.s1);
            return view;
        }
    }

    private List<DeviceInfo> deviceListData = new ArrayList<>();
    private List<DeviceInfo> deviceListDataShow = new ArrayList<>();
    private Adapter listViewAdapter = null;
    private ProgressDialog connectDialog = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_blue, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        uiInit(view);
    }

    @Override
    public void onStart() {
        super.onStart();

        deviceListData.clear();
        deviceListDataShow.clear();
        if (listViewAdapter != null) {
            listViewAdapter.notifyDataSetChanged();
        }

        openBluetoothAdapter();
    }

    @Override
    public void onStop() {
        super.onStop();
        // ECBLE.stopBluetoothDevicesDiscovery(getContext());
    }

    void uiInit(View rootView) {
        Log.i(TAG, "uiInit: blue_fragment");
        // 设置 Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(rootView.findViewById(R.id.fragment_main_blue), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 设置状态栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (getActivity() != null) {
                getActivity().getWindow().setStatusBarColor(0xFF01a4ef);
            }
        }

        // 设置状态栏图标颜色
        WindowInsetsControllerCompat windowInsetsController =
                ViewCompat.getWindowInsetsController(getActivity().getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(false);
        }

        // 设置导航栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (getActivity() != null) {
                getActivity().getWindow().setNavigationBarColor(0xFFFFFFFF);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (getActivity() != null) {
                getActivity().getWindow().setNavigationBarContrastEnforced(false);
            }
        }

        // 设置 SwipeRefreshLayout
        SwipeRefreshLayout swipeRefreshLayout = rootView.findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setColorSchemeColors(0x01a4ef);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            deviceListData.clear();
            deviceListDataShow.clear();
            if (listViewAdapter != null) {
                listViewAdapter.notifyDataSetChanged();
            }
            new Handler().postDelayed(() -> {
                swipeRefreshLayout.setRefreshing(false);
                // 权限
                openBluetoothAdapter();
            }, 1000);
        });

        // 设置 ListView
        ListView listView = rootView.findViewById(R.id.list_view);
        listViewAdapter = new Adapter(requireContext(), R.layout.list_item_blue, deviceListDataShow);
        listView.setAdapter(listViewAdapter);
        listView.setOnItemClickListener((AdapterView<?> adapterView, View view1, int i, long l) -> {
            showConnectDialog();

            DeviceInfo deviceInfo = (DeviceInfo) listView.getItemAtPosition(i);

            // 模拟设备连接（假设连接成功）
            deviceInfo.setConnected(true);  // 假设成功连接设备时更新连接状态

            // 更新设备列表中的连接状态
            listViewAdapter.notifyDataSetChanged();


            ECBLE.onBLEConnectionStateChange((boolean ok, int errCode, String errMsg) -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            Thread.sleep(1500);  // 暂停3秒
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        hideConnectDialog();
                        if (ok) {
                            // ECBLE.stopBluetoothDevicesDiscovery(getContext());
                            // 启动 DeviceActivity
//                            Intent intent = new Intent(requireContext(), DeviceActivity.class);
//                            startActivity(intent);
                            send("AT_ID?\r\n");  // 向设备发送命令
                            // 更新 UI 上的连接状态
                            ECBLE.onBLECharacteristicValueChange((String str, String strHex) -> {
                                String id = str.substring(6,18); // 从索引 6 开始直到字符串末尾
                                Log.i("Extracted ID", id);

                                add_user_device(id);

                                add_device(id);

                                // 更新 UI 和导航
                                Log.i("DeviceActivity", "Context is: " + requireContext());
//                                requireActivity().runOnUiThread(() -> {
//                                    // Show Toast on the main thread
//                                    Toast.makeText(requireContext(), "add device: " + str, Toast.LENGTH_SHORT).show();
//                                    Navigation.findNavController(requireView()).navigate(R.id.action_mainBLUEFragment_to_deviceList);
//                                });
                            });

//                            Navigation.findNavController(requireView()).navigate(R.id.action_mainBLUEFragment_to_deviceList);
//                            Navigation.findNavController(requireView()).navigate(R.id.action_mainBLUEFragment_to_wifi_connect);
                            if (getActivity() != null) {
                                getActivity().overridePendingTransition(R.anim.jump_enter_anim, R.anim.jump_exit_anim);
                            }
                        } else {
                            showToast("蓝牙连接失败,errCode=" + errCode + ",errMsg=" + errMsg);
                            showAlert("提示", "蓝牙连接失败,errCode=" + errCode + ",errMsg=" + errMsg, () -> {
                            });
                        }
                    });
                }
            });
            ECBLE.createBLEConnection(requireContext(), deviceInfo.id);
        });
        listRefresh();
    }

    public void send(String data){
        Log.i(TAG, "send data: "+data);
        ECBLE.writeBLECharacteristicValue(data, false);
    }

    void listRefresh() {
        new Handler().postDelayed(() -> {
            deviceListDataShow.clear();
            for (DeviceInfo tempDevice : deviceListData) {
                deviceListDataShow.add(new DeviceInfo(tempDevice.id, tempDevice.name, tempDevice.mac, tempDevice.rssi, tempDevice.isConnected()));
            }
            if (listViewAdapter != null) {
                listViewAdapter.notifyDataSetChanged();
            }
            listRefresh();
        }, 400);
    }

    void add_user_device(String id) {
        LCUser currentUser = LCUser.getCurrentUser();
        if (currentUser != null) {
            // 获取用户的 deviceId 字段（如果没有则初始化为空列表）
            List<String> deviceIds = currentUser.getList("deviceId");
            if (deviceIds == null) {
                deviceIds = new ArrayList<>();
            }

            // 检查 deviceId 中是否已包含 id
            if (!deviceIds.contains(id)) {
                // 如果没有，添加 id
                deviceIds.add(id);

                // 更新用户对象的 deviceId 字段
                currentUser.put("deviceId", deviceIds);

                // 保存更新后的用户对象
                currentUser.saveInBackground().subscribe(
                        new Observer<LCObject>() {

                            @Override
                            public void onSubscribe(Disposable d) {
                            }

                            @Override
                            public void onNext(LCObject lcObject) {
                                Log.d(TAG, "User deviceId updated.");
                                // 确保 Toast 在主线程中调用
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getContext(), "设备 ID 已添加到用户资料中", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onError(Throwable e) {
                                // 更新失败
                                Log.e(TAG, "Failed to update user deviceId", e);
                                // 确保 Toast 在主线程中调用
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getContext(), "更新用户资料失败", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onComplete() {
                            }
                        }
                );
            } else {
                // deviceId 中已包含 id
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "该设备 ID 已存在", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }


    public static String getTimeZoneOffset() {
        // 获取设备的默认时区
        TimeZone timeZone = TimeZone.getDefault();

        // 获取时区与UTC的偏差，以毫秒为单位
        int offsetInMillis = timeZone.getOffset(System.currentTimeMillis());

        // 转换为小时和分钟
        int offsetInHours = offsetInMillis / (1000 * 60 * 60);
        int offsetInMinutes = (offsetInMillis % (1000 * 60 * 60)) / (1000 * 60);

        // 格式化为 "UTC+08:00" 或 "UTC-05:00" 的格式
        String offset = String.format("UTC%+03d:%02d", offsetInHours, offsetInMinutes);

        return offset;
    }
    void add_device(String id) {
        // 首先查询 Device 表中是否已存在该 deviceId
        LCQuery<LCObject> query = new LCQuery<>("Device");
        query.whereEqualTo("deviceId", id);

        query.findInBackground().subscribe(
                new Observer<List<LCObject>>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(List<LCObject> devices) {
                        if (devices.isEmpty()) {
                            // 如果不存在该 deviceId，创建一个新的 LCObject
                            LCObject deviceObject = new LCObject("Device");
                            String timeZoneOffset = getTimeZoneOffset();

                            // 设置新对象的属性
                            deviceObject.put("deviceId", id);
                            deviceObject.put("mode", "每日精选");
                            deviceObject.put("deviceFrequency", "1h");
                            deviceObject.put("status", "在线");
                            deviceObject.put("hengshu", "横屏");
                            deviceObject.put("timeZone", timeZoneOffset);

                            // 保存新设备到 Device 表
                            deviceObject.saveInBackground().subscribe(
                                    new Observer<LCObject>() {

                                        @Override
                                        public void onSubscribe(Disposable d) {
                                        }

                                        @Override
                                        public void onNext(LCObject lcObject) {
                                            Log.d(TAG, "New device added to Device table.");
                                            // 确保 Toast 在主线程中调用
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(getContext(), "设备已添加到设备表中", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            Log.e(TAG, "Failed to add new device", e);
                                            // 确保 Toast 在主线程中调用
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(getContext(), "添加设备失败", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onComplete() {
                                        }
                                    }
                            );
                        } else {
                            // 如果 deviceId 已存在
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), "该设备 ID 已存在", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Query failed", e);
                        // 确保 Toast 在主线程中调用
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "查询失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onComplete() {
                        requireActivity().runOnUiThread(() -> {
                            // Show Toast on the main thread
//                            Toast.makeText(requireContext(), "add device: " + str, Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(requireView()).navigate(R.id.action_mainBLUEFragment_to_deviceList);
                        });
                    }
                }
        );
    }


    void showAlert(String title, String content, Runnable callback) {
        if (getContext() != null) {
            new AlertDialog.Builder(getContext())
                    .setTitle(title)
                    .setMessage(content)
                    .setPositiveButton("OK", (dialogInterface, i) ->
                            new Thread(callback).start()
                    )
                    .setCancelable(false)
                    .create().show();
        }
    }

    void showConnectDialog() {
        if (connectDialog == null && getContext() != null) {
            connectDialog = new ProgressDialog(getContext());
            connectDialog.setMessage("连接中...");
            connectDialog.setCancelable(false);
        }
        if (connectDialog != null) {
            connectDialog.show();
        }
    }

    void hideConnectDialog() {
        if (connectDialog != null && connectDialog.isShowing()) {
            connectDialog.dismiss();
        }
    }

    void showToast(String text) {
        if (getContext() != null) {
            Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    void openBluetoothAdapter() {
        ECBLE.onBluetoothAdapterStateChange((boolean ok, int errCode, String errMsg) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!ok) {
                        showAlert("提示", "openBluetoothAdapter error,errCode=" + errCode + ",errMsg=" + errMsg, () -> {
                            if (errCode == 10001) {
                                // 蓝牙开关没有打开
                                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                            }
                            if (errCode == 10002) {
                                // 定位开关没有打开
                                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                            // 获取定位权限失败
                            if (errCode == 10003) {
                                new AppSettingsDialog.Builder(requireActivity())
                                        .setTitle("提示")
                                        .setRationale("请打开应用的定位权限")
                                        .build().show();
                            }
                            // 获取蓝牙连接附近设备的权限失败
                            if (errCode == 10004) {
                                new AppSettingsDialog.Builder(requireActivity())
                                        .setTitle("提示")
                                        .setRationale("请打开应用的蓝牙权限，允许应用使用蓝牙连接附近的设备")
                                        .build().show();
                            }
                        });
                    } else {
                        Log.e(TAG, "openBluetoothAdapter: ok");
                        startBluetoothDevicesDiscovery();
                    }
                });
            }
        });
        ECBLE.openBluetoothAdapter((AppCompatActivity) requireContext());
    }

    void startBluetoothDevicesDiscovery() {
        ECBLE.onBluetoothDeviceFound((String id, String name, String mac, int rssi) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    boolean isExist = false;
                    for (DeviceInfo tempDevice : deviceListData) {
                        if (tempDevice.id.equals(id)) {
                            tempDevice.rssi = rssi;
                            tempDevice.name = name;
                            isExist = true;
                            break;
                        }
                    }
                    if (!isExist) {
                        deviceListData.add(new DeviceInfo(id, name, mac, rssi,false));
                    }
                });
            }
        });
        ECBLE.startBluetoothDevicesDiscovery(requireContext());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        ECBLE.onPermissionsGranted((AppCompatActivity) requireContext(), requestCode, perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        ECBLE.onPermissionsDenied(requestCode, perms);
    }
}
