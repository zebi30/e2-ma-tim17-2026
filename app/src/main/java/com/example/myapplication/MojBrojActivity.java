package com.example.myapplication;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.GridLayout;
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Moj broj – 2 runde (2 x 1 min). Igrač u svakoj rundi: klikom na STOP (ili
 * protresanjem telefona) prvo otkriva traženi broj, pa zatim 6 brojeva (4
 * jednocifrena, jedan iz {10,15,20}, jedan iz {25,50,75,100}); ako ne klikne za
 * 5s, otkriva se automatski. Protivnik je simuliran ({@link BotOpponent}).
 * Bodovanje po specifikaciji: tačan broj = 10, inače bliži rezultat = 5,
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
    private int playerScore = 0;
    private int opponentScore = 0;
    private boolean finished = false;
    private Integer playerResult = null;

    private final StringBuilder expression = new StringBuilder();
    private final Random random = new Random();
    private final BotOpponent bot = new BotOpponent();
    private CountDownTimer timer;

    private TextView timerView;
    private TextView targetView;
    private TextView expressionView;
    private TextView resultView;
    private TextView scoreView;
    private TextView statusText;
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

        timerView = findViewById(R.id.timer);
        targetView = findViewById(R.id.target_number);
        expressionView = findViewById(R.id.expression_display);
        resultView = findViewById(R.id.result_display);
        scoreView = findViewById(R.id.score_value);
        statusText = findViewById(R.id.status_text);
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

    // ----- Round lifecycle -----

    private void startRound(int index) {
        roundIndex = index;
        phase = PHASE_STOP_TARGET;
        playerResult = null;
        expression.setLength(0);
        generateGame();
        bindNumberButtons(numbersGrid);
        setNumbersRevealed(false);
        setOperatorsEnabled(false);

        targetView.setText(R.string.mb_hidden);
        expressionView.setText(R.string.expr_placeholder);
        resultView.setText(R.string.result_placeholder);
        scoreView.setText(String.valueOf(playerScore));
        stopButton.setVisibility(Button.VISIBLE);
        stopButton.setEnabled(true);
        stopButton.setText(R.string.mb_stop_show_target);
        submitButton.setEnabled(false);
        statusText.setText(getString(R.string.mb_phase_target, roundIndex));
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
            submitButton.setEnabled(true);
            stopButton.setVisibility(Button.GONE);
            phase = PHASE_PLAY;
            statusText.setText(getString(R.string.mb_phase_play, roundIndex));
            startTimer(60000);
        }
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
        playerResult = value;
        resultView.setText(String.valueOf(value));
        endRound();
    }

    private void endRound() {
        if (timer != null) timer.cancel();
        if (phase == PHASE_DONE) return;
        if (playerResult == null) {
            playerResult = tryEvaluate(); // last-chance evaluate on timeout
        }
        phase = PHASE_DONE;

        String note = scoreRound(roundIndex == 1);
        scoreView.setText(String.valueOf(playerScore));

        if (roundIndex == 1) {
            showTransition(note, () -> startRound(2));
        } else {
            showTransition(note, this::finishMatch);
        }
    }

    /** Applies the spec scoring for one round and returns a short summary note. */
    private String scoreRound(boolean playerOwnsRound) {
        boolean playerExact = playerResult != null && playerResult == target;
        int playerDist = playerResult == null ? Integer.MAX_VALUE : Math.abs(target - playerResult);
        boolean botExact = bot.mojBrojReachesTarget();
        int botDist = botExact ? 0 : bot.mojBrojDistance();

        int p = 0;
        int o = 0;
        if (playerExact) p = 10;
        if (botExact) o = 10;
        if (!playerExact && !botExact) {
            if (playerDist < botDist) {
                p = 5;
            } else if (botDist < playerDist) {
                o = 5;
            } else if (playerDist != Integer.MAX_VALUE) {
                if (playerOwnsRound) p = 5; else o = 5;
            }
        }
        playerScore += p;
        opponentScore += o;

        StringBuilder sb = new StringBuilder();
        if (playerExact) sb.append(getString(R.string.mb_round_exact));
        else if (p == 5) sb.append(getString(R.string.mb_round_closer));
        else if (playerResult == null) sb.append(getString(R.string.mb_round_nothing));
        else sb.append(getString(R.string.mb_round_target_was, target));

        if (botExact) sb.append("\n").append(getString(R.string.mb_round_opp_exact));
        else if (o == 5) sb.append("\n").append(getString(R.string.mb_round_opp_closer));
        return sb.toString();
    }

    private void finishMatch() {
        if (finished) return;
        finished = true;
        phase = PHASE_DONE;
        if (timer != null) timer.cancel();

        boolean won = playerScore > opponentScore;
        User user = userRepository.getCurrentUser();
        if (user != null) {
            resultRepository.insert(new GameResult(user.id, GameResult.GAME_MOJ_BROJ, playerScore, opponentScore,
                    won, playerScore, opponentScore, System.currentTimeMillis()));
        }
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.match_result_title)
                .setMessage(getString(R.string.match_result_message, playerScore, opponentScore))
                .setPositiveButton(android.R.string.ok, (d, w) -> finish())
                .show();
    }

    // ----- Expression building -----

    private void bindNumberButtons(GridLayout grid) {
        for (int i = 0; i < grid.getChildCount() && i < roundNumbers.length; i++) {
            Button button = (Button) grid.getChildAt(i);
            int number = roundNumbers[i];
            button.setOnClickListener(v -> appendToken(String.valueOf(number)));
        }
    }

    private void setNumbersRevealed(boolean revealed) {
        for (int i = 0; i < numbersGrid.getChildCount() && i < roundNumbers.length; i++) {
            Button button = (Button) numbersGrid.getChildAt(i);
            button.setText(revealed ? String.valueOf(roundNumbers[i]) : getString(R.string.mb_hidden));
            button.setEnabled(revealed);
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
        if (expression.length() > 0) expression.append(' ');
        expression.append(token);
        expressionView.setText(expression.toString());
        resultView.setText(R.string.result_placeholder);
    }

    private void removeLastToken() {
        String text = expression.toString().trim();
        int lastSpace = text.lastIndexOf(' ');
        expression.setLength(0);
        if (lastSpace > 0) expression.append(text, 0, lastSpace);
        refreshExpressionView();
    }

    private void clearExpression() {
        expression.setLength(0);
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
                    endRound();
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
