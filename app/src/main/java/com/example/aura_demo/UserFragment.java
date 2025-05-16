package com.example.aura_demo;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.textfield.TextInputEditText;

import cn.leancloud.LCUser;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class UserFragment extends Fragment {

    private MaterialTextView tvStatus;
    private MaterialButton  btnLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_user, container, false);
        tvStatus  = root.findViewById(R.id.tv_user_status);
        btnLogin  = root.findViewById(R.id.btn_user_action);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateUI();
    }

    /** 刷新显示状态 */
    private void updateUI() {
        LCUser current = LCUser.getCurrentUser();
        if (current == null) {
            tvStatus.setText("未登录");
            btnLogin.setText("登录");
            btnLogin.setOnClickListener(v -> showLoginDialog());
        } else {
            tvStatus.setText("你好，" + current.getUsername());
            btnLogin.setText("退出登录");
            btnLogin.setOnClickListener(v -> {
                LCUser.logOut();
                updateUI();
            });
        }
    }

    /** 弹出登录／注册对话框，方法借鉴自 LoginFragment */
    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_login, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        ProgressBar progressBar       = dialogView.findViewById(R.id.progressBar);
        TextInputEditText etEmail        = dialogView.findViewById(R.id.fieldEmail);
        TextInputEditText etPassword     = dialogView.findViewById(R.id.fieldPassword);
        MaterialButton    btnSignIn       = dialogView.findViewById(R.id.btnSignIn);
        MaterialButton    btnCreateAccount= dialogView.findViewById(R.id.btnCreateAccount);

        // 登录
        btnSignIn.setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (!validateForm(email, password, etEmail, etPassword)) return;

            progressBar.setVisibility(View.VISIBLE);
            LCUser.logIn(email, password)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<LCUser>() {
                        @Override public void onSubscribe(Disposable d) {}
                        @Override public void onNext(LCUser user) {
                            Toast.makeText(getContext(),
                                    "登录成功，欢迎 " + user.getUsername(),
                                    Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            updateUI();
                        }
                        @Override public void onError(Throwable e) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(),
                                    "登录失败: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onComplete() {}
                    });
        });

        // 注册
        btnCreateAccount.setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (!validateForm(email, password, etEmail, etPassword)) return;

            progressBar.setVisibility(View.VISIBLE);
            LCUser user = new LCUser();
            user.setUsername(email);
            user.setPassword(password);
            user.setEmail(email);
            user.signUpInBackground()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<LCUser>() {
                        @Override public void onSubscribe(Disposable d) {}
                        @Override public void onNext(LCUser u) {
                            // 注册成功后直接登录
                            LCUser.logIn(email, password);
                            Toast.makeText(getContext(),
                                    "注册并登录成功",
                                    Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            updateUI();
                        }
                        @Override public void onError(Throwable e) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(),
                                    "注册失败: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onComplete() {}
                    });
        });

        dialog.show();
    }

    /** 简单的表单校验，错误时在输入框上显示 */
    private boolean validateForm(String email, String password,
                                 TextInputEditText etEmail,
                                 TextInputEditText etPassword) {
        boolean valid = true;
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Required");
            valid = false;
        } else {
            etEmail.setError(null);
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Required");
            valid = false;
        } else {
            etPassword.setError(null);
        }
        return valid;
    }
}
