package com.example.hands_free;

import static android.content.ContentValues.TAG;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    EditText email, Password;
    Button createAccountBtn;
    FirebaseAuth fAuth;
    //validation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createAccountBtn = findViewById(R.id.btnCreateAccount);
        createAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              try{  Intent intent = new Intent(MainActivity.this, CreateAccountActivity.class);
                startActivity(intent);
              } catch (Exception e){
                  Toast.makeText(getApplicationContext(), "Button clicked!"+e.getMessage(), Toast.LENGTH_SHORT).show();
              }

            }
        });
    }

    //when user clicks on LOGIN button
    public void onLoginClick(View v) {
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$";
        boolean isValid= true;
        email = findViewById(R.id.email);
        Password = findViewById(R.id.password);
        fAuth = FirebaseAuth.getInstance();

        //get the entered data
        String Email =email.getText().toString().trim();
        String password =Password.getText().toString().trim();


        if (TextUtils.isEmpty(Email)){
            email.setError("Email is Required");
            isValid= false;

        }    else if (!Email.matches(emailPattern)) {
            email.setError("Invalid Email Format.");
            isValid = false;

        }
        if (TextUtils.isEmpty(password)){
            Password.setError("Password is Required");
            isValid= false;
        }

        if (isValid) {
            /* user entered all the information and now we need to check*/

            fAuth.signInWithEmailAndPassword(Email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        //Toast.makeText(MainActivity.this, "user logged in", Toast.LENGTH_SHORT).show();

                        // startActivity(new Intent(getApplicationContext().MainActivity.class));
                        FirebaseUser firebaseUser = fAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid(); // Get user ID

                            // Check in customers
                            DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference("Shoppers Accounts").child(userId);
                            customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        // User is a shopper
                                        String email = dataSnapshot.child("email").getValue(String.class);
                                        String name = dataSnapshot.child("name").getValue(String.class);
                                        // Log.d("User Info", "Role: shopper, Email: " + email + ", Name: " + name);
                                        startActivity(new Intent(MainActivity.this, ShopperNavigationActivity.class));
                                        return;
                                    } else {
                                        // Check in stores
                                        DatabaseReference storeRef = FirebaseDatabase.getInstance().getReference("Store Account").child(userId);
                                        storeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.exists()) {
                                                    // User is a store
                                                    String email = dataSnapshot.child("email").getValue(String.class);
                                                    String name = dataSnapshot.child("name").getValue(String.class);
                                                    // Log.d("User Info", "Role: store, Email: " + email + ", Name: " + name);
                                                    startActivity(new Intent(MainActivity.this, StoreNavigationActivity.class));
                                                    return;
                                                } else {
                                                    // Check in collectors
                                                    DatabaseReference collectorRef = FirebaseDatabase.getInstance().getReference("Collector Account").child(userId);
                                                    collectorRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                                            if (dataSnapshot.exists()) {
                                                                // User is a collector
                                                                String email = dataSnapshot.child("email").getValue(String.class);
                                                                String name = dataSnapshot.child("name").getValue(String.class);
                                                                Log.d("User Info", "Role: collector, Email: " + email + ", Name: " + name);
                                                                Intent intent = new Intent(MainActivity.this, CollectorNavigationActivity.class);
                                                                startActivity(intent);
                                                                return;
                                                            } else {
                                                                Toast.makeText(MainActivity.this, "User role not found" + userId, Toast.LENGTH_SHORT).show();
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(DatabaseError databaseError) {
                                                            Toast.makeText(MainActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Toast.makeText(MainActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Toast.makeText(MainActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        //Toast.makeText(MainActivity.this, "user failed :(", Toast.LENGTH_SHORT).show();
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidUserException e) {
                            Toast.makeText(MainActivity.this, "This email does not have an account.", Toast.LENGTH_SHORT).show();

                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            // Handle the case where the password or the email is incorrect
                            Toast.makeText(getApplicationContext(), "wrong email or/password.", Toast.LENGTH_SHORT).show();
                        } catch (FirebaseNetworkException e) {
                            // Catch exceptions related to network issues
                            // Log.e("LoginError", "Network error: " + e.getMessage()
                            Toast.makeText(getApplicationContext(), "Network error. Please check your internet connection.", Toast.LENGTH_SHORT).show();

                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    }

                }
            });
        }
    }

    public void onForgetPasswordClick(View v) {
        Intent intent = new Intent(MainActivity.this, PasswordResetActivity.class);
        startActivity(intent);
    }

    public class NewActivity extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.fragment_collector_home); // Set your layout for the new activity
        }
    }
}