package com.example.hands_free;

import android.icu.util.Calendar;
import android.util.Log;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class Store {

    private FirebaseAuth fAuth;
    private String storeName;
    private String shopperEmail;
    private String shopperPhone;
    private String collector;

    public Store() {
        fAuth = FirebaseAuth.getInstance();
    }

    // Setters
    public void setStoreName(String storeName) {this.storeName = storeName;}
    public void setShopperEmail(String shopperEmail) {this.shopperEmail = shopperEmail;}
    public void setShopperPhone(String shopperPhone) {this.shopperPhone = shopperPhone;}
    public void setCollector(String collector) {this.collector = collector;}

    // Getters
    public String getStoreName() {return storeName;}
    public String getShopperEmail() {return shopperEmail;}
    public String getShopperPhone() {return shopperPhone;}
    public String getCollector() {return collector;}

    public void fetchStoreName(final NameFetchListener listener) {
        if (fAuth.getCurrentUser() != null) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                    .child("Store Account")
                    .child(fAuth.getCurrentUser().getUid())
                    .child("Store_name");

            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String storeName = dataSnapshot.getValue(String.class);
                    setStoreName(storeName);
                    listener.onNameFetched(storeName);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("fetchStoreName", "Error fetching store name: " + databaseError.getMessage());
                }
            });
        }
    }

    public interface NameFetchListener {
        void onNameFetched(String name);
    }

    public void fetchShopperPhoneNumber(final PhoneFetchListener listener) {
        if (fAuth.getCurrentUser() != null && shopperEmail != null) {
            Query shopperPhoneQuery = FirebaseDatabase.getInstance().getReference()
                    .child("Shoppers Accounts")
                    .orderByChild("email")
                    .equalTo(shopperEmail);

            shopperPhoneQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {  // Iterate through children
                            String phone = userSnapshot.child("phone").getValue(String.class);
                            if (phone != null) {
                                setShopperPhone(phone);
                                listener.onPhoneFetched(phone);
                                return;
                            }
                        }
                        listener.onPhoneFetched(null);  // No phone found
                    } else {
                        listener.onPhoneFetched(null);  // No user found
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    listener.onPhoneFetched(null);
                }
            });
        }
    }

    public interface PhoneFetchListener {
        void onPhoneFetched(String phone);
    }

    public String Generate_SN(String shopperEmail) {
        if (storeName == null || storeName.isEmpty() || shopperPhone == null || shopperPhone.isEmpty()) {
            return null;  // Return null if required data is missing
        }

        // Get current time
        Calendar calendar = Calendar.getInstance();
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        // Format day and minute to ensure they are 2 digits
        String timeCode = String.format("%02d%02d", minute, second);

        return storeName + "-" + shopperPhone + "-" + timeCode;
    }

    public interface FirebaseCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void AssignToCollector(String serialNumber, String shopperEmail, String storeName, FirebaseCallback callback) {
        this.storeName = storeName;
        CollectorAssignedItem assignItemToCollector = new CollectorAssignedItem();

        // First check for collector
        assignItemToCollector.checkExistingItemInStore(storeName, new CollectorAssignedItem.CollectorCheckCallback() {
            @Override
            public void onCollectorFound(String foundCollector) {
                // Once collector is found, proceed with adding item
                collector = foundCollector;
                Log.d("Collector Found", "Found collector: onCollectorFound" + collector + "|store class, onCollectorFound");
                addItemToFirebase(serialNumber, shopperEmail, storeName, foundCollector, callback);
            }

            @Override
            public void onNoCollectorFound(String collector) {
                Log.d("Collector Found", "Found collector: " + collector + "|store class, onNoCollectorFound");
                addItemToFirebase(serialNumber, shopperEmail, storeName, collector, callback);
            }

            @Override
            public void onError(String error) {
                Log.e("Error", "Error finding collector: " + error);
                // If collector not found, you can choose to handle here or via callback
                // Uncomment this line to add item without collector
                // addItemToFirebase(serialNumber, shopperEmail, storeName, "omar", callback);
                callback.onFailure(error);
            }
        });
    }

    private void addItemToFirebase(String serialNumber, String shopperEmail,
                                                String storeName, String collector, FirebaseCallback callback) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Items");

        Map<String, Object> itemData = new HashMap<>();
        itemData.put("serial_number", serialNumber);
        itemData.put("shopper_email", shopperEmail);
        itemData.put("status", "In the store");
        itemData.put("store", storeName);
        itemData.put("collector", collector);

        databaseReference.child(serialNumber).setValue(itemData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}