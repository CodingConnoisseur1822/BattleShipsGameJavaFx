package network;

import database.UserDao;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * GameServer is the central multiplayer server that listens for incoming player
 * connections and spawns a dedicated thread for each client.
 * It also manages the active game rooms (creating new lobbies, pairing players
 * by room code, sharing open rooms, and cleaning up empty ones) 
 * while providing shared access to the database.
 */
public class GameServer {

    private static final Map<String, GameRoom> rooms = new HashMap<String, GameRoom>();
    private static final UserDao userDao = new UserDao();

    public static void main(String[] args) {
        System.out.println("Server running on port " + ServerConfig.PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(ServerConfig.PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                try {
                    Thread clientThread = new Thread(new ServerClientHandler(clientSocket));
                    clientThread.start();
                } catch (IOException e) {
                    System.err.println("Could not start client handler: " + e.getMessage());
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static UserDao getUserDao() {
        return userDao;
    }

    public static synchronized String getActiveRoomsList() {
        StringBuilder codes = new StringBuilder();
        for (GameRoom room : rooms.values()) {
            if (!room.isFull()) {
                if (codes.length() > 0) {
                    codes.append(",");
                }
                codes.append(room.getRoomCode());
            }
        }
        return NetworkCommand.ROOMS.format(codes.toString());
    }

    public static synchronized boolean joinOrCreateRoom(String roomCode, ServerClientHandler handler) {
        GameRoom room = rooms.get(roomCode);
        if (room == null) {
            room = new GameRoom(roomCode);
            rooms.put(roomCode, room);
        }

        boolean joined = room.addPlayer(handler);
        if (joined) {
            handler.setAssignedRoom(room);
        }
        return joined;
    }

    public static synchronized void removeRoomIfEmpty(GameRoom room) {
        if (room != null && room.isEmpty()) {
            rooms.remove(room.getRoomCode());
        }
    }
}
