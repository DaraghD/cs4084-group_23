package com.example.WeatherApp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private static final String EXTRA_UID = "uid";

    private EditText nameInput, emailInput, passInput;
    private Button   regBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        findViewById(R.id.backBtn).setOnClickListener(v -> {
            finish();
        });

        nameInput  = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passInput  = findViewById(R.id.passwordInput);
        regBtn     = findViewById(R.id.registerBtn);

        // Link back to login screen if needed
        findViewById(R.id.goLogin).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class))
        );

        regBtn.setOnClickListener(v -> {
            String name  = nameInput .getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String pass  = passInput .getText().toString();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generate a new UID under /users
            String uid = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .push()
                    .getKey();

            // Build the user map
            Map<String,Object> user = new HashMap<>();
            user.put("displayName", name);
            user.put("email",       email);
            user.put("password",    pass);
            user.put("preferences", Map.of(
                    "units",        "metric",
                    "language",     "en",
                    "notifyDaily",  false,
                    "notifySevere", false
            ));

            // Write to Firebase
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .setValue(user)
                    .addOnSuccessListener(x -> {
                        Toast.makeText(this, "Registered!", Toast.LENGTH_SHORT).show();
                        // Launch MainActivity with this new UID
                        Intent i = new Intent(this, MainActivity.class);
                        i.putExtra(EXTRA_UID, uid);
                        startActivity(i);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Register failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
        });
    }
}
