package com.example.hands_free;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.hands_free.databinding.ActivityCollectorNavigationBinding;

public class CollectorNavigationActivity extends AppCompatActivity {

    ActivityCollectorNavigationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCollectorNavigationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        replaceFragment(new CollectorHomeFragment());

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navHome) {
                replaceFragment(new CollectorHomeFragment());
            } else if (itemId == R.id.navRouting) {
                replaceFragment(new CollectorRoutingFragment());
            } else if (itemId == R.id.navStores) {
                replaceFragment(new CollectorStoresAssignedFragment());
            } else if (itemId == R.id.navProfile) {
                replaceFragment(new CollectorAccountFragment());
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
