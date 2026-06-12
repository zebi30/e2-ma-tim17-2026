package com.example.myapplication.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.model.GameResult;
import com.example.myapplication.model.ScoreAggregate;

/** Upis i čitanje rezultata odigranih igara (osnova za statistiku profila). */
public class GameResultRepository {

    private final AppDbHelper helper;

    public GameResultRepository(Context context) {
        helper = new AppDbHelper(context);
    }

    public void insert(GameResult r) {
        ContentValues v = new ContentValues();
        v.put("user_id", r.userId);
        v.put("game", r.game);
        v.put("player_score", r.playerScore);
        v.put("opponent_score", r.opponentScore);
        v.put("won", r.won ? 1 : 0);
        v.put("detail_a", r.detailA);
        v.put("detail_b", r.detailB);
        v.put("played_at", r.playedAt);
        helper.getWritableDatabase().insert(AppDbHelper.T_RESULTS, null, v);
    }

    public ScoreAggregate aggregate(long userId, String game) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*), IFNULL(AVG(player_score), 0), IFNULL(MIN(player_score), 0),"
                        + " IFNULL(MAX(player_score), 0) FROM " + AppDbHelper.T_RESULTS
                        + " WHERE user_id = ? AND game = ?",
                new String[]{String.valueOf(userId), game})) {
            if (c.moveToFirst()) {
                return new ScoreAggregate(c.getInt(0), c.getDouble(1), c.getInt(2), c.getInt(3));
            }
        }
        return new ScoreAggregate(0, 0, 0, 0);
    }

    /** Vraća [SUM(detail_a), SUM(detail_b)] za datu igru. */
    public int[] sumDetails(long userId, String game) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT IFNULL(SUM(detail_a), 0), IFNULL(SUM(detail_b), 0) FROM "
                        + AppDbHelper.T_RESULTS + " WHERE user_id = ? AND game = ?",
                new String[]{String.valueOf(userId), game})) {
            if (c.moveToFirst()) {
                return new int[]{c.getInt(0), c.getInt(1)};
            }
        }
        return new int[]{0, 0};
    }

    public int countGames(long userId) {
        return queryInt("SELECT COUNT(*) FROM " + AppDbHelper.T_RESULTS + " WHERE user_id = ?", userId);
    }

    public int countWins(long userId) {
        return queryInt("SELECT IFNULL(SUM(won), 0) FROM " + AppDbHelper.T_RESULTS + " WHERE user_id = ?", userId);
    }

    private int queryInt(String sql, long userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(userId)})) {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        }
        return 0;
    }
}
