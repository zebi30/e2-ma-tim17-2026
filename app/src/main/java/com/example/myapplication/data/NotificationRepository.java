package com.example.myapplication.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myapplication.model.AppNotification;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {

    private final AppDbHelper helper;

    public NotificationRepository(Context context) {
        helper = new AppDbHelper(context);
    }

    public long insert(long userId, String channel, String title, String message) {
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("channel", channel);
        v.put("title", title);
        v.put("message", message);
        v.put("is_read", 0);
        v.put("created_at", System.currentTimeMillis());
        return helper.getWritableDatabase().insert(AppDbHelper.T_NOTIFICATIONS, null, v);
    }

    public List<AppNotification> getAll(long userId) {
        return query(userId, null, false);
    }

    public List<AppNotification> getByChannel(long userId, String channel) {
        return query(userId, channel, false);
    }

    public List<AppNotification> getUnread(long userId) {
        return query(userId, null, true);
    }

    public List<AppNotification> getReadOnly(long userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<AppNotification> list = new ArrayList<>();
        try (Cursor c = db.query(AppDbHelper.T_NOTIFICATIONS, null,
                "user_id = ? AND is_read = 1",
                new String[]{String.valueOf(userId)}, null, null, "created_at DESC")) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }

    public void markRead(long notifId) {
        ContentValues v = new ContentValues();
        v.put("is_read", 1);
        helper.getWritableDatabase().update(AppDbHelper.T_NOTIFICATIONS, v,
                "id = ?", new String[]{String.valueOf(notifId)});
    }

    public void markAllRead(long userId) {
        ContentValues v = new ContentValues();
        v.put("is_read", 1);
        helper.getWritableDatabase().update(AppDbHelper.T_NOTIFICATIONS, v,
                "user_id = ?", new String[]{String.valueOf(userId)});
    }

    private List<AppNotification> query(long userId, String channel, boolean unreadOnly) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String where = "user_id = ?";
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(userId));
        if (channel != null) {
            where += " AND channel = ?";
            args.add(channel);
        }
        if (unreadOnly) {
            where += " AND is_read = 0";
        }
        List<AppNotification> list = new ArrayList<>();
        try (Cursor c = db.query(AppDbHelper.T_NOTIFICATIONS, null,
                where, args.toArray(new String[0]), null, null, "created_at DESC")) {
            while (c.moveToNext()) list.add(fromCursor(c));
        }
        return list;
    }

    private AppNotification fromCursor(Cursor c) {
        return new AppNotification(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getLong(c.getColumnIndexOrThrow("user_id")),
                c.getString(c.getColumnIndexOrThrow("channel")),
                c.getString(c.getColumnIndexOrThrow("title")),
                c.getString(c.getColumnIndexOrThrow("message")),
                c.getInt(c.getColumnIndexOrThrow("is_read")) == 1,
                c.getLong(c.getColumnIndexOrThrow("created_at")));
    }
}
