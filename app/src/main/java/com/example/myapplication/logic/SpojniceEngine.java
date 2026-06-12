package com.example.myapplication.logic;

import com.example.myapplication.model.SpojnicePair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stanje jedne runde spojnica: 5 parova, desna kolona izmešana.
 * Svaki povezan par nosi 2 boda (spec. 2.d).
 */
public class SpojniceEngine {

    public static final int POINTS_PER_PAIR = 2;

    public enum Connector { NONE, PLAYER, OPPONENT }

    private final List<SpojnicePair> pairs;
    /** rightOrder.get(pozicijaPrikaza) = indeks para čiji je desni pojam na toj poziciji. */
    private final List<Integer> rightOrder = new ArrayList<>();
    private final Connector[] connectedBy;

    public SpojniceEngine(List<SpojnicePair> pairs) {
        this.pairs = pairs;
        for (int i = 0; i < pairs.size(); i++) {
            rightOrder.add(i);
        }
        Collections.shuffle(rightOrder);
        connectedBy = new Connector[pairs.size()];
        for (int i = 0; i < connectedBy.length; i++) {
            connectedBy[i] = Connector.NONE;
        }
    }

    public int size() {
        return pairs.size();
    }

    public String leftText(int leftIndex) {
        return pairs.get(leftIndex).left;
    }

    public String rightText(int displayPos) {
        return pairs.get(rightOrder.get(displayPos)).right;
    }

    public boolean isMatch(int leftIndex, int rightDisplayPos) {
        return rightOrder.get(rightDisplayPos) == leftIndex;
    }

    public int rightPosForLeft(int leftIndex) {
        return rightOrder.indexOf(leftIndex);
    }

    public boolean isConnected(int leftIndex) {
        return connectedBy[leftIndex] != Connector.NONE;
    }

    public Connector connectorOf(int leftIndex) {
        return connectedBy[leftIndex];
    }

    public void connect(int leftIndex, Connector by) {
        connectedBy[leftIndex] = by;
    }

    public List<Integer> unconnectedLefts() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < connectedBy.length; i++) {
            if (connectedBy[i] == Connector.NONE) {
                result.add(i);
            }
        }
        return result;
    }

    public int connectedCount(Connector by) {
        int count = 0;
        for (Connector c : connectedBy) {
            if (c == by) {
                count++;
            }
        }
        return count;
    }
}
