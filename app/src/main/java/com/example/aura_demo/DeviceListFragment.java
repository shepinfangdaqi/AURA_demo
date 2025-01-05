package com.example.aura_demo;

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

import java.util.ArrayList;
import java.util.List;

public class DeviceListFragment extends Fragment {

    private DeviceAdapter deviceAdapter;
    private List<Device> deviceList;

    // Firebase Realtime Database 的引用
    private DatabaseReference databaseReference;

    // 指定的 key
    private static final String SPECIFIC_KEY = "devices"; // 替换为您希望使用的 key

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
                navigateToUploadFragment(device);
                Toast.makeText(getContext(), "点击设备: " + device.getMode(), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getContext(), "点击添加按钮", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });
    }

    // 读取设备列表
    private void readDeviceList(){
        databaseReference.addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                deviceList.clear();
                for(DataSnapshot deviceSnapshot : snapshot.getChildren()){
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
    private void navigateToUploadFragment(Device device){
        Bundle bundle = new Bundle();
        bundle.putString("deviceId", device.getStatus());
        bundle.putString("deviceName", device.getMode());
        Navigation.findNavController(requireView()).navigate(R.id.action_deviceListFragment_to_uploadFragment, bundle);
    }
}
