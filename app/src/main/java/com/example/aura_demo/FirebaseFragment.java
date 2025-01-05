package com.example.aura_demo;

// FirebaseFragment.java

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseFragment extends Fragment {

    private TextView textViewTest;
    private EditText editTextInput;
    private Button buttonUpload;

    // Firebase Realtime Database 的引用
    private DatabaseReference databaseReference;

    // 指定的 key
    private static final String SPECIFIC_KEY = "your_specific_key"; // 替换为您希望使用的 key

    public FirebaseFragment() {
        // Required empty public constructor
    }

    public static FirebaseFragment newInstance() {
        return new FirebaseFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_firebase, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        // 初始化视图
        textViewTest = view.findViewById(R.id.textViewTest);
        editTextInput = view.findViewById(R.id.editTextInput);
        buttonUpload = view.findViewById(R.id.buttonUpload);

        // 初始化 Firebase Realtime Database 引用
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // 读取指定 key 的数据
        readData();

        // 设置按钮点击事件
        buttonUpload.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                uploadData();
            }
        });
    }

    // 读取指定 key 的数据
    private void readData(){
        databaseReference.child(SPECIFIC_KEY).addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                if(snapshot.exists()){
                    String data = snapshot.getValue(String.class);
                    textViewTest.setText(data);
                } else {
                    textViewTest.setText("无数据");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error){
                Toast.makeText(getContext(), "读取数据失败: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 上传输入框的数据到指定 key
    private void uploadData(){
        String input = editTextInput.getText().toString().trim();

        if(TextUtils.isEmpty(input)){
            Toast.makeText(getContext(), "请输入要上传的数据", Toast.LENGTH_SHORT).show();
            return;
        }

        // 将数据写入 Firebase Realtime Database
        databaseReference.child(SPECIFIC_KEY).setValue(input)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        Toast.makeText(getContext(), "数据上传成功", Toast.LENGTH_SHORT).show();
                        editTextInput.setText("");
                    } else {
                        Toast.makeText(getContext(), "数据上传失败: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

