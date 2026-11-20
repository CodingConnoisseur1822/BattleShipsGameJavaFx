package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.AccountRules;
import model.PlayerScore;

/**
 * UserDao handles all the database work for player accounts
 * and match stats. It lets new players sign up, checks login
 * details securely using PostgreSQL's built-in encryption,
 * updates wins and losses together when a match ends,
 * and pulls the top 50 players for the leaderboard.
 */
public class UserDao {

    private static final String UNIQUE_VIOLATION = "23505";

    private static final String SQL_INSERT_USER =
            "INSERT INTO users (username, password_hash) VALUES (?, crypt(?, gen_salt('bf')))";
    private static final String SQL_CHECK_LOGIN =
            "SELECT 1 FROM users WHERE LOWER(username) = LOWER(?) AND password_hash = crypt(?, password_hash)";
    private static final String SQL_ADD_WIN =
            "UPDATE users SET wins = wins + 1 WHERE LOWER(username) = LOWER(?)";
    private static final String SQL_ADD_LOSS =
            "UPDATE users SET losses = losses + 1 WHERE LOWER(username) = LOWER(?)";
    private static final String SQL_SCOREBOARD =
            "SELECT username, wins, losses FROM users ORDER BY wins DESC, losses ASC, username ASC LIMIT 50";

    public AuthResult register(String username, String password) {
        if (!AccountRules.isValidUsername(username)) {
            return AuthResult.INVALID_USERNAME;
        }
        if (!AccountRules.isValidPassword(password)) {
            return AuthResult.INVALID_PASSWORD;
        }

        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(SQL_INSERT_USER)) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.executeUpdate();
            return AuthResult.SUCCESS;
        } catch (SQLException e) {
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                return AuthResult.USERNAME_TAKEN;
            }
            e.printStackTrace();
            return AuthResult.DATABASE_ERROR;
        }
    }

    public AuthResult authenticate(String username, String password) {
        if (username == null || password == null) {
            return AuthResult.WRONG_CREDENTIALS;
        }

        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(SQL_CHECK_LOGIN)) {
            statement.setString(1, username);
            statement.setString(2, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return AuthResult.SUCCESS;
                }
                return AuthResult.WRONG_CREDENTIALS;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return AuthResult.DATABASE_ERROR;
        }
    }

    public boolean recordMatchResult(String winnerName, String loserName) {
        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                addOne(connection, SQL_ADD_WIN, winnerName);
                addOne(connection, SQL_ADD_LOSS, loserName);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<PlayerScore> getScoreboard() {
        List<PlayerScore> scores = new ArrayList<PlayerScore>();

        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(SQL_SCOREBOARD);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                scores.add(new PlayerScore(
                        resultSet.getString("username"),
                        resultSet.getInt("wins"),
                        resultSet.getInt("losses")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return scores;
    }

    private void addOne(Connection connection, String sql, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.executeUpdate();
        }
    }
}
