package com.example.myapplication;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.data.GameResultRepository;
import com.example.myapplication.data.UserRepository;
import com.example.myapplication.model.GameResult;
import com.example.myapplication.model.User;
import com.example.myapplication.ui.PlayerBar;

import java.util.Random;

/**
 * Skočko – 2 runde (2 x 30s) za dva igrača na istom uređaju (hot-seat).
 * Rundu 1 igra Igrač 1, rundu 2 Igrač 2. Bodovanje: 1-2. pokušaj = 20,
 * 3-4. = 15, 5-6. = 10. Ako vlasnik runde ne pogodi, drugi igrač ima jednu
 * priliku (10s) da osvoji 10 bodova. Max 40, min 0.
 */
public class SkockoActivity extends AppCompatActivity {

    private static final String[] SYMBOLS = {"🤡", "🟧", "🔵", "❤️", "🔺", "⭐"};
    private static final int LENGTH = 4;
    private static final int MAX_ATTEMPTS = 6;
    private static final int STEAL_POINTS = 10;

    private static final int PHASE_OWNER = 0;
    private static final int PHASE_STEAL = 1;
    private static final int PHASE_DONE = 2;

    private final String[] target = new String[LENGTH];
    private final String[] currentGuess = new String[LENGTH];
    private int filled = 0;
    private int currentAttempt = 0;
    private int phase = PHASE_OWNER;
    private int roundOwner = 1;
    private int active = 1;
    private int p1 = 0;
    private int p2 = 0;
    private int lastPoints = 0;
    private boolean finished = false;

    private final Random random = new Random();
    private CountDownTimer timer;

    private TextView statusText;
    private TextView attemptText;
    private TextView timerView;
    private TextView p1ScoreView;
    private TextView p2ScoreView;
    private View p1Chip;
    private View p2Chip;
    private TextView[] slots;
    private LinearLayout attemptsContainer;
    private UserRepository userRepository;
    private GameResultRepository resultRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skocko);
        userRepository = new UserRepository(this);
        resultRepository = new GameResultRepository(this);
        PlayerBar.bind(this, userRepository.getCurrentUser());

        Button quit = findViewById(R.id.quit_button);
        Button submit = findViewById(R.id.submit_button);
        Button backspace = findViewById(R.id.backspace_button);
        statusText = findViewById(R.id.status_text);
        attemptText = findViewById(R.id.attempt_text);
        timerView = findViewById(R.id.timer);
        p1ScoreView = findViewById(R.id.p1_score);
        p2ScoreView = findViewById(R.id.p2_score);
        p1Chip = findViewById(R.id.p1_chip);
        p2Chip = findViewById(R.id.p2_chip);
        attemptsContainer = findViewById(R.id.attempts_container);
        slots = new TextView[]{
                findViewById(R.id.slot_0),
                findViewById(R.id.slot_1),
                findViewById(R.id.slot_2),
                findViewById(R.id.slot_3)
        };

        quit.setOnClickListener(v -> confirmExit());
        backspace.setOnClickListener(v -> removeLastSymbol());
        submit.setOnClickListener(v -> submitGuess());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmExit();
            }
        });

        bindSymbolButtons();
        startRound(1);
    }

    private int other(int player) {
        return player == 1 ? 2 : 1;
    }

    private int pointsForAttempt(int attempt) {
        if (attempt <= 2) return 20;
        if (attempt <= 4) return 15;
        return 10;
    }

    private void award(int player, int pts) {
        if (player == 1) p1 += pts; else p2 += pts;
    }

    private void randomizeTarget() {
        for (int i = 0; i < LENGTH; i++) {
            target[i] = SYMBOLS[random.nextInt(SYMBOLS.length)];
        }
    }

    private void renderScores() {
        p1ScoreView.setText(String.valueOf(p1));
        p2ScoreView.setText(String.valueOf(p2));
        p1Chip.setBackgroundResource(active == 1 ? R.drawable.header_chip_active : R.drawable.header_chip);
        p2Chip.setBackgroundResource(active == 2 ? R.drawable.header_chip_active : R.drawable.header_chip);
    }

    private void startRound(int owner) {
        roundOwner = owner;
        active = owner;
        phase = PHASE_OWNER;
        randomizeTarget();
        currentAttempt = 0;
        clearGuess();
        attemptsContainer.removeAllViews();
        statusText.setText(getString(R.string.round_turn, owner, owner));
        attemptText.setText(getString(R.string.attempt, 1));
        renderScores();
        startTimer(30000);
    }

    private void submitGuess() {
        if (phase == PHASE_DONE) return;
        if (filled < LENGTH) {
            Toast.makeText(this, R.string.skocko_pick_full, Toast.LENGTH_SHORT).show();
            return;
        }

        int[] fb = feedback(currentGuess, target);
        int exact = fb[0];
        addAttemptRow(currentGuess.clone(), exact, fb[1], currentAttempt + 1);

        if (phase == PHASE_STEAL) {
            if (timer != null) timer.cancel();
            if (exact == LENGTH) {
                award(active, STEAL_POINTS);
                renderScores();
                Toast.makeText(this, getString(R.string.steal_win_pts, active, STEAL_POINTS), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.steal_none, Toast.LENGTH_SHORT).show();
            }
            afterRound();
            return;
        }

        // PHASE_OWNER
        if (exact == LENGTH) {
            if (timer != null) timer.cancel();
            lastPoints = pointsForAttempt(currentAttempt + 1);
            award(active, lastPoints);
            renderScores();
            showTransition(getString(R.string.player_solved_pts, roundOwner, lastPoints), this::afterRound);
            return;
        }
        currentAttempt++;
        if (currentAttempt >= MAX_ATTEMPTS) {
            ownerFailed();
            return;
        }
        clearGuess();
        attemptText.setText(getString(R.string.attempt, currentAttempt + 1));
    }

    private void ownerFailed() {
        if (timer != null) timer.cancel();
        int stealer = other(roundOwner);
        String msg = getString(R.string.player_missed_combo, roundOwner)
                + "\n" + getString(R.string.turn_handoff, stealer);
        showTransition(msg, () -> beginSteal(stealer));
    }

    private void beginSteal(int stealer) {
        phase = PHASE_STEAL;
        active = stealer;
        clearGuess();
        statusText.setText(getString(R.string.steal_turn_pts, stealer, STEAL_POINTS));
        attemptText.setText(R.string.one_attempt_left);
        renderScores();
        startTimer(10000);
    }

    private void afterRound() {
        if (roundOwner == 1) {
            active = 2;
            renderScores();
            showTransition(getString(R.string.turn_handoff, 2), () -> startRound(2));
        } else {
            finishMatch();
        }
    }

    // ----- Guess board -----

    /** Returns [exact (symbol+position), present (symbol only)]. */
    private int[] feedback(String[] guess, String[] solution) {
        int exact = 0;
        boolean[] solUsed = new boolean[LENGTH];
        boolean[] guessUsed = new boolean[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            if (guess[i] != null && guess[i].equals(solution[i])) {
                exact++;
                solUsed[i] = true;
                guessUsed[i] = true;
            }
        }
        int present = 0;
        for (int i = 0; i < LENGTH; i++) {
            if (guessUsed[i] || guess[i] == null) continue;
            for (int j = 0; j < LENGTH; j++) {
                if (!solUsed[j] && guess[i].equals(solution[j])) {
                    solUsed[j] = true;
                    present++;
                    break;
                }
            }
        }
        return new int[]{exact, present};
    }

    private void bindSymbolButtons() {
        GridLayout grid = findViewById(R.id.symbols_grid);
        for (int i = 0; i < grid.getChildCount() && i < SYMBOLS.length; i++) {
            View tile = grid.getChildAt(i);
            String symbol = SYMBOLS[i];
            tile.setOnClickListener(v -> addSymbol(symbol));
        }
    }

    private void addSymbol(String symbol) {
        if (phase == PHASE_DONE || filled >= LENGTH) return;
        currentGuess[filled] = symbol;
        slots[filled].setText(symbol);
        filled++;
    }

    private void removeLastSymbol() {
        if (filled == 0) return;
        filled--;
        currentGuess[filled] = null;
        slots[filled].setText(R.string.skocko_placeholder);
    }

    private void clearGuess() {
        filled = 0;
        for (int i = 0; i < LENGTH; i++) {
            currentGuess[i] = null;
            slots[i].setText(R.string.skocko_placeholder);
        }
    }

    private void addAttemptRow(String[] guess, int red, int yellow, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.edit_text_background);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(6);
        row.setLayoutParams(rowParams);

        TextView idx = new TextView(this);
        idx.setText(index + ".");
        idx.setTextSize(14);
        idx.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        idx.setPadding(0, 0, dp(8), 0);
        row.addView(idx);

        for (int i = 0; i < LENGTH; i++) {
            TextView sym = new TextView(this);
            sym.setText(guess[i] == null ? "" : guess[i]);
            sym.setTextSize(24);
            sym.setPadding(dp(2), 0, dp(2), 0);
            row.addView(sym);
        }

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        row.addView(spacer);

        for (int i = 0; i < LENGTH; i++) {
            int color;
            if (i < red) color = R.color.peg_correct;
            else if (i < red + yellow) color = R.color.peg_present;
            else color = R.color.peg_absent;
            TextView peg = new TextView(this);
            peg.setText("■");
            peg.setTextSize(18);
            peg.setTypeface(Typeface.DEFAULT_BOLD);
            peg.setTextColor(ContextCompat.getColor(this, color));
            peg.setPadding(dp(2), 0, dp(2), 0);
            row.addView(peg);
        }

        attemptsContainer.addView(row);
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private void startTimer(long ms) {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(ms, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerView.setText(getString(R.string.time_remaining, millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                onTimeExpired();
            }
        }.start();
    }

    private void onTimeExpired() {
        if (phase == PHASE_STEAL) {
            Toast.makeText(this, R.string.steal_none, Toast.LENGTH_SHORT).show();
            afterRound();
        } else if (phase == PHASE_OWNER) {
            ownerFailed();
        }
    }

    private void showTransition(String message, Runnable next) {
        if (isFinishing()) return;
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton(R.string.continue_match, (d, w) -> next.run())
                .show();
    }

    private void finishMatch() {
        if (finished) return;
        finished = true;
        phase = PHASE_DONE;
        if (timer != null) timer.cancel();

        String winner = p1 > p2 ? getString(R.string.winner_player, 1)
                : p2 > p1 ? getString(R.string.winner_player, 2)
                : getString(R.string.winner_draw);
        User user = userRepository.getCurrentUser();
        if (user != null) {
            resultRepository.insert(new GameResult(user.id, GameResult.GAME_SKOCKO, p1, p2,
                    p1 >= p2, p1, p2, System.currentTimeMillis()));
        }
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.match_result_title)
                .setMessage(getString(R.string.match_two, p1, p2, winner))
                .setPositiveButton(android.R.string.ok, (d, w) -> finish())
                .show();
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.exit_game_title)
                .setMessage(R.string.exit_game_message)
                .setPositiveButton(R.string.exit_game_yes, (dialog, which) -> {
                    finished = true;
                    phase = PHASE_DONE;
                    if (timer != null) timer.cancel();
                    finish();
                })
                .setNegativeButton(R.string.exit_game_no, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}
