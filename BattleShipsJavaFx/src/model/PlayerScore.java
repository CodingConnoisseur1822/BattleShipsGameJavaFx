package model;

public class PlayerScore {

    private String username;
    private int wins;
    private int losses;

    public PlayerScore(String username, int wins, int losses) {
        this.username = username;
        this.wins = wins;
        this.losses = losses;
    }

    public String getUsername() {
        return username;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public String toNetworkString() {
        return username + "," + wins + "," + losses;
    }

    public static PlayerScore fromNetworkString(String text) {
        if (text == null) {
            return null;
        }
        String[] parts = text.split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            int wins = Integer.parseInt(parts[1].trim());
            int losses = Integer.parseInt(parts[2].trim());
            return new PlayerScore(parts[0].trim(), wins, losses);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
