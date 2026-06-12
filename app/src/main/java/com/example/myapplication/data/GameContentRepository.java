package com.example.myapplication.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.model.AssociationSet;
import com.example.myapplication.model.Question;
import com.example.myapplication.model.SpojnicePair;

import java.util.ArrayList;
import java.util.List;

/** Pristup podacima za igre (pitanja, spojnice, asocijacije) iz baze. */
public class GameContentRepository {

    private final AppDbHelper helper;

    public GameContentRepository(Context context) {
        helper = new AppDbHelper(context);
    }

    public List<Question> getRandomQuestions(int count) {
        List<Question> result = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, text, answer_a, answer_b, answer_c, answer_d, correct_index FROM "
                        + AppDbHelper.T_QUESTIONS + " ORDER BY RANDOM() LIMIT ?",
                new String[]{String.valueOf(count)})) {
            while (c.moveToNext()) {
                result.add(new Question(
                        c.getLong(0),
                        c.getString(1),
                        new String[]{c.getString(2), c.getString(3), c.getString(4), c.getString(5)},
                        c.getInt(6)));
            }
        }
        return result;
    }

    public List<Long> getSpojniceSetIds() {
        List<Long> ids = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT DISTINCT set_id FROM " + AppDbHelper.T_SPOJNICE + " ORDER BY RANDOM()", null)) {
            while (c.moveToNext()) {
                ids.add(c.getLong(0));
            }
        }
        return ids;
    }

    public List<SpojnicePair> getSpojniceSet(long setId) {
        List<SpojnicePair> pairs = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT left_item, right_item FROM " + AppDbHelper.T_SPOJNICE
                        + " WHERE set_id = ? ORDER BY id",
                new String[]{String.valueOf(setId)})) {
            while (c.moveToNext()) {
                pairs.add(new SpojnicePair(c.getString(0), c.getString(1)));
            }
        }
        return pairs;
    }

    public List<AssociationSet> getAllAssociationSets() {
        List<AssociationSet> sets = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, final_solution FROM " + AppDbHelper.T_ASSOC_SETS + " ORDER BY RANDOM()", null)) {
            while (c.moveToNext()) {
                long setId = c.getLong(0);
                String finalSolution = c.getString(1);
                String[][] cells = new String[4][4];
                String[] solutions = new String[4];
                try (Cursor cc = db.rawQuery(
                        "SELECT col_index, cell_1, cell_2, cell_3, cell_4, solution FROM "
                                + AppDbHelper.T_ASSOC_COLUMNS + " WHERE set_id = ? ORDER BY col_index",
                        new String[]{String.valueOf(setId)})) {
                    while (cc.moveToNext()) {
                        int col = cc.getInt(0);
                        cells[col][0] = cc.getString(1);
                        cells[col][1] = cc.getString(2);
                        cells[col][2] = cc.getString(3);
                        cells[col][3] = cc.getString(4);
                        solutions[col] = cc.getString(5);
                    }
                }
                sets.add(new AssociationSet(setId, cells, solutions, finalSolution));
            }
        }
        return sets;
    }
}
