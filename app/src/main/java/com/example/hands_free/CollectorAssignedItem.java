package com.example.hands_free;

import android.icu.util.Calendar;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class CollectorAssignedItem {
    private String collector;
    private DatabaseReference collectorRef;
    private DatabaseReference itemsRef;

    public CollectorAssignedItem() {
        collectorRef = FirebaseDatabase.getInstance().getReference().child("Collector Account");
        itemsRef = FirebaseDatabase.getInstance().getReference().child("Items");
    }

    public interface CollectorCheckCallback {
        void onCollectorFound(String collector);

        void onNoCollectorFound(String collector);

        void onError(String error);
    }

    public void setCollector(String collector) {

        this.collector = collector;
    }

    public String getCollector() {

        return collector;
    }

    public void checkExistingItemInStore(String storeName, CollectorCheckCallback callback) {
        Query query = itemsRef.orderByChild("store").equalTo(storeName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean itemFound = false;

                for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                    String status = itemSnapshot.child("status").getValue(String.class);
                    if (status != null && status.equalsIgnoreCase("In the store")) {
                        String collectorValue = itemSnapshot.child("collector").getValue(String.class);
                        if (collectorValue != null) {
                            itemFound = true;
                            collector = collectorValue;
                            callback.onCollectorFound(collectorValue);
                            break;
                        }
                    }
                }

                if (!itemFound) {
                    RoundRobinWithLoadBalancing(new RoundRobinCallback() {
                        @Override
                        public void onCollectorAssigned(String assignedCollector) {
                            Log.d("checkExistingItemInStore()", "if (!itemFound), Found collector: " + assignedCollector);
                            callback.onNoCollectorFound(assignedCollector);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public interface RoundRobinCallback {
        void onCollectorAssigned(String collector);
    }

    public interface CreateAvailableCollectorCallback {
        void onCollectorsAvailable(List<String> collectors);
    }

    public interface CountCollectorStoresCallback {
        void onCountingComplete(String selectedCollector);
    }


    public void RoundRobinWithLoadBalancing(RoundRobinCallback callback) {
        createAvailableCollectorList(new CreateAvailableCollectorCallback() {
            @Override
            public void onCollectorsAvailable(List<String> collectors) {
                if (collectors.isEmpty()) {
                    callback.onCollectorAssigned("not assign to collector");
                    return;
                }
                countCollectorStores(collectors, new CountCollectorStoresCallback() {
                    @Override
                    public void onCountingComplete(String selectedCollector) {
                        callback.onCollectorAssigned(selectedCollector);
                    }
                });
            }
        });
    }

    public void createAvailableCollectorList(CreateAvailableCollectorCallback callback) {
        collectorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot collectorsSnapshot) {
                List<String> availableCollectors = new ArrayList<>();

                for (DataSnapshot collectorSnapshot : collectorsSnapshot.getChildren()) {
                    String endTimeStr = collectorSnapshot.child("End_work_time").getValue(String.class);
                    String startTimeStr = collectorSnapshot.child("Start_work_time").getValue(String.class);
                    String collectorEmail = collectorSnapshot.child("Email").getValue(String.class);

                    Log.e("Data Retrieval", "Start Time: " + startTimeStr + " | End Time: " + endTimeStr +"email "+ collectorEmail);

                    if (endTimeStr != null && startTimeStr != null && checkAvailability(endTimeStr, startTimeStr)) {
                        Log.e("createAvailableCollectorList", " | Start: " + startTimeStr + " | End: " + endTimeStr + " | Available: " + collectorEmail);
                        availableCollectors.add(collectorEmail);
                    }
                }
                callback.onCollectorsAvailable(availableCollectors);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Database Error", "Error: " + error.getMessage());
                callback.onCollectorsAvailable(new ArrayList<>()); // Return empty list on error
            }
        });
    }

    private boolean checkAvailability(String endTimeStr, String startTimeStr) {
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        int endHour = Integer.parseInt(endTimeStr.substring(0, 2));
        int endMinute = Integer.parseInt(endTimeStr.substring(3, 5));

        int startHour = Integer.parseInt(startTimeStr.substring(0, 2));
        int startMinute = Integer.parseInt(startTimeStr.substring(3, 5));

        //Add 10 minutes to avoid assign to collector employee if they work time is ending soon.
        int currentTotalMinutes = currentHour * 60 + currentMinute + 10;
        int startTotalMinutes = startHour * 60 + startMinute;
        int endTotalMinutes = endHour * 60 + endMinute;

        // Handle cases where (end time is earlier than start time)
        if (endTotalMinutes < startTotalMinutes) {
            endTotalMinutes += 24 * 60;
        }
        // Check if the current time is within the working hours range
        boolean isAvailable = (startTotalMinutes <= currentTotalMinutes && endTotalMinutes >= currentTotalMinutes);
        // Logging for debugging
        Log.e("checkAvailability", "Current time: " + currentTotalMinutes + " | Start: " + startTotalMinutes + " | End: " + endTotalMinutes + " | Available: " + isAvailable);
        return isAvailable;
    }


    public void countCollectorStores(List<String> availableCollectors, CountCollectorStoresCallback callback) {
        itemsRef.orderByChild("status").equalTo("In the store")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot itemsSnapshot) {
                        Map<String, Set<String>> collectorStores = new HashMap<>();

                        // Initialize all available collectors with empty store sets
                        for (String collectorEmail : availableCollectors) {
                            collectorStores.put(collectorEmail, new HashSet<>());
                        }

                        // Populate stores for each collector
                        for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                            String collector = itemSnapshot.child("collector").getValue(String.class);
                            String store = itemSnapshot.child("store").getValue(String.class);

                            if (collector != null && store != null && availableCollectors.contains(collector)) {
                                collectorStores.get(collector).add(store);
                            }
                        }

                        selectBestCollector(collectorStores, new CollectorSelectionCallback() {
                            @Override
                            public void onCollectorSelected(String selectedCollector) {
                                callback.onCountingComplete(selectedCollector);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Database Error", "Error: " + error.getMessage());
                        callback.onCountingComplete("arwa");
                    }
                });
    }

    private interface CollectorSelectionCallback {
        void onCollectorSelected(String collector);
    }

    // Find collectors with minimum number of stores
    private void selectBestCollector(Map<String, Set<String>> collectorStores, CollectorSelectionCallback callback) {
        int minStores = Integer.MAX_VALUE;
        String selectedCollector = null;

        for (Map.Entry<String, Set<String>> entry : collectorStores.entrySet()) {
            int storeCount = entry.getValue().size();
            if (storeCount < minStores) {
                minStores = storeCount;
                selectedCollector = entry.getKey();
            }
        }

        // Return the collector with minimum stores (or first one if multiple have same count)
        callback.onCollectorSelected(selectedCollector != null ? selectedCollector : "not assign to any collector!");
    }
}