package ui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import util.UserSession;

public class TitleScreen {

    private final VBox menuLayout;

    public TitleScreen(final Stage primaryStage, final Scene gameScene) {
        menuLayout = new VBox(18);
        menuLayout.setAlignment(Pos.CENTER);
        menuLayout.getStyleClass().add("title-screen-container");

        Label subtitleLabel = new Label("NAVAL TACTICAL COMMAND");
        subtitleLabel.getStyleClass().add("subtitle-label");

        Label titleLabel = new Label("BATTLESHIP");
        titleLabel.getStyleClass().add("title-label");

        VBox buttonGroup = new VBox(14);
        buttonGroup.setAlignment(Pos.CENTER);
        buttonGroup.setMaxWidth(280);
        buttonGroup.getStyleClass().add("card-panel");

        Button playOfflineButton = createMenuButton("Play Offline", "menu-button");
        playOfflineButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                primaryStage.setScene(gameScene);
            }
        });

        Button playOnlineButton = createMenuButton("Play Online", "menu-button");
        playOnlineButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (UserSession.isLoggedIn()) {
                    LobbyScreen.show(primaryStage, gameScene);
                } else {
                    LoginScreen.show(primaryStage, gameScene);
                }
            }
        });

        Button scoreboardButton = createMenuButton("Scoreboard", "menu-button");
        scoreboardButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ScoreboardScreen.show(primaryStage);
            }
        });

        Button quitButton = createMenuButton("Quit", "secondary-button");
        quitButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                primaryStage.close();
            }
        });

        buttonGroup.getChildren().addAll(playOfflineButton, playOnlineButton, scoreboardButton, quitButton);
        menuLayout.getChildren().addAll(subtitleLabel, titleLabel, buttonGroup);
    }

    private Button createMenuButton(String text, String styleClass) {
        Button button = new Button(text);
        button.setPrefSize(220, 46);
        button.getStyleClass().add(styleClass);
        return button;
    }

    public VBox getMenuLayout() {
        return menuLayout;
    }
}
