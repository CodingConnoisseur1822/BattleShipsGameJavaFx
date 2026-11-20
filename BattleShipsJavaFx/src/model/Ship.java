package model;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import util.Load;

/**
 * Ship represents an individual vessel in the game, tracking its type,
 * position on the screen, orientation, remaining health, and whether
 * it has been placed on the board. It also handles calculating which
 * grid cells the ship occupies, detecting mouse clicks within its bounds,
 * and drawing itself horizontally or vertically onto the canvas.
 */
public class Ship {

    private final ShipType type;
    private final Image image;
    private double currentX;
    private double currentY;
    private int life;
    private boolean rotated;
    private boolean placedOnBoard;

    public Ship(ShipType type) {
        this.type = type;
        this.image = Load.loadImage(type.getImageName());
        this.life = type.getSize();
        returnToStartPosition();
    }

    public void toggleOrientation() {
        rotated = !rotated;
    }

    public void setRotated(boolean rotated) {
        this.rotated = rotated;
    }

    public void returnToStartPosition() {
        this.rotated = false;
        this.placedOnBoard = false;
        this.currentX = type.getStartX();
        this.currentY = type.getStartY();
    }

    public void placeAt(Board board, int col, int row) {
        this.currentX = board.gridToPixelX(col);
        this.currentY = board.gridToPixelY(row);
        this.placedOnBoard = true;
    }

    public boolean containsPoint(double pointX, double pointY) {
        double width = Board.TILE_SIZE;
        double height = Board.TILE_SIZE;

        if (rotated) {
            height = type.getSize() * Board.TILE_SIZE;
        } else {
            width = type.getSize() * Board.TILE_SIZE;
        }

        return pointX >= currentX && pointX < currentX + width
                && pointY >= currentY && pointY < currentY + height;
    }

    public List<GridPoint> occupiedCells(int anchorCol, int anchorRow) {
        List<GridPoint> cells = new ArrayList<GridPoint>();

        for (int i = 0; i < type.getSize(); i++) {
            if (rotated) {
                cells.add(new GridPoint(anchorCol, anchorRow + i));
            } else {
                cells.add(new GridPoint(anchorCol + i, anchorRow));
            }
        }
        return cells;
    }

    public List<GridPoint> cellsOnBoard(Board board) {
        return occupiedCells(board.nearestCol(currentX), board.nearestRow(currentY));
    }

    public void draw(GraphicsContext gc) {
        if (image == null) {
            return;
        }

        gc.save();
        if (rotated) {
            gc.translate(currentX, currentY);
            gc.rotate(-90);
            gc.drawImage(image, -image.getWidth(), 0);
        } else {
            gc.drawImage(image, currentX, currentY);
        }
        gc.restore();
    }

    public void reduceLife() {
        life--;
    }

    public int getLife() {
        return life;
    }

    public ShipType getType() {
        return type;
    }

    public double getCurrentX() {
        return currentX;
    }

    public void setCurrentX(double currentX) {
        this.currentX = currentX;
    }

    public double getCurrentY() {
        return currentY;
    }

    public void setCurrentY(double currentY) {
        this.currentY = currentY;
    }

    public boolean isRotated() {
        return rotated;
    }

    public boolean isPlacedOnBoard() {
        return placedOnBoard;
    }
}
