package com.example.myapplication;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.GameResultRepository;
import com.example.myapplication.data.UserRepository;
import com.example.myapplication.model.GameResult;
import com.example.myapplication.model.User;
import com.example.myapplication.ui.PlayerBar;

import java.util.Random;

/**
 * Korak po korak – 2 runde (2 x 70s) za dva igrača na istom uređaju (hot-seat).
 * Rundu 1 igra Igrač 1, rundu 2 Igrač 2. Maksimalno 7 koraka po 10 sekundi.
 * Bodovanje: 1. korak = 20, svaki naredni korak -2 boda. Ako vlasnik runde ne
 * pogodi, drugi igrač ima priliku (10s) za 5 bodova. Max 40, min 0.
 */
public class KorakPoKorakActivity extends AppCompatActivity {

    private static final int MAX_STEPS = 7;
    private static final int STEP_MS = 10000;
    private static final int STEAL_POINTS = 5;

    private static final int PHASE_OWNER = 0;
    private static final int PHASE_STEAL = 1;
    private static final int PHASE_DONE = 2;

    private static final class Puzzle {
        final String word;
        final String[] hints;
        Puzzle(String word, String[] hints) {
            this.word = word;
            this.hints = hints;
        }
    }

    private static final Puzzle[] PUZZLES = {
            new Puzzle("ormar", new String[]{
                    "Izrađuje se od drveta ili iverice",
                    "Deo je nameštaja u kući",
                    "Najčešće stoji uza zid sobe",
                    "Ima vrata koja se otvaraju",
                    "Unutra ima police i šipku",
                    "U njega odlažeš svoju odeću",
                    "Sinonim za garderober"
            }),
            new Puzzle("kišobran", new String[]{
                    "Koristiš ga napolju",
                    "Predmet je koji se nosi u ruci",
                    "Najpotrebniji je u jesen",
                    "Štiti te od nečega što pada s neba",
                    "Lako se otvara i zatvara",
                    "Ima ručku i rastegljivo platno",
                    "Spasava te kada pada kiša"
            }),
            new Puzzle("bicikl", new String[]{
                    "To je prevozno sredstvo",
                    "Nema motor ni gorivo",
                    "Pokrećeš ga sopstvenom snagom",
                    "Ima ram i dva točka",
                    "Voziš ga okrećući pedale",
                    "Ima upravljač i sedište",
                    "Deca ga uče bez pomoćnih točkića"
            }),
            new Puzzle("kafa", new String[]{
                    "To je topao napitak",
                    "Pije se iz šoljice",
                    "Najčešće se pije ujutru",
                    "Sadrži kofein koji razbuđuje",
                    "Može biti crna, sa mlekom ili espreso",
                    "Pravi se od prženih i mlevenih zrna",
                    "Mnogi bez nje ne mogu da započnu dan"
            }),
            new Puzzle("sunce", new String[]{
                    "Vidi se samo danju",
                    "Nalazi se visoko na nebu",
                    "Veoma je daleko od nas",
                    "To je ogromna užarena lopta",
                    "Daje nam svetlost i toplotu",
                    "Oko njega kruži planeta Zemlja",
                    "Zvezda u centru našeg sistema"
            })
    };

    private TextView statusText;
    private TextView timerView;
    private TextView stepNumber;
    private TextView hintText;
    private EditText guessInput;
    private TextView p1ScoreView;
    private TextView p2ScoreView;
    private View p1Chip;
    private View p2Chip;
    private Button passButton;

    private CountDownTimer timer;
    private int stepIndex = 0;
    private int phase = PHASE_OWNER;
    private int roundOwner = 1;
    private int active = 1;
    private int p1 = 0;
    private int p2 = 0;
    private int lastPoints = 0;
    private boolean finished = false;
    private Puzzle puzzle;

    private final Random random = new Random();
    private UserRepository userRepository;
    private GameResultRepository resultRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_korak_po_korak);
        userRepository = new UserRepository(this);
        resultRepository = new GameResultRepository(this);
        PlayerBar.bind(this, userRepository.getCurrentUser());

        Button quit = findViewById(R.id.quit_button);
        statusText = findViewById(R.id.status_text);
        timerView = findViewById(R.id.timer);
        stepNumber = findViewById(R.id.step_number);
        hintText = findViewById(R.id.hint_text);
        guessInput = findViewById(R.id.guess_input);
        p1ScoreView = findViewById(R.id.p1_score);
        p2ScoreView = findViewById(R.id.p2_score);
        p1Chip = findViewById(R.id.p1_chip);
        p2Chip = findViewById(R.id.p2_chip);
        Button submitButton = findViewById(R.id.submit_button);
        passButton = findViewById(R.id.pass_button);

        quit.setOnClickListener(v -> confirmExit());
        submitButton.setOnClickListener(v -> submitGuess());
        passButton.setOnClickListener(v -> advanceStep());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmExit();
            }
        });

        startRound(1);
    }

    private int other(int player) {
        return player == 1 ? 2 : 1;
    }

    private int pointsForStep(int stepOneBased) {
        return 20 - 2 * (stepOneBased - 1);
    }

    private void award(int player, int pts) {
        if (player == 1) p1 += pts; else p2 += pts;
    }

    private Puzzle randomPuzzle() {
        return PUZZLES[random.nextInt(PUZZLES.length)];
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
        puzzle = randomPuzzle();
        stepIndex = 0;
        passButton.setEnabled(true);
        statusText.setText(getString(R.string.round_turn, owner, owner));
        renderStep();
        renderScores();
        startTimer(STEP_MS);
    }

    private void renderStep() {
        stepNumber.setText(getString(R.string.step, stepIndex + 1));
        hintText.setText(puzzle.hints[Math.min(stepIndex, puzzle.hints.length - 1)]);
        guessInput.setText("");
    }

    private void submitGuess() {
        if (phase == PHASE_DONE) return;
        String guess = guessInput.getText() == null ? "" : guessInput.getText().toString().trim().toLowerCase();
        if (guess.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (phase == PHASE_STEAL) {
            if (timer != null) timer.cancel();
            if (guess.equals(puzzle.word.toLowerCase())) {
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
        if (guess.equals(puzzle.word.toLowerCase())) {
            if (timer != null) timer.cancel();
            lastPoints = pointsForStep(stepIndex + 1);
            award(active, lastPoints);
            renderScores();
            showTransition(getString(R.string.player_solved_pts, roundOwner, lastPoints), this::afterRound);
        } else {
            Toast.makeText(this, R.string.wrong_try_next_hint, Toast.LENGTH_SHORT).show();
            advanceStep();
        }
    }

    private void advanceStep() {
        if (phase != PHASE_OWNER) return;
        if (timer != null) timer.cancel();
        stepIndex++;
        if (stepIndex >= MAX_STEPS) {
            ownerFailed();
            return;
        }
        renderStep();
        startTimer(STEP_MS);
    }

    private void ownerFailed() {
        if (timer != null) timer.cancel();
        int stealer = other(roundOwner);
        String msg = getString(R.string.player_missed_term, roundOwner)
                + "\n" + getString(R.string.turn_handoff, stealer);
        showTransition(msg, () -> beginSteal(stealer));
    }

    private void beginSteal(int stealer) {
        phase = PHASE_STEAL;
        active = stealer;
        passButton.setEnabled(false);
        // All hints are open now, so the stealer has full information.
        stepNumber.setText(getString(R.string.step, MAX_STEPS));
        StringBuilder all = new StringBuilder();
        for (int i = 0; i < puzzle.hints.length; i++) {
            all.append(i + 1).append(". ").append(puzzle.hints[i]);
            if (i < puzzle.hints.length - 1) all.append("\n");
        }
        hintText.setText(all.toString());
        guessInput.setText("");
        statusText.setText(getString(R.string.steal_turn_pts, stealer, STEAL_POINTS));
        renderScores();
        startTimer(STEP_MS);
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
            advanceStep();
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
            resultRepository.insert(new GameResult(user.id, GameResult.GAME_KORAK_PO_KORAK, p1, p2,
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
