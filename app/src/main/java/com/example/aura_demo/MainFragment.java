package com.example.aura_demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;

import cn.leancloud.LCUser;

public class MainFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // 获取 NavController
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        // 按钮1：跳转到登录页面
        Button buttonToLogin = view.findViewById(R.id.button_to_login);
        buttonToLogin.setOnClickListener(v -> {
//            FirebaseAuth.getInstance().signOut();
            LCUser.logOut();
            navController.navigate(R.id.action_mainFragment_to_loginFragment);
        });

        // 按钮2：跳转到用户资料页面
        Button buttonToProfile = view.findViewById(R.id.button_to_profile);
        buttonToProfile.setOnClickListener(v -> navController.navigate(R.id.action_mainFragment_to_profileFragment));
//        buttonToProfile.setOnClickListener(v -> {
//            // 创建 Intent 来启动 UploadActivity
//            Intent intent = new Intent(requireContext(), UploadActivity.class);
//
//            // 如果需要，可以通过 intent 传递数据
//            // intent.putExtra("key", value);
//
//            // 启动 Activity
//            startActivity(intent);
//        });

        // 按钮3：跳转到设置页面
        Button buttonToSettings = view.findViewById(R.id.button_to_settings);
        buttonToSettings.setOnClickListener(v -> navController.navigate(R.id.action_mainFragment_to_bluetoothFragment));

        Button button_calendar = view.findViewById(R.id.button_calendar);
//        button_calendar.setOnClickListener(v -> navController.navigate(R.id.action_mainFragment_to_firebase));
        button_calendar.setOnClickListener(v -> navController.navigate(R.id.action_mainFragment_to_deviceList));

        return view;
    }
}