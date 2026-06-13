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

    /** Vraća trenutno ulogovanog korisnika ili null ako niko nije ulogovan. */
    public User getCurrentUser() {
        long id = session.getUserId();
        return id > 0 ? findById(id) : null;
    }

    public long getCurrentUserId() {
        User current = getCurrentUser();
        return current != null ? current.id : -1;
    }

    public void updateAvatar(long userId, int avatarIndex) {
        ContentValues v = new ContentValues();
        v.put("avatar", avatarIndex);
        helper.getWritableDatabase().update(AppDbHelper.T_USERS, v, "id = ?",
                new String[]{String.valueOf(userId)});
    }

    public User register(String username, String email, String region, String password) {
        if (findByUsernameOrEmail(username) != null || findByUsernameOrEmail(email) != null) {
            return null;
        }
        String verificationCode = generateVerificationCode(email);
        ContentValues v = new ContentValues();
        v.put("username", username);
        v.put("email", email);
        v.put("region", region);
        v.put("password", password);
        v.put("verified", 0);
        v.put("verification_code", verificationCode);
        v.put("avatar", 0);
        v.put("tokens", 5);
        v.put("stars", 0);
        long id = helper.getWritableDatabase().insert(AppDbHelper.T_USERS, null, v);
        return id > 0 ? new User(id, username, email, region, 0, 5, 0) : null;
    }

    public User login(String identifier, String password) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, username, email, region, avatar, tokens, stars, password, verified FROM " + AppDbHelper.T_USERS
                        + " WHERE username = ? OR email = ?",
                new String[]{identifier, identifier})) {
            if (!c.moveToFirst()) {
                return null;
            }
            String storedPassword = c.getString(c.getColumnIndexOrThrow("password"));
            int verified = c.getInt(c.getColumnIndexOrThrow("verified"));
            if (!storedPassword.equals(password) || verified == 0) {
                return null;
            }
            User user = readUser(c);
            session.setUserId(user.id);
            return user;
        }
    }

    public boolean accountExists(String identifier) {
        return findByUsernameOrEmail(identifier) != null;
    }

    public boolean isVerified(String identifier) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT verified FROM " + AppDbHelper.T_USERS + " WHERE username = ? OR email = ?",
                new String[]{identifier, identifier})) {
            return c.moveToFirst() && c.getInt(0) == 1;
        }
    }

    public String getEmailForIdentifier(String identifier) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT email FROM " + AppDbHelper.T_USERS + " WHERE username = ? OR email = ?",
                new String[]{identifier, identifier})) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    public String getVerificationCode(String identifier) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT verification_code FROM " + AppDbHelper.T_USERS + " WHERE username = ? OR email = ?",
                new String[]{identifier, identifier})) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    public boolean verifyEmail(String identifier, String code) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT id, verification_code, verified FROM " + AppDbHelper.T_USERS + " WHERE username = ? OR email = ?",
                new String[]{identifier, identifier})) {
            if (!c.moveToFirst() || c.getInt(c.getColumnIndexOrThrow("verified")) == 1) {
                return false;
            }
            String storedCode = c.getString(c.getColumnIndexOrThrow("verification_code"));
            if (storedCode == null || !storedCode.equals(code)) {
                return false;
            }
            long userId = c.getLong(c.getColumnIndexOrThrow("id"));
            ContentValues v = new ContentValues();
            v.put("verified", 1);
            v.put("verification_code", (String) null);
            return helper.getWritableDatabase().update(AppDbHelper.T_USERS, v, "id = ?",
                    new String[]{String.valueOf(userId)}) > 0;
        }
    }

    public boolean resendVerificationCode(String identifier) {
        String code = getVerificationCode(identifier);
        if (code == null) {
            return false;
        }
        ContentValues v = new ContentValues();
        v.put("verification_code", code);
        return helper.getWritableDatabase().update(AppDbHelper.T_USERS, v, "username = ? OR email = ?",
                new String[]{identifier, identifier}) > 0;
    }

    public boolean markVerified(long userId) {
        ContentValues v = new ContentValues();
        v.put("verified", 1);
        return helper.getWritableDatabase().update(AppDbHelper.T_USERS, v, "id = ?",
                new String[]{String.valueOf(userId)}) > 0;
    }

    public boolean resetPassword(long userId, String oldPassword, String newPassword) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT password FROM " + AppDbHelper.T_USERS + " WHERE id = ?",
                new String[]{String.valueOf(userId)})) {
            if (!c.moveToFirst() || !oldPassword.equals(c.getString(0))) {
                return false;
            }
        }
        ContentValues v = new ContentValues();
        v.put("password", newPassword);
        return helper.getWritableDatabase().update(AppDbHelper.T_USERS, v, "id = ?",
                new String[]{String.valueOf(userId)}) > 0;
    }

    public void logout() {
        session.clear();
    }

    public User findByUsernameOrEmail(String identifier) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(AppDbHelper.T_USERS,
                new String[]{"id", "username", "email", "region", "avatar", "tokens", "stars"},
                "username = ? OR email = ?", new String[]{identifier, identifier}, null, null, null)) {
            if (c.moveToFirst()) {
                return readUser(c);
            }
        }
        return null;
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

    private String generateVerificationCode(String email) {
        int hash = Math.abs((email + System.currentTimeMillis()).hashCode());
        return String.format("%06d", hash % 1_000_000);
    }
}
