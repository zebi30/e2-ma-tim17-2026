package com.example.myapplication.logic;

/**
 * Određivanje lige na osnovu zvezda (spec. 6.c): prva liga 100 zvezda,
 * svaka naredna duplo više od prethodne.
 */
public final class LeagueService {

    /** Minimalan broj zvezda za ligu sa datim indeksom (0 = nulta liga). */
    public static final int[] THRESHOLDS = {0, 100, 200, 400, 800, 1600};

    private LeagueService() {
    }

    public static int leagueIndexForStars(int stars) {
        int index = 0;
        for (int i = 1; i < THRESHOLDS.length; i++) {
            if (stars >= THRESHOLDS[i]) {
                index = i;
            }
        }
        return index;
    }
}
