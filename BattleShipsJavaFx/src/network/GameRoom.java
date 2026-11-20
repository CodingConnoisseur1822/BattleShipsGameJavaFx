package network;

/**
 * GameRoom pairs two players together in a multiplayer lobby
 * using a shared room code. Once both players join, it links their
 * connections and kicks off the match, and when the game ends
 * (or if one player disconnects early) it notifies the opponent and
 * saves the win and loss to the database.
 */
public class GameRoom {

    private final String roomCode;
    private ServerClientHandler player1;
    private ServerClientHandler player2;
    private boolean full;
    private boolean matchFinished;

    public GameRoom(String roomCode) {
        this.roomCode = roomCode;
        this.player1 = null;
        this.player2 = null;
        this.full = false;
        this.matchFinished = false;
    }

    public synchronized boolean isFull() {
        return full;
    }

    public synchronized boolean isEmpty() {
        return player1 == null && player2 == null;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public synchronized boolean addPlayer(ServerClientHandler handler) {
        if (full) {
            return false;
        }

        if (player1 == null) {
            player1 = handler;
            player1.sendMessage(NetworkCommand.CONNECTED.format("Waiting for Player 2..."));
            return true;
        }

        if (player1.getUsername().equalsIgnoreCase(handler.getUsername())) {
            return false;
        }

        player2 = handler;
        full = true;

        player1.setOpponentHandler(player2);
        player2.setOpponentHandler(player1);

        player1.sendMessage(NetworkCommand.START.format("1:" + player2.getUsername()));
        player2.sendMessage(NetworkCommand.START.format("2:" + player1.getUsername()));
        return true;
    }

    public synchronized void handleGameOver(ServerClientHandler loser) {
        if (!full || matchFinished) {
            return;
        }
        ServerClientHandler winner = getOpponentOf(loser);
        if (winner != null) {
            recordResult(winner, loser);
        }
    }

    public synchronized void removePlayer(ServerClientHandler handler) {
        if (handler != player1 && handler != player2) {
            return;
        }

        ServerClientHandler opponent = getOpponentOf(handler);

        if (full && !matchFinished && opponent != null) {
            recordResult(opponent, handler);
        }

        if (handler == player1) {
            player1 = null;
        } else {
            player2 = null;
        }

        if (opponent != null) {
            opponent.sendMessage(NetworkCommand.DISCONNECTED.format("Opponent has left the game."));
        }
    }

    private ServerClientHandler getOpponentOf(ServerClientHandler handler) {
        if (handler == player1) {
            return player2;
        }
        if (handler == player2) {
            return player1;
        }
        return null;
    }

    private void recordResult(ServerClientHandler winner, ServerClientHandler loser) {
        matchFinished = true;
        GameServer.getUserDao().recordMatchResult(winner.getUsername(), loser.getUsername());
    }
}
