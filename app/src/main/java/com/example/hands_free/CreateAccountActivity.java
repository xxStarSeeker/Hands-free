package com.example.hands_free;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.navigation.ui.AppBarConfiguration;

import com.example.hands_free.databinding.ActivityCreateAccountBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CreateAccountActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityCreateAccountBinding binding;
    private static final String TAG = "REGISTER";

    // Declare EditText variables
    EditText shopperEmail;
    EditText shopperPassword;
    EditText shopperRePassword;
    EditText shopperName;
    EditText shopperPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize EditText fields after the layout has been set
        shopperEmail = findViewById(R.id.shopperEmail);
        shopperPassword = findViewById(R.id.ShopperPassword);
        shopperRePassword = findViewById(R.id.re_password);
        shopperName = findViewById(R.id.shopperName);
        shopperPhone = findViewById(R.id.shopperPhone);

    }

    public void onCreatClick(View view) {

        String email = shopperEmail.getText().toString().trim();
        String pass = shopperPassword.getText().toString().trim();
        String repass = shopperRePassword.getText().toString().trim();
        String name= shopperName.getText().toString().trim();
        String phone= shopperPhone.getText().toString().trim();

        FirebaseAuth fAuth = FirebaseAuth.getInstance();

        // Validation
        boolean isValid = true;
        if (TextUtils.isEmpty(email)) {
            shopperEmail.setError("Email is Required.");
            isValid =false;
        }
        if (TextUtils.isEmpty(phone)) {
            shopperPhone.setError("Phone number is Required.");
            isValid = false;
        } else if (!phone.startsWith("05")) {
            shopperPhone.setError("Phone number must start with '05'.");
            isValid = false;
        } else if(phone.length() != 10) {
            shopperPhone.setError("Phone number must be exactly 10 digits.");
            isValid = false;
        }
        if (TextUtils.isEmpty(name)) {
            shopperName.setError("Name is Required.");
            isValid =false;

        }else if (name.length()<2){
            shopperName.setError("Name length should be more than one letter.");
            isValid =false;

        }else if(!isValidString(name)){
            shopperName.setError("Name should be Letters only.");
            isValid =false;
        }

        if (TextUtils.isEmpty(repass)) {
            shopperRePassword.setError("Re-entering password is required.");
            isValid =false;
        }

        // Check for a valid email format using Patterns
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$";
        if (!email.matches(emailPattern)) {
            shopperEmail.setError("Invalid Email Format.");
            isValid = false;

        }
        if (!pass.equals(repass)) {
            shopperRePassword.setError("Password does not match.");
            isValid =false;
        }
        if (TextUtils.isEmpty(pass)) {
            shopperPassword.setError("Password is Required.");
            isValid = false;
        } else {
            if (pass.length() < 6) {
                shopperPassword.setError("Password length should be more than 6 characters");
                isValid = false;
            } else if (!isValidPassword(pass)) {
                shopperPassword.setError("Password must contain both letters and numbers.");
                isValid = false;
            }
        }

        // Proceed with account creation logic after all checks pass
        //Registering Progress
        if (isValid) {
            fAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        //Adding the next step
                        FirebaseUser firebaseUser = fAuth.getCurrentUser();

                        //Entering data into the Firebase Realtime database name ,phone, id and email
                        String StringShopperName = shopperName.getText().toString();
                        String StringShopperEmail = shopperEmail.getText().toString();
                        String StringShopperPhone = shopperPhone.getText().toString();
                        String userID = firebaseUser.getUid();
                        Shopper writeData = new Shopper( userID ,StringShopperName, StringShopperEmail, StringShopperPhone);
                        DatabaseReference refProfile = FirebaseDatabase.getInstance().getReference("Shoppers Accounts");

                        //Adding to realtime database to store in further data
                        refProfile.child(firebaseUser.getUid()).setValue(writeData).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(CreateAccountActivity.this, "User Has Been Created", Toast.LENGTH_SHORT).show();
                                    //  back to login page
                                    finish();
                                } else {
                                    Toast.makeText(CreateAccountActivity.this, "User creation Failed", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    } else {
                        // Toast.makeText(shopperRegister.this, "Error!"+ task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthUserCollisionException e) {
                            Toast.makeText(CreateAccountActivity.this, "Email already in use", Toast.LENGTH_SHORT).show();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            Toast.makeText(CreateAccountActivity.this, "Your email is invalid or already in use.", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                            Toast.makeText(CreateAccountActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                    }
                }
            });
        }//end of else case
    }

    public static boolean isValidString(String input) {
        // Check if the input is not null and matches the regex for letters only
        return input != null && input.matches("[a-zA-Z]+");
    }

    public static boolean isValidPassword(String password) {
        boolean hasLetter = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }

            // If both conditions are met, no need to check further
            if (hasLetter && hasDigit) {
                return true;
            }
        }
        return false; // Return false if either condition isn't met
    }


    public void onBackClick(View view) {

        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
    }
}