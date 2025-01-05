package com.example.aura_demo;
// Device.java


public class Device {
    private String iconUrl; // 设备图标的 URL 或资源名
    private String status;
    private String mode;
    private String frequency;

    // 空构造函数（Firebase 需要）
    public Device() {
    }

    public Device(String iconUrl, String status, String mode, String frequency) {
        this.iconUrl = iconUrl;
        this.status = status;
        this.mode = mode;
        this.frequency = frequency;
    }

    // Getter 和 Setter
    public String getIconUrl() {
        return iconUrl;
    }

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
}
