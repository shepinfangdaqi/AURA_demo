package com.example.aura_demo;

import android.content.Context;
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
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    // ViewHolder 类
    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewIcon;
        TextView textViewStatus;
        TextView textViewMode;
        TextView textViewFrequency;
        ImageView imageViewArrow;

        ImageButton buttonMore;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
            textViewMode = itemView.findViewById(R.id.textViewMode);
            textViewFrequency = itemView.findViewById(R.id.textViewFrequency);
            buttonMore = itemView.findViewById(R.id.buttonMore);
//            imageViewArrow = itemView.findViewById(R.id.imageViewArrow);
        }

        public void bind(final Device device, final OnItemClickListener listener) {
            // 加载图标
            if (device.getIconUrl() != null && !device.getIconUrl().isEmpty()) {
                // 如果使用 URL 加载图片
                imageViewIcon.setImageResource(R.drawable.image1);
            } else {
                // 使用本地资源
                imageViewIcon.setImageResource(R.drawable.image2);
            }

            // 设置文本
            textViewStatus.setText("状态: " + device.getStatus());
            textViewMode.setText("模式: " + device.getMode());
            textViewFrequency.setText("频率: " + device.getFrequency());

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
