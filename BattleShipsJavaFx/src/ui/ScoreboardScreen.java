package ui;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.PlayerScore;
import network.NetworkCommand;
import util.SceneFactory;
import util.ServerRequest;

public class ScoreboardScreen {

    private final VBox scoreboardLayout;
    private final TableView<PlayerScore> scoreTable;
    private final Label statusLabel;

    public ScoreboardScreen(final Stage primaryStage, final Scene previousScene) {
        scoreboardLayout = new VBox(18);
        scoreboardLayout.setAlignment(Pos.CENTER);
        scoreboardLayout.getStyleClass().add("title-screen-container");

        Label subtitleLabel = new Label("HALL OF FAME");
        subtitleLabel.getStyleClass().add("subtitle-label");

        Label titleLabel = new Label("SCOREBOARD");
        titleLabel.getStyleClass().add("title-label");

        scoreTable = new TableView<PlayerScore>();
        scoreTable.setMaxWidth(560);
        scoreTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        scoreTable.getColumns().add(createColumn("Player", "username"));
        scoreTable.getColumns().add(createColumn("Wins", "wins"));
        scoreTable.getColumns().add(createColumn("Losses", "losses"));

        statusLabel = new Label("");
        statusLabel.getStyleClass().add("status-label");

        Button refreshButton = createButton("Refresh", "menu-button");
        refreshButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                refresh();
            }
        });

        Button backButton = createButton("Back", "secondary-button");
        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                primaryStage.setScene(previousScene);
            }
        });

        HBox buttonRow = new HBox(14, refreshButton, backButton);
        buttonRow.setAlignment(Pos.CENTER);

        scoreboardLayout.getChildren().addAll(subtitleLabel, titleLabel, scoreTable, statusLabel, buttonRow);
    }

    public static void show(Stage primaryStage) {
        Scene previousScene = primaryStage.getScene();
        ScoreboardScreen scoreboardScreen = new ScoreboardScreen(primaryStage, previousScene);
        primaryStage.setScene(SceneFactory.createScene(scoreboardScreen.getScoreboardLayout()));
        scoreboardScreen.refresh();
    }

    public VBox getScoreboardLayout() {
        return scoreboardLayout;
    }

    public void refresh() {
        statusLabel.setText("Loading...");

        ServerRequest.send(NetworkCommand.SCOREBOARD.getPrefix(), new ServerRequest.ResponseListener() {
            @Override
            public void onResponse(String response) {
                if (NetworkCommand.fromMessage(response) == NetworkCommand.SCOREBOARD) {
                    showScores(NetworkCommand.getPayload(response));
                } else {
                    statusLabel.setText(NetworkCommand.getPayload(response));
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                statusLabel.setText(errorMessage);
            }
        });
    }

    private void showScores(String payload) {
        List<PlayerScore> scores = new ArrayList<PlayerScore>();

        if (!payload.isEmpty()) {
            for (String entry : payload.split(";")) {
                PlayerScore score = PlayerScore.fromNetworkString(entry);
                if (score != null) {
                    scores.add(score);
                }
            }
        }

        scoreTable.setItems(FXCollections.observableArrayList(scores));
        if (scores.isEmpty()) {
            statusLabel.setText("No players registered yet.");
        } else {
            statusLabel.setText("");
        }
    }

    private <T> TableColumn<PlayerScore, T> createColumn(String title, String propertyName) {
        TableColumn<PlayerScore, T> column = new TableColumn<PlayerScore, T>(title);
        column.setCellValueFactory(new PropertyValueFactory<PlayerScore, T>(propertyName));
        return column;
    }

    private Button createButton(String text, String styleClass) {
        Button button = new Button(text);
        button.setPrefSize(160, 46);
        button.getStyleClass().add(styleClass);
        return button;
    }
}
