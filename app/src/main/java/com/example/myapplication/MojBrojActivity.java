package com.example.myapplication;

import android.content.Context;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Moj broj – 2 runde (2 x 1 min) za dva igrača na istom uređaju (hot-seat).
 * Rundu započinje njen vlasnik (runda 1 = Igrač 1, runda 2 = Igrač 2) klikom na
 * STOP (ili protresanjem telefona): prvo se otkriva traženi broj, pa 6 brojeva
 * (4 jednocifrena, jedan iz {10,15,20}, jedan iz {25,50,75,100}); ako se ne
 * klikne za 5s, otkriva se automatski. Oba igrača zatim redom sastavljaju izraz
 * nad istim brojevima. Bodovanje: tačan broj = 10, inače bliži rezultat = 5, a
 * kod istog rezultata bodove dobija igrač čija je runda. Max 20, min 0.
 */
public class MojBrojActivity extends AppCompatActivity implements SensorEventListener {

    private static final int[] MEDIUM_NUMBERS = {10, 15, 20};
    private static final int[] BIG_NUMBERS = {25, 50, 75, 100};

    private static final int PHASE_STOP_TARGET = 0;
    private static final int PHASE_STOP_NUMBERS = 1;
    private static final int PHASE_PLAY = 2;
    private static final int PHASE_DONE = 3;

    private int target;
    private int[] roundNumbers;
    private int phase = PHASE_STOP_TARGET;
    private int roundIndex = 1;
    private int roundOwner = 1;
    private int active = 1;
    private int p1 = 0;
    private int p2 = 0;
    private boolean finished = false;
    private final Integer[] roundResults = new Integer[3]; // index 1 = player 1, 2 = player 2

    private final StringBuilder expression = new StringBuilder();
    private final Deque<Integer> usedTiles = new ArrayDeque<>();
    private final Random random = new Random();
    private CountDownTimer timer;

    private TextView timerView;
    private TextView targetView;
    private TextView expressionView;
    private TextView resultView;
    private TextView statusText;
    private TextView p1ScoreView;
    private TextView p2ScoreView;
    private View p1Chip;
    private View p2Chip;
    private Button stopButton;
    private Button submitButton;
    private GridLayout numbersGrid;
    private GridLayout operatorsGrid;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeMs = 0;

    private UserRepository userRepository;
    private GameResultRepository resultRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moj_broj);
        userRepository = new UserRepository(this);
        resultRepository = new GameResultRepository(this);
        PlayerBar.bind(this, userRepository.getCurrentUser());

        timerView = findViewById(R.id.timer);
        targetView = findViewById(R.id.target_number);
        expressionView = findViewById(R.id.expression_display);
        resultView = findViewById(R.id.result_display);
        statusText = findViewById(R.id.status_text);
        p1ScoreView = findViewById(R.id.p1_score);
        p2ScoreView = findViewById(R.id.p2_score);
        p1Chip = findViewById(R.id.p1_chip);
        p2Chip = findViewById(R.id.p2_chip);
        stopButton = findViewById(R.id.stop_button);
        submitButton = findViewById(R.id.submit_button);
        numbersGrid = findViewById(R.id.numbers_grid);
        operatorsGrid = findViewById(R.id.operators_grid);

        Button quit = findViewById(R.id.quit_button);
        Button backspace = findViewById(R.id.backspace_button);
        Button clear = findViewById(R.id.clear_button);

        quit.setOnClickListener(v -> confirmExit());
        stopButton.setOnClickListener(v -> stopPressed());
        submitButton.setOnClickListener(v -> onSubmit());
        backspace.setOnClickListener(v -> removeLastToken());
        clear.setOnClickListener(v -> clearExpression());
        bindOperatorButtons(operatorsGrid);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

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

    private void renderScores() {
        p1ScoreView.setText(String.valueOf(p1));
        p2ScoreView.setText(String.valueOf(p2));
        p1Chip.setBackgroundResource(active == 1 ? R.drawable.header_chip_active : R.drawable.header_chip);
        p2Chip.setBackgroundResource(active == 2 ? R.drawable.header_chip_active : R.drawable.header_chip);
    }

    // ----- Round lifecycle -----

    private void startRound(int index) {
        roundIndex = index;
        roundOwner = index;
        active = index;
        phase = PHASE_STOP_TARGET;
        roundResults[1] = null;
        roundResults[2] = null;
        expression.setLength(0);
        generateGame();
        bindNumberButtons(numbersGrid);
        setNumbersRevealed(false);
        setOperatorsEnabled(false);

        targetView.setText(R.string.mb_hidden);
        expressionView.setText(R.string.expr_placeholder);
        resultView.setText(R.string.result_placeholder);
        stopButton.setVisibility(Button.VISIBLE);
        stopButton.setEnabled(true);
        stopButton.setText(R.string.mb_stop_show_target);
        submitButton.setEnabled(false);
        statusText.setText(getString(R.string.mb_stop_owner, roundIndex, roundOwner));
        renderScores();
        startTimer(5000);
    }

    /** Spec: four single-digit numbers, one of {10,15,20}, one of {25,50,75,100}. */
    private void generateGame() {
        target = 100 + random.nextInt(900);
        roundNumbers = new int[6];
        for (int i = 0; i < 4; i++) {
            roundNumbers[i] = 1 + random.nextInt(9); // 1..9
        }
        roundNumbers[4] = MEDIUM_NUMBERS[random.nextInt(MEDIUM_NUMBERS.length)];
        roundNumbers[5] = BIG_NUMBERS[random.nextInt(BIG_NUMBERS.length)];
        for (int i = roundNumbers.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = roundNumbers[i];
            roundNumbers[i] = roundNumbers[j];
            roundNumbers[j] = tmp;
        }
    }

    private void stopPressed() {
        if (phase == PHASE_STOP_TARGET) {
            targetView.setText(String.valueOf(target));
            phase = PHASE_STOP_NUMBERS;
            stopButton.setText(R.string.mb_stop_show_numbers);
            statusText.setText(R.string.mb_phase_numbers);
            startTimer(5000);
        } else if (phase == PHASE_STOP_NUMBERS) {
            setNumbersRevealed(true);
            setOperatorsEnabled(true);
            stopButton.setVisibility(Button.GONE);
            playTurn(roundOwner);
        }
    }

    /** Starts a 1-minute build turn for the given player on the (already revealed) numbers. */
    private void playTurn(int player) {
        active = player;
        phase = PHASE_PLAY;
        expression.setLength(0);
        usedTiles.clear();
        expressionView.setText(R.string.expr_placeholder);
        resultView.setText(R.string.result_placeholder);
        setNumbersRevealed(true);
        setOperatorsEnabled(true);
        submitButton.setEnabled(true);
        statusText.setText(getString(R.string.mb_turn, roundIndex, player));
        renderScores();
        startTimer(60000);
    }

    private void onSubmit() {
        if (phase != PHASE_PLAY) return;
        if (expression.length() == 0) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        Integer value = tryEvaluate();
        if (value == null) {
            Toast.makeText(this, R.string.invalid_expression, Toast.LENGTH_SHORT).show();
            return;
        }
        resultView.setText(String.valueOf(value));
        endTurn(value);
    }

    private void endTurn(Integer result) {
        if (timer != null) timer.cancel();
        if (phase == PHASE_DONE) return;
        roundResults[active] = result;
        if (active == roundOwner) {
            int next = other(roundOwner);
            showTransition(getString(R.string.turn_handoff, next), () -> playTurn(next));
        } else {
            scoreRoundAndContinue();
        }
    }

    /** Applies spec scoring for the finished round (both players have played). */
    private void scoreRoundAndContinue() {
        Integer r1 = roundResults[1];
        Integer r2 = roundResults[2];
        boolean p1Exact = r1 != null && r1 == target;
        boolean p2Exact = r2 != null && r2 == target;
        int d1 = r1 == null ? Integer.MAX_VALUE : Math.abs(target - r1);
        int d2 = r2 == null ? Integer.MAX_VALUE : Math.abs(target - r2);

        int add1 = 0;
        int add2 = 0;
        if (p1Exact) add1 = 10;
        if (p2Exact) add2 = 10;
        if (!p1Exact && !p2Exact) {
            if (d1 == Integer.MAX_VALUE && d2 == Integer.MAX_VALUE) {
                // neither entered anything -> 0 points
            } else if (d1 < d2) {
                add1 = 5;
            } else if (d2 < d1) {
                add2 = 5;
            } else {
                // equal distance -> the round owner gets the points
                if (roundOwner == 1) add1 = 5; else add2 = 5;
            }
        }
        p1 += add1;
        p2 += add2;
        active = 0; // no active player during the summary
        renderScores();

        String note = playerLine(1, r1, p1Exact) + "\n" + playerLine(2, r2, p2Exact)
                + "\n" + getString(R.string.mb_round_points, add1, add2);

        if (roundIndex == 1) {
            showTransition(note + "\n\n" + getString(R.string.turn_handoff, 2), () -> startRound(2));
        } else {
            phase = PHASE_DONE;
            showTransition(note, this::finishMatch);
        }
    }

    private String playerLine(int player, Integer result, boolean exact) {
        if (exact) return getString(R.string.mb_player_exact, player);
        if (result == null) return getString(R.string.mb_player_none, player);
        return getString(R.string.mb_player_result, player, result);
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
            resultRepository.insert(new GameResult(user.id, GameResult.GAME_MOJ_BROJ, p1, p2,
                    p1 >= p2, p1, p2, System.currentTimeMillis()));
        }
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.match_result_title)
                .setMessage(getString(R.string.match_two, p1, p2, winner))
                .setPositiveButton(android.R.string.ok, (d, w) -> finish())
                .show();
    }

    // ----- Expression building -----

    private void bindNumberButtons(GridLayout grid) {
        for (int i = 0; i < grid.getChildCount() && i < roundNumbers.length; i++) {
            Button button = (Button) grid.getChildAt(i);
            int tileIndex = i;
            int number = roundNumbers[i];
            button.setOnClickListener(v -> appendNumber(tileIndex, number));
        }
    }

    /** Each number tile may be used only once per turn; a tile is disabled after use. */
    private void appendNumber(int tileIndex, int value) {
        if (phase != PHASE_PLAY) return;
        String token = String.valueOf(value);
        if (!isValidNext(token)) return;
        appendRaw(token);
        usedTiles.push(tileIndex);
        setTileUsed(tileIndex, true);
    }

    /** Greys out a used tile (and disables it); restores it when freed. */
    private void setTileUsed(int tileIndex, boolean used) {
        Button button = (Button) numbersGrid.getChildAt(tileIndex);
        button.setEnabled(!used);
        int color = used ? R.color.peg_absent : R.color.primary;
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, color)));
        button.setAlpha(used ? 0.55f : 1f);
    }

    private void setNumbersRevealed(boolean revealed) {
        for (int i = 0; i < numbersGrid.getChildCount() && i < roundNumbers.length; i++) {
            Button button = (Button) numbersGrid.getChildAt(i);
            button.setText(revealed ? String.valueOf(roundNumbers[i]) : getString(R.string.mb_hidden));
            if (revealed) {
                setTileUsed(i, false); // fresh, fully usable
            } else {
                button.setEnabled(false);
            }
        }
    }

    private void setOperatorsEnabled(boolean enabled) {
        for (int i = 0; i < operatorsGrid.getChildCount(); i++) {
            operatorsGrid.getChildAt(i).setEnabled(enabled);
        }
    }

    private void bindOperatorButtons(GridLayout grid) {
        String[] ops = {"(", ")", "+", "-", "*", "/"};
        for (int i = 0; i < grid.getChildCount() && i < ops.length; i++) {
            Button button = (Button) grid.getChildAt(i);
            String op = ops[i];
            button.setOnClickListener(v -> appendToken(op));
        }
    }

    private void appendToken(String token) {
        if (phase != PHASE_PLAY) return;
        if (!isValidNext(token)) return; // silently ignore malformed order
        appendRaw(token);
    }

    private void appendRaw(String token) {
        if (expression.length() > 0) expression.append(' ');
        expression.append(token);
        expressionView.setText(expression.toString());
        resultView.setText(R.string.result_placeholder);
    }

    private void removeLastToken() {
        String text = expression.toString().trim();
        if (text.isEmpty()) return;
        int lastSpace = text.lastIndexOf(' ');
        String last = lastSpace < 0 ? text : text.substring(lastSpace + 1);
        expression.setLength(0);
        if (lastSpace > 0) expression.append(text, 0, lastSpace);
        // If we removed a number, free up the tile it came from.
        if (isNumber(last) && !usedTiles.isEmpty()) {
            int tileIndex = usedTiles.pop();
            if (tileIndex < numbersGrid.getChildCount()) {
                setTileUsed(tileIndex, false);
            }
        }
        refreshExpressionView();
    }

    private void clearExpression() {
        expression.setLength(0);
        usedTiles.clear();
        for (int i = 0; i < numbersGrid.getChildCount() && i < roundNumbers.length; i++) {
            setTileUsed(i, false);
        }
        refreshExpressionView();
    }

    private void refreshExpressionView() {
        expressionView.setText(expression.length() == 0 ? getString(R.string.expr_placeholder) : expression.toString());
        resultView.setText(R.string.result_placeholder);
    }

    private String lastToken() {
        String text = expression.toString().trim();
        if (text.isEmpty()) return null;
        int lastSpace = text.lastIndexOf(' ');
        return lastSpace < 0 ? text : text.substring(lastSpace + 1);
    }

    private boolean isNumber(String s) {
        return s != null && s.matches("\\d+");
    }

    private boolean isOperator(String s) {
        return "+".equals(s) || "-".equals(s) || "*".equals(s) || "/".equals(s);
    }

    private int openParenCount() {
        int count = 0;
        for (String tok : expression.toString().split(" ")) {
            if ("(".equals(tok)) count++;
            else if (")".equals(tok)) count--;
        }
        return count;
    }

    private boolean isValidNext(String token) {
        String prev = lastToken();
        if (isNumber(token)) {
            return prev == null || isOperator(prev) || "(".equals(prev);
        }
        if (isOperator(token)) {
            return isNumber(prev) || ")".equals(prev);
        }
        if ("(".equals(token)) {
            return prev == null || isOperator(prev) || "(".equals(prev);
        }
        if (")".equals(token)) {
            return (isNumber(prev) || ")".equals(prev)) && openParenCount() > 0;
        }
        return false;
    }

    private Integer tryEvaluate() {
        if (expression.length() == 0) return null;
        try {
            return (int) Math.round(evaluate(expression.toString()));
        } catch (Exception e) {
            return null;
        }
    }

    private double evaluate(String expr) {
        Deque<Double> values = new ArrayDeque<>();
        Deque<String> ops = new ArrayDeque<>();
        for (String token : expr.split(" ")) {
            if (token.isEmpty()) continue;
            if (token.matches("\\d+")) {
                values.push(Double.parseDouble(token));
                continue;
            }
            if (token.equals("(")) {
                ops.push(token);
                continue;
            }
            if (token.equals(")")) {
                while (!ops.isEmpty() && !ops.peek().equals("(")) {
                    apply(values, ops.pop());
                }
                if (!ops.isEmpty()) ops.pop();
                continue;
            }
            while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(token)) {
                apply(values, ops.pop());
            }
            ops.push(token);
        }
        while (!ops.isEmpty()) {
            apply(values, ops.pop());
        }
        return values.isEmpty() ? 0 : values.pop();
    }

    private int precedence(String op) {
        if (op.equals("+") || op.equals("-")) return 1;
        if (op.equals("*") || op.equals("/")) return 2;
        return 0;
    }

    private void apply(Deque<Double> values, String op) {
        if (values.size() < 2) return;
        double right = values.pop();
        double left = values.pop();
        switch (op) {
            case "+": values.push(left + right); break;
            case "-": values.push(left - right); break;
            case "*": values.push(left * right); break;
            case "/": values.push(right == 0 ? left : left / right); break;
        }
    }

    // ----- Timer / sensors / dialogs -----

    private void startTimer(long ms) {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(ms, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerView.setText(getString(R.string.time_remaining, millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                if (phase == PHASE_STOP_TARGET || phase == PHASE_STOP_NUMBERS) {
                    stopPressed(); // auto-reveal after 5 seconds
                } else if (phase == PHASE_PLAY) {
                    endTurn(tryEvaluate());
                }
            }
        }.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (phase != PHASE_STOP_TARGET && phase != PHASE_STOP_NUMBERS) return;
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        double gForce = Math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH;
        long now = System.currentTimeMillis();
        if (gForce > 2.2 && now - lastShakeMs > 800) {
            lastShakeMs = now;
            stopPressed();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // no-op
    }

    private void showTransition(String message, Runnable next) {
        if (isFinishing()) return;
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton(R.string.continue_match, (d, w) -> next.run())
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
