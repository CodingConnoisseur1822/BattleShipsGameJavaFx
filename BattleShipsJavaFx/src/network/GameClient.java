package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javafx.application.Platform;
import model.GameState;

/**
 * GameClient manages the client's network connection to the game server.
 * It opens a socket on a background thread to join a room with the player's
 * credentials, sends gameplay messages to the server, and safely passes
 * incoming updates, errors, and match events back onto the JavaFX UI thread.
 */
public class GameClient {

    private static final String CONNECTION_ERROR = "Connection error to server!";

    public interface NetworkListener {
        void onStatus(String message);

        void onError(String message);

        void onGameStart(int playerId, String opponentName);
    }

    private final NetworkListener listener;
    private volatile Socket socket;
    private volatile PrintWriter writer;
    private volatile boolean closedByUser;
    private volatile boolean errorReceived;

    public GameClient(NetworkListener listener) {
        this.listener = listener;
        this.closedByUser = false;
        this.errorReceived = false;
    }

    public void connectToRoom(final String roomCode, final String username, final String password) {
        closedByUser = false;
        errorReceived = false;

        Thread networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(ServerConfig.HOST, ServerConfig.PORT);
                    writer = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    writer.println(NetworkCommand.JOIN.format(roomCode + ":" + username + ":" + password));

                    String serverMessage = reader.readLine();
                    while (serverMessage != null) {
                        dispatchServerMessage(serverMessage);
                        serverMessage = reader.readLine();
                    }
                } catch (IOException e) {
                    System.err.println("Connection problem: " + e.getMessage());
                }

                closeSocket();
                if (!closedByUser && !errorReceived) {
                    postConnectionLost();
                }
            }
        });
        networkThread.setDaemon(true);
        networkThread.start();
    }

    public void sendMessage(String message) {
        PrintWriter currentWriter = writer;
        if (currentWriter != null) {
            currentWriter.println(message);
        }
    }

    public void disconnect() {
        closedByUser = true;
        closeSocket();
    }

    private void closeSocket() {
        Socket currentSocket = socket;
        if (currentSocket == null) {
            return;
        }
        try {
            currentSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dispatchServerMessage(String message) {
        NetworkCommand command = NetworkCommand.fromMessage(message);
        String payload = NetworkCommand.getPayload(message);

        switch (command) {
            case CONNECTED:
                postStatus(payload);
                break;
            case ERROR:
                errorReceived = true;
                postError(payload);
                break;
            case START:
                handleStart(payload);
                break;
            default:
                postGameMessage(message);
                break;
        }
    }

    private void handleStart(String payload) {
        String[] parts = payload.split(":", 2);
        if (parts.length < 2) {
            System.err.println("Malformed start message: " + payload);
            return;
        }
        try {
            int assignedId = Integer.parseInt(parts[0]);
            postGameStart(assignedId, parts[1]);
        } catch (NumberFormatException e) {
            System.err.println("Malformed start message: " + payload);
        }
    }

    private void postStatus(final String status) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                listener.onStatus(status);
            }
        });
    }

    private void postError(final String error) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                listener.onError(error);
            }
        });
    }

    private void postGameStart(final int assignedId, final String opponentName) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                listener.onGameStart(assignedId, opponentName);
            }
        });
    }

    private void postGameMessage(final String message) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                GameState.getInstance().processNetworkMessage(message);
            }
        });
    }

    private void postConnectionLost() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                listener.onError(CONNECTION_ERROR);
                GameState.getInstance().handleConnectionLost();
            }
        });
    }
}
