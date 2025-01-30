package com.example.aura_demo;

import static com.example.aura_demo.R.*;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ValueEventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.leancloud.LCCloud;
import cn.leancloud.LCFile;
import cn.leancloud.LCObject;
import cn.leancloud.LCUser;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DeviceListFragment extends Fragment {

    private static final Logger log = LoggerFactory.getLogger(DeviceListFragment.class);
    private DeviceAdapter deviceAdapter;
    private List<Device> deviceList;

    // Firebase Realtime Database 的引用
    private DatabaseReference databaseReference;

    // 指定的 key
    private static final String SPECIFIC_KEY = "devices"; // 替换为您希望使用的 key
    private final String TAG = "DeviceList";

    public DeviceListFragment() {
        // Required empty public constructor
    }

    public static DeviceListFragment newInstance() {
        return new DeviceListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // 使 Fragment 可以接收菜单事件
        ECBLE.onBLECharacteristicValueChange((String str, String strHex) -> {
            // 更新 ViewModel
            Log.i("DeviceActivity", "onCreate: "+str);
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_device_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        // 初始化视图
        Toolbar topAppBar = view.findViewById(R.id.topAppBar);
        RecyclerView recyclerViewDevices = view.findViewById(R.id.recyclerViewDevices);

        // 设置 Toolbar
        ((AppCompatActivity) getActivity()).setSupportActionBar(topAppBar);

        // 设置 RecyclerView
        recyclerViewDevices.setLayoutManager(new LinearLayoutManager(getContext()));
        deviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(getContext(), deviceList, new DeviceAdapter.OnItemClickListener(){
            @Override
            public void onItemClick(Device device){
                // 处理设备项点击事件
                // TODO: 实现跳转逻辑
//                navigateToUploadFragment(device);
                Toast.makeText(getContext(), "点击设备: " + device.getMode(), Toast.LENGTH_SHORT).show();
            }


            @SuppressLint("CheckResult")
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
//                    navigateToUploadFragment(device,"日历");
                    Log.i(TAG, "onMenuItemClick: 日历");
                } else if (menuItemId == R.id.action_beautiful_image_mode) {
                    navigateToUploadFragment(device,"美图模式");
                    Log.i(TAG, "onMenuItemClick: 美图模式");
                } else if (menuItemId == id.action_custom_image) {
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

        // 初始化 Firebase Realtime Database 引用
        databaseReference = FirebaseDatabase.getInstance().getReference(SPECIFIC_KEY);

        // 读取设备列表
        readDeviceList();

        // 设置顶部栏菜单项点击事件
        topAppBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick(MenuItem item){
                if(item.getItemId() == R.id.action_add){
                    // 处理添加按钮点击事件
                    // TODO: 实现添加设备逻辑，如弹出对话框
                    // 启动 MainBLUEActivity
//                    Intent intent = new Intent(requireContext(), MainBLUEActivity.class);
//                    startActivity(intent);
                    Navigation.findNavController(requireView()).navigate(id.action_deviceListFragment_to_blueFragment);
//                    Toast.makeText(getContext(), "点击添加按钮", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });
        ECBLE.onBLECharacteristicValueChange((String str, String strHex) -> {
            // 更新 ViewModel
            Log.i("DeviceList", "onViewCreated: "+str);
        });
        send("AT_ID?\r\n");
//        test();
    }


    @SuppressLint("CheckResult")
    public void callCloudFunctionWithObservable(String id) {
        // 获取当前用户（如果需要认证用户）
        LCUser currentUser = LCUser.getCurrentUser();

        // 创建一个参数 Map
        Map<String, Object> params = new HashMap<>();
        params.put("fileId", id);

        // 调用云函数并返回 Observable
        Observable<Object> observable = LCCloud.callFunctionInBackground(currentUser, "convert_image_to_bin", params);

        // 订阅 Observable，处理云函数的结果
        observable
                .subscribeOn(Schedulers.io()) // 在 IO 线程进行网络请求
                .observeOn(AndroidSchedulers.mainThread()) // 在主线程更新 UI
                .subscribe(
                        result -> {
                            // 云函数调用成功，处理返回的结果
                            Log.d("CloudFunction", "Result: " + result.toString());
                        },
                        throwable -> {
                            // 云函数调用失败，处理错误
                            Log.e("CloudFunction", "Error: " + throwable.getMessage());
                        }
                );
    }
    public void send(String data){
        Log.i(TAG, "send: data"+data);
        ECBLE.writeBLECharacteristicValue(data, false);
    }
    public void test(){
        // 构建对象
        Log.i(TAG, "test: ");
        LCObject todo = new LCObject("Todo");

// 为属性赋值
        todo.put("title",   "工程师周会");
        todo.put("content", "周二两点，全体成员");

// 将对象保存到云端
        todo.saveInBackground().subscribe(new Observer<LCObject>() {
            public void onSubscribe(Disposable disposable) {}
            public void onNext(LCObject todo) {
                // 成功保存之后，执行其他逻辑
                System.out.println("保存成功。objectId：" + todo.getObjectId());
            }
            public void onError(Throwable throwable) {
                // 异常处理
            }
            public void onComplete() {
                Log.i(TAG, "onComplete: ");
            }
        });
    }

    // 读取设备列表
    private void readDeviceList(){
        databaseReference.addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                deviceList.clear();
                Log.i(TAG, "onDataChange: " + snapshot.getValue());
                for(DataSnapshot deviceSnapshot : snapshot.getChildren()){
                    Log.i(TAG, "onDataChange: " + deviceSnapshot.getKey());
                    Log.d(TAG, "Device Snapshot: " + deviceSnapshot.getValue());

                    // 或者逐个字段输出
                    for (DataSnapshot childSnapshot : deviceSnapshot.getChildren()) {
                        String key = childSnapshot.getKey();
                        Object value = childSnapshot.getValue();
                        Log.d(TAG, "Field: " + key + " - Value: " + value);
                    }
                    Device device = deviceSnapshot.getValue(Device.class);
                    if(device != null){
                        deviceList.add(device);
                    }
                }
                deviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error){
                Toast.makeText(getContext(), "读取设备列表失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 创建菜单
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater){
        inflater.inflate(R.menu.menu_device_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // 导航到 UploadFragment 并传递参数
    private void navigateToUploadFragment(Device device,String deviceName){
        Bundle bundle = new Bundle();
        bundle.putString("deviceId", device.getStatus());
        bundle.putString("deviceName", device.getMode());
        bundle.putString("type",deviceName);
        Navigation.findNavController(requireView()).navigate(R.id.action_deviceListFragment_to_uploadFragment, bundle);
    }
}
