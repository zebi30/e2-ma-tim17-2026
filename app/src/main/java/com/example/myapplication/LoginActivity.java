package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.UserRepository;
import com.example.myapplication.model.User;

public class LoginActivity extends AppCompatActivity {

    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        userRepository = new UserRepository(this);

        EditText identifierInput = findViewById(R.id.email_input);
        EditText passwordInput = findViewById(R.id.password_input);
        Button loginButton = findViewById(R.id.login_button);
        TextView forgotPassword = findViewById(R.id.forgot_password);
        TextView registerLink = findViewById(R.id.register_link);

        loginButton.setOnClickListener(v -> {
            String identifier = identifierInput.getText() == null ? "" : identifierInput.getText().toString().trim();
            String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString().trim();
            if (identifier.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }
            User user = userRepository.login(identifier, password);
            if (user == null) {
                if (userRepository.accountExists(identifier) && !userRepository.isVerified(identifier)) {
                    String email = userRepository.getEmailForIdentifier(identifier);
                    Intent intent = new Intent(this, EmailVerificationActivity.class);
                    intent.putExtra("identifier", identifier);
                    intent.putExtra("email", email == null ? identifier : email);
                    startActivity(intent);
                    Toast.makeText(this, R.string.account_not_verified, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, getString(R.string.login_success, user.username), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        forgotPassword.setOnClickListener(v -> startActivity(new Intent(this, PasswordResetActivity.class)));
        registerLink.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }
}
