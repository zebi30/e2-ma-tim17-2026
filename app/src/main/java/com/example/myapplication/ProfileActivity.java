package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Button changeAvatar = findViewById(R.id.change_avatar_button);
        changeAvatar.setOnClickListener(v ->
                Toast.makeText(this, R.string.change_avatar, Toast.LENGTH_SHORT).show());

        Button logout = findViewById(R.id.logout_button);
        logout.setOnClickListener(v -> finish());
    }
}
