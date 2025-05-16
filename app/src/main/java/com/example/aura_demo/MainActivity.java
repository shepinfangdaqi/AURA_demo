package com.example.aura_demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import cn.leancloud.LCUser;
import cn.leancloud.LeanCloud;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN";
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1;

    // LeanCloud 配置
    private static final String APP_ID  = "R5cCuCbaMwo6WIaEP6FmDGAZ-gzGzoHsz";
    private static final String APP_KEY = "gnwRyNe0uC9LeYAb3BEL58C5";
    private static final String SERVER  = "https://r5ccucba.lc-cn-n1-shared.com";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 初始化 LeanCloud
        LeanCloud.initialize(this, APP_ID, APP_KEY, SERVER);

        // 2. 请求蓝牙权限
        checkBluetoothPermissions();

        // 3. 载入布局
        setContentView(R.layout.activity_main);

        // 4. 设置 BottomNavigation + NavController
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        NavigationUI.setupWithNavController(bottomNav, navController);
    }

    /** 检查并申请 BLE 权限 **/
    private void checkBluetoothPermissions() {
        String[] perms = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        boolean granted = true;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }
        if (!granted) {
            ActivityCompat.requestPermissions(this, perms, BLUETOOTH_PERMISSION_REQUEST_CODE);
        } else {
            initBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                initBluetooth();
            } else {
                Log.e(TAG, "蓝牙权限被拒绝，无法使用蓝牙功能");
            }
        }
    }

    private void initBluetooth() {
        Log.d(TAG, "蓝牙权限授权，初始化蓝牙...");
        // TODO: BLE 扫描／连接逻辑
    }
}
