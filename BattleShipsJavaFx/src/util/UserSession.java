package util;

public class UserSession {

    private static String username = null;
    private static String password = null;

    public static void logIn(String newUsername, String newPassword) {
        username = newUsername;
        password = newPassword;
    }

    public static void logOut() {
        username = null;
        password = null;
    }

    public static boolean isLoggedIn() {
        return username != null;
    }

    public static String getUsername() {
        return username;
    }

    public static String getPassword() {
        return password;
    }
}
