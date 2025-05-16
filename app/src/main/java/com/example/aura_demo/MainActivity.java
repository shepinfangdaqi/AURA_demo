package com.example.aura_demo;

//import cn.leancloud.LeanCloud;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.Navigation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


import cn.leancloud.LCUser;
import cn.leancloud.LeanCloud;
import cn.leancloud.LCException;
import cn.leancloud.LCObject;
import cn.leancloud.LCCloud;
import cn.leancloud.callback.FunctionCallback;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import android.Manifest;


public class MainActivity extends AppCompatActivity {

    private static final Logger log = LoggerFactory.getLogger(MainActivity.class);
    String AppID = "R5cCuCbaMwo6WIaEP6FmDGAZ-gzGzoHsz";
    String AppKey = "gnwRyNe0uC9LeYAb3BEL58C5";
    String Server = "https://r5ccucba.lc-cn-n1-shared.com";
    String TAG = "MAIN";

    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LeanCloud.initialize(this, AppID, AppKey, Server);


        // 检查并请求蓝牙权限
        checkBluetoothPermissions();

//        test();
        setContentView(R.layout.activity_main);
        // 调用云函数测试
//        callCloudFunctionWithObservable();
//git test
        Navigation.findNavController(this, R.id.nav_host_fragment)
                .setGraph(R.navigation.nav_graph);
    }

    // 蓝牙权限检查及申请
    private void checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // 如果没有权限，则请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }, BLUETOOTH_PERMISSION_REQUEST_CODE);
        } else {
            // 已经拥有权限，可以执行蓝牙相关操作
            initBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已被授予，可以进行蓝牙操作
                initBluetooth();
            } else {
                // 权限被拒绝，显示提示
                Log.e(TAG, "蓝牙权限被拒绝，无法进行蓝牙操作");
            }
        }
    }

    // 初始化蓝牙功能
    private void initBluetooth() {
        Log.d(TAG, "蓝牙权限已授予，初始化蓝牙");
        // 这里可以加入其他蓝牙操作代码
    }

    @SuppressLint("CheckResult")
    public void callCloudFunctionWithObservable() {
        // 获取当前用户（如果需要认证用户）
        LCUser currentUser = LCUser.getCurrentUser();

        // 创建一个参数 Map
        Map<String, Object> params = new HashMap<>();
        params.put("name", "value1");

        // 调用云函数并返回 Observable
        Observable<Object> observable = LCCloud.callFunctionInBackground(currentUser, "hello", params);

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

    public void test() {
        // 构建对象
        LCObject todo = new LCObject("Todo");

//// 为属性赋值
//        todo.put("title", "工程师周会");
//        todo.put("content", "周二两点，全体成员");

// 将对象保存到云端
        todo.saveInBackground().subscribe(new Observer<LCObject>() {
            public void onSubscribe(Disposable disposable) {
            }

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
}
