package com.example.hands_free;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.hands_free.databinding.ActivityShopperNavigationBinding;

public class ShopperNavigationActivity extends AppCompatActivity {

    ActivityShopperNavigationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShopperNavigationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        replaceFragment(new ShopperQRFragment());

        //handle bottom menu
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.home) {
                // Handle home page click
                replaceFragment(new ShopperQRFragment());

            } else if (itemId == R.id.item) {
                // Handle item page click
                replaceFragment(new ShopperItemFragment());

            } else if (itemId == R.id.mallMap) {
                // Handle mall map page click
                replaceFragment(new ShopperMallMapFragment());

            } else if (itemId == R.id.Sittings) {
                // Handle settings click
                replaceFragment(new ShopperAccountFragment());
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