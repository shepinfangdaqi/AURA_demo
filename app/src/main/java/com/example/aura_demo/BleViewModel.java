// BleViewModel.java
package com.example.aura_demo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BleViewModel extends ViewModel {
    public static class BleEvent {
        public final String deviceName;
        public final boolean isConnected;
        public BleEvent(String name, boolean connected) {
            deviceName = name;
            isConnected = connected;
        }
    }

    private final MutableLiveData<BleEvent> bleEvent = new MutableLiveData<>();

    /** 发布连接/断开事件 **/
    public void postEvent(String deviceName, boolean isConnected) {
        bleEvent.postValue(new BleEvent(deviceName, isConnected));
    }

    /** 让任意页面观察 **/
    public LiveData<BleEvent> getBleEvent() {
        return bleEvent;
    }
}
