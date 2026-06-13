package com.example.myapplication;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.GameResultRepository;
import com.example.myapplication.data.UserRepository;
import com.example.myapplication.logic.BotOpponent;
import com.example.myapplication.model.GameResult;
import com.example.myapplication.model.User;

import java.util.Random;

/**
 * Korak po korak – 2 runde (2 x 70s). Igrač igra svoju rundu (runda 1), a
 * protivnikova runda (runda 2) je simulirana preko {@link BotOpponent}.
 * Maksimalno 7 koraka po 10 sekundi. Bodovanje: 1. korak = 20, svaki naredni
 * korak -2 boda. Ako vlasnik runde ne pogodi, protivnik ima priliku za 5 bodova.
 * Max 40, min 0.
 */
public class KorakPoKorakActivity extends AppCompatActivity {

    private static final int MAX_STEPS = 7;
    private static final int STEP_MS = 10000;
    private static final int STEAL_POINTS = 5;

    private static final int PHASE_PLAYER = 0;
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
    private TextView scoreValue;
    private Button submitButton;
    private Button passButton;

    private CountDownTimer timer;
    private int stepIndex = 0;
    private int phase = PHASE_PLAYER;
    private int playerScore = 0;
    private int opponentScore = 0;
    private int lastPoints = 0;
    private boolean finished = false;
    private boolean playerSolvedOwnRound = false;
    private Puzzle puzzle;

    private final Random random = new Random();
    private final BotOpponent bot = new BotOpponent();
    private UserRepository userRepository;
    private GameResultRepository resultRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_korak_po_korak);
        userRepository = new UserRepository(this);
        resultRepository = new GameResultRepository(this);

        Button quit = findViewById(R.id.quit_button);
        statusText = findViewById(R.id.status_text);
        timerView = findViewById(R.id.timer);
        stepNumber = findViewById(R.id.step_number);
        hintText = findViewById(R.id.hint_text);
        guessInput = findViewById(R.id.guess_input);
        scoreValue = findViewById(R.id.score_value);
        submitButton = findViewById(R.id.submit_button);
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

        startPlayerRound();
    }

    private int pointsForStep(int stepOneBased) {
        return 20 - 2 * (stepOneBased - 1);
    }

    private Puzzle randomPuzzle() {
        return PUZZLES[random.nextInt(PUZZLES.length)];
    }

    // ----- Round 1: player -----

    private void startPlayerRound() {
        phase = PHASE_PLAYER;
        puzzle = randomPuzzle();
        stepIndex = 0;
        statusText.setText(getString(R.string.round_label, 1) + " — " + getString(R.string.your_round));
        passButton.setEnabled(true);
        renderStep();
        startTimer(STEP_MS);
    }

    private void renderStep() {
        stepNumber.setText(getString(R.string.step, stepIndex + 1));
        hintText.setText(puzzle.hints[Math.min(stepIndex, puzzle.hints.length - 1)]);
        scoreValue.setText(String.valueOf(playerScore));
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
                playerScore += STEAL_POINTS;
                scoreValue.setText(String.valueOf(playerScore));
                Toast.makeText(this, getString(R.string.your_steal_win, STEAL_POINTS), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.your_steal_fail, Toast.LENGTH_SHORT).show();
            }
            finishMatch();
            return;
        }

        // PHASE_PLAYER
        if (guess.equals(puzzle.word.toLowerCase())) {
            lastPoints = pointsForStep(stepIndex + 1);
            playerScore += lastPoints;
            playerSolvedOwnRound = true;
            scoreValue.setText(String.valueOf(playerScore));
            playerRoundEnded(true);
        } else {
            Toast.makeText(this, R.string.wrong_try_next_hint, Toast.LENGTH_SHORT).show();
            advanceStep();
        }
    }

    private void advanceStep() {
        if (phase != PHASE_PLAYER) return;
        if (timer != null) timer.cancel();
        stepIndex++;
        if (stepIndex >= MAX_STEPS) {
            playerRoundEnded(false);
            return;
        }
        renderStep();
        startTimer(STEP_MS);
    }

    private void playerRoundEnded(boolean solved) {
        if (timer != null) timer.cancel();
        StringBuilder sb = new StringBuilder();
        if (solved) {
            sb.append(getString(R.string.you_solved, lastPoints));
        } else {
            sb.append(getString(R.string.you_failed_term));
            if (bot.korakStealSucceeds()) {
                opponentScore += STEAL_POINTS;
                sb.append("\n").append(getString(R.string.opp_steal_win, STEAL_POINTS));
            } else {
                sb.append("\n").append(getString(R.string.opp_steal_fail));
            }
        }
        showTransition(sb.toString(), this::opponentRound);
    }

    // ----- Round 2: opponent (simulated) -----

    private void opponentRound() {
        phase = PHASE_PLAYER;
        puzzle = randomPuzzle();
        statusText.setText(getString(R.string.round_label, 2) + " — " + getString(R.string.opponent_round));

        int botStep = bot.korakSolveStep();
        if (botStep > 0) {
            int pts = pointsForStep(botStep);
            opponentScore += pts;
            stepNumber.setText(getString(R.string.step, botStep));
            hintText.setText(puzzle.hints[botStep - 1]);
            guessInput.setText("");
            showTransition(getString(R.string.opponent_solved, pts), this::finishMatch);
        } else {
            // Bot failed: all hints are open, player gets one steal attempt.
            phase = PHASE_STEAL;
            passButton.setEnabled(false);
            stepNumber.setText(getString(R.string.step, MAX_STEPS));
            StringBuilder all = new StringBuilder();
            for (int i = 0; i < puzzle.hints.length; i++) {
                all.append(i + 1).append(". ").append(puzzle.hints[i]);
                if (i < puzzle.hints.length - 1) all.append("\n");
            }
            hintText.setText(all.toString());
            guessInput.setText("");
            statusText.setText(getString(R.string.korak_steal_chance, STEAL_POINTS));
            startTimer(STEP_MS);
        }
    }

    // ----- Shared -----

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
            Toast.makeText(this, R.string.your_steal_fail, Toast.LENGTH_SHORT).show();
            finishMatch();
        } else if (phase == PHASE_PLAYER) {
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

        boolean won = playerScore > opponentScore;
        User user = userRepository.getCurrentUser();
        if (user != null) {
            resultRepository.insert(new GameResult(user.id, GameResult.GAME_KORAK_PO_KORAK, playerScore,
                    opponentScore, won, playerSolvedOwnRound ? 1 : 0, 1, System.currentTimeMillis()));
        }
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.match_result_title)
                .setMessage(getString(R.string.match_result_message, playerScore, opponentScore))
                .setPositiveButton(android.R.string.ok, (d, w) -> finish())
                .show();
    }

    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.exit_game_title)
                .setMessage(R.string.exit_game_message)
                .setPositiveButton(R.string.exit_game_yes, (dialog, which) -> {
                    finished = true;
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
