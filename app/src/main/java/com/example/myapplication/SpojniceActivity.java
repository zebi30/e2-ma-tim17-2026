package com.example.myapplication;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.data.GameContentRepository;
import com.example.myapplication.data.GameResultRepository;
import com.example.myapplication.data.UserRepository;
import com.example.myapplication.logic.BotOpponent;
import com.example.myapplication.logic.SpojniceEngine;
import com.example.myapplication.model.GameResult;
import com.example.myapplication.model.SpojnicePair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Spojnice: 2 runde, svaku započinje po jedan igrač (spec. 2.a). Nakon što
 * igrač koji počinje prođe kroz sve pojmove, preostale pojmove povezuje
 * drugi igrač (spec. 2.c). Svaki povezan par nosi 2 boda. Igra se protiv
 * simuliranog protivnika; rezultat se upisuje u bazu za statistiku.
 */
public class SpojniceActivity extends AppCompatActivity {

    private static final int ROUND_TIME_MS = 30000;
    private static final int TOTAL_ROUNDS = 2;

    private enum Phase { PLAYER_MAIN, OPPONENT_CLEANUP, OPPONENT_MAIN, PLAYER_CLEANUP, DONE }

    private GameContentRepository contentRepository;
    private GameResultRepository resultRepository;
    private UserRepository userRepository;
    private final BotOpponent bot = new BotOpponent();

    private final List<List<SpojnicePair>> roundSets = new ArrayList<>();
    private SpojniceEngine engine;
    private Phase phase = Phase.PLAYER_MAIN;
    private int round = 1;

    private int playerScore = 0;
    private int opponentScore = 0;
    private int playerConnected = 0;
    private int playerOpportunities = 0;
    private final Set<Integer> failedThisPhase = new HashSet<>();
    private int selectedLeft = -1;

    private Button[] leftButtons;
    private Button[] rightButtons;
    private TextView timerView;
    private TextView scoreView;
    private TextView opponentScoreView;
    private TextView roundView;
    private TextView turnView;
    private CountDownTimer timer;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spojnice);

        contentRepository = new GameContentRepository(this);
        resultRepository = new GameResultRepository(this);
        userRepository = new UserRepository(this);

        timerView = findViewById(R.id.timer);
        scoreView = findViewById(R.id.score_value);
        opponentScoreView = findViewById(R.id.opponent_score_value);
        roundView = findViewById(R.id.round_value);
        turnView = findViewById(R.id.turn_text);

        leftButtons = new Button[]{
                findViewById(R.id.left_1), findViewById(R.id.left_2), findViewById(R.id.left_3),
                findViewById(R.id.left_4), findViewById(R.id.left_5), findViewById(R.id.left_6),
                findViewById(R.id.left_7), findViewById(R.id.left_8)
        };
        rightButtons = new Button[]{
                findViewById(R.id.right_1), findViewById(R.id.right_2), findViewById(R.id.right_3),
                findViewById(R.id.right_4), findViewById(R.id.right_5), findViewById(R.id.right_6),
                findViewById(R.id.right_7), findViewById(R.id.right_8)
        };

        for (int i = 0; i < leftButtons.length; i++) {
            final int index = i;
            leftButtons[i].setOnClickListener(v -> onLeftClicked(index));
        }
        for (int i = 0; i < rightButtons.length; i++) {
            final int pos = i;
            rightButtons[i].setOnClickListener(v -> onRightClicked(pos));
        }

        findViewById(R.id.quit_button).setOnClickListener(v -> finish());

        loadRoundSets();
        startRound(1);
    }

    private void loadRoundSets() {
        List<Long> setIds = contentRepository.getSpojniceSetIds();
        for (int i = 0; i < TOTAL_ROUNDS; i++) {
            long setId = setIds.get(i % setIds.size());
            roundSets.add(contentRepository.getSpojniceSet(setId));
        }
    }

    private void startRound(int roundNumber) {
        round = roundNumber;
        engine = new SpojniceEngine(roundSets.get(roundNumber - 1));
        failedThisPhase.clear();
        selectedLeft = -1;
        renderBoard();
        roundView.setText(getString(R.string.round_label, round, TOTAL_ROUNDS));
        updateScores();

        if (round == 1) {
            // Prvu rundu započinje igrač.
            phase = Phase.PLAYER_MAIN;
            turnView.setText(R.string.spojnice_your_round);
            playerOpportunities += engine.size();
            startTimer();
        } else {
            // Drugu rundu započinje protivnik.
            phase = Phase.OPPONENT_MAIN;
            turnView.setText(R.string.spojnice_opponent_round);
            timerView.setText(getString(R.string.time_seconds, 0));
            List<Integer> targets = new ArrayList<>(engine.unconnectedLefts());
            runBotSequence(targets, this::startPlayerCleanup);
        }
    }

    private void renderBoard() {
        int normal = ContextCompat.getColor(this, R.color.background_light);
        int textColor = ContextCompat.getColor(this, R.color.primary);
        for (int i = 0; i < leftButtons.length; i++) {
            leftButtons[i].setText(engine.leftText(i));
            leftButtons[i].setBackgroundTintList(ColorStateList.valueOf(normal));
            leftButtons[i].setTextColor(textColor);
        }
        for (int i = 0; i < rightButtons.length; i++) {
            rightButtons[i].setText(engine.rightText(i));
            rightButtons[i].setBackgroundTintList(ColorStateList.valueOf(normal));
            rightButtons[i].setTextColor(textColor);
        }
    }

    private boolean isPlayerPhase() {
        return phase == Phase.PLAYER_MAIN || phase == Phase.PLAYER_CLEANUP;
    }

    private boolean availableToPlayer(int leftIndex) {
        return !engine.isConnected(leftIndex) && !failedThisPhase.contains(leftIndex);
    }

    private void onLeftClicked(int index) {
        if (!isPlayerPhase() || !availableToPlayer(index)) {
            return;
        }
        int normal = ContextCompat.getColor(this, R.color.background_light);
        for (int i = 0; i < leftButtons.length; i++) {
            if (availableToPlayer(i)) {
                leftButtons[i].setBackgroundTintList(ColorStateList.valueOf(normal));
            }
        }
        selectedLeft = index;
        leftButtons[index].setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.warning)));
    }

    private void onRightClicked(int pos) {
        if (!isPlayerPhase() || selectedLeft < 0) {
            return;
        }
        int rightPairIndex = -1;
        for (int i = 0; i < engine.size(); i++) {
            if (engine.rightPosForLeft(i) == pos) {
                rightPairIndex = i;
                break;
            }
        }
        if (rightPairIndex >= 0 && engine.isConnected(rightPairIndex)) {
            return;
        }

        int leftIndex = selectedLeft;
        selectedLeft = -1;

        if (engine.isMatch(leftIndex, pos)) {
            engine.connect(leftIndex, SpojniceEngine.Connector.PLAYER);
            playerScore += SpojniceEngine.POINTS_PER_PAIR;
            playerConnected++;
            paintPair(leftIndex, R.color.success);
            updateScores();
        } else {
            // Pogrešan pokušaj: pojam ostaje nepovezan i čeka protivnika (spec. 2.c).
            failedThisPhase.add(leftIndex);
            paintFailedLeft(leftIndex);
            flashRight(pos);
        }

        if (playerPhaseDone()) {
            endPlayerPhase();
        }
    }

    private boolean playerPhaseDone() {
        for (int i = 0; i < engine.size(); i++) {
            if (availableToPlayer(i)) {
                return false;
            }
        }
        return true;
    }

    private void paintPair(int leftIndex, int colorRes) {
        int color = ContextCompat.getColor(this, colorRes);
        int white = ContextCompat.getColor(this, R.color.white);
        int rightPos = engine.rightPosForLeft(leftIndex);
        leftButtons[leftIndex].setBackgroundTintList(ColorStateList.valueOf(color));
        leftButtons[leftIndex].setTextColor(white);
        rightButtons[rightPos].setBackgroundTintList(ColorStateList.valueOf(color));
        rightButtons[rightPos].setTextColor(white);
    }

    private void paintFailedLeft(int leftIndex) {
        leftButtons[leftIndex].setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.divider)));
    }

    private void flashRight(int pos) {
        int error = ContextCompat.getColor(this, R.color.error);
        int normal = ContextCompat.getColor(this, R.color.background_light);
        rightButtons[pos].setBackgroundTintList(ColorStateList.valueOf(error));
        final int flashedPos = pos;
        handler.postDelayed(() -> {
            boolean connected = false;
            for (int i = 0; i < engine.size(); i++) {
                if (engine.rightPosForLeft(i) == flashedPos && engine.isConnected(i)) {
                    connected = true;
                    break;
                }
            }
            if (!connected) {
                rightButtons[flashedPos].setBackgroundTintList(ColorStateList.valueOf(normal));
            }
        }, 500);
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new CountDownTimer(ROUND_TIME_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerView.setText(getString(R.string.time_seconds,
                        (int) Math.ceil(millisUntilFinished / 1000.0)));
            }

            @Override
            public void onFinish() {
                timerView.setText(getString(R.string.time_seconds, 0));
                endPlayerPhase();
            }
        }.start();
    }

    private void endPlayerPhase() {
        if (timer != null) {
            timer.cancel();
        }
        if (phase == Phase.PLAYER_MAIN) {
            startOpponentCleanup();
        } else if (phase == Phase.PLAYER_CLEANUP) {
            finishGame();
        }
    }

    private void startOpponentCleanup() {
        phase = Phase.OPPONENT_CLEANUP;
        turnView.setText(R.string.spojnice_opponent_cleanup);
        List<Integer> targets = new ArrayList<>(engine.unconnectedLefts());
        runBotSequence(targets, () -> startRound(2));
    }

    private void startPlayerCleanup() {
        phase = Phase.PLAYER_CLEANUP;
        failedThisPhase.clear();
        selectedLeft = -1;
        List<Integer> remaining = engine.unconnectedLefts();
        if (remaining.isEmpty()) {
            finishGame();
            return;
        }
        turnView.setText(R.string.spojnice_your_cleanup);
        playerOpportunities += remaining.size();
        startTimer();
    }

    /** Protivnik redom prolazi kroz zadate pojmove; uspeh zavisi od simulacije. */
    private void runBotSequence(List<Integer> targets, Runnable onDone) {
        if (isFinishing() || phase == Phase.DONE) {
            return;
        }
        if (targets.isEmpty()) {
            handler.postDelayed(onDone, 600);
            return;
        }
        int leftIndex = targets.remove(0);
        handler.postDelayed(() -> {
            if (isFinishing()) {
                return;
            }
            if (bot.attemptsSpojnicePair()) {
                engine.connect(leftIndex, SpojniceEngine.Connector.OPPONENT);
                opponentScore += SpojniceEngine.POINTS_PER_PAIR;
                paintPair(leftIndex, R.color.primary_dark);
                updateScores();
            } else {
                flashLeft(leftIndex);
            }
            runBotSequence(targets, onDone);
        }, bot.stepDelayMs());
    }

    private void flashLeft(int leftIndex) {
        int error = ContextCompat.getColor(this, R.color.error);
        int normal = ContextCompat.getColor(this, R.color.background_light);
        leftButtons[leftIndex].setBackgroundTintList(ColorStateList.valueOf(error));
        handler.postDelayed(() -> {
            if (!engine.isConnected(leftIndex)) {
                leftButtons[leftIndex].setBackgroundTintList(ColorStateList.valueOf(normal));
            }
        }, 500);
    }

    private void updateScores() {
        scoreView.setText(String.valueOf(playerScore));
        opponentScoreView.setText(String.valueOf(opponentScore));
    }

    private void finishGame() {
        if (phase == Phase.DONE) {
            return;
        }
        phase = Phase.DONE;
        if (timer != null) {
            timer.cancel();
        }

        boolean won = playerScore > opponentScore;
        resultRepository.insert(new GameResult(
                userRepository.getCurrentUserId(),
                GameResult.GAME_SPOJNICE,
                playerScore,
                opponentScore,
                won,
                playerConnected,
                playerOpportunities,
                System.currentTimeMillis()));

        if (isFinishing()) {
            return;
        }
        int title = won ? R.string.result_win
                : playerScore == opponentScore ? R.string.result_draw
                : R.string.result_loss;
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(getString(R.string.final_score_fmt, playerScore, opponentScore))
                .setCancelable(false)
                .setPositiveButton(R.string.exit, (d, w) -> finish())
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
        handler.removeCallbacksAndMessages(null);
    }
}
