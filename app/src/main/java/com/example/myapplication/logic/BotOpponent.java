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

    // ---------------------------------------------------------------------
    // Skočko
    // ---------------------------------------------------------------------

    /** Attempt (1..6) at which the bot solves its own combo, or -1 if it fails. */
    public int skockoSolveAttempt() {
        double roll = random.nextDouble();
        if (roll < 0.12) return 1;
        if (roll < 0.26) return 2;
        if (roll < 0.42) return 3;
        if (roll < 0.56) return 4;
        if (roll < 0.66) return 5;
        if (roll < 0.74) return 6;
        return -1; // ~26% chance the bot fails
    }

    /** Bot's single 10s steal attempt when the player failed their own combo. */
    public boolean skockoStealSucceeds() {
        return random.nextDouble() < 0.30;
    }

    // ---------------------------------------------------------------------
    // Korak po korak
    // ---------------------------------------------------------------------

    /** Step (1..7) at which the bot solves its own term, or -1 if it fails. */
    public int korakSolveStep() {
        double roll = random.nextDouble();
        if (roll < 0.05) return 1;
        if (roll < 0.12) return 2;
        if (roll < 0.23) return 3;
        if (roll < 0.38) return 4;
        if (roll < 0.54) return 5;
        if (roll < 0.68) return 6;
        if (roll < 0.80) return 7;
        return -1; // ~20% chance the bot fails
    }

    /** Bot's single 10s steal attempt when the player failed their own term. */
    public boolean korakStealSucceeds() {
        return random.nextDouble() < 0.35;
    }

    // ---------------------------------------------------------------------
    // Moj broj
    // ---------------------------------------------------------------------

    /** Whether the bot reaches the target exactly in its own round. */
    public boolean mojBrojReachesTarget() {
        return random.nextDouble() < 0.45;
    }

    /** When the bot does not reach the target, how far off its best result is. */
    public int mojBrojDistance() {
        return 1 + random.nextInt(40);
    }
}
