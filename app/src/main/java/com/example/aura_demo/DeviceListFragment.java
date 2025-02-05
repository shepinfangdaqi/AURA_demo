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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DeviceListFragment extends Fragment {

    private static final String TAG = "DeviceListFragment";

    private DeviceAdapter deviceAdapter;
    private List<Device> deviceList;

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

        // Initialize views
        Toolbar topAppBar = view.findViewById(R.id.topAppBar);

        RecyclerView recyclerViewDevices = view.findViewById(R.id.recyclerViewDevices);

        // Set up the top app bar
        topAppBar.setTitle("Devices");


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
                Log.i(TAG, "onMenuItemClick: "+  R.id.action_calendar_mode);
                if (menuItemId == R.id.action_calendar_mode){
//                    LCFile file = LCFile.withAbsoluteLocalPath("11.jpeg", "/storage/emulated/0/Download/11.jpeg");
//                    file.saveInBackground().subscribe(new Observer<LCFile>() {
//                        public void onSubscribe(Disposable disposable) {}
//                        public void onNext(LCFile file) {
//                            System.out.println("文件保存完成。URL：" + file.getUrl() + "，文件名：" + file.getObjectId());
//                            String id = file.getObjectId();
//                            callCloudFunctionWithObservable(id);
//                        }
//                        public void onError(Throwable throwable) {
//                            // 保存失败，可能是文件无法被读取，或者上传过程中出现问题
//                        }
//                        public void onComplete() {}
//                    });



                    send("AT_ID?\r\n");
                    send("AT_STA?\r\n");
//                    navigateToUploadFragment(device,"日历");
                    Log.i(TAG, "onMenuItemClick: 日历");
                    Log.i(TAG, "onMenuItemClick: id: "+device.getDeviceId());
                    changeMode(device.getDeviceId(),"日历模式");
                } else if (menuItemId == R.id.action_beautiful_image_mode) {
                    send("AT_STA?\r\n");
//                    navigateToUploadFragment(device,"美图模式");
                    Log.i(TAG, "onMenuItemClick: 美图模式");
                    changeMode(device.getDeviceId(),"美图模式");
                } else if (menuItemId == R.id.action_custom_image) {
                    navigateToUploadFragment(device,"自选模式");
                    Log.i(TAG, "onMenuItemClick: 自选图片");
                    changeMode(device.getDeviceId(),"自选图片");
                } else if (menuItemId == R.id.action_custom_hv_choose) {
                    Log.i(TAG, "onMenuItemClick: 横竖切换");
                    changeHengshu(device.getDeviceId());

                }else if (menuItemId == R.id.action_time) {
                    // 创建一个输入框
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("请输入时间");

                    // 设置输入框为数字类型
                    final EditText input = new EditText(getContext());
                    input.setInputType(InputType.TYPE_CLASS_NUMBER); // 限制为输入数字
                    builder.setView(input);

                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String inputText = input.getText().toString();

                            // 检查输入是否为有效的整数
                            if (!inputText.isEmpty()) {
                                try {
                                    int time = Integer.parseInt(inputText);
                                    // 这里你可以处理输入的整数数据，例如存储或使用它
                                    Toast.makeText(getContext(), "输入的时间是: " + time + "h", Toast.LENGTH_SHORT).show();
                                    changeFrequency(device.getDeviceId(), time+"H");
                                } catch (NumberFormatException e) {
                                    Toast.makeText(getContext(), "请输入有效的整数", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(getContext(), "输入不能为空", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    builder.show();
                }
                else if (menuItemId == R.id.action_wifi) {
                    Navigation.findNavController(requireView()).navigate(R.id.action_deviceListFragment_to_wifiFragment);
                }
            }
        });
        recyclerViewDevices.setAdapter(deviceAdapter);

        // Fetch the device list from LeanCloud
        fetchDeviceList();

        // Set up top app bar menu item click listener
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_add) {
                // Handle the add button click event
                Navigation.findNavController(requireView()).navigate(R.id.action_deviceListFragment_to_blueFragment);
                return true;
            }
            return false;
        });


    }

    public void changeMode(String id,String mode){
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
                    deviceObject.put("mode", mode);  // 将 "新的模式" 替换为你想设置的值

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
        ECBLE.writeBLECharacteristicValue(data, false);
    }

    // Fetch the device list from LeanCloud
    private void fetchDeviceList() {
        LCQuery<LCObject> query = new LCQuery<>("Device"); // Assuming the class name is "Device" in LeanCloud
        query.findInBackground().subscribe(new Observer<List<LCObject>>() {
            @Override
            public void onSubscribe(Disposable d) {}

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onNext(List<LCObject> devices) {
                deviceList.clear();
                for (LCObject deviceObject : devices) {
                    // Parse the device object and add it to the list
                    Device device = new Device();
                    device.setMode(deviceObject.getString("mode"));
                    device.setFrequency(deviceObject.getString("deviceFrequency"));
                    device.setStatus(deviceObject.getString("status"));
                    device.setDeviceId(deviceObject.getString("deviceId"));
                    device.setHengshu(deviceObject.getString("hengshu"));
                    deviceList.add(device);
                }
                deviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "Failed to fetch devices", e);
                Toast.makeText(getContext(), "Failed to fetch devices", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {}
        });
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
