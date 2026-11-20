package application;

import controller.InputManager;
import java.util.Optional;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import model.GameState;
import network.GameClient;
import ui.TitleScreen;
import util.SceneFactory;

public class Main extends Application {

    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 700;
    public static final int CONTROL_PANEL_HEIGHT = 100;

    private static final String BACKGROUND_COLOR = "#0f1c2e";
    private static final long FRAME_INTERVAL_NANOSECONDS = 1000000000L / 30;

    private static Label statusLabel;

    private GameState gameState;
    private GraphicsContext graphicsContext;
    private AnimationTimer gameLoop;
    private Stage primaryStage;
    private Scene gameScene;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        gameState = new GameState();
        InputManager inputManager = new InputManager(gameState);

        primaryStage.setTitle("Battle Ship");
        primaryStage.setResizable(false);

        Canvas canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        graphicsContext = canvas.getGraphicsContext2D();

        statusLabel = new Label("Place your ships to start!");
        statusLabel.getStyleClass().add("status-label");

        HBox statusBox = new HBox(statusLabel);
        statusBox.setAlignment(Pos.BOTTOM_RIGHT);
        statusBox.setPadding(new Insets(0, 25, 20, 0));
        statusBox.setMouseTransparent(true);

        StackPane centerPane = new StackPane();
        centerPane.getChildren().addAll(canvas, statusBox);

        BorderPane gameRootPane = new BorderPane();
        gameRootPane.setCenter(centerPane);
        gameRootPane.setBottom(createControlPanel(canvas));
        gameScene = SceneFactory.createScene(gameRootPane);

        gameState.setOnReturnToMenu(new Runnable() {
            @Override
            public void run() {
                returnToMainMenu();
            }
        });

        inputManager.registerInputListeners(canvas, gameScene);

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                if (confirm("Exit Game", "Are you sure you want to exit the application?")) {
                    leaveMultiplayerMatch();
                    gameLoop.stop();
                } else {
                    event.consume();
                }
            }
        });

        showTitleScreen();
        primaryStage.show();

        canvas.setFocusTraversable(true);
        startGameLoop();
    }

    public static void updateStatusMessage(final String message) {
        if (statusLabel == null) {
            return;
        }
        if (Platform.isFxApplicationThread()) {
            statusLabel.setText(message);
            return;
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                statusLabel.setText(message);
            }
        });
    }

    private boolean confirm(String header, String content) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Battleship");
        alert.setHeaderText(header);
        alert.setContentText(content);
        SceneFactory.styleAlert(alert);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showTitleScreen() {
        TitleScreen titleScreen = new TitleScreen(primaryStage, gameScene);
        primaryStage.setScene(SceneFactory.createScene(titleScreen.getMenuLayout()));
    }

    private void leaveMultiplayerMatch() {
        GameClient client = gameState.getGameClient();
        if (gameState.isMultiplayer() && client != null) {
            client.disconnect();
        }
    }

    private void returnToMainMenu() {
        leaveMultiplayerMatch();
        gameState.resetGameSession();
        showTitleScreen();
    }

    private HBox createControlPanel(final Canvas canvas) {
        HBox panel = new HBox(50);
        panel.setPrefSize(WINDOW_WIDTH, CONTROL_PANEL_HEIGHT);
        panel.setAlignment(Pos.CENTER);
        panel.getStyleClass().add("control-panel");

        Button startButton = createGameButton("Start Game");
        startButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!gameState.attemptStartGame()) {
                    updateStatusMessage("Error: Place all ships first!");
                }
                canvas.requestFocus();
            }
        });

        Button resetButton = createGameButton("Reset Ships");
        resetButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!gameState.resetShipPlacement()) {
                    updateStatusMessage("Cannot reset ships after readying up or starting!");
                }
                canvas.requestFocus();
            }
        });

        Button menuButton = createGameButton("Main Menu");
        menuButton.getStyleClass().setAll("button", "danger-button");
        menuButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (confirm("Return to Main Menu", "Are you sure you want to leave the match/game?")) {
                    returnToMainMenu();
                } else {
                    canvas.requestFocus();
                }
            }
        });

        panel.getChildren().addAll(startButton, resetButton, menuButton);
        return panel;
    }

    private Button createGameButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(140, 40);
        button.getStyleClass().add("game-button");
        return button;
    }

    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            private long lastFrameTime = 0;

            @Override
            public void handle(long currentNanoTime) {
                if (lastFrameTime == 0) {
                    lastFrameTime = currentNanoTime;
                    return;
                }

                if (currentNanoTime - lastFrameTime >= FRAME_INTERVAL_NANOSECONDS) {
                    gameState.updateGameState();
                    drawFrame();
                    lastFrameTime = currentNanoTime;
                }
            }
        };
        gameLoop.start();
    }

    private void drawFrame() {
        graphicsContext.setFill(Color.web(BACKGROUND_COLOR));
        graphicsContext.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        gameState.renderAllGameObjects(graphicsContext);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
