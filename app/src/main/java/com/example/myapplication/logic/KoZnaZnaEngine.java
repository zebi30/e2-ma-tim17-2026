package com.example.myapplication.logic;

import com.example.myapplication.model.Question;

import java.util.List;

/**
 * Pravila igre Ko zna zna: 5 pitanja, tačan odgovor +10, netačan -5,
 * bez odgovora 0. Ako oba igrača odgovore tačno, bodove dobija brži (spec. 1.d).
 */
public class KoZnaZnaEngine {

    public static final int CORRECT_POINTS = 10;
    public static final int WRONG_POINTS = -5;

    public static class QuestionOutcome {
        public final int correctIndex;
        public final boolean playerAnswered;
        public final boolean playerCorrect;
        public final boolean opponentAnswered;
        public final boolean opponentCorrect;
        /** Relevantno samo kad su oba odgovora tačna. */
        public final boolean opponentFaster;
        public final int playerDelta;
        public final int opponentDelta;

        QuestionOutcome(int correctIndex, boolean playerAnswered, boolean playerCorrect,
                        boolean opponentAnswered, boolean opponentCorrect, boolean opponentFaster,
                        int playerDelta, int opponentDelta) {
            this.correctIndex = correctIndex;
            this.playerAnswered = playerAnswered;
            this.playerCorrect = playerCorrect;
            this.opponentAnswered = opponentAnswered;
            this.opponentCorrect = opponentCorrect;
            this.opponentFaster = opponentFaster;
            this.playerDelta = playerDelta;
            this.opponentDelta = opponentDelta;
        }
    }

    private final List<Question> questions;
    private final BotOpponent bot;

    private int index = 0;
    private int playerScore = 0;
    private int opponentScore = 0;
    private int playerCorrectCount = 0;
    private int playerWrongCount = 0;
    private BotOpponent.KzzMove botMove;

    public KoZnaZnaEngine(List<Question> questions, BotOpponent bot) {
        this.questions = questions;
        this.bot = bot;
    }

    public Question currentQuestion() {
        return questions.get(index);
    }

    public int questionNumber() {
        return index + 1;
    }

    public int totalQuestions() {
        return questions.size();
    }

    /** Poziva se na početku svakog pitanja; protivnik tada odlučuje svoj potez. */
    public void startQuestion() {
        botMove = bot.decideKzzMove();
    }

    /** Vreme protivnikovog odgovora u ms, ili -1 ako ne odgovara. */
    public long opponentAnswerTimeMs() {
        return botMove != null && botMove.answered ? botMove.timeMs : -1;
    }

    /** Razrešava pitanje; playerAnswer je null ako igrač nije odgovorio. */
    public QuestionOutcome resolve(Integer playerAnswer, long playerTimeMs) {
        Question q = currentQuestion();
        boolean pAnswered = playerAnswer != null;
        boolean pCorrect = pAnswered && playerAnswer == q.correctIndex;
        boolean oAnswered = botMove != null && botMove.answered;
        boolean oCorrect = oAnswered && botMove.correct;
        boolean oFaster = false;
        int pDelta = 0;
        int oDelta = 0;

        if (pAnswered) {
            if (pCorrect) {
                playerCorrectCount++;
                if (oCorrect) {
                    oFaster = botMove.timeMs < playerTimeMs;
                    if (oFaster) {
                        oDelta = CORRECT_POINTS;
                    } else {
                        pDelta = CORRECT_POINTS;
                    }
                } else {
                    pDelta = CORRECT_POINTS;
                    if (oAnswered) {
                        oDelta = WRONG_POINTS;
                    }
                }
            } else {
                playerWrongCount++;
                pDelta = WRONG_POINTS;
                if (oCorrect) {
                    oDelta = CORRECT_POINTS;
                } else if (oAnswered) {
                    oDelta = WRONG_POINTS;
                }
            }
        } else {
            if (oCorrect) {
                oDelta = CORRECT_POINTS;
            } else if (oAnswered) {
                oDelta = WRONG_POINTS;
            }
        }

        playerScore += pDelta;
        opponentScore += oDelta;
        return new QuestionOutcome(q.correctIndex, pAnswered, pCorrect,
                oAnswered, oCorrect, oFaster, pDelta, oDelta);
    }

    public boolean hasNext() {
        return index + 1 < questions.size();
    }

    public void next() {
        index++;
    }

    public int playerScore() {
        return playerScore;
    }

    public int opponentScore() {
        return opponentScore;
    }

    public int playerCorrectCount() {
        return playerCorrectCount;
    }

    public int playerWrongCount() {
        return playerWrongCount;
    }
}
