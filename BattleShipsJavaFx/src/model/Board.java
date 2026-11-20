package model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Board defines a 10x10 game grid and handles converting
 * between grid coordinates (rows and columns) and exact
 * pixel positions on the screen. It also checks whether
 * a coordinate falls inside the playable grid and draws
 * the board's background image onto the canvas.
 */
public class Board {

    public static final int SIZE = 10;
    public static final double TILE_SIZE = 50.0;
    public static final double PIXEL_SIZE = TILE_SIZE * (SIZE + 1);

    private final Image image;
    private final double originX;
    private final double originY;

    public Board(Image image, double originX, double originY) {
        this.image = image;
        this.originX = originX;
        this.originY = originY;
    }

    public double gridToPixelX(int col) {
        return originX + TILE_SIZE + (col * TILE_SIZE);
    }

    public double gridToPixelY(int row) {
        return originY + TILE_SIZE + (row * TILE_SIZE);
    }

    public double tileCenterX(int col) {
        return gridToPixelX(col) + (TILE_SIZE / 2.0);
    }

    public double tileCenterY(int row) {
        return gridToPixelY(row) + (TILE_SIZE / 2.0);
    }

    public int colAtPixel(double pixelX) {
        return (int) Math.floor((pixelX - originX - TILE_SIZE) / TILE_SIZE);
    }

    public int rowAtPixel(double pixelY) {
        return (int) Math.floor((pixelY - originY - TILE_SIZE) / TILE_SIZE);
    }

    public int nearestCol(double pixelX) {
        return (int) Math.round((pixelX - originX - TILE_SIZE) / TILE_SIZE);
    }

    public int nearestRow(double pixelY) {
        return (int) Math.round((pixelY - originY - TILE_SIZE) / TILE_SIZE);
    }

    public static boolean isInside(int col, int row) {
        return col >= 0 && col < SIZE && row >= 0 && row < SIZE;
    }

    public void draw(GraphicsContext gc) {
        if (image != null) {
            gc.drawImage(image, originX, originY);
        }
    }

    public double getOriginX() {
        return originX;
    }

    public double getOriginY() {
        return originY;
    }
}