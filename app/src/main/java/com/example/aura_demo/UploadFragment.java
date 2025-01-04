package com.example.aura_demo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.example.aura_demo.databinding.FragmentUploadBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment to upload and download photos from Firebase Storage.
 *
 * See {@link MyUploadService} for upload example.
 * See {@link MyDownloadService} for download example.
 */
public class UploadFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "UploadFragment";

    private static final String KEY_FILE_URI = "key_file_uri";
    private static final String KEY_DOWNLOAD_URL = "key_download_url";

    private FirebaseAuth mAuth;

    private Uri mDownloadUrl = null;
    private Uri mFileUri = null;

    private FragmentUploadBinding binding;

    private ActivityResultLauncher<String[]> intentLauncher;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(getContext(), "Notifications permission granted", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(getContext(),
                            "Can't post notifications without POST_NOTIFICATIONS permission",
                            Toast.LENGTH_LONG).show();
                }
            });

    // BroadcastReceiver to handle upload and download events
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
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

    public UploadFragment() {
        // Required empty public constructor
    }

    // Inflate the layout for this fragment
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        binding = FragmentUploadBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    // Set up the Fragment after the view is created
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Click listeners
        binding.buttonCamera.setOnClickListener(this);
        binding.buttonSignIn.setOnClickListener(this);
//        binding.buttonDownload.setOnClickListener(this);

        // Restore instance state
        if (savedInstanceState != null){
            mFileUri = savedInstanceState.getParcelable(KEY_FILE_URI);
            mDownloadUrl = savedInstanceState.getParcelable(KEY_DOWNLOAD_URL);
        }

        // Register ActivityResultLauncher for picking images
        intentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), fileUri -> {
                    if (fileUri != null){
                        uploadFromUri(fileUri);
                    } else {
                        Log.w(TAG, "File URI is null");
                        Toast.makeText(getContext(), "未选择任何文件", Toast.LENGTH_SHORT).show();
                    }
                });

        // Request notification permissions
        askNotificationPermission();
    }

    @Override
    public void onStart(){
        super.onStart();
        updateUI(mAuth.getCurrentUser());

        // Register receiver for uploads and downloads
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(requireContext());
        manager.registerReceiver(mBroadcastReceiver, MyDownloadService.getIntentFilter());
        manager.registerReceiver(mBroadcastReceiver, MyUploadService.getIntentFilter());
    }

    @Override
    public void onStop(){
        super.onStop();

        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_FILE_URI, mFileUri);
        outState.putParcelable(KEY_DOWNLOAD_URL, mDownloadUrl);
    }

    /**
     * Starts the upload process from the given URI
     */
    private void uploadFromUri(Uri fileUri){
        Log.d(TAG, "uploadFromUri:src:" + fileUri.toString());

        // Save the File URI
        mFileUri = fileUri;

        // Clear the last download, if any
        updateUI(mAuth.getCurrentUser());
        mDownloadUrl = null;

        // Start MyUploadService to upload the file, so that the file is uploaded
        // even if this Fragment is killed or put in the background
        Intent uploadIntent = new Intent(requireContext(), MyUploadService.class)
                .putExtra(MyUploadService.EXTRA_FILE_URI, fileUri)
                .setAction(MyUploadService.ACTION_UPLOAD);
        requireContext().startService(uploadIntent);

        // Show loading spinner
        showProgressBar(getString(R.string.progress_uploading));
    }

    /**
     * Starts the download process (Example)
     */
    private void beginDownload(){
        if(mFileUri == null){
            Toast.makeText(getContext(), "No file selected for download", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get path
        String path = "photos/" + mFileUri.getLastPathSegment();

        // Kick off MyDownloadService to download the file
        Intent intent = new Intent(requireContext(), MyDownloadService.class)
                .putExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH, path)
                .setAction(MyDownloadService.ACTION_DOWNLOAD);
        requireContext().startService(intent);

        // Show loading spinner
        showProgressBar(getString(R.string.progress_downloading));
    }

    /**
     * Launches the image picker to select a photo
     */
    private void launchCamera(){
        Log.d(TAG, "launchCamera");

        // Pick an image from storage
        intentLauncher.launch(new String[]{"image/*"});
    }

    /**
     * Signs in anonymously using Firebase Authentication
     */
    private void signInAnonymously(){
        // Sign in anonymously. Authentication is required to read or write from Firebase Storage.
        showProgressBar(getString(R.string.progress_auth));
        mAuth.signInAnonymously()
                .addOnSuccessListener(getActivity(), new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult){
                        Log.d(TAG, "signInAnonymously:SUCCESS");
                        hideProgressBar();
                        updateUI(authResult.getUser());
                        // After sign-in, fetch user image paths
                        fetchUserImagePaths();
                    }
                })
                .addOnFailureListener(getActivity(), new OnFailureListener(){
                    @Override
                    public void onFailure(@NonNull Exception exception){
                        Log.e(TAG, "signInAnonymously:FAILURE", exception);
                        hideProgressBar();
                        updateUI(null);
                    }
                });
    }

    /**
     * Handles the result intent from the upload service
     */
    private void onUploadResultIntent(Intent intent){
        // Got a new intent from MyUploadService with a success or failure
        mDownloadUrl = intent.getParcelableExtra(MyUploadService.EXTRA_DOWNLOAD_URL);
        mFileUri = intent.getParcelableExtra(MyUploadService.EXTRA_FILE_URI_RESULT);

        updateUI(mAuth.getCurrentUser());
    }

    /**
     * Updates the UI based on the user's authentication state
     */
    private void updateUI(FirebaseUser user){
        // Update UI based on user login state
        if(user != null){
            binding.layoutSignin.setVisibility(View.GONE);
            binding.layoutStorage.setVisibility(View.VISIBLE);
        }
        else{
            binding.layoutSignin.setVisibility(View.VISIBLE);
            binding.layoutStorage.setVisibility(View.GONE);
        }

        // Initialize ViewPager2
        ViewPager2 viewPager = binding.viewPager;
        // Initialize adapter with empty list
        ImageAdapter adapter = new ImageAdapter(requireContext(), new ArrayList<>());
        viewPager.setAdapter(adapter);

        // If user is logged in, fetch user image paths
        if(user != null){
            Log.i(TAG, "updateUI: user");
            fetchUserImagePaths();
        }
    }

    /**
     * Fetches the current user's image paths from Firestore and loads them into ViewPager2
     */
    private void fetchUserImagePaths(){
        FirebaseUser user = mAuth.getCurrentUser();
        if(user == null){
            Log.w(TAG, "fetchUserImagePaths: user is null");
            return;
        }

        String userId = user.getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDocRef = db.collection("users").document(userId);

        // Fetch the user document
        userDocRef.get()
                .addOnSuccessListener(new OnSuccessListener<com.google.firebase.firestore.DocumentSnapshot>() {
                    @Override
                    public void onSuccess(com.google.firebase.firestore.DocumentSnapshot documentSnapshot){
                        if(documentSnapshot.exists()){
                            List<String> imagePaths = (List<String>) documentSnapshot.get("imagePaths");
                            if(imagePaths != null && !imagePaths.isEmpty()){
                                // Fetch download URLs
                                Log.i(TAG, "fetchUserImagePaths: "+imagePaths);
                                fetchDownloadUrls(imagePaths);
                            }
                            else{
                                Log.d(TAG, "No image paths found for user.");
                                Toast.makeText(getContext(), "没有找到上传的图片", Toast.LENGTH_SHORT).show();
                                // Clear ViewPager2
                                ViewPager2 viewPager = binding.viewPager;
                                ImageAdapter adapter = (ImageAdapter) viewPager.getAdapter();
                                if(adapter != null){
                                    adapter.setImageUrls(new ArrayList<>());
                                }
                            }
                        }
                        else{
                            Log.d(TAG, "User document does not exist.");
                            Toast.makeText(getContext(), "用户文档不存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener(){
                    @Override
                    public void onFailure(@NonNull Exception e){
                        Log.e(TAG, "Error fetching user document", e);
                        Toast.makeText(getContext(), "获取用户数据失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Fetches download URLs from Firebase Storage based on the image paths
     */
    private void fetchDownloadUrls(List<String> imagePaths){
        FirebaseStorage storage = FirebaseStorage.getInstance();
        List<String> downloadUrls = new ArrayList<>();
        int total = imagePaths.size();
        final int[] count = {0};

        for(String path : imagePaths){
            StorageReference storageRef = storage.getReference().child(path);
            storageRef.getDownloadUrl()
                    .addOnSuccessListener(new OnSuccessListener<Uri>(){
                        @Override
                        public void onSuccess(Uri uri){
                            downloadUrls.add(uri.toString());
                            count[0]++;
                            if(count[0] == total){
                                // All download URLs fetched, update ViewPager2
                                updateViewPager(downloadUrls);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener(){
                        @Override
                        public void onFailure(@NonNull Exception e){
                            Log.e(TAG, "Failed to get download URL for path: " + path, e);
                            count[0]++;
                            if(count[0] == total){
                                // All download URLs fetched, update ViewPager2
                                updateViewPager(downloadUrls);
                            }
                        }
                    });
        }
    }

    /**
     * Updates ViewPager2 with the list of image download URLs
     */
    private void updateViewPager(List<String> downloadUrls){
        Log.i(TAG, "updateViewPager: "+downloadUrls);
        ViewPager2 viewPager = binding.viewPager;
        ImageAdapter adapter = (ImageAdapter) viewPager.getAdapter();
        if(adapter != null){
            adapter.setImageUrls(downloadUrls);
            Log.i(TAG, "updateViewPager: success");
            binding.layoutDownload.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Shows a message dialog with the given title and message
     */
    private void showMessageDialog(String title, String message){
        AlertDialog ad = new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .create();
        ad.show();
    }

    /**
     * Shows the progress bar with the given caption
     */
    private void showProgressBar(String caption){
        binding.caption.setText(caption);
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the progress bar
     */
    private void hideProgressBar(){
        binding.caption.setText("");
        binding.progressBar.setVisibility(View.INVISIBLE);
    }

    /**
     * Requests notification permission for Android 13+
     */
    private void askNotificationPermission(){
        // This is only necessary for API level >= 33 (TIRAMISU)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED){
                // Your app can post notifications.
            }
            else{
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu){
//        // If using a toolbar, you can inflate menu here
//        // getActivity().getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

    @Override
    public void onClick(View v){
        int i = v.getId();
        if(i == R.id.buttonCamera){
            launchCamera();
        }
        else if(i == R.id.buttonSignIn){
            signInAnonymously();
        }
        else if(i == R.id.buttonDownload){
            beginDownload();
        }
    }
}
