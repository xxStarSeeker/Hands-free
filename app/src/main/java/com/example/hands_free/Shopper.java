package com.example.hands_free;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/*
* shopper class work as a back end for shopper interfaces.
* it retrieval data by fetching it from Firebase Realtime Database.
* and it update data when it needed.
* It includes 7 methods other than constructors, setter and getter methods.
* method:
* to update:  1- shopper info, 2- shopper Box
* to fetch the shopper's: 1- phone number, 2- email, 3- name,  4- Box
* to count the items based on their status.
*/

public class Shopper {
    //Attributes
    private String Name , Email , Phone , id , Box ="000";
    private int reachedCount = 0, collectedCount = 0, inStoreCount = 0, totalCount=0;
    FirebaseAuth fAuth;

    //constructors
    public Shopper(String id , String Name, String Email, String Phone ){
        this.id= id;
        this.Name = Name ;
        this.Email = Email;
        this.Phone =  Phone;

    }

    public Shopper() {

        fAuth = FirebaseAuth.getInstance();
    }

    //get methods
    public String getName() {return Name;}
    public String getEmail() {return Email;}
    public String getPhone() {return Phone;}
    public String getId () {return id;}
    public String getBox () {return Box;}
    public int getReachedCount(){return reachedCount;}
    public int getCollectedCount(){return collectedCount;}
    public int getInStoreCount(){return inStoreCount;}
    public int getTotalCount() {return totalCount;}

    //set methods
    public void setEmail(String email) {Email = email;}
    public void setName(String name) {Name = name;}
    public void setPhone(String phone) {Phone = phone;}
    public void setBox(String box) {Box = box;}
    public void setReachedCount(int reachedCount){this.reachedCount = reachedCount;}
    public void setCollectedCount(int collectedCount){this.collectedCount = collectedCount;}
    public void setInStoreCount(int inStoreCount){this.inStoreCount = inStoreCount;}
    public void setTotalCount(int totalCount) {this.totalCount = totalCount;}


    //2- update shopper Box to be 000. this use after timer of active is done
    public void updateShopperBox() {
        fAuth = FirebaseAuth.getInstance();
        if (fAuth.getCurrentUser() != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                    .child("Shoppers Accounts")
                    .child(fAuth.getCurrentUser().getUid());
            ref.child("box").setValue("000");
        }
    }

    //4- fetch Shopper Email
    public void fetchShopperEmail(final EmailFetchListener listener) {
        fAuth = FirebaseAuth.getInstance();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child("Shoppers Accounts")
                .child(fAuth.getCurrentUser().getUid())
                .child("email");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String email = dataSnapshot.getValue(String.class);
                setEmail(email);
                listener.onEmailFetched(email);
            }
            // Handle any errors
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("fetchShopperEmail", "Error fetching shopper Email: " + databaseError.getMessage());
            }
        });
    }

    public interface EmailFetchListener {
        void onEmailFetched(String email);
    }

    //5- fetch Shopper name
    public void fetchShopperName(final NameFetchListener listener) {
        fAuth = FirebaseAuth.getInstance();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child("Shoppers Accounts")
                .child(fAuth.getCurrentUser().getUid())
                .child("name");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.getValue(String.class);
                setName(name);
                listener.onNameFetched(name);
            }
            // Handle any errors
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("fetchShopperName", "Error fetching shopper name: " + databaseError.getMessage());
            }
        });
    }

    public interface NameFetchListener {
        void onNameFetched(String name);
    }

    //6- fetch Shopper box (use to determine if the account active or not (has value=active , 000=inactive)).
    public void fetchShopperBox(final BoxFetchListener listener) {
        fAuth = FirebaseAuth.getInstance();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child("Shoppers Accounts")
                .child(fAuth.getCurrentUser().getUid())
                .child("box");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String box = dataSnapshot.getValue(String.class);
                setEmail(box);
                listener.onBoxFetched(box);
            }
            // Handle any errors
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("fetchShopperBox", "Error fetching Box value: " + databaseError.getMessage());
            }
        });
    }

    public interface BoxFetchListener {
        void onBoxFetched(String box);
    }

    //7- item count
    // Count number of items
    public void summaryCount(final ItemCountListener listener) {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("Items");
        fetchShopperEmail(new EmailFetchListener() {
            @Override
            public void onEmailFetched(String email) {
                itemsRef.orderByChild("shopper_email").equalTo(email)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                int total = 0, inStore = 0, collected = 0, reached = 0;

                                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
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
                                listener.onItemsCounted(reached, collected, inStore, total);
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("ItemCounter", "Failed to read items");
                            }
                        });
            }
        });
    }

    public interface ItemCountListener {
        void onItemsCounted(int reachedCount, int collectedCount, int inStoreCount, int totalCount);
    }

    //3- fetch Shopper Phone Number
    public void fetchShopperPhoneNumber(final PhoneFetchListener listener) {
        fAuth = FirebaseAuth.getInstance();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child("Shoppers Accounts")
                .child(fAuth.getCurrentUser().getUid())
                .child("phone");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String phone = dataSnapshot.getValue(String.class);
                setPhone(phone);
                listener.onPhoneFetched(phone);
            }
            // Handle any errors
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("fetchShopperPhoneNumber", "Error fetching phone number: " + databaseError.getMessage());
            }
        });
    }

    public interface PhoneFetchListener {
        void onPhoneFetched(String phone);
    }

    //8- fetching the mall map
    public void fetchMallMap(final MallMapFetchListener listener) {
        fAuth = FirebaseAuth.getInstance();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child("Mall")
                .child("Map");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String mallMap = dataSnapshot.getValue(String.class);
                listener.onMallMapFetched(mallMap);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    public interface MallMapFetchListener {
        void onMallMapFetched(String mallMap);
    }

    public void fetchEndWorkHour(final EndWorkHourFetchListener listener) {
        fAuth = FirebaseAuth.getInstance();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child("Mall")
                .child("End_work_hour");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String End_work_hour = dataSnapshot.getValue(String.class);
                listener.onEndWorkFetched(End_work_hour);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    public interface EndWorkHourFetchListener {
        void onEndWorkFetched(String End_work_hour);
    }

    public void fetchStartWorkHour(final StartWorkHourFetchListener listener) {
        fAuth = FirebaseAuth.getInstance();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child("Mall")
                .child("Start_work_hour");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String Start_work_hour = dataSnapshot.getValue(String.class);
                listener.onStartWorkFetched(Start_work_hour);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    public interface StartWorkHourFetchListener {
        void onStartWorkFetched(String Start_work_hour);
    }

}//end class
