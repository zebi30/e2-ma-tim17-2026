package com.example.myapplication.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppNotification {
    public static final String CHANNEL_CHAT = "chat";
    public static final String CHANNEL_RANKING = "ranking";
    public static final String CHANNEL_REWARDS = "rewards";
    public static final String CHANNEL_OTHER = "other";

    public long id;
    public long userId;
    public String channel;
    public String title;
    public String message;
    public boolean isRead;
    public long createdAt;

    public AppNotification(long id, long userId, String channel, String title,
                           String message, boolean isRead, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.channel = channel;
        this.title = title;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public String formattedTime() {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(new Date(createdAt));
    }

    public String channelIcon() {
        switch (channel) {
            case CHANNEL_CHAT:    return "💬";
            case CHANNEL_RANKING: return "📊";
            case CHANNEL_REWARDS: return "🎁";
            default:              return "🔔";
        }
    }
}
