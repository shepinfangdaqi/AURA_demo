package com.example.aura_demo;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.navigation.Navigation;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import cn.leancloud.LCCloud;
import cn.leancloud.LCException;
import cn.leancloud.LCFile;
import cn.leancloud.LCObject;
import cn.leancloud.LCQuery;
import cn.leancloud.LCUser;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.JSONObject;

public class DeviceListFragment extends Fragment {

    private static final String TAG = "DeviceListFragment";

    private DeviceAdapter deviceAdapter;
    private List<Device> deviceList;

    /** 当前正在查询 WiFi 状态的设备 BLE 名称 */
    private String currentStaDevice;

    /** WiFi 连接进度对话框 */
    private AlertDialog wifiProgressDialog;

    private boolean bleListenerRegistered = false;

    /** 固件更新进度对话框 */
    private AlertDialog otaDialog;
    /** 固件更新水平进度条 */
    private ProgressBar otaProgressBar;

    private String mode_txt;


    public DeviceListFragment() {
        // Required empty public constructor
    }

    public static DeviceListFragment newInstance() {
        return new DeviceListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Enable the options menu
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_device_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        RecyclerView recyclerViewDevices = view.findViewById(R.id.recyclerViewDevices);

        // 2. 注册 BLE 数据接收监听
        ECBLE.onBLECharacteristicValueChange((String str, String strHex) -> {
            Log.d(TAG, "收到 BLE 数据: " + str);
            // 只处理 AT_STA_ 开头的 WiFi 状态回复
            if (str.startsWith("AT_STA_")) {
                boolean wifiOk = str.startsWith("AT_STA_1");
                // 在主线程更新 UI
                Log.i(TAG, "onViewCreated: "+currentStaDevice);
//                requireActivity().runOnUiThread(() -> updateWifiStatus(currentStaDevice, wifiOk));
                requireActivity().runOnUiThread(() -> {
                    // 收到回复后，先关闭进度对话框
                    if (wifiProgressDialog != null && wifiProgressDialog.isShowing()) {
                        wifiProgressDialog.dismiss();
                    }
                    // 更新 WiFi 状态并提示
                    updateWifiStatus(currentStaDevice, wifiOk);
                    Toast.makeText(getContext(), wifiOk ? getString(R.string.wifi_ok) : getString(R.string.wifi_NG), Toast.LENGTH_SHORT).show();
                });
            }



            if (str.startsWith("OTA_NO_NEED")) {
                requireActivity().runOnUiThread(() -> {
                    if (otaDialog != null && otaDialog.isShowing()) {
                        otaDialog.dismiss();
                    }
                    Toast.makeText(getContext(), getString(R.string.ota_no_need), Toast.LENGTH_SHORT).show();
                });
            }
            else if (str.startsWith("OTA_ING:")) {
                // 取出数字并 trim 再 parse
                String percentStr = str
                        .substring("OTA_ING:".length(), str.indexOf('%'))
                        .trim();
                int percent = Integer.parseInt(percentStr);
                requireActivity().runOnUiThread(() -> {
                    if (otaProgressBar != null) {
                        otaProgressBar.setProgress(percent);
                        if (percent == 100) {
                            otaDialog.setTitle(getString(R.string.checking_wait_2mins));
                        }
                    }
                });
            }
            else if (str.startsWith("OTA_OK")) {
                requireActivity().runOnUiThread(() -> {
                    if (otaDialog != null && otaDialog.isShowing()) {
                        otaDialog.dismiss();
                    }
                    Toast.makeText(getContext(), getString(R.string.ota_ok_reset), Toast.LENGTH_LONG).show();
                });
            }
            else if (str.startsWith("OTA_NG")) {
                requireActivity().runOnUiThread(() -> {
                    if (otaDialog != null && otaDialog.isShowing()) {
                        otaDialog.dismiss();
                    }
                    Toast.makeText(getContext(), getString(R.string.ota_fail_retry), Toast.LENGTH_LONG).show();
                });
            }

            if (str.trim().equals("AT_UPDATE")) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                                    getString(R.string.updateing_image),
                                    Toast.LENGTH_SHORT)    // ≈2 秒
                            .show();
                });
            }



        });

        // Set up the RecyclerView
        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(getContext()));
        deviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(getContext(), deviceList, new DeviceAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Device device) {
                // Handle device item click
                Toast.makeText(getContext(), "Clicked device: " + device.getMode(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMenuItemClick(Device device, int menuItemId) throws FileNotFoundException {
                Log.i(TAG, "onMenuItemClick: itemId=" + menuItemId + ", deviceId=" + device.getDeviceId());
                // 日历模式
                if (menuItemId == R.id.action_calendar_mode) {
                    changeMode(device.getDeviceId(), getString(R.string.data_mode));
                }
                // 美图模式
                else if (menuItemId == R.id.action_beautiful_image_mode) {
                    changeMode(device.getDeviceId(), getString(R.string.image_mode));
                }
                // 自选图片
                else if (menuItemId == R.id.action_custom_image) {
                    navigateToUploadFragment(device, getString(R.string.diy_mode));
                    changeMode(device.getDeviceId(), getString(R.string.diy_mode));
                }
                // 设置 WiFi
                else if (menuItemId == R.id.action_wifi) {
                    if (device.isConnected()) {
                        showWifiDialog();
                    } else {
                        Toast.makeText(getContext(), getString(R.string.connect_ble_first), Toast.LENGTH_SHORT).show();
                    }
                }

                else if (menuItemId == R.id.action_update) {
                    if (device.isConnected()) {
                        send("AT_UPDATE\r\n");           // 发送更新状态指令
                    } else {
                        Toast.makeText(getContext(), getString(R.string.connect_ble_first), Toast.LENGTH_SHORT).show();
                    }
                }


                // 固件更新
                else if (menuItemId == R.id.action_ota) {
                    if (device.isConnected()) {
                        // 1) 打开一个带 ProgressBar 的对话框
                        showOtaDialog();
                        // 2) 通过 BluetoothDriver 发 AT_OTA\r\n
                        send("AT_OTA\r\n");
                    } else {
                        Toast.makeText(getContext(), getString(R.string.connect_ble_first), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        recyclerViewDevices.setAdapter(deviceAdapter);

        // Fetch the device list from LeanCloud
        fetchDeviceList();

        Log.i(TAG, "fetchDeviceList444: size"+deviceList.size());

    }

    private void showWifiDialog() {
        // 1. 准备对话框布局
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.fragment_wifi_connect, null);
        TextInputEditText etSsid     = dialogView.findViewById(R.id.et_ssid);
        TextInputEditText etPassword = dialogView.findViewById(R.id.et_password);

        // 2. 构建并显示对话框
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.input_wifi)
                .setView(dialogView)
                .setPositiveButton(R.string.my_input, (dialog, which) -> {
                    // 3. 获取并校验输入
                    String ssid = etSsid.getText().toString().trim();
                    String pwd  = etPassword.getText().toString().trim();
                    boolean valid = true;
                    if (ssid.isEmpty()) {
                        etSsid.setError(getString(R.string.input_wifi_ssid));
                        valid = false;
                    }
                    if (pwd.isEmpty()) {
                        etPassword.setError(getString(R.string.input_wifi_passwd));
                        valid = false;
                    }
                    if (!valid) return;

                    sendWifiConfig(ssid,pwd);


                })
                .setNegativeButton(R.string.my_undo, null)
                .show();
    }


    private void sendWifiConfig(String ssid, String password) {
        try {
            // 1. 构建 JSON 对象
            JSONObject payload = new JSONObject();
            payload.put("ssid", ssid);
            payload.put("pass", password);

            // 2. 拼接成 AT 指令
            String command = "AT_WIFI_JSON" + payload.toString() + "\r\n";

            // 3. 通过你的 BLE 库发送
            // 假设 ECBLE 有一个 sendData(byte[]) 方法
            send(command);

            Log.d(TAG, "已发送 WiFi 配置指令: " + command);
            // 显示进度对话框
            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireContext());
            builder.setTitle(R.string.my_connect_wifi);
            builder.setView(new ProgressBar(requireContext()));
            builder.setCancelable(false);
            wifiProgressDialog = builder.create();
            wifiProgressDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "构建或发送 WiFi JSON 指令失败", e);
            Toast.makeText(getContext(),
                    getString(R.string.send_config_fail) + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showOtaDialog() {
        // 创建一个 horizontal ProgressBar
        ProgressBar progressBar = new ProgressBar(
                requireContext(), null,
                android.R.attr.progressBarStyleHorizontal
        );
        progressBar.setMax(100);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(getString(R.string.device_ota))
                .setView(progressBar)
                .setCancelable(false);

        otaDialog = builder.create();
        otaDialog.show();
        otaProgressBar = progressBar;
    }

    private void registerBleListenerIfNeeded() {
        if (bleListenerRegistered) return;
        bleListenerRegistered = true;

        getParentFragmentManager().setFragmentResultListener(
                "ble_connected", this, (requestKey, bundle) -> {
                    String name = bundle.getString("ble_connected_name");
                    boolean ok   = bundle.getBoolean("is_connected", false);
                    currentStaDevice = name;
                    Log.d(TAG, "BLE事件到达 → " + name + " isConnected=" + ok);
                    updateBleStatus(name, ok);
                }
        );
    }

    private void updateBleStatus(String fullName, boolean isConnected) {
        Log.i(TAG, "updateBleStatus:start: "+deviceList.size());
        for (int i = 0; i < deviceList.size(); i++) {
            Device d = deviceList.get(i);
            Log.i(TAG, d.getBleName());
            if (fullName.equals(d.getBleName())) {
                d.setConnected(isConnected);
                deviceAdapter.notifyItemChanged(i);
                // 如果 BLE 已连接，则发起 WiFi 状态查询
                if (isConnected) {
                    sendStaQuery();
                }
                break;
            }
        }
    }

    private void sendStaQuery() {
        String cmd = "AT_STA? \r\n";
        send(cmd);
        Log.d(TAG, "已发送 WiFi 状态查询指令: " + cmd);
    }

    /** 根据 WiFi 查询回复更新列表中该设备的 WiFi 状态 **/
    /**
     * 根据 WiFi 查询回复更新列表中该设备的 WiFi 状态
     */
    private void updateWifiStatus(String bleName, boolean wifiConnected) {
        if (bleName == null) {
            Log.w(TAG, "updateWifiStatus: bleName is null, skip update");
            return;
        }
        for (int i = 0; i < deviceList.size(); i++) {
            Device d = deviceList.get(i);
            String targetName = d.getBleName();
            if (targetName != null && targetName.equals(bleName)) {
                d.setWifiok(wifiConnected);
                deviceAdapter.notifyItemChanged(i);
                Log.d(TAG, "updateWifiStatus: " + bleName + " -> wifiConnected=" + wifiConnected);
                break;
            }
        }
    }


    public void changeMode(String id,String mode){
        // 创建查询对象，查找 Device 表中的 deviceId 字段
        LCQuery<LCObject> query = new LCQuery<>("Device");
        query.whereEqualTo("deviceId", id); // 使用 device.getDeviceId() 获取设备 ID


        if(Objects.equals(mode, getString(R.string.diy_mode)))
        {
            mode_txt = "自选图片";
        }
        else if(Objects.equals(mode, getString(R.string.image_mode)))
        {
            mode_txt = "美图模式";
        }
        else{
            mode_txt = "日历模式";
        }


        query.findInBackground().subscribe(new Observer<List<LCObject>>() {
            @Override
            public void onSubscribe(Disposable disposable) {
                // 订阅开始时的操作
            }

            @Override
            public void onNext(List<LCObject> devices) {
                // 查找到满足条件的设备对象
                if (!devices.isEmpty()) {
                    // 假设查询返回的是一个设备列表，取第一个设备对象
                    LCObject deviceObject = devices.get(0);

                    // 修改 mode 字段
                    deviceObject.put("mode", mode_txt);  // 将 "新的模式" 替换为你想设置的值

                    // 保存修改
                    deviceObject.saveInBackground().subscribe(new Observer<LCObject>() {
                        @Override
                        public void onSubscribe(Disposable disposable) {
                            // 处理保存时的操作
                        }

                        @Override
                        public void onNext(LCObject lcObject) {
                            // 成功保存后的操作
                            Log.d("TAG", "Device mode updated successfully");
//                            fetchDeviceList();
                            // 2. 本地更新这一条数据，并刷新该行
                            for (int i = 0; i < deviceList.size(); i++) {
                                Device d = deviceList.get(i);
                                if (d.getDeviceId().equals(id)) {
                                    d.setMode(mode);
                                    deviceAdapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            // 保存失败时的操作
                            Log.e("TAG", "Failed to update device mode", throwable);
                        }

                        @Override
                        public void onComplete() {
                            // 操作完成时的处理
                        }
                    });
                } else {
                    // 如果没有找到符合条件的设备
                    Log.w("TAG", "No device found with the specified deviceId");
                }
            }

            @Override
            public void onError(Throwable throwable) {
                // 查询失败时的处理
                Log.e("TAG", "Failed to query devices", throwable);
            }

            @Override
            public void onComplete() {
                // 查询完成后的操作
            }
        });
    }

    public void changeFrequency(String id,String time){
        // 创建查询对象，查找 Device 表中的 deviceId 字段
        LCQuery<LCObject> query = new LCQuery<>("Device");
        query.whereEqualTo("deviceId", id); // 使用 device.getDeviceId() 获取设备 ID

        query.findInBackground().subscribe(new Observer<List<LCObject>>() {
            @Override
            public void onSubscribe(Disposable disposable) {
                // 订阅开始时的操作
            }

            @Override
            public void onNext(List<LCObject> devices) {
                // 查找到满足条件的设备对象
                if (!devices.isEmpty()) {
                    // 假设查询返回的是一个设备列表，取第一个设备对象
                    LCObject deviceObject = devices.get(0);

                    // 修改 mode 字段
                    deviceObject.put("deviceFrequency", time);  // 将 "新的模式" 替换为你想设置的值

                    // 保存修改
                    deviceObject.saveInBackground().subscribe(new Observer<LCObject>() {
                        @Override
                        public void onSubscribe(Disposable disposable) {
                            // 处理保存时的操作
                        }

                        @Override
                        public void onNext(LCObject lcObject) {
                            // 成功保存后的操作
                            Log.d("TAG", "Device mode updated successfully");
                            fetchDeviceList();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            // 保存失败时的操作
                            Log.e("TAG", "Failed to update device mode", throwable);
                        }

                        @Override
                        public void onComplete() {
                            // 操作完成时的处理
                        }
                    });
                } else {
                    // 如果没有找到符合条件的设备
                    Log.w("TAG", "No device found with the specified deviceId");
                }
            }

            @Override
            public void onError(Throwable throwable) {
                // 查询失败时的处理
                Log.e("TAG", "Failed to query devices", throwable);
            }

            @Override
            public void onComplete() {
                // 查询完成后的操作
            }
        });
    }

    public void changeHengshu(String id) {
        // 创建查询对象，查找 Device 表中的 deviceId 字段
        LCQuery<LCObject> query = new LCQuery<>("Device");
        query.whereEqualTo("deviceId", id); // 使用设备的 deviceId 获取设备信息

        query.findInBackground().subscribe(new Observer<List<LCObject>>() {
            @Override
            public void onSubscribe(Disposable disposable) {
                // 订阅开始时的操作
            }

            @Override
            public void onNext(List<LCObject> devices) {
                // 查找到满足条件的设备对象
                if (!devices.isEmpty()) {
                    // 假设查询返回的是一个设备列表，取第一个设备对象
                    LCObject deviceObject = devices.get(0);

                    // 获取当前的 hengshu 字段值
                    String hengshu = deviceObject.getString("hengshu");

                    // 判断并更新 hengshu 字段的值
                    if ("横屏".equals(hengshu)) {
                        // 如果当前是横屏，则修改为竖屏
                        deviceObject.put("hengshu", "竖屏");
                    } else if ("竖屏".equals(hengshu)) {
                        // 如果当前是竖屏，则修改为横屏
                        deviceObject.put("hengshu", "横屏");
                    }

                    // 保存修改
                    deviceObject.saveInBackground().subscribe(new Observer<LCObject>() {
                        @Override
                        public void onSubscribe(Disposable disposable) {
                            // 处理保存时的操作
                        }

                        @Override
                        public void onNext(LCObject lcObject) {
                            // 成功保存后的操作
                            Log.d("TAG", "Device hengshu updated successfully");
                            fetchDeviceList();  // 可能需要刷新设备列表
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            // 保存失败时的操作
                            Log.e("TAG", "Failed to update device hengshu", throwable);
                        }

                        @Override
                        public void onComplete() {
                            // 操作完成时的处理
                        }
                    });
                } else {
                    // 如果没有找到符合条件的设备
                    Log.w("TAG", "No device found with the specified deviceId");
                }
            }

            @Override
            public void onError(Throwable throwable) {
                // 查询失败时的处理
                Log.e("TAG", "Failed to query devices", throwable);
            }

            @Override
            public void onComplete() {
                // 查询完成后的操作
            }
        });
    }


    public void send(String data){
        Log.i(TAG, "send data: "+data);
        ECBLE.setChineseTypeUTF8();
        // 直接把 String 转为 UTF-8 byte[]，然后发
        byte[] payload = data.getBytes(StandardCharsets.UTF_8);
        ECBLE.writeBLECharacteristicValue(data, false);
    }

    // Fetch the device list from LeanCloud
    private void fetchDeviceList() {
        LCUser currentUser = LCUser.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "fetchUserImagePaths: user is null");
            return;
        }

// 重新查询最新的用户数据
        currentUser.fetchInBackground().subscribe(new Observer<LCObject>() {
            @Override
            public void onSubscribe(Disposable d) {}

            @Override
            public void onNext(LCObject lcObject) {
                // 成功获取最新的用户数据
                List<String> deviceIds = lcObject.getList("deviceId");
                if (deviceIds != null && !deviceIds.isEmpty()) {
                    Log.i(TAG, "fetchUserImagePaths: " + deviceIds);

                    // 通过 deviceIds 查询 Device 表
                    LCQuery<LCObject> query = new LCQuery<>("Device");
                    query.whereContainedIn("deviceId", deviceIds); // 根据用户的 deviceId 列表查询 Device 表
                    query.findInBackground().subscribe(new Observer<List<LCObject>>() {
                        @Override
                        public void onSubscribe(Disposable d) {}

                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void onNext(List<LCObject> devices) {

                            deviceList.clear();
                            for (LCObject deviceObject : devices) {
                                // 解析设备对象并添加到列表
                                Device device = new Device();
                                device.setMode(deviceObject.getString("mode"));
                                device.setFrequency(deviceObject.getString("deviceFrequency"));
                                device.setStatus(deviceObject.getString("status"));
                                device.setDeviceId(deviceObject.getString("deviceId"));
                                device.setHengshu(deviceObject.getString("hengshu"));
                                device.setPower(deviceObject.getString("power"));


                                deviceList.add(device);
                            }
                            deviceAdapter.notifyDataSetChanged();
                            Log.i(TAG, "fetchDeviceList 完成，size=" + deviceList.size());

                            // 数据加载完毕之后，再注册一次 BLE 监听
                            registerBleListenerIfNeeded();
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG, "Failed to fetch devices", e);
                            Toast.makeText(getContext(), "Failed to fetch devices", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onComplete() {}
                    });

                } else {
                    Log.d(TAG, "No device IDs found for user.");
                }
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "Error fetching user data", e);
                Toast.makeText(getContext(), "获取用户数据失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {
//                Log.i(TAG, "fetchDeviceList2222: size"+deviceList.toString());
            }
        });

        Log.i(TAG, "fetchDeviceList222: size"+deviceList.toString());

    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_device_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // Navigate to the UploadFragment and pass parameters
    private void navigateToUploadFragment(Device device, String deviceName) {
        Bundle bundle = new Bundle();
        bundle.putString("deviceId", device.getDeviceId());
        bundle.putString("deviceName", device.getMode());
        bundle.putString("type", deviceName);
        Navigation.findNavController(requireView()).navigate(R.id.action_deviceListFragment_to_uploadFragment, bundle);
    }

    // Handle cloud function call for image processing (if needed)
    @SuppressLint("CheckResult")
    public void callCloudFunctionWithObservable(String id) {
        // Get the current user (if user authentication is required)
        LCUser currentUser = LCUser.getCurrentUser();

        // Create a parameter map
        Map<String, Object> params = new HashMap<>();
        params.put("fileId", id);

        // Call the cloud function and return Observable
        Observable<Object> observable = LCCloud.callFunctionInBackground(currentUser, "convert_image_to_bin", params);

        // Subscribe to the Observable and handle the cloud function result
        observable
                .subscribeOn(Schedulers.io()) // Make the network request on the IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Update UI on the main thread
                .subscribe(
                        result -> {
                            // Handle the successful result from the cloud function
                            Log.d("CloudFunction", "Result: " + result.toString());
                        },
                        throwable -> {
                            // Handle the error from the cloud function
                            Log.e("CloudFunction", "Error: " + throwable.getMessage());
                        }
                );
    }
}
