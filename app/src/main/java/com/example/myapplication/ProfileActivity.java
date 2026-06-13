package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.GameResultRepository;
import com.example.myapplication.data.UserRepository;
import com.example.myapplication.logic.LeagueService;
import com.example.myapplication.logic.StatsService;
import com.example.myapplication.model.ProfileStats;
import com.example.myapplication.model.ScoreAggregate;
import com.example.myapplication.model.User;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Prikaz profila (spec. 2): podaci korisnika, avatar sa okvirom, tokeni,
 * zvezde, liga, region, QR kod za poziv prijatelja, izmena avatara,
 * statistika odigranih igara i odjava.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String[] AVATARS = {"👤", "🦊", "🐼", "🦁", "🐸", "🤖"};
    private static final String[] LEAGUE_ICONS = {"⚪", "🥉", "🥈", "🥇", "🔷", "💎"};
    private static final int[] LEAGUE_NAMES = {
            R.string.league_0, R.string.league_1, R.string.league_2,
            R.string.league_3, R.string.league_4, R.string.league_5
    };
    private static final int QR_SIZE_PX = 400;

    private UserRepository userRepository;
    private StatsService statsService;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userRepository = new UserRepository(this);
        statsService = new StatsService(new GameResultRepository(this));

        findViewById(R.id.change_avatar_button).setOnClickListener(v -> showAvatarPicker());
        findViewById(R.id.logout_button).setOnClickListener(v -> logout());

        user = userRepository.getCurrentUser();
        bindUser();
        bindStats();
    }

    private void bindUser() {
        TextView avatarView = findViewById(R.id.avatar);
        avatarView.setText(AVATARS[user.avatarIndex % AVATARS.length]);

        ((TextView) findViewById(R.id.username_text)).setText(user.username);
        ((TextView) findViewById(R.id.email_text)).setText(user.email);
        ((TextView) findViewById(R.id.tokens_value)).setText(String.valueOf(user.tokens));
        ((TextView) findViewById(R.id.stars_value)).setText(String.valueOf(user.stars));
        ((TextView) findViewById(R.id.region_value)).setText(user.region);

        int leagueIndex = LeagueService.leagueIndexForStars(user.stars);
        ((TextView) findViewById(R.id.league_value)).setText(String.format(Locale.getDefault(),
                "%s %s", LEAGUE_ICONS[leagueIndex], getString(LEAGUE_NAMES[leagueIndex])));

        bindQrCode();
    }

    private void bindQrCode() {
        ImageView qrView = findViewById(R.id.qr_image);
        String content = "slagalica://user/" + user.id + "/" + user.username;
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE,
                    QR_SIZE_PX, QR_SIZE_PX);
            Bitmap bitmap = Bitmap.createBitmap(QR_SIZE_PX, QR_SIZE_PX, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < QR_SIZE_PX; x++) {
                for (int y = 0; y < QR_SIZE_PX; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            qrView.setImageBitmap(bitmap);
        } catch (WriterException ignored) {
        }
    }

    private void showAvatarPicker() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_avatar)
                .setItems(AVATARS, (d, which) -> {
                    userRepository.updateAvatar(user.id, which);
                    user = userRepository.getCurrentUser();
                    bindUser();
                    Toast.makeText(this, R.string.avatar_changed, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void bindStats() {
        ProfileStats stats = statsService.loadFor(user.id);

        setText(R.id.stat_avg_value, buildAvgLine(stats));

        int kzzTotal = stats.kzzCorrect + stats.kzzWrong;
        setText(R.id.stat_kzz_value, kzzTotal == 0
                ? getString(R.string.stat_kzz_no_data)
                : getString(R.string.stat_kzz_fmt,
                percent(stats.kzzCorrect, kzzTotal), percent(stats.kzzWrong, kzzTotal)));

        setText(R.id.stat_moj_broj_value, getString(R.string.stat_moj_broj_na));
        setText(R.id.stat_korak_value, getString(R.string.stat_korak_na));
        setText(R.id.stat_skocko_value, getString(R.string.stat_skocko_na));

        setText(R.id.stat_asocijacije_value, stats.asocTotalRounds == 0
                ? getString(R.string.stat_asocijacije_no_data)
                : getString(R.string.stat_asocijacije_fmt,
                stats.asocSolvedRounds, stats.asocTotalRounds - stats.asocSolvedRounds));

        setText(R.id.stat_spojnice_value, stats.spojniceOpportunities == 0
                ? getString(R.string.stat_spojnice_no_data)
                : getString(R.string.stat_spojnice_fmt,
                percent(stats.spojniceConnected, stats.spojniceOpportunities)));

        setText(R.id.stat_total_value, getString(R.string.stat_total_games_fmt, stats.totalGames));

        setText(R.id.stat_win_loss_value, stats.totalGames == 0
                ? getString(R.string.stat_win_loss_no_data)
                : getString(R.string.stat_win_loss_fmt,
                percent(stats.wins, stats.totalGames),
                percent(stats.totalGames - stats.wins, stats.totalGames)));
    }

    private String buildAvgLine(ProfileStats stats) {
        List<String> parts = new ArrayList<>();
        addAvgPart(parts, getString(R.string.ko_zna_zna), stats.koZnaZna);
        addAvgPart(parts, getString(R.string.spojnice), stats.spojnice);
        addAvgPart(parts, getString(R.string.asocijacije), stats.asocijacije);
        if (parts.isEmpty()) {
            return getString(R.string.stat_avg_no_data);
        }
        return getString(R.string.stat_avg_fmt, String.join(" • ", parts));
    }

    private void addAvgPart(List<String> parts, String gameName, ScoreAggregate aggregate) {
        if (aggregate != null && aggregate.hasData()) {
            parts.add(String.format(Locale.getDefault(), "%s: %.1f (%d–%d)",
                    gameName, aggregate.avg, aggregate.min, aggregate.max));
        }
    }

    private int percent(int part, int total) {
        return total == 0 ? 0 : Math.round(100f * part / total);
    }

    private void setText(int viewId, String text) {
        ((TextView) findViewById(viewId)).setText(text);
    }

    private void logout() {
        userRepository.logout();
        Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
        finish();
    }
}
