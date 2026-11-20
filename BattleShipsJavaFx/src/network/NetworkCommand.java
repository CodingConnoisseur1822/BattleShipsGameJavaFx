package network;

public enum NetworkCommand {
    READY("READY"),
    DISCONNECTED("DISCONNECTED"),
    ATTACK("ATTACK"),
    ATTACK_RESULT("ATTACK_RESULT"),
    SHIP_SUNK("SHIP_SUNK"),
    GAME_OVER("GAME_OVER"),
    LOGIN("LOGIN"),
    LOGIN_OK("LOGIN_OK"),
    REGISTER("REGISTER"),
    REGISTER_OK("REGISTER_OK"),
    GET_ROOMS("GET_ROOMS"),
    ROOMS("ROOMS"),
    SCOREBOARD("SCOREBOARD"),
    JOIN("JOIN"),
    CONNECTED("CONNECTED"),
    START("START"),
    ERROR("ERROR"),
    UNKNOWN("UNKNOWN");

    private final String prefix;

    NetworkCommand(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String format(String payload) {
        if (payload == null || payload.isEmpty()) {
            return prefix;
        }
        return prefix + ":" + payload;
    }

    public static String getPayload(String message) {
        if (message == null) {
            return "";
        }
        int colonIndex = message.indexOf(':');
        if (colonIndex == -1) {
            return "";
        }
        return message.substring(colonIndex + 1);
    }

    public static NetworkCommand fromMessage(String message) {
        if (message == null || message.isEmpty()) {
            return UNKNOWN;
        }

        String commandPart = message;
        int colonIndex = message.indexOf(':');
        if (colonIndex != -1) {
            commandPart = message.substring(0, colonIndex);
        }

        for (NetworkCommand command : values()) {
            if (command.prefix.equals(commandPart)) {
                return command;
            }
        }
        return UNKNOWN;
    }
}
