package com.example.myapplication;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.data.GameContentRepository;
import com.example.myapplication.data.GameResultRepository;
import com.example.myapplication.data.UserRepository;
import com.example.myapplication.logic.AsocijacijeEngine;
import com.example.myapplication.logic.AsocijacijeEngine.Side;
import com.example.myapplication.logic.BotOpponent;
import com.example.myapplication.model.AssociationSet;
import com.example.myapplication.model.GameResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Asocijacije: 2 runde po 2 minuta, svaku započinje po jedan igrač (spec. 3.a).
 * Igrači naizmenično otvaraju polja; nakon otvorenog polja igrač može da pogađa
 * rešenja kolona ili konačno rešenje sve dok ne pogreši (spec. 3.d, 3.e).
 * Igra se protiv simuliranog protivnika; rezultat se upisuje u bazu.
 */
public class AsocijacijeActivity extends AppCompatActivity {

    private static final int ROUND_TIME_MS = 120000;
    private static final int TOTAL_ROUNDS = 2;

    private GameContentRepository contentRepository;
    private GameResultRepository resultRepository;
    private UserRepository userRepository;
    private final BotOpponent bot = new BotOpponent();

    private List<AssociationSet> sets;
    private AsocijacijeEngine engine;
    private int round = 1;
    private Side turn = Side.PLAYER;
    private boolean openedThisTurn = false;
    private boolean botBusy = false;
    private boolean roundEnded = false;

    private int totalPlayerScore = 0;
    private int totalOpponentScore = 0;
    private int roundsSolvedByPlayer = 0;

    private final Button[][] cellButtons = new Button[AsocijacijeEngine.COLS][AsocijacijeEngine.ROWS];
    private final TextView[] solViews = new TextView[AsocijacijeEngine.COLS];
    private final String[] colLabels = {"A", "B", "C", "D"};

    private TextView timerView;
    private TextView scoreView;
    private TextView opponentScoreView;
    private TextView roundView;
    private TextView turnView;
    private EditText finalInput;
    private CountDownTimer timer;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asocijacije);

        contentRepository = new GameContentRepository(this);
        resultRepository = new GameResultRepository(this);
        userRepository = new UserRepository(this);

        timerView = findViewById(R.id.timer);
        scoreView = findViewById(R.id.score_value);
        opponentScoreView = findViewById(R.id.opponent_score_value);
        roundView = findViewById(R.id.round_value);
        turnView = findViewById(R.id.turn_text);
        finalInput = findViewById(R.id.final_solution_input);

        int[][] cellIds = {
                {R.id.cell_a1, R.id.cell_a2, R.id.cell_a3, R.id.cell_a4},
                {R.id.cell_b1, R.id.cell_b2, R.id.cell_b3, R.id.cell_b4},
                {R.id.cell_c1, R.id.cell_c2, R.id.cell_c3, R.id.cell_c4},
                {R.id.cell_d1, R.id.cell_d2, R.id.cell_d3, R.id.cell_d4}
        };
        for (int c = 0; c < AsocijacijeEngine.COLS; c++) {
            for (int r = 0; r < AsocijacijeEngine.ROWS; r++) {
                final int col = c;
                final int row = r;
                cellButtons[c][r] = findViewById(cellIds[c][r]);
                cellButtons[c][r].setOnClickListener(v -> onCellClicked(col, row));
            }
        }

        solViews[0] = findViewById(R.id.sol_a);
        solViews[1] = findViewById(R.id.sol_b);
        solViews[2] = findViewById(R.id.sol_c);
        solViews[3] = findViewById(R.id.sol_d);
        for (int c = 0; c < AsocijacijeEngine.COLS; c++) {
            final int col = c;
            solViews[c].setOnClickListener(v -> onColumnGuess(col));
        }

        findViewById(R.id.submit_button).setOnClickListener(v -> onFinalGuess());
        findViewById(R.id.pass_button).setOnClickListener(v -> onPassTurn());
        findViewById(R.id.quit_button).setOnClickListener(v -> finish());

        sets = contentRepository.getAllAssociationSets();
        startRound(1);
    }

    private void startRound(int roundNumber) {
        round = roundNumber;
        engine = new AsocijacijeEngine(sets.get((roundNumber - 1) % sets.size()));
        roundEnded = false;
        openedThisTurn = false;
        botBusy = false;
        finalInput.setText("");

        int normalBg = ContextCompat.getColor(this, R.color.background_light);
        int normalText = ContextCompat.getColor(this, R.color.primary);
        int solBg = ContextCompat.getColor(this, R.color.primary_dark);
        for (int c = 0; c < AsocijacijeEngine.COLS; c++) {
            for (int r = 0; r < AsocijacijeEngine.ROWS; r++) {
                cellButtons[c][r].setText(R.string.cell_hidden);
                cellButtons[c][r].setBackgroundTintList(ColorStateList.valueOf(normalBg));
                cellButtons[c][r].setTextColor(normalText);
            }
            solViews[c].setText(colLabels[c]);
            solViews[c].setBackgroundColor(solBg);
        }

        roundView.setText(getString(R.string.round_label, round, TOTAL_ROUNDS));
        updateScores();
        startTimer();

        // Prvu rundu započinje igrač, drugu protivnik (spec. 3.a).
        if (round == 1) {
            setTurn(Side.PLAYER);
        } else {
            setTurn(Side.OPPONENT);
            scheduleBotTurn();
        }
    }

    private void setTurn(Side side) {
        turn = side;
        openedThisTurn = false;
        turnView.setText(side == Side.PLAYER ? R.string.turn_player : R.string.turn_opponent);
    }

    private boolean playerCanAct() {
        return !roundEnded && !botBusy && turn == Side.PLAYER;
    }

    private void onCellClicked(int col, int row) {
        if (!playerCanAct()) {
            return;
        }
        if (openedThisTurn) {
            Toast.makeText(this, R.string.asoc_already_opened, Toast.LENGTH_SHORT).show();
            return;
        }
        if (engine.openCell(col, row)) {
            revealCell(col, row);
            openedThisTurn = true;
        }
    }

    private void revealCell(int col, int row) {
        cellButtons[col][row].setText(engine.cellText(col, row));
        cellButtons[col][row].setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_dark)));
        cellButtons[col][row].setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private boolean hasOpenableCell() {
        for (int c = 0; c < AsocijacijeEngine.COLS; c++) {
            if (engine.isColumnSolved(c)) {
                continue;
            }
            if (engine.unopenedInColumn(c) > 0) {
                return true;
            }
        }
        return false;
    }

    private void onColumnGuess(int col) {
        if (!playerCanAct() || engine.isColumnSolved(col)) {
            return;
        }
        if (!openedThisTurn && hasOpenableCell()) {
            Toast.makeText(this, R.string.asoc_open_field_first, Toast.LENGTH_SHORT).show();
            return;
        }
        showGuessDialog(getString(R.string.guess_column, colLabels[col]), guess -> {
            if (engine.checkColumnGuess(col, guess)) {
                engine.solveColumn(col, Side.PLAYER);
                showColumnSolved(col, Side.PLAYER);
                updateScores();
                Toast.makeText(this, R.string.correct, Toast.LENGTH_SHORT).show();
                // Igrač nastavlja da pogađa dok ne pogreši (spec. 3.e).
            } else {
                Toast.makeText(this, R.string.asoc_wrong_turn_passes, Toast.LENGTH_SHORT).show();
                endPlayerTurn();
            }
        });
    }

    private void onFinalGuess() {
        if (!playerCanAct()) {
            return;
        }
        if (!openedThisTurn && hasOpenableCell()) {
            Toast.makeText(this, R.string.asoc_open_field_first, Toast.LENGTH_SHORT).show();
            return;
        }
        String guess = finalInput.getText().toString().trim();
        if (guess.isEmpty()) {
            Toast.makeText(this, R.string.empty_guess, Toast.LENGTH_SHORT).show();
            return;
        }
        if (engine.checkFinalGuess(guess)) {
            engine.solveFinal(Side.PLAYER);
            revealAll();
            updateScores();
            endRound();
        } else {
            Toast.makeText(this, R.string.asoc_wrong_turn_passes, Toast.LENGTH_SHORT).show();
            finalInput.setText("");
            endPlayerTurn();
        }
    }

    private void onPassTurn() {
        if (!playerCanAct()) {
            return;
        }
        endPlayerTurn();
    }

    private void endPlayerTurn() {
        if (roundEnded) {
            return;
        }
        setTurn(Side.OPPONENT);
        scheduleBotTurn();
    }

    private void scheduleBotTurn() {
        botBusy = true;
        handler.postDelayed(this::botOpenCell, bot.stepDelayMs());
    }

    private void botOpenCell() {
        if (roundEnded || isFinishing()) {
            return;
        }
        List<int[]> candidates = new ArrayList<>();
        for (int c = 0; c < AsocijacijeEngine.COLS; c++) {
            if (engine.isColumnSolved(c)) {
                continue;
            }
            for (int r = 0; r < AsocijacijeEngine.ROWS; r++) {
                if (!engine.isOpened(c, r)) {
                    candidates.add(new int[]{c, r});
                }
            }
        }
        if (!candidates.isEmpty()) {
            int[] cell = candidates.get(bot.pickIndex(candidates.size()));
            engine.openCell(cell[0], cell[1]);
            revealCell(cell[0], cell[1]);
        }
        handler.postDelayed(this::botGuessStep, bot.stepDelayMs());
    }

    private void botGuessStep() {
        if (roundEnded || isFinishing()) {
            return;
        }
        // Protivnik bira nerešenu kolonu sa najviše otvorenih polja.
        int bestCol = -1;
        int bestOpened = 0;
        for (int c = 0; c < AsocijacijeEngine.COLS; c++) {
            if (engine.isColumnSolved(c)) {
                continue;
            }
            int openedCells = engine.openedInColumn(c);
            if (openedCells > bestOpened) {
                bestOpened = openedCells;
                bestCol = c;
            }
        }

        if (bestCol >= 0 && bot.wantsColumnGuess(bestOpened)) {
            if (bot.columnGuessCorrect()) {
                engine.solveColumn(bestCol, Side.OPPONENT);
                showColumnSolved(bestCol, Side.OPPONENT);
                updateScores();
                Toast.makeText(this,
                        getString(R.string.asoc_opp_solved_column, colLabels[bestCol]),
                        Toast.LENGTH_SHORT).show();
                // Protivnik nastavlja da pogađa dok ne pogreši (spec. 3.e).
                handler.postDelayed(this::botGuessStep, bot.stepDelayMs());
            } else {
                Toast.makeText(this, R.string.asoc_opp_wrong, Toast.LENGTH_SHORT).show();
                endBotTurn();
            }
            return;
        }

        if (engine.solvedColumnsCount() >= 2 && bot.wantsFinalGuess(engine.solvedColumnsCount())) {
            if (bot.finalGuessCorrect()) {
                engine.solveFinal(Side.OPPONENT);
                revealAll();
                updateScores();
                Toast.makeText(this, R.string.asoc_opp_solved_final, Toast.LENGTH_SHORT).show();
                endRound();
            } else {
                Toast.makeText(this, R.string.asoc_opp_wrong, Toast.LENGTH_SHORT).show();
                endBotTurn();
            }
            return;
        }

        endBotTurn();
    }

    private void endBotTurn() {
        if (roundEnded) {
            return;
        }
        botBusy = false;
        setTurn(Side.PLAYER);
    }

    private void showColumnSolved(int col, Side by) {
        solViews[col].setText(engine.columnSolution(col));
        solViews[col].setBackgroundColor(ContextCompat.getColor(this,
                by == Side.PLAYER ? R.color.success : R.color.warning));
        int revealedBg = ContextCompat.getColor(this, R.color.divider);
        int textColor = ContextCompat.getColor(this, R.color.text_primary);
        for (int r = 0; r < AsocijacijeEngine.ROWS; r++) {
            cellButtons[col][r].setText(engine.cellText(col, r));
            if (!engine.isOpened(col, r)) {
                cellButtons[col][r].setBackgroundTintList(ColorStateList.valueOf(revealedBg));
                cellButtons[col][r].setTextColor(textColor);
            }
        }
    }

    private void revealAll() {
        for (int c = 0; c < AsocijacijeEngine.COLS; c++) {
            showColumnSolved(c, engine.columnSolvedBy(c));
        }
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new CountDownTimer(ROUND_TIME_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long totalSeconds = millisUntilFinished / 1000;
                timerView.setText(String.format(Locale.getDefault(), "%d:%02d",
                        totalSeconds / 60, totalSeconds % 60));
            }

            @Override
            public void onFinish() {
                timerView.setText("0:00");
                if (!roundEnded) {
                    Toast.makeText(AsocijacijeActivity.this, R.string.time_up, Toast.LENGTH_SHORT).show();
                    revealAll();
                    endRound();
                }
            }
        }.start();
    }

    private void updateScores() {
        scoreView.setText(String.valueOf(totalPlayerScore + engine.playerScore()));
        opponentScoreView.setText(String.valueOf(totalOpponentScore + engine.opponentScore()));
    }

    private void endRound() {
        if (roundEnded) {
            return;
        }
        roundEnded = true;
        botBusy = false;
        if (timer != null) {
            timer.cancel();
        }
        handler.removeCallbacksAndMessages(null);

        totalPlayerScore += engine.playerScore();
        totalOpponentScore += engine.opponentScore();
        if (engine.finalSolvedBy() == Side.PLAYER) {
            roundsSolvedByPlayer++;
        }

        if (isFinishing()) {
            return;
        }
        String message = getString(R.string.asoc_final_was, engine.finalSolution())
                + "\n" + getString(R.string.final_score_fmt, totalPlayerScore, totalOpponentScore);

        if (round < TOTAL_ROUNDS) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.asoc_round_done, round))
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.continue_button, (d, w) -> startRound(round + 1))
                    .show();
        } else {
            finishGame(message);
        }
    }

    private void finishGame(String message) {
        boolean won = totalPlayerScore > totalOpponentScore;
        resultRepository.insert(new GameResult(
                userRepository.getCurrentUserId(),
                GameResult.GAME_ASOCIJACIJE,
                totalPlayerScore,
                totalOpponentScore,
                won,
                roundsSolvedByPlayer,
                TOTAL_ROUNDS,
                System.currentTimeMillis()));

        int title = won ? R.string.result_win
                : totalPlayerScore == totalOpponentScore ? R.string.result_draw
                : R.string.result_loss;
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.exit, (d, w) -> finish())
                .show();
    }

    private interface GuessCallback {
        void onGuess(String guess);
    }

    private void showGuessDialog(String title, GuessCallback callback) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.enter_solution);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton(R.string.submit, (d, w) -> {
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) {
                        Toast.makeText(this, R.string.empty_guess, Toast.LENGTH_SHORT).show();
                    } else {
                        callback.onGuess(text);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
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
