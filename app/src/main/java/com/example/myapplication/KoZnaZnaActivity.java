package com.example.myapplication;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.data.GameContentRepository;
import com.example.myapplication.data.GameResultRepository;
import com.example.myapplication.data.UserRepository;
import com.example.myapplication.logic.BotOpponent;
import com.example.myapplication.logic.KoZnaZnaEngine;
import com.example.myapplication.model.GameResult;
import com.example.myapplication.ui.PlayerBar;

/**
 * Ko zna zna: 5 pitanja iz baze, 5 sekundi po pitanju, +10 / -5.
 * Igra se protiv simuliranog protivnika; ako oba igrača odgovore tačno,
 * bodove dobija brži (spec. 1.d). Rezultat se upisuje u bazu za statistiku.
 */
public class KoZnaZnaActivity extends AppCompatActivity {

    private static final int QUESTION_TIME_MS = 5000;
    private static final int QUESTION_COUNT = 5;
    private static final int REVEAL_DELAY_MS = 1800;

    private GameContentRepository contentRepository;
    private GameResultRepository resultRepository;
    private UserRepository userRepository;

    private KoZnaZnaEngine engine;
    private boolean answered = false;
    private long questionStartTime;

    private TextView timerView;
    private TextView progressView;
    private TextView questionView;
    private TextView scoreView;
    private TextView opponentScoreView;
    private TextView feedbackView;
    private Button[] answerButtons;
    private CountDownTimer timer;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ko_zna_zna);

        contentRepository = new GameContentRepository(this);
        resultRepository = new GameResultRepository(this);
        userRepository = new UserRepository(this);
        PlayerBar.bind(this, userRepository.getCurrentUser());

        timerView = findViewById(R.id.timer);
        progressView = findViewById(R.id.question_progress);
        questionView = findViewById(R.id.question_text);
        scoreView = findViewById(R.id.score_value);
        opponentScoreView = findViewById(R.id.opponent_score_value);
        feedbackView = findViewById(R.id.feedback_text);
        answerButtons = new Button[]{
                findViewById(R.id.answer_a),
                findViewById(R.id.answer_b),
                findViewById(R.id.answer_c),
                findViewById(R.id.answer_d)
        };

        for (int i = 0; i < answerButtons.length; i++) {
            final int index = i;
            answerButtons[i].setOnClickListener(v -> onAnswerSelected(index));
        }

        findViewById(R.id.quit_button).setOnClickListener(v -> finish());

        startNewGame();
    }

    private void startNewGame() {
        engine = new KoZnaZnaEngine(contentRepository.getRandomQuestions(QUESTION_COUNT), new BotOpponent());
        updateScores();
        showQuestion();
    }

    private void showQuestion() {
        answered = false;
        engine.startQuestion();
        feedbackView.setText("");

        progressView.setText(getString(R.string.question_progress,
                engine.questionNumber(), engine.totalQuestions()));
        questionView.setText(engine.currentQuestion().text);

        int normal = ContextCompat.getColor(this, R.color.background_light);
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setText(engine.currentQuestion().answers[i]);
            answerButtons[i].setEnabled(true);
            answerButtons[i].setBackgroundTintList(ColorStateList.valueOf(normal));
            answerButtons[i].setTextColor(ContextCompat.getColor(this, R.color.primary));
        }

        questionStartTime = SystemClock.elapsedRealtime();
        startTimer();

        long botTime = engine.opponentAnswerTimeMs();
        if (botTime > 0) {
            handler.postDelayed(() -> {
                if (!answered) {
                    feedbackView.setText(R.string.opponent_answered);
                }
            }, botTime);
        }
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new CountDownTimer(QUESTION_TIME_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerView.setText(getString(R.string.time_seconds,
                        (int) Math.ceil(millisUntilFinished / 1000.0)));
            }

            @Override
            public void onFinish() {
                timerView.setText(getString(R.string.time_seconds, 0));
                if (!answered) {
                    answered = true;
                    applyOutcome(engine.resolve(null, QUESTION_TIME_MS), -1);
                }
            }
        }.start();
    }

    private void onAnswerSelected(int index) {
        if (answered) {
            return;
        }
        answered = true;
        if (timer != null) {
            timer.cancel();
        }
        long elapsed = SystemClock.elapsedRealtime() - questionStartTime;
        applyOutcome(engine.resolve(index, elapsed), index);
    }

    private void applyOutcome(KoZnaZnaEngine.QuestionOutcome outcome, int selectedIndex) {
        int green = ContextCompat.getColor(this, R.color.success);
        int red = ContextCompat.getColor(this, R.color.error);
        int white = ContextCompat.getColor(this, R.color.white);
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setEnabled(false);
            if (i == outcome.correctIndex) {
                answerButtons[i].setBackgroundTintList(ColorStateList.valueOf(green));
                answerButtons[i].setTextColor(white);
            } else if (i == selectedIndex) {
                answerButtons[i].setBackgroundTintList(ColorStateList.valueOf(red));
                answerButtons[i].setTextColor(white);
            }
        }

        updateScores();
        feedbackView.setText(buildFeedback(outcome));
        handler.postDelayed(this::nextQuestion, REVEAL_DELAY_MS);
    }

    private String buildFeedback(KoZnaZnaEngine.QuestionOutcome outcome) {
        String playerLine;
        if (!outcome.playerAnswered) {
            playerLine = getString(R.string.fb_no_answer);
        } else if (outcome.playerCorrect) {
            if (outcome.opponentCorrect) {
                playerLine = getString(outcome.opponentFaster
                        ? R.string.fb_player_correct_slower
                        : R.string.fb_player_correct_faster);
            } else {
                playerLine = getString(R.string.fb_player_correct);
            }
        } else {
            playerLine = getString(R.string.fb_player_wrong);
        }

        String opponentLine;
        if (!outcome.opponentAnswered) {
            opponentLine = getString(R.string.fb_opp_none);
        } else if (outcome.opponentCorrect) {
            opponentLine = getString(outcome.playerCorrect && !outcome.opponentFaster
                    ? R.string.fb_opp_correct_slower
                    : R.string.fb_opp_correct);
        } else {
            opponentLine = getString(R.string.fb_opp_wrong);
        }

        return playerLine + "\n" + opponentLine;
    }

    private void updateScores() {
        scoreView.setText(String.valueOf(engine.playerScore()));
        opponentScoreView.setText(String.valueOf(engine.opponentScore()));
    }

    private void nextQuestion() {
        if (engine.hasNext()) {
            engine.next();
            showQuestion();
        } else {
            endGame();
        }
    }

    private void endGame() {
        boolean won = engine.playerScore() > engine.opponentScore();
        resultRepository.insert(new GameResult(
                userRepository.getCurrentUserId(),
                GameResult.GAME_KO_ZNA_ZNA,
                engine.playerScore(),
                engine.opponentScore(),
                won,
                engine.playerCorrectCount(),
                engine.playerWrongCount(),
                System.currentTimeMillis()));

        if (isFinishing()) {
            return;
        }
        int title = won ? R.string.result_win
                : engine.playerScore() == engine.opponentScore() ? R.string.result_draw
                : R.string.result_loss;
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(getString(R.string.final_score_fmt,
                        engine.playerScore(), engine.opponentScore()))
                .setCancelable(false)
                .setPositiveButton(R.string.play_again, (d, w) -> startNewGame())
                .setNegativeButton(R.string.exit, (d, w) -> finish())
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
