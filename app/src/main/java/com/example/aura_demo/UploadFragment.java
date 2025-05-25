package com.example.aura_demo;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.aura_demo.databinding.FragmentUploadBinding;
import com.google.android.gms.common.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.leancloud.LCCloud;
import cn.leancloud.LCFile;
import cn.leancloud.LCObject;
import cn.leancloud.LCQuery;
import cn.leancloud.LCUser;
import cn.leancloud.types.LCNull;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.yalantis.ucrop.UCrop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import com.theartofdev.edmodo.cropper.CropImage;
//import com.theartofdev.edmodo.cropper.CropImageView;


/**
 * Fragment to pick images from local storage and upload to LeanCloud,
 * then display them via ViewPager2.
 */
public class UploadFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "UploadFragment";

    private static final String KEY_FILE_URI = "key_file_uri";
    private static final String KEY_DOWNLOAD_URL = "key_download_url";
    private static final Logger log = LoggerFactory.getLogger(UploadFragment.class);

    private FragmentUploadBinding binding;

    // 选择的图片 URI
    private Uri mFileUri = null;
    // 上传后获得的下载地址
    private Uri mDownloadUrl = null;

    private String currentUrl = "";

    // 用于存储用户图片的地址列表
    private List<String> imagePaths = new ArrayList<>();
    private List<String> downloadUrls = new ArrayList<>();

    // 向系统选择图片的 ActivityResultLauncher
    private ActivityResultLauncher<String[]> intentLauncher;

    private String deviceId;

    private boolean isPortrait = true;  // 默认纵向裁剪

    private View bottomNav;

    private ViewPager2 viewPager;
    private ImageView imageViewFrame;

    // 请求通知权限（仅示例）
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(getContext(), "Notifications permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(),
                            "Can't post notifications without POST_NOTIFICATIONS permission",
                            Toast.LENGTH_LONG).show();
                }
            });

    public UploadFragment() {
        // Required empty public constructor
    }

    // Inflate the layout for this fragment
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUploadBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    // Set up the Fragment after the view is created
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);


        viewPager = view.findViewById(R.id.viewPager);

        // 拿到 Activity 的 BottomNavigationView
        bottomNav = requireActivity().findViewById(R.id.bottom_nav);
        // 隐藏它
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);

        // 点击按钮：选择本地图片
        binding.buttonCamera.setOnClickListener(this);

        binding.buttonChoose.setOnClickListener(this);

        binding.buttonDelete.setOnClickListener(this);


        // 注册翻页回调
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position >= 0 && position < downloadUrls.size()) {
                    currentUrl = downloadUrls.get(position);
                    Log.d(TAG, getString(R.string.page_switch) + position + ", 图片 URI: " + currentUrl);
                }
            }
        });

        // 恢复之前的状态
        if (savedInstanceState != null){
            mFileUri = savedInstanceState.getParcelable(KEY_FILE_URI);
            mDownloadUrl = savedInstanceState.getParcelable(KEY_DOWNLOAD_URL);
        }

        // 注册获取图片的 ActivityResultLauncher
        intentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                fileUri -> {
                    if (fileUri != null){
                        startCrop(fileUri);  // 启动裁剪界面
//                        uploadFromUri(fileUri);
                    } else {
                        Log.w(TAG, "File URI is null");
                        Toast.makeText(getContext(), R.string.choose_no_file, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 请求通知权限（仅在 Android 13+）
        askNotificationPermission();

        // 获取传递的参数并设置标题
        Bundle args = getArguments();
        String type = "";
        if (args != null) {
            deviceId = args.getString("deviceId");
            String deviceName = args.getString("deviceName");
            type = args.getString("type");
            Log.d("UploadFragment", "Device ID: " + deviceId + ", Device Name: " + deviceName);
            Toast.makeText(getContext(), "Device ID: " + deviceId + ", Device Name: " + deviceName, Toast.LENGTH_LONG).show();
        }
//        binding.topAppBar.setTitle(type);

        // 如果想测试存数据
//        testLeanCloudSave();

        // 初始化 ViewPager2 的适配器
        ViewPager2 viewPager = binding.viewPager;
        ImageAdapter adapter = new ImageAdapter(requireContext(), new ArrayList<>());
        viewPager.setAdapter(adapter);

        // 判断当前用户是否已登录
        updateUI(LCUser.getCurrentUser());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 离开页面时再把底部栏显示回来
        if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_FILE_URI, mFileUri);
        outState.putParcelable(KEY_DOWNLOAD_URL, mDownloadUrl);
    }

    /**
     * Launches the image picker to select a photo
     */
    private void launchCamera() throws FileNotFoundException {
        Log.d(TAG, "launchCamera");
        // 打开系统文件选择器，过滤 image/*
        intentLauncher.launch(new String[]{"image/*"});
    }

    // 启动裁剪功能
    private void startCrop(Uri sourceUri) {
        // 创建目标裁剪的Uri
        Uri destinationUri = Uri.fromFile(new File(getActivity().getCacheDir(), "cropped_image.jpg"));

        // 设置裁剪比例
        if (isPortrait) {
            // 默认纵向裁剪 800x480
            UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(800, 480)  // 设置裁切框的比例
                    .withMaxResultSize(800, 480)  // 设置裁切后图片的最大尺寸
                    .start(getContext(), this);  // 启动裁剪活动
        } else {
            // 切换为横向裁剪 480x800
            UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(480, 800)  // 设置裁切框的比例
                    .withMaxResultSize(480, 800)  // 设置裁切后图片的最大尺寸
                    .start(getContext(), this);  // 启动裁剪活动
        }
    }




    // 处理裁剪结果
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                // 在这里处理裁剪后的图片，例如上传
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), resultUri);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Bitmap scaledBitmap;
                // 等比缩放图片到 800x480
                if(isPortrait){
                     scaledBitmap = scaleBitmap(bitmap, 800, 480);
                }
//                Bitmap scaledBitmap = scaleBitmap(bitmap, 800, 481);
                else {
                     scaledBitmap = scaleBitmap(bitmap, 480, 800);
                }
                // 在这里上传或保存图片
                uploadFromBitmap(scaledBitmap);
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Log.e(TAG, "Crop error: ", cropError);
                Toast.makeText(getContext(), getString(R.string.cut_fail), Toast.LENGTH_SHORT).show();
            }
        }
    }
    // 图片等比缩放
    private Bitmap scaleBitmap(Bitmap originalBitmap, int width, int height) {
        float ratioBitmap = (float) originalBitmap.getWidth() / (float) originalBitmap.getHeight();
        float ratioDesired = (float) width / (float) height;

        int scaledWidth = width;
        int scaledHeight = height;

        // 如果需要等比缩放
        if (ratioBitmap > ratioDesired) {
            scaledHeight = (int) (width / ratioBitmap);
        } else {
            scaledWidth = (int) (height * ratioBitmap);
        }

        return Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, false);
    }

    public void uploadFromBitmap(Bitmap bitmap) {
        try {
//            showProgressBar(getString(R.string.progress_uploading));
            // 将 Bitmap 转换为 byte[]
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream); // 压缩成 JPEG 格式，质量为 80
            byte[] data = byteArrayOutputStream.toByteArray();

            // 创建 LCFile 对象，用于上传
            LCFile lcFile = new LCFile("uploaded_image.jpg", data);  // 文件名可以根据需要动态生成

            // 异步上传文件
            lcFile.saveInBackground().subscribe(new Observer<LCFile>() {
                @Override
                public void onSubscribe(Disposable d) {
                    // 上传开始时的处理
                }

                @Override
                public void onNext(LCFile lcFile) {
                    // 上传成功的处理
                    Log.d(TAG, "File uploaded: " + lcFile.getUrl() + " | ObjectId: " + lcFile.getObjectId());

                    // 获取当前登录的用户
                    LCUser currentUser = LCUser.getCurrentUser();
                    if (currentUser != null) {

                        // 获取用户的 imagePaths 字段（如果没有则初始化为空列表）
                        List<String> imagePaths = currentUser.getList("imagePaths");
                        if (imagePaths == null) {
                            imagePaths = new ArrayList<>();
                        }

                        // 将新上传的文件 URL 添加到 imagePaths 中
                        imagePaths.add(lcFile.getUrl());

                        // 更新用户对象的 imagePaths 字段
                        currentUser.put("imagePaths", imagePaths);
                        Log.d(TAG, "File imagePaths: " + imagePaths);

                        // 保存更新后的用户对象
                        currentUser.saveInBackground().subscribe(
                                new Observer<LCObject>() {

                                    @Override
                                    public void onSubscribe(Disposable d) {
                                        
                                    }

                                    @Override
                                    public void onNext(LCObject lcObject) {
                                        Log.d(TAG, "User imagePaths updated.");
                                        Toast.makeText(getContext(), getString(R.string.url_add_to_user), Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        // 更新失败
                                        Log.e(TAG, "Failed to update user imagePaths", e);
                                        Toast.makeText(getContext(), getString(R.string.update_user_profile_fail), Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onComplete() {

                                    }
                                }
                        );
                    }

                    callCloudFunctionWithObservable(lcFile.getObjectId(),lcFile.getUrl());
                    mDownloadUrl = Uri.parse(lcFile.getUrl());  // 获取上传后的文件 URL
                    Toast.makeText(getContext(), getString(R.string.file_upload_ok), Toast.LENGTH_SHORT).show();
                    updateUI(currentUser);
                    // 你可以在这里保存文件的 URL 或进行其他操作
                }

                @Override
                public void onError(Throwable e) {
                    // 上传失败的处理
                    Log.e(TAG, "File upload failed", e);
                    Toast.makeText(getContext(), getString(R.string.file_update_fail), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onComplete() {
                    // 上传完成后的处理
                }
            });
        } catch (Exception e) {
            // 捕获转换和上传过程中可能的异常
            Log.e(TAG, "Error uploading bitmap", e);
            Toast.makeText(getContext(), getString(R.string.file_update_fail), Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteFile(String fileUrl) {
        try {
//            showProgressBar(getString(R.string.progress_deleting));

            // 获取当前登录的用户
            LCUser currentUser = LCUser.getCurrentUser();
            if (currentUser != null) {
                // 获取用户的 imagePaths 字段（如果没有则初始化为空列表）
                List<String> imagePaths = currentUser.getList("imagePaths");
                if (imagePaths != null && imagePaths.contains(fileUrl)) {
                    // 删除 imagePaths 中对应的 URL
                    imagePaths.remove(fileUrl);

                    // 更新用户对象的 imagePaths 字段
                    currentUser.put("imagePaths", imagePaths);

                    // 保存更新后的用户对象
                    currentUser.saveInBackground().subscribe(new Observer<LCObject>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            // 上传开始时的处理
                        }

                        @Override
                        public void onNext(LCObject lcObject) {
                            Log.d(TAG, "User imagePaths updated after deletion.");
                            Toast.makeText(getContext(), getString(R.string.file_delete_from_user), Toast.LENGTH_SHORT).show();
                            updateUI(currentUser); // 更新 UI
                        }

                        @Override
                        public void onError(Throwable e) {
                            // 更新失败
                            Log.e(TAG, "Failed to update user imagePaths after deletion", e);
                            Toast.makeText(getContext(), getString(R.string.update_user_profile_fail), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onComplete() {
                            // 操作完成后的处理
                        }
                    });
                    deleteFile_from_url(fileUrl); //删除图片文件
                    findBinUrlByUrl(fileUrl,true); //删除bin文件
                    deleteBinUrlByUrl(fileUrl);
                } else {
                    Log.w(TAG, "No matching URL found in user imagePaths.");
                    Toast.makeText(getContext(), getString(R.string.url_no_exit), Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "No current user found");
                Toast.makeText(getContext(), getString(R.string.no_logoin), Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            // 捕获删除过程中的异常
            Log.e(TAG, "Error deleting file", e);
            Toast.makeText(getContext(), getString(R.string.file_delete_fail), Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteBinUrlByUrl(String url) {
        try {
            // 创建查询对象，查询 bin_url 表
            LCQuery<LCObject> query = new LCQuery<>("bin_url");

            // 根据 url 字段查找对应记录
            query.whereEqualTo("url", url);

            // 执行查询并删除记录
            query.findInBackground().subscribe(new Observer<List<LCObject>>() {
                @Override
                public void onSubscribe(Disposable d) {}

                @Override
                public void onNext(List<LCObject> results) {
                    if (results != null && !results.isEmpty()) {
                        // 获取查询结果中的第一个对象
                        LCObject binUrlObject = results.get(0);

                        // 删除该记录
                        binUrlObject.deleteInBackground().subscribe(new Observer<LCNull>() {
                            @Override
                            public void onSubscribe(Disposable d) {}

                            @Override
                            public void onNext(LCNull lcNull) {
                                // 删除成功
                                Log.d("CloudFunction", "Record with URL " + url + " deleted successfully.");
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e("CloudFunction", "Error deleting record: " + e.getMessage());
                            }

                            @Override
                            public void onComplete() {}
                        });
                    } else {
                        Log.d("CloudFunction", "No matching record found for URL: " + url);
                    }
                }

                @Override
                public void onError(Throwable e) {
                    Log.e("CloudFunction", "Error querying bin_url: " + e.getMessage());
                }

                @Override
                public void onComplete() {}
            });
        } catch (Exception e) {
            Log.e("CloudFunction", "Error: " + e.getMessage());
        }
    }


    public void deleteFile_from_url(String fileUrl) {
        LCUser currentUser = LCUser.getCurrentUser();
        Log.i(TAG, "deleteFile: "+fileUrl);
        int startIndex = fileUrl.indexOf("lcfile.com/") + "lcfile.com/".length();
        String key = fileUrl.substring(startIndex);
        Log.i(TAG, "deleteFile_from_url: key"+key);

        LCQuery<LCObject> query = new LCQuery<>("_File");
        query.whereEqualTo("key", key );
        query.findInBackground().subscribe(new Observer<List<LCObject>>() {
            public void onSubscribe(Disposable disposable) {}
            public void onNext(List<LCObject> students) {
                // students 是包含满足条件的 Student 对象的数组
                Log.i(TAG, "onNext: success" + students);
                if (students.isEmpty()) {
                    Log.w(TAG, "No matching record found for URL: " + fileUrl);
                    return;
                }
                String id = students.get(0).getObjectId();
                LCObject file = LCObject.createWithoutData("_File", id);
                file.deleteInBackground().subscribe(new Observer<LCNull>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        // 删除开始时的处理
                    }

                    @Override
                    public void onNext(LCNull response) {
                        Log.d(TAG, "File deleted successfully: " + fileUrl);
                        Toast.makeText(getContext(), getString(R.string.file_delete_ok), Toast.LENGTH_SHORT).show();
                        updateUI(currentUser); // 更新 UI
                    }

                    @Override
                    public void onError(Throwable e) {
                        // 删除失败
                        Log.e(TAG, "File deletion failed", e);
                        Toast.makeText(getContext(), getString(R.string.file_delete_fail), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {
                        // 删除完成后的处理
                    }
                });

            }
            public void onError(Throwable throwable) {}
            public void onComplete() {}
        });
    }

    public void changeCurrentUrl(String id, String currentUrl){
        // 创建查询对象，查找 Device 表中的 deviceId 字段
        LCQuery<LCObject> query = new LCQuery<>("Device");
        query.whereEqualTo("deviceId", id); // 使用 device.getDeviceId() 获取设备 ID

        query.findInBackground().subscribe(new Observer<List<LCObject>>() {
            @Override
            public void onSubscribe(Disposable disposable) {
                // 订阅开始时的操作
            }

            @Override
            public void onNext(List<LCObject> devices) {
                // 查找到满足条件的设备对象
                if (!devices.isEmpty()) {
                    // 假设查询返回的是一个设备列表，取第一个设备对象
                    LCObject deviceObject = devices.get(0);

                    // 修改 mode 字段
                    deviceObject.put("currentUrl", currentUrl);  // 将 "新的模式" 替换为你想设置的值

                    // 保存修改
                    deviceObject.saveInBackground().subscribe(new Observer<LCObject>() {
                        @Override
                        public void onSubscribe(Disposable disposable) {
                            // 处理保存时的操作
                        }

                        @Override
                        public void onNext(LCObject lcObject) {
                            // 成功保存后的操作
                            Log.d("TAG", "Device mode updated successfully");
//                            fetchDeviceList();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            // 保存失败时的操作
                            Log.e("TAG", "Failed to update device mode", throwable);
                        }

                        @Override
                        public void onComplete() {
                            // 操作完成时的处理
                        }
                    });
                } else {
                    // 如果没有找到符合条件的设备
                    Log.w("TAG", "No device found with the specified deviceId");
                }
            }

            @Override
            public void onError(Throwable throwable) {
                // 查询失败时的处理
                Log.e("TAG", "Failed to query devices", throwable);
            }

            @Override
            public void onComplete() {
                // 查询完成后的操作
            }
        });
    }

    //云函数
    @SuppressLint("CheckResult")
    public void callCloudFunctionWithObservable(String id,String url) {
        // Get the current user (if user authentication is required)
        LCUser currentUser = LCUser.getCurrentUser();

        // Create a parameter map
        Map<String, Object> params = new HashMap<>();
        params.put("fileId", id);

        // Call the cloud function and return Observable
        Observable<Object> observable = LCCloud.callFunctionInBackground(currentUser, "convert_image_to_bin", params);

        // Subscribe to the Observable and handle the cloud function result
        observable
                .subscribeOn(Schedulers.io()) // Make the network request on the IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Update UI on the main thread
                .subscribe(
                        result -> {
                            // Handle the successful result from the cloud function
                            Log.d("CloudFunction", "Result: " + result.toString());
                            String regex = "binFileUrl=([\\S]+)";

                            // 创建 Pattern 对象
                            Pattern pattern = Pattern.compile(regex);

                            // 创建 Matcher 对象
                            Matcher matcher = pattern.matcher(result.toString());
                            String binFileUrl ="";
                            if (matcher.find()) {
                                // 提取匹配到的 URL
                                binFileUrl = matcher.group(1);
                                // 检查 binFileUrl 是否以逗号结尾，如果是则删除最后一个字符
                                if (binFileUrl.endsWith(",")) {
                                    binFileUrl = binFileUrl.substring(0, binFileUrl.length() - 1);
                                }

                                Log.i(TAG, "callCloudFunctionWithObservable: Extracted URL: " + binFileUrl);
                            } else {
                                System.out.println("No match found.");
                            }
                            insertBinUrl(url,binFileUrl);
                        },
                        throwable -> {
                            // Handle the error from the cloud function
                            Log.e("CloudFunction", "Error: " + throwable.getMessage());
                        }
                );
    }


    public void insertBinUrl(String url, String binUrl) {
        try {
            // 创建一个新的 LCObject 对象，假设是 bin_url 表
            LCObject binUrlObject = new LCObject("bin_url");

            // 将 url 和 bin_url 字段设置为传入的值
            binUrlObject.put("url", url);
            binUrlObject.put("bin_url", binUrl);

            // 保存到 LeanCloud bin_url 表
            binUrlObject.saveInBackground().subscribe(new Observer<LCObject>() {
                @Override
                public void onSubscribe(Disposable d) {}

                @Override
                public void onNext(LCObject savedObject) {
                    Log.d("CloudFunction", "Data inserted successfully: " + savedObject.toString());
                }

                @Override
                public void onError(Throwable e) {
                    Log.e("CloudFunction", "Error inserting data: " + e.getMessage());
                }

                @Override
                public void onComplete() {}
            });
        } catch (Exception e) {
            Log.e("CloudFunction", "Error: " + e.getMessage());
        }
    }


    /**
     * Fetches the current user's image paths from LeanCloud and loads them into ViewPager2
     */
    private void fetchUserImagePaths() {
        LCUser currentUser = LCUser.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "fetchUserImagePaths: user is null");
//            hideProgressBar();
            return;
        }

        // 重新查询最新的用户数据
        currentUser.fetchInBackground().subscribe(new Observer<LCObject>() {
            @Override
            public void onSubscribe(Disposable d) {}

            @Override
            public void onNext(LCObject lcObject) {
                // 成功获取最新的用户数据
                List<String> paths = lcObject.getList("imagePaths");
                if (paths != null && !paths.isEmpty()) {
                    Log.i(TAG, "fetchUserImagePaths: " + paths);
                    imagePaths = paths;
                    // 这一步其实 LeanCloud 就直接给了 URL，无需再次“下载URL”
                    // 你可以直接用 imagePaths 作为下载URL
                    downloadUrls.clear();
                    downloadUrls.addAll(imagePaths);
                    updateViewPager(downloadUrls);
                } else {
                    Log.d(TAG, "No image paths found for user.");
                    imagePaths.clear();
                    downloadUrls.clear();
                    updateViewPager(downloadUrls);
                }
//                hideProgressBar();
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "Error fetching user data", e);
//                hideProgressBar();
                Toast.makeText(getContext(), getString(R.string.get_user_date_fail), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {}
        });
    }

    /**
     * Updates ViewPager2 with the list of image download URLs
     */
    private void updateViewPager(List<String> urls) {
        Log.i(TAG, "updateViewPager: " + urls);
        ViewPager2 viewPager = binding.viewPager;
        ImageAdapter adapter = (ImageAdapter) viewPager.getAdapter();
        if (adapter != null) {
            adapter.setImageUrls(urls);
            binding.layoutDownload.setVisibility(View.VISIBLE);
            binding.layoutChoosePhoto.setVisibility(View.VISIBLE);
//            binding.layoutChoosehengshu.setVisibility(View.VISIBLE);
            binding.layoutDelete.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Requests notification permission for Android 13+
     */
    private void askNotificationPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED){
                // Your app can post notifications.
            } else {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /**
     * Updates the UI based on the user's authentication state (LeanCloud)
     */
    private void updateUI(LCUser user){
        binding.layoutStorage.setVisibility(View.VISIBLE);
        fetchUserImagePaths();
        if (user != null) {
            // 用户已登录
            binding.layoutStorage.setVisibility(View.VISIBLE);
            // 获取该用户的图片
            fetchUserImagePaths();
        } else {
            Log.i(TAG, "updateUI: failed");
            binding.layoutStorage.setVisibility(View.GONE);
        }
    }

    public void findBinUrlByUrl(String url,boolean isDelete) {
        try {
            // 创建查询对象，查询 bin_url 表
            LCQuery<LCObject> query = new LCQuery<>("bin_url");
//            Student = leancloud.Object.extend('Student')

            // 根据 url 字段查找对应记录
            query.whereEqualTo("url", url);
//            String bin_Url;

            // 执行查询
            query.findInBackground().subscribe(new Observer<List<LCObject>>() {
                @Override
                public void onSubscribe(Disposable d) {}

                @Override
                public void onNext(List<LCObject> results) {
                    if (results != null && !results.isEmpty()) {
                        // 获取查询结果中的第一个对象
                        LCObject binUrlObject = results.get(0);

                        // 获取 bin_url 字段的值
                        String binUrl = binUrlObject.getString("bin_url");


                        if(isDelete){
                            deleteFile_from_url(binUrl.substring(0,binUrl.length()-1));
                        }else{
                            changeCurrentUrl(deviceId,binUrl);
                        }

                        // 打印或返回 bin_url
                        Log.d("CloudFunction", "Found bin_url: " + binUrl);
                    } else {
                        Log.d("CloudFunction", "No matching record found.");
                    }
                }

                @Override
                public void onError(Throwable e) {
                    Log.e("CloudFunction", "Error querying bin_url: " + e.getMessage());
                }

                @Override
                public void onComplete() {}
            });
        } catch (Exception e) {
            Log.e("CloudFunction", "Error: " + e.getMessage());
        }

    }


    private void updataUrl(){
        Log.i(TAG, "updataUrl: "+deviceId);
//        changeCurrentUrl(deviceId,currentUrl);
        findBinUrlByUrl(currentUrl,false);
        Log.i(TAG, "updataUrl: "+currentUrl);
        Toast.makeText(getContext(), getString(R.string.my_update_ok), Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onClick(View v){
        int i = v.getId();
        Log.i(TAG, "onClick: "+i);
        if(i == R.id.buttonCamera){
            try {
                launchCamera();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

         else if (i == R.id.button_choose) {
             Log.i(TAG, "onClick: choose");
             updataUrl();
         }
//         else if (i == R.id.button_toggle_orientation) {
//             toggleOrientation();
//         }
         else if(i == R.id.button_delete){
             deleteFile(currentUrl); //去掉http://
        }
        // else if (i == R.id.buttonDownload) {...}
    }
}
