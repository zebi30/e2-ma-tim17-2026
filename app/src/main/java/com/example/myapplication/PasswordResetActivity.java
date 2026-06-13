package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.SessionManager;
import com.example.myapplication.data.UserRepository;

public class PasswordResetActivity extends AppCompatActivity {

    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset);
        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);

        Button backButton = findViewById(R.id.back_button);
        EditText oldPasswordInput = findViewById(R.id.old_password_input);
        EditText newPasswordInput = findViewById(R.id.new_password_input);
        EditText confirmNewPasswordInput = findViewById(R.id.confirm_new_password_input);
        Button resetButton = findViewById(R.id.reset_button);

        backButton.setOnClickListener(v -> finish());

        resetButton.setOnClickListener(v -> {
            String oldPw = text(oldPasswordInput);
            String newPw = text(newPasswordInput);
            String confirmPw = text(confirmNewPasswordInput);

            if (oldPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPw.equals(confirmPw)) {
                Toast.makeText(this, R.string.passwords_do_not_match, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(this, R.string.must_be_logged_in, Toast.LENGTH_SHORT).show();
                return;
            }
            long userId = sessionManager.getUserId();
            boolean ok = userRepository.resetPassword(userId, oldPw, newPw);
            if (ok) {
                Toast.makeText(this, R.string.password_reset_success, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, R.string.password_reset_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String text(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
