package network;

import database.AuthResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import model.PlayerScore;

/**
 * ServerClientHandler manages a single player's connection
 * on the server side. It processes one-off requests like 
 * logging in, registering, fetching open rooms, or loading
 * the scoreboard, and once a player joins a match, it keeps
 * the connection open to relay moves and game events back
 * and forth with their opponent.
 */
public class ServerClientHandler implements Runnable {

    private static final String MESSAGE_INVALID_REQUEST = "Invalid request.";
    private static final String MESSAGE_ROOM_UNAVAILABLE = "Room is full or unavailable.";

    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private volatile ServerClientHandler opponentHandler;
    private String username;
    private GameRoom assignedRoom;

    public ServerClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.opponentHandler = null;
        this.username = "";
        this.assignedRoom = null;
    }

    @Override
    public void run() {
        try {
            String firstMessage = reader.readLine();
            if (firstMessage == null) {
                return;
            }
            boolean stayConnected = handleInitialCommand(firstMessage);
            if (stayConnected) {
                relayMessagesToOpponent();
            }
        } catch (IOException e) {
            System.out.println("User " + username + " disconnected.");
        } finally {
            cleanUp();
        }
    }

    public void sendMessage(String message) {
        writer.println(message);
    }

    public void setOpponentHandler(ServerClientHandler opponent) {
        this.opponentHandler = opponent;
    }

    public void setAssignedRoom(GameRoom room) {
        this.assignedRoom = room;
    }

    public String getUsername() {
        return username;
    }

    private boolean handleInitialCommand(String message) {
        NetworkCommand command = NetworkCommand.fromMessage(message);

        switch (command) {
            case GET_ROOMS:
                sendMessage(GameServer.getActiveRoomsList());
                return false;
            case SCOREBOARD:
                sendScoreboard();
                return false;
            case REGISTER:
                handleRegister(message);
                return false;
            case LOGIN:
                handleLogin(message);
                return false;
            case JOIN:
                return handleJoin(message);
            default:
                sendMessage(NetworkCommand.ERROR.format(MESSAGE_INVALID_REQUEST));
                return false;
        }
    }

    private void handleRegister(String message) {
        String[] parts = message.split(":", 3);
        if (parts.length < 3) {
            sendMessage(NetworkCommand.ERROR.format(MESSAGE_INVALID_REQUEST));
            return;
        }
        String name = parts[1].trim();
        AuthResult result = GameServer.getUserDao().register(name, parts[2]);
        replyToAuth(result, NetworkCommand.REGISTER_OK, name);
    }

    private void handleLogin(String message) {
        String[] parts = message.split(":", 3);
        if (parts.length < 3) {
            sendMessage(NetworkCommand.ERROR.format(MESSAGE_INVALID_REQUEST));
            return;
        }
        String name = parts[1].trim();
        AuthResult result = GameServer.getUserDao().authenticate(name, parts[2]);
        replyToAuth(result, NetworkCommand.LOGIN_OK, name);
    }

    private void replyToAuth(AuthResult result, NetworkCommand successCommand, String name) {
        if (result == AuthResult.SUCCESS) {
            sendMessage(successCommand.format(name));
        } else {
            sendMessage(NetworkCommand.ERROR.format(result.getMessage()));
        }
    }

    private boolean handleJoin(String message) {
        String[] parts = message.split(":", 4);
        if (parts.length < 4) {
            sendMessage(NetworkCommand.ERROR.format(MESSAGE_INVALID_REQUEST));
            return false;
        }

        String roomCode = parts[1].trim().toUpperCase();
        String enteredUsername = parts[2].trim();
        String enteredPassword = parts[3];

        AuthResult result = GameServer.getUserDao().authenticate(enteredUsername, enteredPassword);
        if (result != AuthResult.SUCCESS) {
            sendMessage(NetworkCommand.ERROR.format(result.getMessage()));
            return false;
        }

        this.username = enteredUsername;
        boolean joined = GameServer.joinOrCreateRoom(roomCode, this);
        if (!joined) {
            sendMessage(NetworkCommand.ERROR.format(MESSAGE_ROOM_UNAVAILABLE));
        }
        return joined;
    }

    private void sendScoreboard() {
        List<PlayerScore> scores = GameServer.getUserDao().getScoreboard();
        StringBuilder payload = new StringBuilder();
        for (int i = 0; i < scores.size(); i++) {
            if (i > 0) {
                payload.append(";");
            }
            payload.append(scores.get(i).toNetworkString());
        }
        sendMessage(NetworkCommand.SCOREBOARD.format(payload.toString()));
    }

    private void relayMessagesToOpponent() throws IOException {
        String packet = reader.readLine();
        while (packet != null) {
            NetworkCommand command = NetworkCommand.fromMessage(packet);

            switch (command) {
                case GAME_OVER:
                    if (assignedRoom != null) {
                        assignedRoom.handleGameOver(this);
                    }
                    forwardToOpponent(packet);
                    break;
                case READY:
                case ATTACK:
                case ATTACK_RESULT:
                case SHIP_SUNK:
                    forwardToOpponent(packet);
                    break;
                default:
                    break;
            }

            packet = reader.readLine();
        }
    }

    private void forwardToOpponent(String packet) {
        ServerClientHandler opponent = opponentHandler;
        if (opponent != null) {
            opponent.sendMessage(packet);
        }
    }

    private void cleanUp() {
        if (assignedRoom != null) {
            assignedRoom.removePlayer(this);
            GameServer.removeRoomIfEmpty(assignedRoom);
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
