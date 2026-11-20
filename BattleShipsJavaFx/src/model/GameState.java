package model;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import application.Main;
import network.GameClient;
import network.NetworkCommand;
import util.Load;
import util.SceneFactory;

/**
 * GameState is the central brain of a Battleship match.
 * It keeps track of both boards, ship placements, turn order,
 * and whether the game is in setup, active play, or finished,
 * while coordinating single-player AI turns or online multiplayer
 * network messages and rendering everything onto the screen.
 */
public class GameState {

    private static final double PLAYER_BOARD_X = 20.0;
    private static final double ENEMY_BOARD_X = 600.0;
    private static final double BOARD_Y = 20.0;
    private static final double USERNAME_LABEL_OFFSET_Y = 565.0;
    private static final int ENEMY_DELAY_FRAMES = 15;
    private static final String BOARD_IMAGE = "Board.png";
    private static final String DEFAULT_PLAYER_NAME = "Player";
    private static final String DEFAULT_OPPONENT_NAME = "Opponent";
    private static final String VICTORY_HEADER = "Victory!";
    private static final String DEFEAT_HEADER = "Defeat";

    private static GameState instance;

    private final Board playerBoard;
    private final Board enemyBoard;
    private final Enemy enemy = new Enemy();
    private final List<Ship> playerShips = new ArrayList<Ship>();
    private final List<Ship> revealedEnemyShips = new ArrayList<Ship>();
    private final List<HitMarker> playerBoardMarkers = new ArrayList<HitMarker>();
    private final List<HitMarker> enemyBoardMarkers = new ArrayList<HitMarker>();
    private final boolean[][] attackedEnemyCells = new boolean[Board.SIZE][Board.SIZE];

    private GamePhase phase;
    private boolean isPlayerTurn;
    private boolean isMultiplayer;
    private boolean opponentReady;
    private int enemyThinkFrames;

    private int localPlayerId;
    private String localUsername;
    private String opponentUsername;
    private GameClient gameClient;
    private Runnable onReturnToMenu;

    public GameState() {
        instance = this;
        playerBoard = new Board(Load.loadImage(BOARD_IMAGE), PLAYER_BOARD_X, BOARD_Y);
        enemyBoard = new Board(Load.loadImage(BOARD_IMAGE), ENEMY_BOARD_X, BOARD_Y);
        resetGameSession();
    }

    public static GameState getInstance() {
        return instance;
    }

    public void setOnReturnToMenu(Runnable onReturnToMenu) {
        this.onReturnToMenu = onReturnToMenu;
    }

    public void resetGameSession() {
        phase = GamePhase.PLACING_SHIPS;
        isPlayerTurn = true;
        isMultiplayer = false;
        opponentReady = false;
        enemyThinkFrames = 0;
        localPlayerId = 1;
        localUsername = DEFAULT_PLAYER_NAME;
        opponentUsername = DEFAULT_OPPONENT_NAME;
        gameClient = null;

        playerBoardMarkers.clear();
        enemyBoardMarkers.clear();
        revealedEnemyShips.clear();
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                attackedEnemyCells[row][col] = false;
            }
        }

        playerShips.clear();
        for (ShipType type : ShipType.values()) {
            playerShips.add(new Ship(type));
        }
        enemy.reset(enemyBoard);

        Main.updateStatusMessage("Place your ships to start!");
    }

    public void enableMultiplayerMode(int assignedPlayerId, String username, String opponentName, GameClient client) {
        isMultiplayer = true;
        phase = GamePhase.PLACING_SHIPS;
        opponentReady = false;
        localPlayerId = assignedPlayerId;
        localUsername = username;
        opponentUsername = opponentName;
        gameClient = client;
        isPlayerTurn = (assignedPlayerId == 1);
    }

    public boolean tryPlaceShip(Ship ship, int col, int row) {
        if (phase != GamePhase.PLACING_SHIPS) {
            return false;
        }
        if (!Fleet.canPlace(playerShips, ship, playerBoard, col, row)) {
            return false;
        }
        ship.placeAt(playerBoard, col, row);
        return true;
    }

    public boolean resetShipPlacement() {
        if (phase != GamePhase.PLACING_SHIPS) {
            return false;
        }
        for (Ship ship : playerShips) {
            ship.returnToStartPosition();
        }
        Main.updateStatusMessage("Ships reset. Place your ships!");
        return true;
    }

    public boolean attemptStartGame() {
        for (Ship ship : playerShips) {
            if (!ship.isPlacedOnBoard()) {
                return false;
            }
        }

        if (phase != GamePhase.PLACING_SHIPS) {
            return true;
        }

        if (isMultiplayer) {
            phase = GamePhase.WAITING_FOR_OPPONENT;
            gameClient.sendMessage(NetworkCommand.READY.getPrefix());
            if (opponentReady) {
                startMultiplayerMatch();
            } else {
                Main.updateStatusMessage("Waiting for " + opponentUsername + " to place ships & ready up...");
            }
        } else {
            phase = GamePhase.PLAYING;
            isPlayerTurn = true;
            Main.updateStatusMessage("Game Started! Your Turn");
        }
        return true;
    }

    private void startMultiplayerMatch() {
        phase = GamePhase.PLAYING;
        isPlayerTurn = (localPlayerId == 1);
        if (isPlayerTurn) {
            Main.updateStatusMessage("Both ready! " + localUsername + "'s Turn");
        } else {
            Main.updateStatusMessage("Both ready! " + opponentUsername + "'s Turn");
        }
    }

    public void attackEnemyTile(int col, int row) {
        if (phase != GamePhase.PLAYING) {
            return;
        }
        if (!isPlayerTurn) {
            Main.updateStatusMessage("Wait for opponent's turn!");
            return;
        }
        if (attackedEnemyCells[row][col]) {
            Main.updateStatusMessage("You already attacked this tile!");
            return;
        }

        if (isMultiplayer) {
            isPlayerTurn = false;
            gameClient.sendMessage(NetworkCommand.ATTACK.format(col + "," + row));
            Main.updateStatusMessage("Attack sent! " + opponentUsername + "'s Turn");
        } else {
            resolveSinglePlayerAttack(col, row);
        }
    }

    private void resolveSinglePlayerAttack(int col, int row) {
        Ship hitShip = enemy.findShipAt(enemyBoard, col, row);
        boolean hit = hitShip != null;

        if (hit) {
            hitShip.reduceLife();
        }
        addEnemyMarker(col, row, hit);
        isPlayerTurn = false;

        if (!hit) {
            Main.updateStatusMessage("Miss! Enemy's Turn");
        } else if (hitShip.getLife() == 0) {
            Main.updateStatusMessage("You sunk the enemy's " + hitShip.getType().getName() + "!");
        } else {
            Main.updateStatusMessage("Hit! Enemy's Turn");
        }
    }

    public void updateGameState() {
        if (phase != GamePhase.PLAYING || isMultiplayer) {
            return;
        }

        if (enemy.isAllShipsSunk()) {
            handleMatchEnded(VICTORY_HEADER, "Congratulations! You destroyed the entire enemy fleet!");
            return;
        }

        if (isPlayerTurn) {
            enemyThinkFrames = 0;
            return;
        }

        enemyThinkFrames++;
        if (enemyThinkFrames >= ENEMY_DELAY_FRAMES) {
            enemyThinkFrames = 0;
            runEnemyTurn();
        }
    }

    private void runEnemyTurn() {
        GridPoint target = enemy.pickNextTarget();
        Ship hitShip = receiveAttackOnPlayerBoard(target.getCol(), target.getRow());

        if (areAllPlayerShipsSunk()) {
            handleMatchEnded(DEFEAT_HEADER, "Game Over: All of your ships have been sunk!");
            return;
        }

        isPlayerTurn = true;
        if (hitShip != null) {
            Main.updateStatusMessage("Enemy hit your " + hitShip.getType().getName() + "! Your Turn");
        } else {
            Main.updateStatusMessage("Enemy missed! Your Turn");
        }
    }

    private Ship receiveAttackOnPlayerBoard(int col, int row) {
        Ship hitShip = Fleet.shipAt(playerShips, playerBoard.tileCenterX(col), playerBoard.tileCenterY(row));
        boolean hit = hitShip != null;

        if (hit) {
            hitShip.reduceLife();
        }
        addPlayerMarker(col, row, hit);
        return hitShip;
    }

    public void processNetworkMessage(String message) {
        NetworkCommand command = NetworkCommand.fromMessage(message);

        try {
            switch (command) {
                case READY:
                    handleOpponentReady();
                    break;
                case DISCONNECTED:
                    handleOpponentDisconnected();
                    break;
                case ATTACK:
                    handleIncomingAttack(message);
                    break;
                case ATTACK_RESULT:
                    handleAttackResult(message);
                    break;
                case SHIP_SUNK:
                    handleEnemyShipSunk(message);
                    break;
                case GAME_OVER:
                    handleOpponentGameOver();
                    break;
                default:
                    System.err.println("Unknown network command received: " + message);
                    break;
            }
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Malformed network message: " + message);
        }
    }

    public void handleConnectionLost() {
        if (isMultiplayer) {
            handleMatchEnded("Connection Lost", "The connection to the server was lost.");
        }
    }

    private void handleOpponentReady() {
        if (phase == GamePhase.PLACING_SHIPS) {
            opponentReady = true;
            Main.updateStatusMessage(opponentUsername + " is ready! Place your ships and click Start.");
        } else if (phase == GamePhase.WAITING_FOR_OPPONENT) {
            opponentReady = true;
            startMultiplayerMatch();
        }
    }

    private void handleOpponentDisconnected() {
        if (isMultiplayer) {
            handleMatchEnded(VICTORY_HEADER, opponentUsername + " has left the match. You win by forfeiture!");
        }
    }

    private void handleIncomingAttack(String message) {
        if (phase != GamePhase.PLAYING) {
            return;
        }
        GridPoint cell = parseCell(NetworkCommand.getPayload(message));
        if (cell == null) {
            return;
        }

        Ship hitShip = receiveAttackOnPlayerBoard(cell.getCol(), cell.getRow());
        boolean hit = hitShip != null;

        gameClient.sendMessage(NetworkCommand.ATTACK_RESULT.format(cell.getCol() + "," + cell.getRow() + ":" + hit));

        if (hit && hitShip.getLife() == 0) {
            sendShipSunk(hitShip);
        }

        if (areAllPlayerShipsSunk()) {
            gameClient.sendMessage(NetworkCommand.GAME_OVER.getPrefix());
            handleMatchEnded(DEFEAT_HEADER, "All of your ships were sunk by " + opponentUsername + ".");
        } else {
            isPlayerTurn = true;
            Main.updateStatusMessage(opponentUsername + " attacked! " + localUsername + "'s Turn");
        }
    }

    private void sendShipSunk(Ship ship) {
        int shipCol = playerBoard.nearestCol(ship.getCurrentX());
        int shipRow = playerBoard.nearestRow(ship.getCurrentY());
        String payload = ship.getType().name() + ":" + shipCol + "," + shipRow + ":" + ship.isRotated();
        gameClient.sendMessage(NetworkCommand.SHIP_SUNK.format(payload));
    }

    private void handleAttackResult(String message) {
        if (phase != GamePhase.PLAYING) {
            return;
        }
        String[] parts = NetworkCommand.getPayload(message).split(":");
        GridPoint cell = parseCell(parts[0]);
        if (cell == null) {
            return;
        }
        boolean hit = Boolean.parseBoolean(parts[1].trim());

        addEnemyMarker(cell.getCol(), cell.getRow(), hit);
        isPlayerTurn = false;

        if (hit) {
            Main.updateStatusMessage("Hit! " + opponentUsername + "'s Turn");
        } else {
            Main.updateStatusMessage("Miss! " + opponentUsername + "'s Turn");
        }
    }

    private void handleEnemyShipSunk(String message) {
        if (phase != GamePhase.PLAYING) {
            return;
        }
        String[] parts = NetworkCommand.getPayload(message).split(":");
        ShipType type = ShipType.valueOf(parts[0].trim());
        GridPoint cell = parseCell(parts[1]);
        if (cell == null) {
            return;
        }

        Ship sunkShip = new Ship(type);
        sunkShip.setRotated(Boolean.parseBoolean(parts[2].trim()));
        sunkShip.placeAt(enemyBoard, cell.getCol(), cell.getRow());
        revealedEnemyShips.add(sunkShip);

        Main.updateStatusMessage("You sunk " + opponentUsername + "'s " + type.getName() + "!");
    }

    private void handleOpponentGameOver() {
        if (phase == GamePhase.PLAYING) {
            handleMatchEnded(VICTORY_HEADER, "Congratulations! You destroyed all of " + opponentUsername + "'s ships!");
        }
    }

    private GridPoint parseCell(String text) {
        String[] parts = text.split(",");
        int col = Integer.parseInt(parts[0].trim());
        int row = Integer.parseInt(parts[1].trim());
        if (!Board.isInside(col, row)) {
            return null;
        }
        return new GridPoint(col, row);
    }

    private boolean areAllPlayerShipsSunk() {
        return Fleet.allSunk(playerShips);
    }

    private void addPlayerMarker(int col, int row, boolean hit) {
        playerBoardMarkers.add(new HitMarker(
                MarkerType.fromResult(hit),
                playerBoard.gridToPixelX(col),
                playerBoard.gridToPixelY(row)));
    }

    private void addEnemyMarker(int col, int row, boolean hit) {
        attackedEnemyCells[row][col] = true;
        enemyBoardMarkers.add(new HitMarker(
                MarkerType.fromResult(hit),
                enemyBoard.gridToPixelX(col),
                enemyBoard.gridToPixelY(row)));
    }

    private void handleMatchEnded(final String header, final String content) {
        if (phase == GamePhase.GAME_OVER) {
            return;
        }
        phase = GamePhase.GAME_OVER;
        Main.updateStatusMessage(header);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Battleship");
                alert.setHeaderText(header);
                alert.setContentText(content);
                SceneFactory.styleAlert(alert);
                alert.showAndWait();

                if (onReturnToMenu != null) {
                    onReturnToMenu.run();
                }
            }
        });
    }

    public void renderAllGameObjects(GraphicsContext gc) {
        playerBoard.draw(gc);
        enemyBoard.draw(gc);

        if (isMultiplayer) {
            drawMultiplayerUsernames(gc);
        }

        Fleet.drawAll(playerShips, gc);

        if (isMultiplayer) {
            Fleet.drawAll(revealedEnemyShips, gc);
        } else {
            enemy.drawSunkShips(gc);
        }

        for (HitMarker marker : playerBoardMarkers) {
            marker.draw(gc);
        }
        for (HitMarker marker : enemyBoardMarkers) {
            marker.draw(gc);
        }
    }

    private void drawMultiplayerUsernames(GraphicsContext gc) {
        double labelY = playerBoard.getOriginY() + USERNAME_LABEL_OFFSET_Y;

        gc.save();
        gc.setFont(Font.font("System", FontWeight.BOLD, 16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.TOP);

        gc.setFill(Color.web("#7dd3fc"));
        gc.fillText(localUsername + " (You)", playerBoard.getOriginX() + (Board.PIXEL_SIZE / 2.0), labelY);

        gc.setFill(Color.web("#f87171"));
        gc.fillText(opponentUsername + " (Enemy)", enemyBoard.getOriginX() + (Board.PIXEL_SIZE / 2.0), labelY);

        gc.restore();
    }

    public Board getPlayerBoard() {
        return playerBoard;
    }

    public Board getEnemyBoard() {
        return enemyBoard;
    }

    public List<Ship> getShips() {
        return playerShips;
    }

    public boolean canPlaceShips() {
        return phase == GamePhase.PLACING_SHIPS;
    }

    public boolean isGameStarted() {
        return phase == GamePhase.PLAYING;
    }

    public boolean isMultiplayer() {
        return isMultiplayer;
    }

    public GameClient getGameClient() {
        return gameClient;
    }
}
