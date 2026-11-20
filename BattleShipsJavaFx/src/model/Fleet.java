package model;

import java.util.List;
import javafx.scene.canvas.GraphicsContext;

/**
 * Fleet is a utility class that manages shared operations
 * across a collection of ships. It handles finding which
 * ship sits at a given pixel position, checking whether a ship
 * can be placed without going off the board or overlapping
 * other ships, seeing if an entire fleet has been sunk,
 * and drawing all the ships onto the canvas.
 */
final class Fleet {

    private Fleet() {
    }

    static Ship shipAt(List<Ship> ships, double pixelX, double pixelY) {
        for (Ship ship : ships) {
            if (ship.containsPoint(pixelX, pixelY)) {
                return ship;
            }
        }
        return null;
    }

    static boolean allSunk(List<Ship> ships) {
        for (Ship ship : ships) {
            if (ship.getLife() > 0) {
                return false;
            }
        }
        return true;
    }

    static boolean canPlace(List<Ship> ships, Ship ship, Board board, int col, int row) {
        List<GridPoint> newCells = ship.occupiedCells(col, row);

        for (GridPoint cell : newCells) {
            if (!Board.isInside(cell.getCol(), cell.getRow())) {
                return false;
            }
        }

        for (Ship other : ships) {
            if (other != ship && other.isPlacedOnBoard()) {
                if (cellsOverlap(newCells, other.cellsOnBoard(board))) {
                    return false;
                }
            }
        }
        return true;
    }

    static void drawAll(List<Ship> ships, GraphicsContext gc) {
        for (Ship ship : ships) {
            ship.draw(gc);
        }
    }

    private static boolean cellsOverlap(List<GridPoint> first, List<GridPoint> second) {
        for (GridPoint firstCell : first) {
            for (GridPoint secondCell : second) {
                if (firstCell.isSameCell(secondCell)) {
                    return true;
                }
            }
        }
        return false;
    }
}
