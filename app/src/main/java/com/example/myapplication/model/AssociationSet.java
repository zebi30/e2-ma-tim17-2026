package com.example.myapplication.model;

public class AssociationSet {
    public final long id;
    /** [kolona][red] - 4x4 skrivenih polja. */
    public final String[][] cells;
    public final String[] columnSolutions;
    public final String finalSolution;

    public AssociationSet(long id, String[][] cells, String[] columnSolutions, String finalSolution) {
        this.id = id;
        this.cells = cells;
        this.columnSolutions = columnSolutions;
        this.finalSolution = finalSolution;
    }
}
