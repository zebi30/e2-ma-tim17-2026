package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.data.SessionManager;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        sessionManager = new SessionManager(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupNotificationButtons();
        setupGameButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
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

        Button btnSpojnice = findViewById(R.id.btn_spojnice);
        btnSpojnice.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SpojniceActivity.class)));

        Button btnAsocijacije = findViewById(R.id.btn_asocijacije);
        btnAsocijacije.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AsocijacijeActivity.class)));

        Button btnProfile = findViewById(R.id.btn_profile);
        btnProfile.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
    }
}