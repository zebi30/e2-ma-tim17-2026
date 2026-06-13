package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.UserRepository;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        UserRepository repository = new UserRepository(this);

        Button changeAvatar = findViewById(R.id.change_avatar_button);
        changeAvatar.setOnClickListener(v ->
                Toast.makeText(this, R.string.change_avatar, Toast.LENGTH_SHORT).show());

        Button changePassword = findViewById(R.id.change_password_button);
        changePassword.setOnClickListener(v ->
                startActivity(new Intent(this, PasswordResetActivity.class)));

        Button logout = findViewById(R.id.logout_button);
        logout.setOnClickListener(v -> {
            repository.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
