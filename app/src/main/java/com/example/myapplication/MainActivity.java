package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.data.UserRepository;

public class MainActivity extends AppCompatActivity {

    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        userRepository = new UserRepository(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Spec: neregistrovan (gost) igrač sme samo da igra igre. Meni i igre su otvoreni,
        // a funkcije za registrovane korisnike (profil, notifikacije) traže prijavu.
        setupGameButtons();
        setupRegisteredOnlyButtons();
    }

    private void setupGameButtons() {
        findViewById(R.id.btn_korak_po_korak).setOnClickListener(v ->
                startActivity(new Intent(this, KorakPoKorakActivity.class)));
        findViewById(R.id.btn_moj_broj).setOnClickListener(v ->
                startActivity(new Intent(this, MojBrojActivity.class)));
        findViewById(R.id.btn_skocko).setOnClickListener(v ->
                startActivity(new Intent(this, SkockoActivity.class)));
        findViewById(R.id.btn_ko_zna_zna).setOnClickListener(v ->
                startActivity(new Intent(this, KoZnaZnaActivity.class)));
        findViewById(R.id.btn_spojnice).setOnClickListener(v ->
                startActivity(new Intent(this, SpojniceActivity.class)));
        findViewById(R.id.btn_asocijacije).setOnClickListener(v ->
                startActivity(new Intent(this, AsocijacijeActivity.class)));
    }

    private void setupRegisteredOnlyButtons() {
        Button btnNotifications = findViewById(R.id.btn_notifications);
        btnNotifications.setOnClickListener(v -> requireLogin(NotificationsActivity.class));

        Button btnProfile = findViewById(R.id.btn_profile);
        btnProfile.setOnClickListener(v -> requireLogin(ProfileActivity.class));
    }

    /** Otvara ekran namenjen registrovanim korisnicima; gosta šalje na prijavu. */
    private void requireLogin(Class<?> target) {
        if (userRepository.getCurrentUser() == null) {
            Toast.makeText(this, R.string.login_required_feature, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        startActivity(new Intent(this, target));
    }
}
