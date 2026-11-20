package ui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.AccountRules;
import network.NetworkCommand;
import util.SceneFactory;
import util.ServerRequest;
import util.UserSession;

public class LoginScreen {

    private final Stage primaryStage;
    private final Scene gameScene;
    private final VBox loginLayout;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Label statusLabel;
    private final Button loginButton;
    private final Button registerButton;

    public LoginScreen(Stage primaryStage, Scene gameScene) {
        this.primaryStage = primaryStage;
        this.gameScene = gameScene;

        loginLayout = new VBox(18);
        loginLayout.setAlignment(Pos.CENTER);
        loginLayout.getStyleClass().add("title-screen-container");

        Label subtitleLabel = new Label("PLAYER IDENTIFICATION");
        subtitleLabel.getStyleClass().add("subtitle-label");

        Label titleLabel = new Label("LOGIN");
        titleLabel.getStyleClass().add("title-label");

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(240);

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(240);

        statusLabel = new Label("");
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setWrapText(true);

        loginButton = createButton("Login", "menu-button");
        loginButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                submit(NetworkCommand.LOGIN);
            }
        });

        registerButton = createButton("Register", "menu-button");
        registerButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                submit(NetworkCommand.REGISTER);
            }
        });

        Button backButton = createButton("Back", "secondary-button");
        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                showTitleScreen();
            }
        });

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(320);
        card.getStyleClass().add("card-panel");
        card.getChildren().addAll(usernameField, passwordField, statusLabel, loginButton, registerButton, backButton);

        loginLayout.getChildren().addAll(subtitleLabel, titleLabel, card);
    }

    public static void show(Stage primaryStage, Scene gameScene) {
        LoginScreen loginScreen = new LoginScreen(primaryStage, gameScene);
        primaryStage.setScene(SceneFactory.createScene(loginScreen.getLoginLayout()));
    }

    public VBox getLoginLayout() {
        return loginLayout;
    }

    private void showTitleScreen() {
        TitleScreen titleScreen = new TitleScreen(primaryStage, gameScene);
        primaryStage.setScene(SceneFactory.createScene(titleScreen.getMenuLayout()));
    }

    private Button createButton(String text, String styleClass) {
        Button button = new Button(text);
        button.setPrefSize(220, 46);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private boolean inputsAreValid(String username, String password) {
        if (!AccountRules.isValidUsername(username)) {
            statusLabel.setText("Username must be 3-20 letters, digits or underscores.");
            return false;
        }
        if (!AccountRules.isValidPassword(password)) {
            statusLabel.setText("Password must be 4-40 characters.");
            return false;
        }
        return true;
    }

    private void submit(NetworkCommand command) {
        final String username = usernameField.getText().trim();
        final String password = passwordField.getText();
        if (!inputsAreValid(username, password)) {
            return;
        }

        setBusy(true);
        statusLabel.setText("Please wait...");

        ServerRequest.send(command.format(username + ":" + password), new ServerRequest.ResponseListener() {
            @Override
            public void onResponse(String response) {
                setBusy(false);
                NetworkCommand reply = NetworkCommand.fromMessage(response);

                if (reply == NetworkCommand.LOGIN_OK) {
                    UserSession.logIn(username, password);
                    LobbyScreen.show(primaryStage, gameScene);
                } else if (reply == NetworkCommand.REGISTER_OK) {
                    statusLabel.setText("Account created. You can now log in.");
                } else {
                    statusLabel.setText(NetworkCommand.getPayload(response));
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                setBusy(false);
                statusLabel.setText(errorMessage);
            }
        });
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        registerButton.setDisable(busy);
        usernameField.setDisable(busy);
        passwordField.setDisable(busy);
    }
}
