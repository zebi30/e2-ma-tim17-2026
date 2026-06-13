package com.example.myapplication.logic;

import java.util.Random;

/**
 * Simulirani protivnik za igranje igara dok ne bude implementirano uparivanje
 * pravih igrača (funkcionalni zahtev 3 - Igranje partija, KO). Sve odluke
 * protivnika donose se ovde, tako da se kasnije lako zamenjuje pravim igračem.
 */
public class BotOpponent {

    public static class KzzMove {
        public final boolean answered;
        public final boolean correct;
        public final long timeMs;

        public KzzMove(boolean answered, boolean correct, long timeMs) {
            this.answered = answered;
            this.correct = correct;
            this.timeMs = timeMs;
        }
    }

    private final Random random = new Random();

    /** Ko zna zna: 45% tačno, 25% netačno, 30% bez odgovora; vreme 1.3 - 4.7 s. */
    public KzzMove decideKzzMove() {
        double roll = random.nextDouble();
        long time = 1300 + random.nextInt(3400);
        if (roll < 0.45) {
            return new KzzMove(true, true, time);
        }
        if (roll < 0.70) {
            return new KzzMove(true, false, time);
        }
        return new KzzMove(false, false, 0);
    }

    /** Spojnice: verovatnoća da protivnik uspešno poveže jedan pojam. */
    public boolean attemptsSpojnicePair() {
        return random.nextDouble() < 0.55;
    }

    /** Pauza između poteza protivnika, da igrač može da prati šta se dešava. */
    public long stepDelayMs() {
        return 800 + random.nextInt(700);
    }

    /** Asocijacije: da li protivnik pokušava da reši kolonu (zavisno od broja otvorenih polja). */
    public boolean wantsColumnGuess(int openedCellsInColumn) {
        return random.nextDouble() < 0.18 * openedCellsInColumn;
    }

    public boolean columnGuessCorrect() {
        return random.nextDouble() < 0.75;
    }

    /** Asocijacije: da li protivnik pokušava konačno rešenje (zavisno od broja rešenih kolona). */
    public boolean wantsFinalGuess(int solvedColumns) {
        return random.nextDouble() < 0.12 * solvedColumns;
    }

    public boolean finalGuessCorrect() {
        return random.nextDouble() < 0.6;
    }

    public int pickIndex(int bound) {
        return random.nextInt(bound);
    }
}
