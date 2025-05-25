package com.example.aura_demo;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.example.aura_demo.databinding.FragmentLoginBinding;

import cn.leancloud.LCException;
import cn.leancloud.LCUser;
import cn.leancloud.callback.LCCallback;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class LoginFragment extends BaseFragment {

    private static final String TAG = "EmailPassword";

    private FragmentLoginBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentLoginBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setProgressBar(mBinding.progressBar);

        // Buttons
        mBinding.emailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mBinding.fieldEmail.getText().toString();
                String password = mBinding.fieldPassword.getText().toString();
                signIn(email, password);
            }
        });

        mBinding.emailCreateAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mBinding.fieldEmail.getText().toString();
                String password = mBinding.fieldPassword.getText().toString();
                createAccount(email, password);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        // 检查 LeanCloud 是否已有登录的用户
        LCUser currentUser = LCUser.getCurrentUser();
        if (currentUser != null) {
            // 如果用户已登录，直接更新 UI 并跳转到用户资料页面
            updateUI(currentUser);
        }
    }

    private void createAccount(String email, String password) {
        Log.d(TAG, "createAccount:" + email);
        if (!validateForm()) {
            return;
        }

        showProgressBar();

        LCUser user = new LCUser();
        user.setUsername(email);  // 使用邮箱作为用户名
        user.setPassword(password);  // 设置密码
        user.setEmail(email);  // 设置邮箱

        // 可选的其他用户属性
        user.put("gender", "secret");

        // 注册用户
        user.signUpInBackground().subscribe(new Observer<LCUser>() {
            @Override
            public void onSubscribe(Disposable disposable) {}

            @Override
            public void onNext(LCUser user) {
                // 注册成功
                Log.d(TAG, "注册成功。objectId：" + user.getObjectId());
                signIn(email, password);
                updateUI(user);
            }

            @Override
            public void onError(Throwable throwable) {
                // 注册失败（通常是因为用户名已被使用）
                Log.e(TAG, "注册失败", throwable);
                Toast.makeText(getContext(), getString(R.string.register_fail), Toast.LENGTH_SHORT).show();
                updateUI(null);
            }

            @Override
            public void onComplete() {}
        });
    }

    private void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);
        if (!validateForm()) {
            return;
        }

        showProgressBar();

        LCUser.logIn(email, password).subscribe( new Observer<LCUser>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(LCUser lcUser) {
                Log.d(TAG, "signInWithEmail:success");
                updateUI(lcUser);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "signInWithEmail:failure", e);
                Toast.makeText(getContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                updateUI(null);
            }

            @Override
            public void onComplete() {

            }
        });
    }

    private boolean validateForm() {
        boolean valid = true;

        String email = mBinding.fieldEmail.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mBinding.fieldEmail.setError("Required.");
            valid = false;
        } else {
            mBinding.fieldEmail.setError(null);
        }

        String password = mBinding.fieldPassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mBinding.fieldPassword.setError("Required.");
            valid = false;
        } else {
            mBinding.fieldPassword.setError(null);
        }

        return valid;
    }

    private void updateUI(LCUser user) {
        if (user != null) {
            // 如果用户登录成功，跳转到用户资料页面
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_emailPasswordFragment_to_userProfileFragment);
//            NavHostFragment.findNavController(this)
//                    .navigate(R.id.action_loginFragment_to_deviceList);
        } else {
            // 如果用户未登录，更新 UI 状态
            mBinding.status.setText(R.string.signed_out);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }
}
