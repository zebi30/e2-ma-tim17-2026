package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup button listeners
        setupLoginButtons();
        setupNotificationButtons();
        setupGameButtons();
        setupProfileButton();
    }

    private void setupProfileButton() {
        Button btnProfile = findViewById(R.id.btn_profile);
        btnProfile.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
    }

    private void setupLoginButtons() {
        Button btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, LoginActivity.class)));

        Button btnRegister = findViewById(R.id.btn_register);
        btnRegister.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RegisterActivity.class)));

        Button btnPasswordReset = findViewById(R.id.btn_password_reset);
        btnPasswordReset.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, PasswordResetActivity.class)));

        Button btnEmailVerification = findViewById(R.id.btn_email_verification);
        btnEmailVerification.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, EmailVerificationActivity.class)));
    }

    private void setupNotificationButtons() {
        Button btnNotifications = findViewById(R.id.btn_notifications);
        btnNotifications.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, NotificationsActivity.class)));
    }

    private void setupGameButtons() {
        Button btnKorakPoKorak = findViewById(R.id.btn_korak_po_korak);
        btnKorakPoKorak.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, KorakPoKorakActivity.class)));

        Button btnMojBroj = findViewById(R.id.btn_moj_broj);
        btnMojBroj.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, MojBrojActivity.class)));

        Button btnSkocko = findViewById(R.id.btn_skocko);
        btnSkocko.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SkockoActivity.class)));

        Button btnKoZnaZna = findViewById(R.id.btn_ko_zna_zna);
        btnKoZnaZna.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, KoZnaZnaActivity.class)));
    }
}