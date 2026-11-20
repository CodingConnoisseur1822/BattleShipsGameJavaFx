package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javafx.scene.canvas.GraphicsContext;

/**
 * Enemy controls the computer opponent's fleet and firing
 * logic. At the start of a match, it randomly positions
 * all its ships on the board and shuffles a list of every
 * grid square so it can pick unique targets each turn,
 * while also checking for incoming hits and revealing its
 * ships once they are sunk.
 */
public class Enemy {

    private final Random random = new Random();
    private final List<Ship> ships = new ArrayList<Ship>();
    private final List<GridPoint> remainingTargets = new ArrayList<GridPoint>();

    public void reset(Board board) {
        ships.clear();
        remainingTargets.clear();

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                remainingTargets.add(new GridPoint(col, row));
            }
        }
        Collections.shuffle(remainingTargets, random);

        placeShipsRandomly(board);
    }

    private void placeShipsRandomly(Board board) {
        for (ShipType type : ShipType.values()) {
            Ship ship = new Ship(type);
            boolean placed = false;

            while (!placed) {
                ship.setRotated(random.nextBoolean());
                int col = random.nextInt(Board.SIZE);
                int row = random.nextInt(Board.SIZE);

                if (Fleet.canPlace(ships, ship, board, col, row)) {
                    ship.placeAt(board, col, row);
                    ships.add(ship);
                    placed = true;
                }
            }
        }
    }

    public GridPoint pickNextTarget() {
        return remainingTargets.remove(remainingTargets.size() - 1);
    }

    public Ship findShipAt(Board board, int col, int row) {
        return Fleet.shipAt(ships, board.tileCenterX(col), board.tileCenterY(row));
    }

    public boolean isAllShipsSunk() {
        return Fleet.allSunk(ships);
    }

    public void drawSunkShips(GraphicsContext gc) {
        for (Ship ship : ships) {
            if (ship.getLife() == 0) {
                ship.draw(gc);
            }
        }
    }
}
