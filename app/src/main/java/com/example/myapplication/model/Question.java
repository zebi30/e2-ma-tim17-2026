package com.example.myapplication.model;

public class Question {
    public final long id;
    public final String text;
    public final String[] answers;
    public final int correctIndex;

    public Question(long id, String text, String[] answers, int correctIndex) {
        this.id = id;
        this.text = text;
        this.answers = answers;
        this.correctIndex = correctIndex;
    }
}
