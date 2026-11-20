package database;

/**
 * AuthResult represents the possible outcomes when
 * a player tries to log in or register an account.
 */
public enum AuthResult {
    SUCCESS("Success."),
    INVALID_USERNAME("Username must be 3-20 letters, digits or underscores."),
    INVALID_PASSWORD("Password must be 4-40 characters."),
    USERNAME_TAKEN("That username is already taken."),
    WRONG_CREDENTIALS("Wrong username or password."),
    DATABASE_ERROR("Database error. Please try again later.");

    private final String message;

    AuthResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
