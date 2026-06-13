package com.example.myapplication.model;

public class GameResult {
    public static final String GAME_KO_ZNA_ZNA = "KO_ZNA_ZNA";
    public static final String GAME_SPOJNICE = "SPOJNICE";
    public static final String GAME_ASOCIJACIJE = "ASOCIJACIJE";
    public static final String GAME_KORAK_PO_KORAK = "KORAK_PO_KORAK";
    public static final String GAME_MOJ_BROJ = "MOJ_BROJ";
    public static final String GAME_SKOCKO = "SKOCKO";

    public final long userId;
    public final String game;
    public final int playerScore;
    public final int opponentScore;
    public final boolean won;
    /** Značenje zavisi od igre: KZZ = broj tačnih, Spojnice = broj povezanih, Asocijacije = broj rešenih rundi. */
    public final int detailA;
    /** KZZ = broj netačnih, Spojnice = broj prilika, Asocijacije = ukupan broj rundi. */
    public final int detailB;
    public final long playedAt;

    public GameResult(long userId, String game, int playerScore, int opponentScore,
                      boolean won, int detailA, int detailB, long playedAt) {
        this.userId = userId;
        this.game = game;
        this.playerScore = playerScore;
        this.opponentScore = opponentScore;
        this.won = won;
        this.detailA = detailA;
        this.detailB = detailB;
        this.playedAt = playedAt;
    }
}
