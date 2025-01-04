package com.example.aura_demo;

import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service to handle uploading files to Firebase Storage.
 */
public class MyUploadService extends MyBaseTaskService {

    private static final String TAG = "MyUploadService";

    /** Intent Actions **/
    public static final String ACTION_UPLOAD = "action_upload";
    public static final String UPLOAD_COMPLETED = "upload_completed";
    public static final String UPLOAD_ERROR = "upload_error";

    /** Intent Extras **/
    public static final String EXTRA_FILE_URI = "extra_file_uri";
    public static final String EXTRA_DOWNLOAD_URL = "extra_download_url";

    public static final String EXTRA_FILE_URI_RESULT = "extra_file_uri";

    // [START declare_ref]
    private StorageReference mStorageRef;
    // [END declare_ref]

    @Override
    public void onCreate() {
        super.onCreate();

        // [START get_storage_ref]
        mStorageRef = FirebaseStorage.getInstance().getReference();
        // [END get_storage_ref]
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand:" + intent + ":" + startId);
        if (ACTION_UPLOAD.equals(intent.getAction())) {
            Uri fileUri = intent.getParcelableExtra(EXTRA_FILE_URI);

            // Make sure we have permission to read the data
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                assert fileUri != null;
                getContentResolver().takePersistableUriPermission(
                        fileUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            assert fileUri != null;
            uploadFromUri(fileUri);
        }

        return START_REDELIVER_INTENT;
    }

    // [START upload_from_uri]
    private void uploadFromUri(final Uri fileUri) {
        Log.d(TAG, "uploadFromUri:src:" + fileUri.toString());

        // 获取当前用户
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User is not authenticated");
            stopSelf();
            return;
        }

        String userId = user.getUid();

        // 获取文件名
        String fileName = getFileName(fileUri);
        if (fileName == null) {
            fileName = "image_" + UUID.randomUUID().toString() + ".jpg";
        }

        // 构建存储路径
        String storagePath = userId + "/" + fileName;

        // [START_EXCLUDE]
        taskStarted();
        showProgressNotification(getString(R.string.progress_uploading), 0, 0);
        // [END_EXCLUDE]

        // [START get_child_ref]
        // Get a reference to store file at photos/<FILENAME>.jpg
//        final StorageReference photoRef = mStorageRef.child("photos")
//                .child(fileUri.getLastPathSegment());

        final StorageReference photoRef = mStorageRef.child(storagePath);
        // [END get_child_ref]

        // Upload file to Firebase Storage
        Log.d(TAG, "uploadFromUri:dst:" + photoRef.getPath());
        photoRef.putFile(fileUri).
                addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        showProgressNotification(getString(R.string.progress_uploading),
                                taskSnapshot.getBytesTransferred(),
                                taskSnapshot.getTotalByteCount());
                    }
                })
                .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        // Forward any exceptions
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        // 更新 Firestore 中的 imagePaths 字段
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        // 创建更新数据
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("imagePaths", FieldValue.arrayUnion(storagePath));

                        db.collection("users").document(userId)
                                .set(updates, SetOptions.merge());


                        Log.d(TAG, "uploadFromUri: upload success");

                        // Request the public download URL
                        return photoRef.getDownloadUrl();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(@NonNull Uri downloadUri) {
                        // Upload succeeded
                        Log.d(TAG, "uploadFromUri: getDownloadUri success");

                        // [START_EXCLUDE]
                        broadcastUploadFinished(downloadUri, fileUri);
                        showUploadFinishedNotification(downloadUri, fileUri);
                        taskCompleted();
                        // [END_EXCLUDE]
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Upload failed
                        Log.w(TAG, "uploadFromUri:onFailure", exception);

                        // [START_EXCLUDE]
                        broadcastUploadFinished(null, fileUri);
                        showUploadFinishedNotification(null, fileUri);
                        taskCompleted();
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END upload_from_uri]

    /**
     * Broadcast finished upload (success or failure).
     */
    private void broadcastUploadFinished(@Nullable Uri downloadUrl, @Nullable Uri fileUri) {
        boolean success = downloadUrl != null;

        String action = success ? UPLOAD_COMPLETED : UPLOAD_ERROR;

        Intent broadcast = new Intent(action)
                .putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
                .putExtra(EXTRA_FILE_URI, fileUri);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(broadcast);
    }

    /**
     * Show a notification for a finished upload.
     */
    private void showUploadFinishedNotification(@Nullable Uri downloadUrl, @Nullable Uri fileUri) {
        // Hide the progress notification
        dismissProgressNotification();

        // Make Intent to MainActivity
        Intent intent = new Intent(this, MainActivity.class)
                .putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
                .putExtra(EXTRA_FILE_URI, fileUri)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        boolean success = downloadUrl != null;
        String caption = success ? getString(R.string.upload_success) : getString(R.string.upload_failure);
        showFinishedNotification(caption, intent, success);
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPLOAD_COMPLETED);
        filter.addAction(UPLOAD_ERROR);

        return filter;
    }

    private void handleUpload(Uri fileUri) {
        // 获取当前用户
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User is not authenticated");
            stopSelf();
            return;
        }

        String userId = user.getUid();

        // 获取文件名
        String fileName = getFileName(fileUri);
        if (fileName == null) {
            fileName = "image_" + UUID.randomUUID().toString() + ".jpg";
        }

        // 构建存储路径
        String storagePath = userId + "/" + fileName;
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(storagePath);

        // 开始上传文件
        UploadTask uploadTask = storageRef.putFile(fileUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // 获取下载 URL
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();

                // 更新 Firestore 中的 imagePaths 字段
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                // 创建更新数据
                Map<String, Object> updates = new HashMap<>();
                updates.put("imagePaths", FieldValue.arrayUnion(storagePath));

                db.collection("users").document(userId)
                        .set(updates, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Image path added to Firestore");

                            // 发送上传成功的广播
                            Intent broadcastIntent = new Intent("com.example.aura_demo.UPLOAD_COMPLETE");
                            broadcastIntent.putExtra("storagePath", storagePath);
                            broadcastIntent.putExtra("downloadUrl", downloadUrl);
                            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

                            stopSelf();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating Firestore", e);

                            // 发送上传失败的广播
                            Intent broadcastIntent = new Intent("com.example.aura_demo.UPLOAD_FAILED");
                            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

                            stopSelf();
                        });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get download URL", e);
                stopSelf();
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Upload failed", e);

            // 发送上传失败的广播
            Intent broadcastIntent = new Intent("com.example.aura_demo.UPLOAD_FAILED");
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

            stopSelf();
        });
    }

    /**
     * 获取文件名
     */
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

}
