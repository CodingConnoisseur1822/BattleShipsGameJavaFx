package ui;

import application.Main;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.GameState;
import network.GameClient;
import network.NetworkCommand;
import util.SceneFactory;
import util.ServerRequest;
import util.UserSession;

/**
 * LobbyScreen builds the multiplayer lobby interface where logged-in
 * players can view active rooms, enter or select a room code to
 * start a match, and check the scoreboard or log out.
 * Once a second player joins the same room, it hands the connection over
 * to GameState and transitions straight into the game screen.
 */
public class LobbyScreen implements GameClient.NetworkListener {

    private static final int MAX_ROOM_CODE_LENGTH = 12;
    private static final String NORMAL_STATUS_STYLE = "-fx-text-fill: #7dd3fc; -fx-font-size: 13px; -fx-font-weight: bold;";
    private static final String ERROR_STATUS_STYLE = "-fx-text-fill: #f87171; -fx-font-size: 13px; -fx-font-weight: bold;";

    private final VBox lobbyLayout;
    private final TextField roomCodeField;
    private final Button joinButton;
    private final Label statusLabel;
    private final ListView<String> roomListView;

    private final Stage primaryStage;
    private final Scene gameScene;
    private GameClient gameClient;

    public LobbyScreen(Stage stage, Scene targetGameScene) {
        this.primaryStage = stage;
        this.gameScene = targetGameScene;

        lobbyLayout = new VBox(16);
        lobbyLayout.setAlignment(Pos.CENTER);
        lobbyLayout.getStyleClass().add("title-screen-container");

        Label subtitleLabel = new Label("COMMANDER " + UserSession.getUsername().toUpperCase());
        subtitleLabel.getStyleClass().add("subtitle-label");

        Label titleLabel = new Label("MULTIPLAYER LOBBY");
        titleLabel.getStyleClass().add("title-label");

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(420);
        card.getStyleClass().add("card-panel");

        Label roomPrompt = new Label("ROOM CODE");
        roomPrompt.getStyleClass().add("subtitle-label");

        roomCodeField = new TextField();
        roomCodeField.setPromptText("Enter or select a room code");
        roomCodeField.setMaxWidth(Double.MAX_VALUE);

        Label roomsLabel = new Label("ACTIVE OPEN LOBBIES (CLICK TO SELECT)");
        roomsLabel.getStyleClass().add("subtitle-label");

        roomListView = new ListView<String>();
        roomListView.setMaxWidth(Double.MAX_VALUE);
        roomListView.setPrefHeight(150);
        roomListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                String selectedRoom = roomListView.getSelectionModel().getSelectedItem();
                if (selectedRoom != null) {
                    roomCodeField.setText(selectedRoom);
                }
            }
        });

        joinButton = createButton("Join / Create Room", 44, "menu-button");
        joinButton.setMaxWidth(Double.MAX_VALUE);
        joinButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                joinRoom();
            }
        });

        Button refreshButton = createButton("Refresh List", 36, "secondary-button");
        refreshButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                fetchLobbies();
            }
        });

        Button scoreboardButton = createButton("Scoreboard", 36, "secondary-button");
        scoreboardButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ScoreboardScreen.show(primaryStage);
            }
        });

        Button logoutButton = createButton("Log Out", 36, "secondary-button");
        logoutButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                leaveRoom();
                UserSession.logOut();
                showTitleScreen();
            }
        });

        Button backButton = createButton("Back to Menu", 36, "secondary-button");
        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                leaveRoom();
                showTitleScreen();
            }
        });

        HBox firstRow = new HBox(12, refreshButton, scoreboardButton);
        firstRow.setAlignment(Pos.CENTER);

        HBox secondRow = new HBox(12, logoutButton, backButton);
        secondRow.setAlignment(Pos.CENTER);

        statusLabel = new Label("Enter a room code to deploy.");
        statusLabel.setStyle(NORMAL_STATUS_STYLE);
        statusLabel.setWrapText(true);

        card.getChildren().addAll(roomPrompt, roomCodeField, roomsLabel, roomListView,
                joinButton, firstRow, secondRow, statusLabel);
        lobbyLayout.getChildren().addAll(subtitleLabel, titleLabel, card);

        fetchLobbies();
    }

    public static void show(Stage primaryStage, Scene gameScene) {
        LobbyScreen lobbyScreen = new LobbyScreen(primaryStage, gameScene);
        primaryStage.setScene(SceneFactory.createScene(lobbyScreen.getLobbyLayout()));
    }

    public VBox getLobbyLayout() {
        return lobbyLayout;
    }

    private void showTitleScreen() {
        TitleScreen titleScreen = new TitleScreen(primaryStage, gameScene);
        primaryStage.setScene(SceneFactory.createScene(titleScreen.getMenuLayout()));
    }

    private void leaveRoom() {
        if (gameClient != null) {
            gameClient.disconnect();
            gameClient = null;
        }
    }

    private void joinRoom() {
        String roomCode = roomCodeField.getText().trim().toUpperCase();

        if (roomCode.isEmpty()) {
            setStatus("Please enter a room code!", true);
            return;
        }
        if (roomCode.length() > MAX_ROOM_CODE_LENGTH) {
            setStatus("Room code is too long.", true);
            return;
        }
        if (roomCode.indexOf(':') != -1 || roomCode.indexOf(',') != -1) {
            setStatus("Room code cannot contain ':' or ','.", true);
            return;
        }

        setControlsDisabled(true);
        setStatus("Connecting to command server...", false);

        gameClient = new GameClient(this);
        gameClient.connectToRoom(roomCode, UserSession.getUsername(), UserSession.getPassword());
    }

    private void fetchLobbies() {
        ServerRequest.send(NetworkCommand.GET_ROOMS.getPrefix(), new ServerRequest.ResponseListener() {
            @Override
            public void onResponse(String response) {
                roomListView.getItems().clear();
                String payload = NetworkCommand.getPayload(response);
                if (payload.isEmpty()) {
                    return;
                }
                for (String room : payload.split(",")) {
                    roomListView.getItems().add(room.trim());
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                setStatus(errorMessage, true);
            }
        });
    }

    private void setControlsDisabled(boolean disabled) {
        joinButton.setDisable(disabled);
        roomCodeField.setDisable(disabled);
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        if (isError) {
            statusLabel.setStyle(ERROR_STATUS_STYLE);
        } else {
            statusLabel.setStyle(NORMAL_STATUS_STYLE);
        }
    }

    private Button createButton(String text, double height, String styleClass) {
        Button button = new Button(text);
        button.setPrefHeight(height);
        button.setPrefWidth(165);
        button.getStyleClass().add(styleClass);
        return button;
    }

    @Override
    public void onStatus(String message) {
        setStatus(message, false);
    }

    @Override
    public void onError(String message) {
        setStatus(message, true);
        setControlsDisabled(false);
    }

    @Override
    public void onGameStart(int assignedPlayerId, String opponentName) {
        GameState gameState = GameState.getInstance();
        gameState.resetGameSession();
        gameState.enableMultiplayerMode(assignedPlayerId, UserSession.getUsername(), opponentName, gameClient);
        gameClient = null;

        Main.updateStatusMessage("Matched with " + opponentName + "! Place your ships and click Start.");
        primaryStage.setScene(gameScene);
    }
}
