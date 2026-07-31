package com.example.hands_free;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

/* This class enable shopper to see his information and updated.
he also can log in
 */

public class ShopperAccountFragment extends Fragment {
    //Attributes
    private TextView greeting, shopperEmail, statusAccount;
    private EditText shopperName, shopperPhone;
    private FirebaseAuth fAuth;
    private Shopper shopper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shopper_account, container, false);

        //Initialize
        fAuth = FirebaseAuth.getInstance();
        greeting = view.findViewById(R.id.greeting);
        statusAccount = view.findViewById(R.id.statusAccount);
        shopperName = view.findViewById(R.id.shopperName);
        shopperPhone = view.findViewById(R.id.shopperPhone);
        shopperEmail = view.findViewById(R.id.shopperEmail);
        Button updateBtn = view.findViewById(R.id.updateButton);

        loadCurrentData(); // Load current data (name, phone, email, status of account)

        //Set up update button
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateInfo();
            }
        });

        //on email filed click
        shopperEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "You cannot modify email", Toast.LENGTH_SHORT).show();
            }
        });

        //log out
        logOutClick(view);

        return view;
    }

    private void loadCurrentData() {
        if (fAuth.getCurrentUser() != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                    .child("Shoppers Accounts")
                    .child(fAuth.getCurrentUser().getUid());

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && isAdded()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String phone = snapshot.child("phone").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);
                        String box = snapshot.child("box").getValue(String.class);

                        // Update TextViews
                        if (name != null) {
                            greeting.setText("Hello, " + name);
                            shopperName.setText(name);
                        }
                        if (phone != null) {
                            shopperPhone.setText(phone);
                        }
                        if (email != null) {
                            shopperEmail.setText(email);
                        }
                        if (box != null && !box.equals("000")) {
                            statusAccount.setText("Active");
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

    private void updateInfo() {
        String newName = shopperName.getText().toString().trim();
        String newPhone = shopperPhone.getText().toString().trim();

        if (validation(newName, newPhone)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Confirm Update");
            builder.setMessage("Are you sure you want to update your information?");

            builder.setPositiveButton("update", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    shopper = new Shopper();
                    shopper.updateShopperInfo(newName, newPhone);

                    // Add a listener to check if update is successful
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                            .child("Shoppers Accounts")
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Toast.makeText(getContext(), "Updated Successfully", Toast.LENGTH_SHORT).show();
                                loadCurrentData(); // Reload the data
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(getContext(), "Update failed: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(requireContext(), "Update cancelled", Toast.LENGTH_SHORT).show();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            Toast.makeText(getContext(), "Failed update. check your input", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean validation(String newName, String newPhone){
        boolean isValid = true;
        if (TextUtils.isEmpty(newName)) {
            shopperName.setError("Name is Required.");
            isValid = false;
        } else if (newName.length() > 30) {
            shopperName.setError("Name must be maximum 30 characters.");
            isValid = false;
        }
        if (TextUtils.isEmpty(newPhone) || !TextUtils.isDigitsOnly(newPhone)) {
            shopperPhone.setError("Please enter a valid phone number (digits only).");
            isValid = false;
        } else if (!newPhone.startsWith("05")) {
            shopperPhone.setError("Phone number must start with '05'.");
            isValid = false;
        } else if (newPhone.length() != 10) {
            shopperPhone.setError("Phone number must be exactly 10 digits.");
            isValid = false;
        }
        return isValid;
    }

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