package com.example.aura_demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.aura_demo.databinding.FragmentUserProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserProfileFragment extends Fragment {

    private FragmentUserProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            binding.tvUserInfo.setText(String.format("Email: %s\nUID: %s", user.getEmail(), user.getUid()));
        }

        binding.btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                NavHostFragment.findNavController(UserProfileFragment.this)
                        .navigate(R.id.action_userProfileFragment_to_emailPasswordFragment);
                // Optionally navigate back or handle logout
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;  // Clean up binding when view is destroyed
    }
}