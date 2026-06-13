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

        int[] mojBroj = results.sumDetails(userId, GameResult.GAME_MOJ_BROJ);
        stats.mojBrojExact = mojBroj[0];
        stats.mojBrojRounds = mojBroj[1];

        int[] korak = results.sumDetails(userId, GameResult.GAME_KORAK_PO_KORAK);
        stats.korakSolved = korak[0];
        stats.korakRounds = korak[1];

        int[] skocko = results.sumDetails(userId, GameResult.GAME_SKOCKO);
        stats.skockoSolved = skocko[0];
        stats.skockoRounds = skocko[1];

        stats.totalGames = results.countGames(userId);
        stats.wins = results.countWins(userId);
        return stats;
    }
}
