package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.UserRepository;
import com.example.myapplication.model.User;

public class RegisterActivity extends AppCompatActivity {

    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        userRepository = new UserRepository(this);

        EditText emailInput = findViewById(R.id.email_input);
        EditText usernameInput = findViewById(R.id.username_input);
        Spinner regionSpinner = findViewById(R.id.region_spinner);
        EditText passwordInput = findViewById(R.id.password_input);
        EditText confirmPasswordInput = findViewById(R.id.confirm_password_input);
        Button registerButton = findViewById(R.id.register_button);
        Button backButton = findViewById(R.id.back_button);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.regions,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        regionSpinner.setAdapter(adapter);

        backButton.setOnClickListener(v -> finish());

        registerButton.setOnClickListener(v -> {
            String email = textOf(emailInput);
            String username = textOf(usernameInput);
            String password = textOf(passwordInput);
            String confirm = textOf(confirmPasswordInput);
            String region = String.valueOf(regionSpinner.getSelectedItem());

            if (email.isEmpty() || username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(this, R.string.passwords_do_not_match, Toast.LENGTH_SHORT).show();
                return;
            }

            User user = userRepository.register(username, email, region, password);
            if (user == null) {
                Toast.makeText(this, R.string.register_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, EmailVerificationActivity.class);
            intent.putExtra("identifier", username);
            intent.putExtra("email", email);
            startActivity(intent);
            finish();
        });
    }

    private String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
