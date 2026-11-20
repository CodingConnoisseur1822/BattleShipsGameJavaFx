package util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.image.Image;

public class Load {

    private static final String IMAGE_FOLDER = "/Images/";
    private static final Map<String, Image> imageCache = new HashMap<String, Image>();

    public static Image loadImage(String fileName) {
        Image cachedImage = imageCache.get(fileName);
        if (cachedImage != null) {
            return cachedImage;
        }

        try (InputStream stream = Load.class.getResourceAsStream(IMAGE_FOLDER + fileName)) {
            if (stream == null) {
                System.err.println("Missing image: " + IMAGE_FOLDER + fileName);
                return null;
            }
            Image image = new Image(stream);
            imageCache.put(fileName, image);
            return image;
        } catch (IOException e) {
            System.err.println("Could not load image " + fileName + ": " + e.getMessage());
            return null;
        }
    }
}
