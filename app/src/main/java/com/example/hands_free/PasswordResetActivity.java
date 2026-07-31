package com.example.hands_free;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hands_free.databinding.ActivityResetPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class PasswordResetActivity extends AppCompatActivity {

    private ActivityResetPasswordBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Set up the send button click listener
        binding.SendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendForgetMSG();
            }
        });
    }

    private void sendForgetMSG() {
        //flag to validate before sending
        boolean isValid = true;
        //email pattern
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$";

        String email = binding.email.getText().toString().trim();
        EditText Email = findViewById(R.id.email);

        if (email.isEmpty()) {
            Email.setError( "Please enter your email");
            return;
        }if (!email.matches(emailPattern)) {
            Email.setError("Invalid Email Format.");
        isValid = false;
        }

        if(isValid){
        // Send password reset email
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "If this email is registered, a reset link has been sent.", Toast.LENGTH_SHORT).show();
                            finish(); // Optionally, navigate back to the login screen
                        } else {
                            // Handle exceptions
                            Exception exception = task.getException();
                            if (exception != null) {
                                String errorMessage;
                                if (exception instanceof FirebaseAuthInvalidUserException) {
                                    errorMessage = "No user found with this email. Please sign up first.";
                                } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                    errorMessage = "Invalid email format.";
                                } else {
                                    errorMessage = "Error: " + exception.getMessage();
                                }
                                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                                Log.e("FirebaseAuthError", "Error: ", exception); // Log the error for debugging
                            } else {
                                Toast.makeText(this, "An unknown error occurred.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });}
    }

    public void onBackClick(View view) {

        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);

    }
}