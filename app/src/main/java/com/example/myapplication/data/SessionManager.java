package com.example.myapplication.data;

import android.content.Context;
import android.content.SharedPreferences;

/** Čuva id trenutno ulogovanog korisnika (lokalna sesija). */
public class SessionManager {
    private static final String PREFS = "session";
    private static final String KEY_USER_ID = "user_id";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    public void setUserId(long userId) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply();
    }

    public boolean isLoggedIn() {
        return getUserId() > 0;
    }

    public void clear() {
        prefs.edit().remove(KEY_USER_ID).apply();
    }
}
