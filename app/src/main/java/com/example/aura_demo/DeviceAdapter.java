package com.example.aura_demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;


import java.io.FileNotFoundException;
import java.util.List;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private final Context context;
    private List<Device> deviceList;
    private final OnItemClickListener listener;

    // 构造函数
    public DeviceAdapter(Context context, List<Device> deviceList, OnItemClickListener listener) {
        this.context = context;
        this.deviceList = deviceList;
        this.listener = listener;
    }

    // 定义点击事件接口
    public interface OnItemClickListener {
        void onItemClick(Device device);
        void onMenuItemClick(Device device, int menuItemId) throws FileNotFoundException;
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);


    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.bind(device, listener);

        Log.i("DeviceListFragment", device.getDeviceId()+"---->"+device.isConnected());

        holder.textViewBLEStatus.setText(
                device.isConnected() ? "BLE: Connected" : "BLE: Disconnect"
        );

        holder.textwifi.setText(
                device.isWifiok() ? "WiFi: Connected" : "WiFi: Disconnect"
        );

    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }
    // 更新设备的连接状态并刷新 UI
    public void updateDeviceConnectionStatus(Device device, boolean isConnected) {
        device.setConnected(isConnected);
        notifyDataSetChanged();  // 刷新 UI
    }


    // ViewHolder 类
    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewIcon;
        TextView textViewStatus;
        TextView textViewMode;
        TextView textViewFrequency;

        TextView textViewHengshu;

        TextView textViewPower;
        ImageView imageViewArrow;

        ImageButton buttonMore;


        TextView textViewDeviceId;  // 修改这里：用于显示 deviceId

        TextView textViewBLEStatus;  // 显示蓝牙连接状态

        TextView textwifi;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            textViewDeviceId = itemView.findViewById(R.id.textViewDeviceId);  // 修改这里
            textViewBLEStatus = itemView.findViewById(R.id.textViewBLEStatus);  // 蓝牙连接状态显示
            textViewMode = itemView.findViewById(R.id.textViewMode);
            textViewPower = itemView.findViewById(R.id.textViewPower);
            buttonMore = itemView.findViewById(R.id.buttonMore);
            textwifi = itemView.findViewById(R.id.textViewWIFIStatus);

//            imageViewArrow = itemView.findViewById(R.id.imageViewArrow);
        }

        @SuppressLint("SetTextI18n")
        public void bind(final Device device, final OnItemClickListener listener) {

            textViewMode.setText("Mode:" + device.getMode());
            textViewPower.setText("Power: " + device.getPower());
            // 格式化 deviceId，取前四位并拼接 "AURA_GALLERY_"
            String formattedDeviceId = "AURA_GALLERY_" + device.getDeviceId().substring(0, 4);
            textViewDeviceId.setText(formattedDeviceId);  // 显示格式化后的 deviceId

            // 设置蓝牙连接状态
            if (device.isConnected()) {
                textViewBLEStatus.setText("BLE: Connected");
            } else {
                textViewBLEStatus.setText("BLE: Disconnect");
            }

            // 设置点击事件
            itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    listener.onItemClick(device);
                }
            });

            buttonMore.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    showPopupMenu(v, device, listener);
                }
            });
        }
    }

    // 更新数据
    public void updateData(List<Device> newDeviceList) {
        this.deviceList = newDeviceList;
        notifyDataSetChanged();
    }

    private static void showPopupMenu(View view, final Device device, final OnItemClickListener listener){
        // 创建 PopupMenu
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.menu_device_options, popupMenu.getMenu());

        // 设置菜单项点击监听器
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick(MenuItem item){
                try {
                    listener.onMenuItemClick(device, item.getItemId());
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        });

        // 显示菜单
        popupMenu.show();
    }
}
