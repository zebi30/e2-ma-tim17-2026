package com.example.myapplication.logic;

import com.example.myapplication.model.AssociationSet;

/**
 * Stanje jedne runde asocijacija i pravila bodovanja:
 * rešenje kolone = 2 boda + 1 za svako neotvoreno polje (spec. 3.f);
 * konačno rešenje = 7 bodova + 6 za svaku neotvorenu kolonu + bodovi
 * za otvorene/delimično otvorene nerešene kolone (spec. 3.g).
 */
public class AsocijacijeEngine {

    public enum Side { NONE, PLAYER, OPPONENT }

    public static final int COLS = 4;
    public static final int ROWS = 4;

    private final AssociationSet set;
    private final boolean[][] opened = new boolean[COLS][ROWS];
    private final boolean[] columnSolved = new boolean[COLS];
    private final Side[] columnSolvedBy = new Side[COLS];
    private boolean finalSolved = false;
    private Side finalSolvedBy = Side.NONE;
    private int playerScore = 0;
    private int opponentScore = 0;

    public AsocijacijeEngine(AssociationSet set) {
        this.set = set;
        for (int c = 0; c < COLS; c++) {
            columnSolvedBy[c] = Side.NONE;
        }
    }

    public boolean openCell(int col, int row) {
        if (finalSolved || columnSolved[col] || opened[col][row]) {
            return false;
        }
        opened[col][row] = true;
        return true;
    }

    public boolean isOpened(int col, int row) {
        return opened[col][row];
    }

    public String cellText(int col, int row) {
        return set.cells[col][row];
    }

    public boolean isColumnSolved(int col) {
        return columnSolved[col];
    }

    public Side columnSolvedBy(int col) {
        return columnSolvedBy[col];
    }

    public String columnSolution(int col) {
        return set.columnSolutions[col];
    }

    public String finalSolution() {
        return set.finalSolution;
    }

    public boolean isFinalSolved() {
        return finalSolved;
    }

    public Side finalSolvedBy() {
        return finalSolvedBy;
    }

    public int openedInColumn(int col) {
        int count = 0;
        for (int r = 0; r < ROWS; r++) {
            if (opened[col][r]) {
                count++;
            }
        }
        return count;
    }

    public int unopenedInColumn(int col) {
        return ROWS - openedInColumn(col);
    }

    public int solvedColumnsCount() {
        int count = 0;
        for (int c = 0; c < COLS; c++) {
            if (columnSolved[c]) {
                count++;
            }
        }
        return count;
    }

    public boolean checkColumnGuess(int col, String guess) {
        return set.columnSolutions[col].equalsIgnoreCase(guess.trim());
    }

    public boolean checkFinalGuess(String guess) {
        return set.finalSolution.equalsIgnoreCase(guess.trim());
    }

    /** Rešava kolonu i dodeljuje bodove (2 + 1 po neotvorenom polju). Vraća osvojene bodove. */
    public int solveColumn(int col, Side by) {
        if (finalSolved || columnSolved[col]) {
            return 0;
        }
        int points = 2 + unopenedInColumn(col);
        columnSolved[col] = true;
        columnSolvedBy[col] = by;
        addScore(by, points);
        return points;
    }

    /**
     * Rešava konačno rešenje: 7 bodova + 6 za svaku netaknutu nerešenu kolonu
     * + (2 + neotvorena polja) za delimično otvorene nerešene kolone.
     */
    public int solveFinal(Side by) {
        if (finalSolved) {
            return 0;
        }
        int points = 7;
        for (int c = 0; c < COLS; c++) {
            if (columnSolved[c]) {
                continue;
            }
            if (openedInColumn(c) == 0) {
                points += 6;
            } else {
                points += 2 + unopenedInColumn(c);
            }
            columnSolved[c] = true;
            columnSolvedBy[c] = by;
        }
        finalSolved = true;
        finalSolvedBy = by;
        addScore(by, points);
        return points;
    }

    public int playerScore() {
        return playerScore;
    }

    public int opponentScore() {
        return opponentScore;
    }

    private void addScore(Side side, int points) {
        if (side == Side.PLAYER) {
            playerScore += points;
        } else if (side == Side.OPPONENT) {
            opponentScore += points;
        }
    }
}
