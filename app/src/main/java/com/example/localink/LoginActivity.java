package com.example.localink;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText emailInput, passwordInput, usernameInput;
    private Button loginButton;
    private TextView toggleSignUpText, titleText, usernameLabel;
    private ImageView passwordVisibilityToggle;

    private boolean isLoginMode = true; // Track whether the activity is in login mode or sign-up mode
    private boolean isPasswordVisible = false; // Track password visibility

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        usernameInput = findViewById(R.id.usernameInput); // Username input field
        loginButton = findViewById(R.id.loginButton);
        usernameLabel = findViewById(R.id.usernameLabel);
        toggleSignUpText = findViewById(R.id.toggleSignUpText); // Toggle sign-up/login
        titleText = findViewById(R.id.title); // Bind title TextView for dynamic changes
        passwordVisibilityToggle = findViewById(R.id.passwordVisibilityToggle);

        // Set click listener for login button
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            // Validate inputs
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            } else {
                if (isLoginMode) {
                    loginUser(email, password);
                } else {
                    signUpUser(email, password);
                }
            }
        });

        // Toggle between login and sign-up
        toggleSignUpText.setOnClickListener(v -> {
            if (isLoginMode) {
                // Switch to sign-up mode
                isLoginMode = false;
                loginButton.setText("Sign Up");
                titleText.setText("Sign Up");
                toggleSignUpText.setText("Already have an account? Log in");
                usernameInput.setVisibility(View.VISIBLE);
                usernameLabel.setVisibility(View.VISIBLE);// Show username input
            } else {
                // Switch to login mode
                isLoginMode = true;
                loginButton.setText("Log In");
                titleText.setText("Log In");
                toggleSignUpText.setText("Don't have an account? Sign up");
                usernameInput.setVisibility(View.GONE); // Hide username input
                usernameLabel.setVisibility(View.GONE);
            }
        });

        // Toggle password visibility
        passwordVisibilityToggle.setOnClickListener(v -> {
            if (isPasswordVisible) {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordVisibilityToggle.setImageResource(R.drawable.eye_visibility);
                isPasswordVisible = false;
            } else {
                passwordInput.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                passwordVisibilityToggle.setImageResource(R.drawable.eye_visibility);
                isPasswordVisible = true;
            }
        });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signUpUser(String email, String password) {
        String username = usernameInput.getText().toString().trim();

        if (username.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Please enter a username.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserDataToFirestore(user, username);
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Sign Up Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserDataToFirestore(FirebaseUser user, String username) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("username", username);

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LoginActivity.this, "User signed up successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Error saving user data.", Toast.LENGTH_SHORT).show());
    }
}
