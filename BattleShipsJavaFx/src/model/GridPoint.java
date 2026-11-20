package model;

public class GridPoint {

    private final int col;
    private final int row;

    public GridPoint(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public boolean isSameCell(GridPoint other) {
        return col == other.col && row == other.row;
    }
}
