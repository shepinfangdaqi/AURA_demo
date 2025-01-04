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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthMultiFactorException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.MultiFactorResolver;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoginFragment extends BaseFragment {

    private static final String TAG = "EmailPassword";

    private FragmentLoginBinding mBinding;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Nullable
    @Override
//    展示页面，通过android studio的自动绑定绑定的fragment fragment_emailpassword.xml 自动生成的FragmentLogin类来创建页面
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentLoginBinding.inflate(inflater, container, false);
        mFirestore = FirebaseFirestore.getInstance();
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
//        mBinding.signOutButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                signOut();
//            }
//        });
//        mBinding.verifyEmailButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                sendEmailVerification();
//            }
//        });
//        mBinding.reloadButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                reload();
//            }
//        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            reload();
        }
    }

    private void createAccount(String email, String password) {
        Log.d(TAG, "createAccount:" + email);
        if (!validateForm()) {
            return;
        }

        showProgressBar();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                createUserProfile(user.getUid(), user.getDisplayName(), user.getEmail());
                            }

                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(getContext(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        hideProgressBar();
                    }
                });
    }

    private void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);
        if (!validateForm()) {
            return;
        }

        showProgressBar();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(getContext(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
//                            checkForMultiFactorFailure(task.getException());
                        }

                        if (!task.isSuccessful()) {
                            mBinding.status.setText(R.string.auth_failed);
                        }
                        hideProgressBar();
                    }
                });
    }

    private void signOut() {
        mAuth.signOut();
        updateUI(null);
    }

//    private void sendEmailVerification() {
//        // Disable button
//        mBinding.verifyEmailButton.setEnabled(false);
//
//        // Send verification email
//        final FirebaseUser user = mAuth.getCurrentUser();
//        user.sendEmailVerification()
//                .addOnCompleteListener(requireActivity(), new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                        // Re-enable button
//                        mBinding.verifyEmailButton.setEnabled(true);
//
//                        if (task.isSuccessful()) {
//                            Toast.makeText(getContext(),
//                                    "Verification email sent to " + user.getEmail(),
//                                    Toast.LENGTH_SHORT).show();
//                        } else {
//                            Log.e(TAG, "sendEmailVerification", task.getException());
//                            Toast.makeText(getContext(),
//                                    "Failed to send verification email.",
//                                    Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });
//    }

    private void reload() {
        mAuth.getCurrentUser().reload().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    updateUI(mAuth.getCurrentUser());
                    Toast.makeText(getContext(),
                            "Reload successful!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "reload", task.getException());
                    Toast.makeText(getContext(),
                            "Failed to reload user.",
                            Toast.LENGTH_SHORT).show();
                }
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

    private void updateUI(FirebaseUser user) {
        if (user!=null){
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_emailPasswordFragment_to_userProfileFragment);
        }
//        NavHostFragment.findNavController(this)
//                .navigate(R.id.action_emailPasswordFragment_to_userProfileFragment);
//        hideProgressBar();
//        if (user != null) {
//            mBinding.status.setText(getString(R.string.emailpassword_status_fmt,
//                    user.getEmail(), user.isEmailVerified()));
//            mBinding.detail.setText(getString(R.string.firebase_status_fmt, user.getUid()));
//
//            mBinding.emailPasswordButtons.setVisibility(View.GONE);
//            mBinding.emailPasswordFields.setVisibility(View.GONE);
//            mBinding.signedInButtons.setVisibility(View.VISIBLE);
//
//            if (user.isEmailVerified()) {
//                mBinding.verifyEmailButton.setVisibility(View.GONE);
//            } else {
//                mBinding.verifyEmailButton.setVisibility(View.VISIBLE);
//            }
//        } else {
//            mBinding.status.setText(R.string.signed_out);
//            mBinding.detail.setText(null);
//
//            mBinding.emailPasswordButtons.setVisibility(View.VISIBLE);
//            mBinding.emailPasswordFields.setVisibility(View.VISIBLE);
//            mBinding.signedInButtons.setVisibility(View.GONE);
//        }
    }

//    private void checkForMultiFactorFailure(Exception e) {
//        // Multi-factor authentication with SMS is currently only available for
//        // Google Cloud Identity Platform projects. For more information:
//        // https://cloud.google.com/identity-platform/docs/android/mfa
//        if (e instanceof FirebaseAuthMultiFactorException) {
//            Log.w(TAG, "multiFactorFailure", e);
//            MultiFactorResolver resolver = ((FirebaseAuthMultiFactorException) e).getResolver();
//            Bundle args = new Bundle();
//            args.putParcelable(MultiFactorSignInFragment.EXTRA_MFA_RESOLVER, resolver);
//        }
//    }
    private void createUserProfile(String uid, String name, String email) {
        // 创建一个新的用户对象
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);

        // 将用户信息添加到 Firestore 的 "users" 集合
        // 使用 UID 作为文档 ID，这样确保每个用户的数据是唯一的
        if (mFirestore != null){
            mFirestore.collection("users").document(uid).set(user)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Firestore", "DocumentSnapshot successfully written!");
                            // 处理成功写入数据后的逻辑，比如跳转界面或显示消息
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("Firestore", "Error writing document", e);
                            // 处理错误情况，比如提示用户
                        }
                    });
        } else {
            Log.e("EmailPasswordFragment", "Firestore instance is null");
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }
}
