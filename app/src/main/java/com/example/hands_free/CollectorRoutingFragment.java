package com.example.hands_free;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class CollectorRoutingFragment extends Fragment {
    private DatabaseReference itemsRef, collectorRef, mallRef;
    private String collectorEmail;
    private FirebaseAuth fAuth;
    private TextView storeAssignedList, storeAssignedTitle;
    private EditText nearestStore;
    LinearLayout search_card, result;
    Set<String> storeSet = new HashSet<>();
    private Button shortestPathButton;
    private ImageView mallMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collector_routing, container, false);
        itemsRef = FirebaseDatabase.getInstance().getReference().child("Items");
        collectorRef = FirebaseDatabase.getInstance().getReference().child("Collector Account");
        mallRef = FirebaseDatabase.getInstance().getReference().child("Mall");
        fAuth = FirebaseAuth.getInstance();
        storeAssignedList = view.findViewById(R.id.storeAssignedList);
        storeAssignedTitle = view.findViewById(R.id.storeAssignedTitle);
        shortestPathButton = view.findViewById(R.id.shortestPathButton);
        nearestStore = view.findViewById(R.id.nearestStore);
        search_card = view.findViewById(R.id.search_card);
        result = view.findViewById(R.id.result);
        mallMap = view.findViewById(R.id.mallMap);

        fetchMallMap();
        fetchCollectorEmail();
        shortestPathButton.setOnClickListener(v -> shortestPath());

        return view;
    }

    public void fetchMallMap() {
        fAuth = FirebaseAuth.getInstance();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child("Mall")
                .child("Map");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String mallMapUrl = dataSnapshot.getValue(String.class);
                Glide.with(requireContext()).load(mallMapUrl).into(mallMap);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
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
                                if (collectorEmail != null) {
                                    Log.e("Error", "collector email" + collectorEmail);
                                    listStore();
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

    private void listStore() {
        itemsRef.orderByChild("collector").equalTo(collectorEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        storeSet.clear(); // Clear the storeSet before adding new stores
                        for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                            String store = itemSnapshot.child("store").getValue(String.class);
                            String status = itemSnapshot.child("status").getValue(String.class);
                            if (store != null && status != null && status.equalsIgnoreCase("In the Store")) {
                                storeSet.add(store);
                            }
                        }

                        if (!storeSet.isEmpty()) {
                            result.setVisibility(View.VISIBLE);
                            ShortPathWhereTheStartPointIsOffice();
                            search_card.setVisibility(View.VISIBLE);
                            shortestPathButton.setOnClickListener(v -> shortestPath());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle errors appropriately
                        Log.e("Error", "Failed to load shortest path: " + error.getMessage());
                        Toast.makeText(getContext(), "Failed to load shortest path: ", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void shortestPath() {
        String firstStore = nearestStore.getText().toString().trim();
        if (TextUtils.isEmpty(firstStore) || firstStore.equalsIgnoreCase("Office") ||
                firstStore.equalsIgnoreCase("HandsFree Office") ||
                firstStore.equalsIgnoreCase("Hands Free Office") ||
                firstStore.equalsIgnoreCase("Hands Free") ) {
            ShortPathWhereTheStartPointIsOffice();
        } else {
            shortestPathWithSpecificStore();
        }
    }

    public void ShortPathWhereTheStartPointIsOffice(){
        ShortestPath shortestPath = new ShortestPath(new ArrayList<>(storeSet));
        shortestPath.calculateShortestPath(new ShortestPath.OnPathCalculatedListener() {
            @Override
            public void onPathCalculated(List<String> orderedStores) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        StringBuilder storesBuilder = new StringBuilder();
                        for (int i = 0; i < orderedStores.size(); i++) {
                            storesBuilder.append(orderedStores.get(i));
                            if (i < orderedStores.size() - 1) {
                                storesBuilder.append(", ");
                            } else {
                                storesBuilder.append(".");
                            }
                        }
                        String stores = storesBuilder.toString().trim();
                        result.setVisibility(View.VISIBLE);
                        storeAssignedTitle.setText("Shortest path to visit stores starting from office:");
                        storeAssignedList.setText(stores);
                    });
                }
            }
        });
    }

    public void shortestPathWithSpecificStore(){
        String firstStore = nearestStore.getText().toString().trim();
        DatabaseReference storeRef = FirebaseDatabase.getInstance().getReference("Store Account");
        storeRef.orderByChild("Store_name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String storeName = snapshot.child("Store_name").getValue(String.class);
                            if (storeName != null && storeName.equalsIgnoreCase(firstStore)) {
                                String X_axis = snapshot.child("X_axis").getValue(String.class);
                                String Y_axis = snapshot.child("Y_axis").getValue(String.class);
                                if (X_axis != null && Y_axis != null) {
                                    double x = Double.parseDouble(X_axis);
                                    double y = Double.parseDouble(Y_axis);

                                    // Create the ShortestPath instance with the coordinates
                                    ShortestPath shortestPath = new ShortestPath(new ArrayList<>(storeSet), firstStore, x, y);
                                    shortestPath.calculateShortestPath(new ShortestPath.OnPathCalculatedListener() {

                                        @Override
                                        public void onPathCalculated(List<String> orderedStores) {
                                            if (getActivity() != null) {
                                                getActivity().runOnUiThread(() -> {
                                                    StringBuilder storesBuilder = new StringBuilder();
                                                    for (int i = 0; i < orderedStores.size(); i++) {
                                                        storesBuilder.append(orderedStores.get(i));
                                                        if (i < orderedStores.size() - 1) {
                                                            storesBuilder.append(", ");
                                                        } else {
                                                            storesBuilder.append(".");
                                                        }
                                                    }
                                                    String stores = storesBuilder.toString().trim();
                                                    result.setVisibility(View.VISIBLE);
                                                    storeAssignedTitle.setText("Shortest path to visit stores starting from "+firstStore+":");
                                                    storeAssignedList.setText(stores);
                                                });
                                            }
                                        }
                                    });
                                } else {
                                    Toast.makeText(getActivity(), "Store not found", Toast.LENGTH_SHORT).show();
                                }
                                return;
                            }
                        }
                        Toast.makeText(getActivity(), "Store not found", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Error", "Failed to find nearest store: " + error.getMessage());
                        Toast.makeText(getActivity(), "Failed to find nearest store", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}