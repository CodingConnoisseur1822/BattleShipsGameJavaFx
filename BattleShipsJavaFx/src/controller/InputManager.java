package controller;

import java.util.List;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import model.Board;
import model.GameState;
import model.Ship;

/**
 * InputManager takes care of the player's mouse and keyboard controls.
 * Before the match begins, it lets you drag your ships onto the grid
 * and rotate them with a right-click or the R key; once the game is
 * underway, clicking on the enemy grid fires an attack at that tile.
 */
public class InputManager {

    private static final double HALF_TILE = Board.TILE_SIZE / 2.0;

    private final GameState gameState;
    private Ship selectedShip;
    private double grabOffsetX;
    private double grabOffsetY;

    public InputManager(GameState gameState) {
        this.gameState = gameState;
    }

    public void registerInputListeners(Canvas canvas, Scene scene) {
        canvas.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                processMousePress(event);
            }
        });

        canvas.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                processMouseDrag(event);
            }
        });

        canvas.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                processMouseRelease(event);
            }
        });

        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                processKeyPress(event);
            }
        });
    }

    private void processMousePress(MouseEvent event) {
        if (gameState.canPlaceShips()) {
            if (event.getButton() == MouseButton.SECONDARY) {
                rotateSelectedShip();
            } else if (event.getButton() == MouseButton.PRIMARY) {
                pickUpShip(event.getX(), event.getY());
            }
            return;
        }

        if (gameState.isGameStarted() && event.getButton() == MouseButton.PRIMARY) {
            attackAt(event.getX(), event.getY());
        }
    }

    private void pickUpShip(double mouseX, double mouseY) {
        List<Ship> ships = gameState.getShips();

        for (int i = ships.size() - 1; i >= 0; i--) {
            Ship ship = ships.get(i);
            if (ship.containsPoint(mouseX, mouseY)) {
                selectedShip = ship;
                grabOffsetX = mouseX - ship.getCurrentX();
                grabOffsetY = mouseY - ship.getCurrentY();
                return;
            }
        }
    }

    private void processMouseDrag(MouseEvent event) {
        if (selectedShip == null) {
            return;
        }

        if (!gameState.canPlaceShips()) {
            dropSelectedShipAtStart();
            return;
        }

        if (event.isPrimaryButtonDown()) {
            selectedShip.setCurrentX(event.getX() - grabOffsetX);
            selectedShip.setCurrentY(event.getY() - grabOffsetY);
        }
    }

    private void processMouseRelease(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || selectedShip == null) {
            return;
        }

        if (!gameState.canPlaceShips()) {
            dropSelectedShipAtStart();
            return;
        }

        Board board = gameState.getPlayerBoard();
        int col = board.colAtPixel(selectedShip.getCurrentX() + HALF_TILE);
        int row = board.rowAtPixel(selectedShip.getCurrentY() + HALF_TILE);

        if (!gameState.tryPlaceShip(selectedShip, col, row)) {
            selectedShip.returnToStartPosition();
        }
        selectedShip = null;
    }

    private void dropSelectedShipAtStart() {
        selectedShip.returnToStartPosition();
        selectedShip = null;
    }

    private void processKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.R && gameState.canPlaceShips()) {
            rotateSelectedShip();
        }
    }

    private void rotateSelectedShip() {
        if (selectedShip == null) {
            return;
        }
        selectedShip.toggleOrientation();
        grabOffsetX = HALF_TILE;
        grabOffsetY = HALF_TILE;
    }

    private void attackAt(double clickX, double clickY) {
        Board enemyBoard = gameState.getEnemyBoard();
        int col = enemyBoard.colAtPixel(clickX);
        int row = enemyBoard.rowAtPixel(clickY);

        if (Board.isInside(col, row)) {
            gameState.attackEnemyTile(col, row);
        }
    }
}
