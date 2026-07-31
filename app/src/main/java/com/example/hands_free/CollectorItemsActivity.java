package com.example.hands_free;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.hands_free.databinding.ActivityCollectorItemsBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CollectorItemsActivity extends AppCompatActivity {

    //Attribute
    private String storeName, collectorEmail;
    private TextView totalItems, inTheStore, collectedTextview, reachedTextview;
    private ImageView storeLogoImage;
    private ActivityCollectorItemsBinding binding;
    private DatabaseReference itemsRef;
    private LinearLayout cardContainer;
    private List<String> serialNumbers = new ArrayList<>(), statuses = new ArrayList<>();
    private Button allButton, reachedButton, collectedButton, storeButton, searchButton;
    private String currentFilter = "all"; // Default filter
    private EditText searchEditText;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCollectorItemsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize
        cardContainer = binding.cardContainer;
        storeLogoImage = binding.storeLogoImage;
        totalItems = binding.totalItems;
        inTheStore = binding.inTheStore;
        collectedTextview = binding.collectedTextview;
        reachedTextview = binding.reachedTextview;
        searchEditText = binding.searchEditText;
        searchButton = binding.searchButton;
        backButton = binding.backButton;
        itemsRef = FirebaseDatabase.getInstance().getReference("Items");
        allButton = binding.allButton;
        reachedButton = binding.reachedButton;
        collectedButton = binding.collectedButton;
        storeButton = binding.storeButton;

        // Get data from intent
        Intent intent = getIntent();
        storeName = intent.getStringExtra("storeName");
        collectorEmail = intent.getStringExtra("collectorEmail");

        // Load store logo
        String logoUrl = intent.getStringExtra("logoUrl");
        if (logoUrl != null && !logoUrl.isEmpty()) {
            Glide.with(this)
                    .load(logoUrl)
                    .into(storeLogoImage);
        }

        // Navigate to privuos page
        backButton.setOnClickListener(v -> onBackPressed());

        // Set up button click listeners
        setupFilterButtons();
        summaryCount();
        loadItems();
        searchButton.setOnClickListener(v -> search());
    }

    //search
    public void search() {
        String searchText = searchEditText.getText().toString().trim();
        if (TextUtils.isEmpty(searchText)) {
            Toast.makeText(CollectorItemsActivity.this, "Kindly enter a serial number first.", Toast.LENGTH_SHORT).show();
            return;
        }
        searchItem(searchText);
    }

    private void searchItem(String serialNumber) {
        // First: check the format
        if (!isValidSerialFormat(serialNumber)) {
            Toast.makeText(this, "Invalid serial number format", Toast.LENGTH_SHORT).show();
            return;
        }

        // second: Check store name
        String inputStoreName = serialNumber.split("-")[0];
        if (!inputStoreName.equals(storeName)) {
            Toast.makeText(this, "Item not found in this store", Toast.LENGTH_SHORT).show();
            return;
        }

        // Search in Firebase
        itemsRef.orderByChild("serial_number").equalTo(serialNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        cardContainer.removeAllViews(); // Clear current cards
                        // if item not found.
                        if (!dataSnapshot.exists()) {
                            Toast.makeText(CollectorItemsActivity.this,
                                    "No item found with this serial number",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Item found, create and show the card
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String itemKey = snapshot.getKey();
                            String serialNumber = snapshot.child("serial_number").getValue(String.class);
                            String status = snapshot.child("status").getValue(String.class);
                            createItemCard(itemKey, serialNumber, status);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CollectorItemsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isValidSerialFormat(String serialNumber) {
        // Format: storeName-phoneNumber-4randomNumber
        String regex = "^[a-zA-Z0-9\\s!@#$%^&*_:,./?]+-[0-9]+-[0-9]{4}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(serialNumber);
        return matcher.matches();
    }

    //count number of items
    public void summaryCount() {
        itemsRef.orderByChild("collector").equalTo(collectorEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int total = 0, inStore = 0, collected = 0, reached = 0;

                        for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                            String itemStore = itemSnapshot.child("store").getValue(String.class);
                            String status = itemSnapshot.child("status").getValue(String.class);

                            if (itemStore != null && itemStore.equals(storeName)) {
                                if (status != null) {
                                    total++;
                                    // Convert status to lowercase for comparison
                                    switch (status.toLowerCase()) {
                                        case "in store":
                                        case "in the store":
                                            inStore++;
                                            break;
                                        case "collected":
                                            collected++;
                                            break;
                                        case "reached":
                                            reached++;
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            }
                        }
                        // Update UI with counts
                        totalItems.setText("Total items: " + total);
                        inTheStore.setText("In store: " + inStore);
                        collectedTextview.setText("Collected: " + collected);
                        reachedTextview.setText("Reached: " + reached);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CollectorItemsActivity.this, "Error counting", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    ///Filters
    private void setupFilterButtons() {
        allButton.setOnClickListener(v -> {
            currentFilter = "all";
            updateButtonStyles();
            loadItems();
        });

        reachedButton.setOnClickListener(v -> {
            currentFilter = "reached";
            updateButtonStyles();
            loadItems();
        });

        collectedButton.setOnClickListener(v -> {
            currentFilter = "collected";
            updateButtonStyles();
            loadItems();
        });

        storeButton.setOnClickListener(v -> {
            currentFilter = "in the store";
            updateButtonStyles();
            loadItems();
        });

        updateButtonStyles();
    }

    private void updateButtonStyles() {
        // Reset all buttons to default style
        allButton.setBackgroundResource(R.drawable.shape_grey_rounded);
        reachedButton.setBackgroundResource(R.drawable.shape_grey_rounded);
        collectedButton.setBackgroundResource(R.drawable.shape_grey_rounded);
        storeButton.setBackgroundResource(R.drawable.shape_grey_rounded);
        // Highlight selected button
        switch (currentFilter) {
            case "all":
                allButton.setBackgroundResource(R.drawable.shape_gradient_purple_rounded);
                break;
            case "reached":
                reachedButton.setBackgroundResource(R.drawable.shape_gradient_purple_rounded);
                break;
            case "collected":
                collectedButton.setBackgroundResource(R.drawable.shape_gradient_purple_rounded);
                break;
            case "in the store":
                storeButton.setBackgroundResource(R.drawable.shape_gradient_purple_rounded);
                break;
        }
    }

    //cards
    private void loadItems() {
        itemsRef.orderByChild("collector").equalTo(collectorEmail)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        cardContainer.removeAllViews();
                        serialNumbers.clear();
                        statuses.clear();

                        for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                            String store = itemSnapshot.child("store").getValue(String.class);
                            String status = itemSnapshot.child("status").getValue(String.class);

                            if (store != null && store.equals(storeName)) {
                                // Apply filter
                                if (currentFilter.equals("all") ||
                                        (status != null && status.toLowerCase().equals(currentFilter))) {

                                    String serialNumber = itemSnapshot.child("serial_number").getValue(String.class);

                                    if (serialNumber != null && status != null) {
                                        createItemCard(itemSnapshot.getKey(), serialNumber, status);
                                        serialNumbers.add(serialNumber);
                                        statuses.add(status);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CollectorItemsActivity.this,
                                "Error loading items: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createItemCard(String itemKey, String serialNumber, String status) {
        View cardView = getLayoutInflater().inflate(R.layout.collector_card_of_items, null);
        CardView card = (CardView) cardView;
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int marginInDp = 10;
        int marginInPixels = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                marginInDp,
                getResources().getDisplayMetrics());
        layoutParams.setMargins(marginInPixels, marginInPixels, marginInPixels, marginInPixels);
        card.setLayoutParams(layoutParams);

        TextView serialNumberText = cardView.findViewById(R.id.serialNumberTextView);
        Button cardButton = cardView.findViewById(R.id.cardButton);
        ProgressBar progressBar = cardView.findViewById(R.id.progressBar);
        TextView textStart = cardView.findViewById(R.id.textStart);
        TextView textMiddle = cardView.findViewById(R.id.textMiddle);
        TextView textEnd = cardView.findViewById(R.id.textEnd);

        serialNumberText.setText("SN: " + serialNumber);

        // Update UI based on status
        switch (status) {
            case "In the store":
                progressBar.setProgress(0);
                cardButton.setText("Collected");
                textStart.setVisibility(View.VISIBLE);
                textMiddle.setVisibility(View.GONE);
                textEnd.setVisibility(View.GONE);
                break;
            case "Collected":
                progressBar.setProgress(1);
                cardButton.setText("Reached");
                textStart.setVisibility(View.GONE);
                textMiddle.setVisibility(View.VISIBLE);
                textEnd.setVisibility(View.GONE);
                break;
            case "Reached":
                progressBar.setProgress(2);
                cardButton.setVisibility(View.GONE);
                textStart.setVisibility(View.GONE);
                textMiddle.setVisibility(View.GONE);
                textEnd.setVisibility(View.VISIBLE);
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.greenForComplete));
                break;
        }

        cardButton.setOnClickListener(v -> {
            String newStatus = status.equals("In the store") ? "Collected" : "Reached";
            showConfirmationDialog(itemKey, serialNumber, newStatus);
        });

        cardContainer.addView(card);
    }

    //change status
    private void showConfirmationDialog(String itemKey, String serialNumber, String newStatus) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Status Change")
                .setMessage("Are you sure you want to change status of " + serialNumber + " to " + newStatus + "?")
                .setPositiveButton("Yes", (dialog, which) -> updateItemStatus(itemKey, newStatus) )
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateItemStatus(String itemKey, String newStatus) {
        itemsRef.child(itemKey).child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    summaryCount();
                    Toast.makeText(this, "Status updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    summaryCount();
                    Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onBackPressed() {
        // Get the current fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main);

        // Check if the current fragment is Collector_home_Fragment
        if (currentFragment instanceof CollectorHomeFragment) {
            // Navigate to the Collector_storesAssigned_Fragment
            CollectorStoresAssignedFragment storesFragment = new CollectorStoresAssignedFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.main, storesFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        } else {
            super.onBackPressed();
        }
    }
}
