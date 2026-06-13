package com.example.myapplication.model;

public class ScoreAggregate {
    public final int games;
    public final double avg;
    public final int min;
    public final int max;

    public ScoreAggregate(int games, double avg, int min, int max) {
        this.games = games;
        this.avg = avg;
        this.min = min;
        this.max = max;
    }

    public boolean hasData() {
        return games > 0;
    }
}
