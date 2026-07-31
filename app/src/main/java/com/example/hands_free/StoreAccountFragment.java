package com.example.hands_free;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class StoreAccountFragment extends Fragment {

    // Attributes
    private TextView greeting, store_email, store_name, number_item;
    private EditText store_phone;
    private Button updateButton;
    private FirebaseAuth fAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_store_account, container, false);

        // Initialize Firebase Authentication
        fAuth = FirebaseAuth.getInstance();

        // Initialize Views
        greeting = view.findViewById(R.id.greeting);
        store_email = view.findViewById(R.id.store_email);
        store_name = view.findViewById(R.id.store_name);
        store_phone = view.findViewById(R.id.store_phone);
        number_item = view.findViewById(R.id.number_item);
        updateButton = view.findViewById(R.id.updateButton);

        loadCurrentData(); // Load current data (name, phone, email)

        // Set up update button
        updateButton.setOnClickListener(v -> updatePhoneNumber());

        // Email field click behavior
        store_email.setOnClickListener(v -> {
            Toast.makeText(getContext(), "You cannot modify email, contact admin if you want to change", Toast.LENGTH_SHORT).show();
        });

        // Store name field click behavior
        store_name.setOnClickListener(v -> {
            Toast.makeText(getContext(), "You cannot modify store name, contact admin if you want to change", Toast.LENGTH_SHORT).show();
        });

        // Log out functionality
        logOutClick(view);

        return view;
    }


    private void loadCurrentData() {
        if (fAuth.getCurrentUser() != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                    .child("Store Account")
                    .child(fAuth.getCurrentUser().getUid());

            // Fetch store profile data from Firebase
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && isAdded()) {
                        String email = snapshot.child("Email").getValue(String.class);
                        String phone = snapshot.child("Phon_Number").getValue(String.class);
                        String name = snapshot.child("Store_name").getValue(String.class);

                        // Update the UI with data from Store Accounts
                        if (name != null) {
                            greeting.setText("Hello, " + name);
                            store_name.setText(name);
                            countItems(name);
                        }
                        if (phone != null) {
                            store_phone.setText(phone);
                        }
                        if (email != null) {
                            store_email.setText(email);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }


    }

    public void countItems(String store) {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("Items");

        // Query to get items where the store matches the specified store and status is "In the store"
        Query query = itemsRef.orderByChild("store").equalTo(store); // Filter by store ID
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int itemCount = 0;
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    String status = itemSnapshot.child("status").getValue(String.class);
                    // Check if the status is "In the store"
                    if ("In the store".equals(status)) {
                        itemCount++;
                    }
                }
                number_item.setText(String.valueOf(itemCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database errors
                Toast.makeText(getContext(), "Failed to load item count", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void updatePhoneNumber() {

        // Get the new phone number from the EditText
        String newPhone = store_phone.getText().toString().trim();

        // Validate the phone number before proceeding
        if (validation(newPhone)) {
            // Show a confirmation dialog before updating
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext()); // Or getContext() if in Activity
            builder.setTitle("Confirm Update");
            builder.setMessage("Are you sure you want to update the phone number?");

            builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Initialize FirebaseAuth instance
                    FirebaseAuth fAuth = FirebaseAuth.getInstance();

                    // Ensure the user is logged in
                    if (fAuth.getCurrentUser() != null) {
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                                .child("Store Account")  // Correct Firebase path
                                .child(fAuth.getCurrentUser().getUid()); // Using current user's UID

                        // Update the phone number for the current user's store
                        ref.child("Phon_Number").setValue(newPhone)
                                .addOnSuccessListener(aVoid -> {
                                    if (isAdded()) {
                                        store_phone.setText(newPhone); // Update the UI
                                        Toast.makeText(getContext(), "Phone number updated successfully!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (isAdded()) {
                                        Toast.makeText(getContext(), "Failed to update phone number", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // Handle the case where the user is not logged in
                        Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Handle the cancel action
                    Toast.makeText(requireContext(), "Update cancelled", Toast.LENGTH_SHORT).show();
                }
            });

            // Show the confirmation dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            // If validation fails, show an error
            Toast.makeText(getContext(), "Failed update. Check your input.", Toast.LENGTH_SHORT).show();
        }
    }

    // Validation method for phone number
    public boolean validation(String newPhone) {
        boolean isValid = true;

        // Check if phone number is empty or contains non-digit characters
        if (TextUtils.isEmpty(newPhone) || !TextUtils.isDigitsOnly(newPhone)) {
            store_phone.setError("Please enter a valid phone number (digits only).");
            isValid = false;
        }
        // Ensure phone number starts with "05"
        else if (!newPhone.startsWith("05")) {
            store_phone.setError("Phone number must start with '05'.");
            isValid = false;
        }
        // Ensure phone number is exactly 10 digits
        else if (newPhone.length() != 10) {
            store_phone.setError("Phone number must be exactly 10 digits.");
            isValid = false;
        }

        return isValid;
    }


    //logout
    private void logOutClick(View view) {
        TextView signOutTextView = view.findViewById(R.id.signOutTextView);
        signOutTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("Confirm Log out");
                builder.setMessage("Are you sure you want to Log out?");

                builder.setPositiveButton("Log out", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fAuth = FirebaseAuth.getInstance();
                        fAuth.signOut();
                        Intent intent = new Intent(requireActivity(), MainActivity.class);
                        startActivity(intent);
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(requireContext(), "Log out cancelled", Toast.LENGTH_SHORT).show();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

}

