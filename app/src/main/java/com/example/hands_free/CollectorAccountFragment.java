package com.example.hands_free;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.icu.util.Calendar;
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
import com.google.firebase.database.ValueEventListener;

public class CollectorAccountFragment extends Fragment {

    // Attributes
    private TextView greeting, collectorEmail, startWork, endWork, availability;
    private EditText collectorPhone, collectorName;
    private Button updateButton;
    private FirebaseAuth fAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collector_account, container, false);

        //Initialize
        fAuth = FirebaseAuth.getInstance();
        greeting = view.findViewById(R.id.greeting);
        availability = view.findViewById(R.id.availability);
        collectorName = view.findViewById(R.id.collectorName);
        collectorEmail = view.findViewById(R.id.collectorEmail);
        startWork = view.findViewById(R.id.startWork);
        endWork = view.findViewById(R.id.endWork);
        collectorPhone = view.findViewById(R.id.collectorPhone);
        updateButton = view.findViewById(R.id.updateButton);

        loadCurrentData(); //Load current data

        updateButton.setOnClickListener(v -> update());
        collectorEmail.setOnClickListener(v -> Toast.makeText(getContext(), "You cannot modify email", Toast.LENGTH_SHORT).show());
        startWork.setOnClickListener(v -> Toast.makeText(getContext(), "You cannot modify work time", Toast.LENGTH_SHORT).show());
        endWork.setOnClickListener(v -> Toast.makeText(getContext(), "You cannot modify work time", Toast.LENGTH_SHORT).show());

        //log out
        logOutClick(view);

        return view;
    }

    private void loadCurrentData() {
        if (fAuth.getCurrentUser() != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                    .child("Collector Account")
                    .child(fAuth.getCurrentUser().getUid());

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && isAdded()) {
                        String email = snapshot.child("Email").getValue(String.class);
                        String name = snapshot.child("Username").getValue(String.class);
                        String phone = snapshot.child("Phone_number").getValue(String.class);
                        String endWotkTime = snapshot.child("End_work_time").getValue(String.class);
                        String startWotkTime = snapshot.child("Start_work_time").getValue(String.class);

                        // Update TextViews
                        if (name != null) {
                            greeting.setText("Hello, " + name);
                            collectorName.setText(name);
                        }
                        if (phone != null) {
                            collectorPhone.setText(phone);
                        }
                        if (email != null) {
                            collectorEmail.setText(email);
                        }
                        if (endWotkTime != null) {
                            endWork.setText(endWotkTime);
                        }
                        if (startWotkTime != null){
                            startWork.setText(startWotkTime);
                        }
                        if(checkAvailability(endWotkTime,startWotkTime )){
                            availability.setText("available");
                        }else{
                            availability.setText("unavailable");
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to load collector data", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private boolean checkAvailability(String endTimeStr, String startTimeStr) {
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        int endHour = Integer.parseInt(endTimeStr.substring(0, 2));
        int endMinute = Integer.parseInt(endTimeStr.substring(3, 5));

        int startHour = Integer.parseInt(startTimeStr.substring(0, 2));
        int startMinute = Integer.parseInt(startTimeStr.substring(3, 5));

        int currentTotalMinutes = currentHour * 60 + currentMinute;
        int startTotalMinutes = startHour * 60 + startMinute;
        int endTotalMinutes = endHour * 60 + endMinute;

        // Handle cases where the time range spans across midnight (i.e., end time is earlier than start time)
        if (endTotalMinutes < startTotalMinutes) {
            endTotalMinutes += 24 * 60; // Add 24 hours worth of minutes to the end time
        }

        // Check if the current time is within the working hours range
        boolean isAvailable = (startTotalMinutes <= currentTotalMinutes && endTotalMinutes >= currentTotalMinutes);

        return isAvailable;
    }

    public void update() {
        String newPhone = collectorPhone.getText().toString().trim();
        String newName = collectorName.getText().toString().trim();

        if (validation(newName,newPhone)) {
            // Show a confirmation dialog before updating
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext()); // Or getContext() if in Activity
            builder.setTitle("Confirm Update");
            builder.setMessage("Are you sure you want to update your information?");

            builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FirebaseAuth fAuth = FirebaseAuth.getInstance();
                    if (fAuth.getCurrentUser() != null) {
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                                .child("Collector Account")
                                .child(fAuth.getCurrentUser().getUid());
                        ref.child("Phone_number").setValue(newPhone);
                        ref.child("Username").setValue(newName)
                                .addOnSuccessListener(aVoid -> {
                                    if (isAdded()) {
                                        collectorPhone.setText(newPhone);
                                        collectorName.setText(newName);
                                        Toast.makeText(getContext(), "update successfully!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (isAdded()) {
                                        Toast.makeText(getContext(), "Failed to update phone number", Toast.LENGTH_SHORT).show();
                                    }
                                });
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
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            Toast.makeText(getContext(), "Failed update. Check your input.", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean validation(String newName, String newPhone){
        boolean isValid = true;
        if (TextUtils.isEmpty(newName)) {
            collectorName.setError("Name is Required.");
            isValid = false;
        } else if (newName.length() > 30) {
            collectorName.setError("Name must be maximum 30 characters.");
            isValid = false;
        }
        if (TextUtils.isEmpty(newPhone) || !TextUtils.isDigitsOnly(newPhone)) {
            collectorPhone.setError("Please enter a valid phone number (digits only).");
            isValid = false;
        } else if (!newPhone.startsWith("05")) {
            collectorPhone.setError("Phone number must start with '05'.");
            isValid = false;
        } else if (newPhone.length() != 10) {
            collectorPhone.setError("Phone number must be exactly 10 digits.");
            isValid = false;
        }
        return isValid;
    }

    private void logOutClick(View view) {
        TextView signOutTextView = view.findViewById(R.id.signOutTextView);
        signOutTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
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
