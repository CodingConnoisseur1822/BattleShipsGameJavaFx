package util;

import application.Main;
import java.net.URL;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;

public class SceneFactory {

    private static final String STYLESHEET = "/style.css";

    public static Scene createScene(Parent root) {
        Scene scene = new Scene(root, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT + Main.CONTROL_PANEL_HEIGHT);
        String stylesheetUrl = findStylesheet();
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl);
        }
        return scene;
    }

    public static void styleAlert(Alert alert) {
        String stylesheetUrl = findStylesheet();
        if (stylesheetUrl != null) {
            alert.getDialogPane().getStylesheets().add(stylesheetUrl);
        }
    }

    private static String findStylesheet() {
        URL url = SceneFactory.class.getResource(STYLESHEET);
        if (url == null) {
            System.err.println("Missing stylesheet: " + STYLESHEET);
            return null;
        }
        return url.toExternalForm();
    }
}