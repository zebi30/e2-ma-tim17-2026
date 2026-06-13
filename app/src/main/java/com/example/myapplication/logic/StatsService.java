package com.example.myapplication.logic;

import com.example.myapplication.data.GameResultRepository;
import com.example.myapplication.model.GameResult;
import com.example.myapplication.model.ProfileStats;

/** Računa statistiku profila (spec. 2.c) iz zabeleženih rezultata igara. */
public class StatsService {

    private final GameResultRepository results;

    public StatsService(GameResultRepository results) {
        this.results = results;
    }

    public ProfileStats loadFor(long userId) {
        ProfileStats stats = new ProfileStats();

        stats.koZnaZna = results.aggregate(userId, GameResult.GAME_KO_ZNA_ZNA);
        stats.spojnice = results.aggregate(userId, GameResult.GAME_SPOJNICE);
        stats.asocijacije = results.aggregate(userId, GameResult.GAME_ASOCIJACIJE);

        int[] kzz = results.sumDetails(userId, GameResult.GAME_KO_ZNA_ZNA);
        stats.kzzCorrect = kzz[0];
        stats.kzzWrong = kzz[1];

        int[] spojnice = results.sumDetails(userId, GameResult.GAME_SPOJNICE);
        stats.spojniceConnected = spojnice[0];
        stats.spojniceOpportunities = spojnice[1];

        int[] asoc = results.sumDetails(userId, GameResult.GAME_ASOCIJACIJE);
        stats.asocSolvedRounds = asoc[0];
        stats.asocTotalRounds = asoc[1];

        stats.totalGames = results.countGames(userId);
        stats.wins = results.countWins(userId);
        return stats;
    }
}
