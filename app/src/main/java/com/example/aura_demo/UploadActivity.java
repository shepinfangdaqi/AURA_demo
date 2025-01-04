package com.example.aura_demo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.aura_demo.databinding.UploadActivityBinding;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity to upload and download photos from Firebase Storage.
 *
 * See {@link MyUploadService} for upload example.
 * See {@link MyDownloadService} for download example.
 */
public class UploadActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "Storage#MainActivity";

    private static final String KEY_FILE_URI = "key_file_uri";
    private static final String KEY_DOWNLOAD_URL = "key_download_url";

    private BroadcastReceiver mBroadcastReceiver;
    private FirebaseAuth mAuth;

    private Uri mDownloadUrl = null;
    private Uri mFileUri = null;

    private UploadActivityBinding binding;

    private ActivityResultLauncher<String[]> intentLauncher;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(this,
                            "Can't post notifications without POST_NOTIFICATIONS permission",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = UploadActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Click listeners
        binding.buttonCamera.setOnClickListener(this);
        binding.buttonSignIn.setOnClickListener(this);
        binding.buttonDownload.setOnClickListener(this);

        // Restore instance state
        if (savedInstanceState != null) {
            mFileUri = savedInstanceState.getParcelable(KEY_FILE_URI);
            mDownloadUrl = savedInstanceState.getParcelable(KEY_DOWNLOAD_URL);
        }
        onNewIntent(getIntent());

        intentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), fileUri -> {
                    if (fileUri != null) {
                        uploadFromUri(fileUri);
                    } else {
                        Log.w(TAG, "File URI is null");
                    }
                });

        // Local broadcast receiver
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive:" + intent);
                hideProgressBar();

                switch (intent.getAction()) {
                    case MyDownloadService.DOWNLOAD_COMPLETED:
                        // Get number of bytes downloaded
                        long numBytes = intent.getLongExtra(MyDownloadService.EXTRA_BYTES_DOWNLOADED, 0);

                        // Alert success
                        showMessageDialog(getString(R.string.success), String.format(Locale.getDefault(),
                                "%d bytes downloaded from %s",
                                numBytes,
                                intent.getStringExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH)));
                        break;
                    case MyDownloadService.DOWNLOAD_ERROR:
                        // Alert failure
                        showMessageDialog("Error", String.format(Locale.getDefault(),
                                "Failed to download from %s",
                                intent.getStringExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH)));
                        break;
                    case MyUploadService.UPLOAD_COMPLETED:
                    case MyUploadService.UPLOAD_ERROR:
                        onUploadResultIntent(intent);
                        break;
                }
            }
        };

        askNotificationPermission();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Check if this Activity was launched by clicking on an upload notification
        if (intent.hasExtra(MyUploadService.EXTRA_DOWNLOAD_URL)) {
            onUploadResultIntent(intent);
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        updateUI(mAuth.getCurrentUser());

        // Register receiver for uploads and downloads
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(mBroadcastReceiver, MyDownloadService.getIntentFilter());
        manager.registerReceiver(mBroadcastReceiver, MyUploadService.getIntentFilter());
    }

    @Override
    public void onStop() {
        super.onStop();

        // Unregister download receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putParcelable(KEY_FILE_URI, mFileUri);
        out.putParcelable(KEY_DOWNLOAD_URL, mDownloadUrl);
    }

    private void uploadFromUri(Uri fileUri) {
        Log.d(TAG, "uploadFromUri:src:" + fileUri.toString());

        // Save the File URI
        mFileUri = fileUri;

//        // 12.08 上传图片到firebase storage中保存的同时，将存储的路径记录在firestore database里。路径的构成基于user的ID或者email。
//        FirebaseUser user = mAuth.getCurrentUser();
//
//        assert user != null;
//        String ID = user.getUid();

        // Clear the last download, if any
        updateUI(mAuth.getCurrentUser());
        mDownloadUrl = null;

        // Start MyUploadService to upload the file, so that the file is uploaded
        // even if this Activity is killed or put in the background
        startService(new Intent(this, MyUploadService.class)
                .putExtra(MyUploadService.EXTRA_FILE_URI, fileUri)
                .setAction(MyUploadService.ACTION_UPLOAD));

        // Show loading spinner
        showProgressBar(getString(R.string.progress_uploading));
    }

    private void beginDownload() {
        // Get path
        String path = "photos/" + mFileUri.getLastPathSegment();

        // Kick off MyDownloadService to download the file
        Intent intent = new Intent(this, MyDownloadService.class)
                .putExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH, path)
                .setAction(MyDownloadService.ACTION_DOWNLOAD);
        startService(intent);

        // Show loading spinner
        showProgressBar(getString(R.string.progress_downloading));
    }

    private void launchCamera() {
        Log.d(TAG, "launchCamera");

        // Pick an image from storage
        intentLauncher.launch(new String[]{ "image/*" });
    }

    private void signInAnonymously() {
        // Sign in anonymously. Authentication is required to read or write from Firebase Storage.
        showProgressBar(getString(R.string.progress_auth));
        mAuth.signInAnonymously()
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Log.d(TAG, "signInAnonymously:SUCCESS");
                        hideProgressBar();
                        updateUI(authResult.getUser());
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e(TAG, "signInAnonymously:FAILURE", exception);
                        hideProgressBar();
                        updateUI(null);
                    }
                });
    }

    private void onUploadResultIntent(Intent intent) {
        // Got a new intent from MyUploadService with a success or failure
        mDownloadUrl = intent.getParcelableExtra(MyUploadService.EXTRA_DOWNLOAD_URL);
        mFileUri = intent.getParcelableExtra(MyUploadService.EXTRA_FILE_URI);

        updateUI(mAuth.getCurrentUser());
    }

//    private void updateUI(FirebaseUser user) {
//        // Signed in or Signed out
//        if (user != null) {
//            binding.layoutSignin.setVisibility(View.GONE);
//            binding.layoutStorage.setVisibility(View.VISIBLE);
//        } else {
//            binding.layoutSignin.setVisibility(View.VISIBLE);
//            binding.layoutStorage.setVisibility(View.GONE);
//        }
//        ViewPager2 viewPager = findViewById(R.id.viewPager);
//        List<Integer> imageList = new ArrayList<>();
//        imageList.add(R.drawable.image1);  // 请替换为你的图片资源
//        imageList.add(R.drawable.image2);  // 请替换为你的图片资源
//        imageList.add(R.drawable.image3);  // 请替换为你的图片资源
//
//        // 创建适配器并设置到 ViewPager2
//        ImageAdapter adapter = new ImageAdapter(this, imageList);
//        viewPager.setAdapter(adapter);
//        binding.layoutDownload.setVisibility(View.VISIBLE);
//
//
//        // 假设 mDownloadUrl 是从网络获取的图片 URL
////        if (mDownloadUrl != null) {
////            // 存储图片 URL 的列表
////            List<String> imageUrls = new ArrayList<>();
////            imageUrls.add(mDownloadUrl.toString());  // 加入您的图片 URL
////
////            // 如果需要添加更多的图片 URL，可以在这里继续添加
////            // imageUrls.add("http://example.com/image2.jpg");
////
////            // 创建适配器并设置到 ViewPager2
////            ImageAdapter adapter = new ImageAdapter(this, imageUrls);
////            viewPager.setAdapter(adapter);
////
////            // 显示下载布局
////            binding.layoutDownload.setVisibility(View.VISIBLE);
////        } else {
////            // 处理没有下载 URL 的情况
////            binding.layoutDownload.setVisibility(View.GONE);
////        }
//    }

    private void updateUI(FirebaseUser user) {
        // 根据用户登录状态更新 UI
        if (user != null) {
            binding.layoutSignin.setVisibility(View.GONE);
            binding.layoutStorage.setVisibility(View.VISIBLE);
        } else {
            binding.layoutSignin.setVisibility(View.VISIBLE);
            binding.layoutStorage.setVisibility(View.GONE);
        }

        // 初始化 ViewPager2
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        // 使用空列表初始化适配器
        ImageAdapter adapter = new ImageAdapter(this, new ArrayList<>());
        viewPager.setAdapter(adapter);

        // 如果用户已登录，加载用户的图片路径
        if (user != null) {
            Log.i(TAG, "updateUI: user");
            fetchUserImagePaths();
        }
    }

    /**
     * 从 Firestore 获取当前用户的所有图片路径，并加载到 ViewPager2
     */
    private void fetchUserImagePaths() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "fetchUserImagePaths: user is null");
            return;
        }

        String userId = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDocRef = db.collection("users").document(userId);
//        ViewPager2 viewPager = findViewById(R.id.viewPager);
        // 获取用户文档
        userDocRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> imagePaths = (List<String>) documentSnapshot.get("imagePaths");
                        if (imagePaths != null && !imagePaths.isEmpty()) {
                            // 获取下载 URL 列表
                            Log.i(TAG, "fetchUserImagePaths: "+imagePaths);
                            fetchDownloadUrls(imagePaths);
                        } else {
                            Log.d(TAG, "No image paths found for user.");
                            Toast.makeText(this, "没有找到上传的图片", Toast.LENGTH_SHORT).show();
                            // 清空 ViewPager2
                            ViewPager2 viewPager = findViewById(R.id.viewPager);
                            ImageAdapter adapter = (ImageAdapter) viewPager.getAdapter();
                            if (adapter != null) {
                                adapter = new ImageAdapter(this, new ArrayList<>());
                                viewPager.setAdapter(adapter);
//                                adapter.setImageUrls(new ArrayList<>());
                            }
                        }
                    } else {
                        Log.d(TAG, "User document does not exist.");
                        Toast.makeText(this, "用户文档不存在", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user document", e);
                    Toast.makeText(this, "获取用户数据失败", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * 根据存储路径获取下载 URL 并更新 ViewPager2
     */
    private void fetchDownloadUrls(List<String> imagePaths) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        List<String> downloadUrls = new ArrayList<>();
        int total = imagePaths.size();
        final int[] count = {0};

        for (String path : imagePaths) {
            StorageReference storageRef = storage.getReference().child(path);
            storageRef.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        downloadUrls.add(uri.toString());
                        count[0]++;
                        if (count[0] == total) {
                            // 所有下载 URL 获取完成，更新 ViewPager2
                            updateViewPager(downloadUrls);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get download URL for path: " + path, e);
                        count[0]++;
                        if (count[0] == total) {
                            // 所有下载 URL 获取完成，更新 ViewPager2
                            updateViewPager(downloadUrls);
                        }
                    });
        }
    }

    /**
     * 更新 ViewPager2 显示图片
     */
    private void updateViewPager(List<String> downloadUrls) {
        Log.i(TAG, "updateViewPager: "+downloadUrls);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        ImageAdapter adapter = (ImageAdapter) viewPager.getAdapter();
        if (adapter != null) {
            adapter = new ImageAdapter(this, downloadUrls);
            viewPager.setAdapter(adapter);
            Log.i(TAG, "updateViewPager: success");
            binding.layoutDownload.setVisibility(View.VISIBLE);
        }
    }

    private void showMessageDialog(String title, String message) {
        AlertDialog ad = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .create();
        ad.show();
    }

    private void showProgressBar(String caption) {
        binding.caption.setText(caption);
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        binding.caption.setText("");
        binding.progressBar.setVisibility(View.INVISIBLE);
    }

    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Your app can post notifications.
            } else{
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int i = item.getItemId();
//        if (i == R.id.action_logout) {
//            FirebaseAuth.getInstance().signOut();
//            updateUI(null);
//            return true;
//        } else {
//            return super.onOptionsItemSelected(item);
//        }
//    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.buttonCamera) {
            launchCamera();
        } else if (i == R.id.buttonSignIn) {
            signInAnonymously();
        } else if (i == R.id.buttonDownload) {
            beginDownload();
        }
    }
}
