package com.example.hands_free;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.hands_free.databinding.ActivityStoreNavigationBinding;
import com.example.hands_free.databinding.ActivityStoreNavigationBinding;

public class StoreNavigationActivity extends AppCompatActivity {

    ActivityStoreNavigationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityStoreNavigationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        replaceFragment(new StoreScanQRFragment());

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navScanQr) {
                replaceFragment(new StoreScanQRFragment());
            } else if (itemId == R.id.navProfile_page) {
                replaceFragment(new StoreAccountFragment());
            }
            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }
}
