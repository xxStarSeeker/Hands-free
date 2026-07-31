package com.example.hands_free;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CollectorHomeFragment extends Fragment {
    private FirebaseAuth fAuth;
    private DatabaseReference collectorRef, itemsRef;
    private String collectorEmail, collectorName;
    private TextView totalItems, inStoreTextview, collectedTextview, reachedTextview, greeting;
    private Button viewStoresButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collector_home, container, false);
        collectorRef = FirebaseDatabase.getInstance().getReference().child("Collector Account");
        itemsRef = FirebaseDatabase.getInstance().getReference().child("Items");
        fAuth = FirebaseAuth.getInstance();
        viewStoresButton = view.findViewById(R.id.view_stores_button);
        totalItems = view.findViewById(R.id.totalItems);
        inStoreTextview = view.findViewById(R.id.inStore);
        collectedTextview = view.findViewById(R.id.collected);
        reachedTextview = view.findViewById(R.id.reached);
        greeting = view.findViewById(R.id.greeting);

        fetchCollectorName();

        viewStoresButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToStoresFragment();
            }
        });

        return view;
    }

    private void navigateToStoresFragment() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, new CollectorStoresAssignedFragment())
                .addToBackStack(null)
                .commit();
    }

    private void fetchCollectorName() {
        collectorRef = FirebaseDatabase.getInstance().getReference().child("Collector Account");
        if (fAuth.getCurrentUser() != null) {
            collectorRef.child(fAuth.getCurrentUser().getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists() && isAdded()) {
                                collectorName = snapshot.child("Username").getValue(String.class);
                                if (collectorName != null) {
                                    greeting.setText("Hello," + collectorName);
                                    fetchCollectorEmail();
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

    private void fetchCollectorEmail() {
        collectorRef = FirebaseDatabase.getInstance().getReference().child("Collector Account");
        if (fAuth.getCurrentUser() != null) {
            collectorRef.child(fAuth.getCurrentUser().getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists() && isAdded()) {
                                collectorEmail = snapshot.child("Email").getValue(String.class);
                                if (collectorEmail != null) {
                                    summaryCount();
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

    public void summaryCount() {
        itemsRef.orderByChild("collector").equalTo(collectorEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int total = 0, inStore = 0, collected = 0, reached = 0;

                        for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                            String itemStore = itemSnapshot.child("store").getValue(String.class);
                            String status = itemSnapshot.child("status").getValue(String.class);
                                if (status != null) {
                                    total++;
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
                        // Update UI with counts
                        totalItems.setText("Total items\n" + total);
                        inStoreTextview.setText("In store\n" + inStore);
                        collectedTextview.setText("Collected\n" + collected);
                        reachedTextview.setText("Reached\n" + reached);
                        if(reached > 0){
                            reachedTextview.setOnClickListener(v -> Toast.makeText(getContext(), "Great job!", Toast.LENGTH_SHORT).show());
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Error counting", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
