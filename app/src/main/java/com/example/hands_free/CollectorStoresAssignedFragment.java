package com.example.hands_free;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectorStoresAssignedFragment extends Fragment {
    private View view;
    private RecyclerView recyclerView;
    private Collector_Items_Adapter adapter;
    private String collectorEmail;
    private DatabaseReference itemsRef, collectorRef, storeRef;
    private FirebaseAuth fAuth;
    private Map<String, String> storeLogos = new HashMap<>();

    public CollectorStoresAssignedFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_collector_stores, container, false);

        // Initialize Firebase references
        itemsRef = FirebaseDatabase.getInstance().getReference().child("Items");
        collectorRef = FirebaseDatabase.getInstance().getReference().child("Collector Account");
        storeRef = FirebaseDatabase.getInstance().getReference().child("Store Account");
        fAuth = FirebaseAuth.getInstance();

        // Start data loading
        fetchStoreLogos();
        fetchCollectorEmail();

        return view;
    }

    public void recyclerView(){
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new Collector_Items_Adapter(requireContext(), collectorEmail);
        recyclerView.setAdapter(adapter);

    }

    private void fetchStoreLogos() {
        storeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot storeSnapshot : snapshot.getChildren()) {
                    String storeName = storeSnapshot.child("Store_name").getValue(String.class);
                    String logo = storeSnapshot.child("Logo").getValue(String.class);
                    if (storeName != null && logo != null) {
                        storeLogos.put(storeName, logo);
                    }
                }
                // After fetching logos, proceed with loading items
                if (collectorEmail != null) {
                    loadCollectorItems();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Database Error", "Failed to fetch store logos: " + error.getMessage());
            }
        });
    }

    private void fetchCollectorEmail() {
        if (fAuth.getCurrentUser() != null) {
            collectorRef.child(fAuth.getCurrentUser().getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists() && isAdded()) {
                                collectorEmail = snapshot.child("Email").getValue(String.class);
                                if (collectorEmail != null && !storeLogos.isEmpty()) {
                                    // Setup RecyclerView
                                    recyclerView();
                                    loadCollectorItems();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Failed to load email collector", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void loadCollectorItems() {
        itemsRef.orderByChild("collector").equalTo(collectorEmail)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot itemsSnapshot) {
                        Map<String, StoreItemCount> storeCountMap = new HashMap<>();

                        for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                            String storeName = itemSnapshot.child("store").getValue(String.class);
                            String status = itemSnapshot.child("status").getValue(String.class);

                            if (storeName != null) {
                                StoreItemCount count = storeCountMap.getOrDefault(storeName,
                                        new StoreItemCount(storeName, storeLogos.get(storeName)));

                                count.totalItems++;
                                if (status != null) {
                                    switch (status.toLowerCase()) {
                                        case "in the store":
                                            count.inStoreCount++;
                                            break;
                                        case "collected":
                                            count.collectedCount++;
                                            break;
                                        case "reached":
                                            count.reachedCount++;
                                            break;
                                    }
                                }
                                storeCountMap.put(storeName, count);
                            }
                        }

                        updateUI(new ArrayList<>(storeCountMap.values()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Database Error", error.getMessage());
                    }
                });
    }

    private void updateUI(List<StoreItemCount> itemCounts) {

        adapter.updateData(itemCounts);
    }

    private static class StoreItemCount {
        String storeName;
        String logoUrl;
        int totalItems;
        int inStoreCount;
        int collectedCount;
        int reachedCount;

        StoreItemCount(String storeName, String logoUrl) {
            this.storeName = storeName;
            this.logoUrl = logoUrl;
            this.totalItems = 0;
            this.inStoreCount = 0;
            this.collectedCount = 0;
            this.reachedCount = 0;
        }
    }

    private class Collector_Items_Adapter extends RecyclerView.Adapter<Collector_Items_Adapter.ViewHolder> {
        private List<StoreItemCount> itemCounts = new ArrayList<>();
        private final Context context;
        private String collectorEmail;

        public Collector_Items_Adapter(Context context, String collectorEmail) {
            this.context = context;
            this.collectorEmail = collectorEmail;
            this.itemCounts = new ArrayList<>();
        }

        public void updateData(List<StoreItemCount> newItemCounts) {
            this.itemCounts = newItemCounts;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.collector_card_of_stores, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StoreItemCount item = itemCounts.get(position);
            try {
                // Load store logo using Glide
                if (item.logoUrl != null && !item.logoUrl.isEmpty()) {
                    Glide.with(context)
                            .load(item.logoUrl)
                            .into(holder.cardImage);
                }

                holder.cardText.setText("Total Items: " + item.totalItems);
                holder.cardText.setTypeface(Typeface.DEFAULT_BOLD);
                holder.inTheStore.setText("In Store: " + item.inStoreCount);
                holder.collected.setText("Collected: " + item.collectedCount);
                holder.reached.setText("Reached: " + item.reachedCount);

                holder.ItemButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Intent intent = new Intent(context, CollectorItemsActivity.class);
                            intent.putExtra("storeName", item.storeName);
                            intent.putExtra("collectorEmail", collectorEmail);
                            intent.putExtra("logoUrl", item.logoUrl);

                            // Add flags to start new activity
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        } catch (Exception e) {
                            Log.e("Button Click Error", "Error starting activity: " + e.getMessage());
                            Toast.makeText(context, "Error opening items view", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("Binding Error", "Error binding view holder: " + e.getMessage());
            }
        }

        @Override
        public int getItemCount() {
            return itemCounts.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView cardText, inTheStore, collected, reached;
            Button ItemButton;
            ImageView cardImage;

            ViewHolder(View itemView) {
                super(itemView);
                cardText = itemView.findViewById(R.id.cardText);
                inTheStore = itemView.findViewById(R.id.inTheStore);
                collected = itemView.findViewById(R.id.collected);
                reached = itemView.findViewById(R.id.reached);
                cardImage = itemView.findViewById(R.id.cardImage);
                ItemButton = itemView.findViewById(R.id.ItemButton);
            }
        }
    }
}