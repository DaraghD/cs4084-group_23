package com.example.WeatherApp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG      = "LoginActivity";
    private static final String EXTRA_UID = "uid";

    private EditText emailInput, passInput;
    private Button   loginBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        findViewById(R.id.backBtn).setOnClickListener(v -> {
            finish();
        });

        emailInput = findViewById(R.id.emailInput);
        passInput  = findViewById(R.id.passwordInput);
        loginBtn   = findViewById(R.id.loginBtn);

        // Navigate to Registration screen
        findViewById(R.id.goRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );

        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String pass  = passInput.getText().toString();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Query /users where email == provided email
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .orderByChild("email")
                    .equalTo(email)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (!snap.exists()) {
                            Toast.makeText(this, "No such user", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Should be exactly one match
                        for (DataSnapshot userSnap : snap.getChildren()) {
                            String storedPw = userSnap.child("password").getValue(String.class);
                            if (pass.equals(storedPw)) {
                                // Success! Grab their UID and launch MainActivity
                                String uid = userSnap.getKey();
                                // Store uid locally, use when welcome screen is launched like
                                SharedPreferences pref = getSharedPreferences("user", MODE_PRIVATE);
                                SharedPreferences.Editor editor = pref.edit();
                                editor.putString("uid", uid);
                                editor.apply();

                                Intent i = new Intent(this, MainActivity.class);
                                i.putExtra(EXTRA_UID, uid);
                                startActivity(i);
                                finish();
                            } else {
                                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase query failed", e);
                        Toast.makeText(this,
                                "Login failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });
    }
}


