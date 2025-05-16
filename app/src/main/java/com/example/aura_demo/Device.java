package com.example.aura_demo;
// Device.java


public class Device {
    private String iconUrl; // 设备图标的 URL 或资源名
    private String status;
    private String mode;
    private String frequency;
    private String deviceId;

    private String hengshu;

    private String timeZone;

    private String power;

    private String bleName;  // 新增的字段，用于存储蓝牙设备名称

    private boolean connected;  // 连接状态

    // 空构造函数（Firebase 需要）
    public Device() {
    }

    public Device(String iconUrl, String status, String mode, String frequency, String deviceId, String hengshu,String timeZone,String power) {
        this.iconUrl = iconUrl;
        this.status = status;
        this.mode = mode;
        this.frequency = frequency;
        this.deviceId = deviceId;
        this.hengshu = hengshu;
        this.timeZone = timeZone;
        this.power = power;
        this.bleName = bleName;  // 初始化 BLE 名称
        this.connected = connected;  // 默认未连接
    }
    public String gettimeZone() {
        return timeZone;
    }

    public void settimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    // Getter 和 Setter
    public String getIconUrl() {
        return iconUrl;
    }

    public String getDeviceId(){return deviceId;}

    public void setDeviceId(String deviceId){this.deviceId = deviceId;}

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getHengshu() {
        return hengshu;
    }
    public void setHengshu(String hengshu) {
        this.hengshu = hengshu;
    }

    public String getPower() {
        return power;
    }
    public void setPower(String power) {
        this.power = power;
    }

    public String getBleName() { return bleName;}
    public void setBleName(String bleName) {this.bleName = bleName;}

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
