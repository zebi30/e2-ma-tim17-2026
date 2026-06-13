package com.example.myapplication.model;

public class User {
    public final long id;
    public final String username;
    public final String email;
    public final String region;
    public final int avatarIndex;
    public final int tokens;
    public final int stars;

    public User(long id, String username, String email, String region,
                int avatarIndex, int tokens, int stars) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.region = region;
        this.avatarIndex = avatarIndex;
        this.tokens = tokens;
        this.stars = stars;
    }
}
