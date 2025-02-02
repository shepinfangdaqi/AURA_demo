package com.example.aura_demo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
                    LCFile file = LCFile.withAbsoluteLocalPath("11.jpeg", "/storage/emulated/0/Download/11.jpeg");
                    file.saveInBackground().subscribe(new Observer<LCFile>() {
                        public void onSubscribe(Disposable disposable) {}
                        public void onNext(LCFile file) {
                            System.out.println("文件保存完成。URL：" + file.getUrl() + "，文件名：" + file.getObjectId());
                            String id = file.getObjectId();
                            callCloudFunctionWithObservable(id);
                        }
                        public void onError(Throwable throwable) {
                            // 保存失败，可能是文件无法被读取，或者上传过程中出现问题
                        }
                        public void onComplete() {}
                    });



                    send("AT_ID?\r\n");
                    navigateToUploadFragment(device,"日历");
                    Log.i(TAG, "onMenuItemClick: 日历");
                } else if (menuItemId == R.id.action_beautiful_image_mode) {
                    navigateToUploadFragment(device,"美图模式");
                    Log.i(TAG, "onMenuItemClick: 美图模式");
                } else if (menuItemId == R.id.action_custom_image) {
                    navigateToUploadFragment(device,"自选模式");
                    Log.i(TAG, "onMenuItemClick: 自选图片");
                }
//                switch(menuItemId) {
//                    case R.id.action_calendar_mode:
//                        // 处理日历模式
//                        Toast.makeText(getContext(), "选择了日历模式: " + device.getDeviceName(), Toast.LENGTH_SHORT).show();
//                        //
//                        break;
//                    case R.id.action_beautiful_image_mode:
//                        // 处理美图模式
//                        Toast.makeText(getContext(), "选择了美图模式: " + device.getDeviceName(), Toast.LENGTH_SHORT).show();
//                        //
//                        break;
//                    case R.id.action_custom_image:
//                        // 处理自选图片
//                        Toast.makeText(getContext(), "选择了自选图片: " + device.getDeviceName(), Toast.LENGTH_SHORT).show();
//                        // 
//                        break;
//                    default:
//                        break;
//                }
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
    public void send(String data){
        Log.i(TAG, "send: data"+data);
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
        bundle.putString("deviceId", device.getStatus());
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
