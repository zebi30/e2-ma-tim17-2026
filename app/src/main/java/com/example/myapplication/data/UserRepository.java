package com.example.myapplication.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.model.User;

public class UserRepository {

    private final AppDbHelper helper;
    private final SessionManager session;

    public UserRepository(Context context) {
        helper = new AppDbHelper(context);
        session = new SessionManager(context);
    }

    /**
     * Vraća trenutno ulogovanog korisnika. Dok registracija/logovanje (Student 1)
     * ne budu povezani sa bazom, pri prvom pozivu kreira se lokalni demo nalog.
     */
    public User getCurrentUser() {
        long id = session.getUserId();
        User user = id > 0 ? findById(id) : null;
        if (user == null) {
            user = createDemoUser();
            session.setUserId(user.id);
        }
        return user;
    }

    public long getCurrentUserId() {
        return getCurrentUser().id;
    }

    public void updateAvatar(long userId, int avatarIndex) {
        ContentValues v = new ContentValues();
        v.put("avatar", avatarIndex);
        helper.getWritableDatabase().update(AppDbHelper.T_USERS, v, "id = ?",
                new String[]{String.valueOf(userId)});
    }

    public void logout() {
        session.clear();
    }

    private User findById(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(AppDbHelper.T_USERS,
                new String[]{"id", "username", "email", "region", "avatar", "tokens", "stars"},
                "id = ?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (c.moveToFirst()) {
                return readUser(c);
            }
        }
        return null;
    }

    private User createDemoUser() {
        ContentValues v = new ContentValues();
        v.put("username", "Igrac123");
        v.put("email", "igrac@email.com");
        v.put("region", "Vojvodina");
        v.put("avatar", 0);
        v.put("tokens", 5);
        v.put("stars", 120);
        long id = helper.getWritableDatabase().insert(AppDbHelper.T_USERS, null, v);
        return new User(id, "Igrac123", "igrac@email.com", "Vojvodina", 0, 5, 120);
    }

    private User readUser(Cursor c) {
        return new User(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("username")),
                c.getString(c.getColumnIndexOrThrow("email")),
                c.getString(c.getColumnIndexOrThrow("region")),
                c.getInt(c.getColumnIndexOrThrow("avatar")),
                c.getInt(c.getColumnIndexOrThrow("tokens")),
                c.getInt(c.getColumnIndexOrThrow("stars")));
    }
}
