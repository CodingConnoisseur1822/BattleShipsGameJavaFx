package model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import util.Load;

public class HitMarker {

    private final Image image;
    private final double pixelX;
    private final double pixelY;

    public HitMarker(MarkerType type, double pixelX, double pixelY) {
        this.image = Load.loadImage(type.getImageName());
        this.pixelX = pixelX;
        this.pixelY = pixelY;
    }

    public void draw(GraphicsContext gc) {
        if (image != null) {
            gc.drawImage(image, pixelX, pixelY);
        }
    }
}
