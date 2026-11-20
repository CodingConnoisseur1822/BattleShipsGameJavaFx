package model;

public enum ShipType {
    BATTLESHIP("Battleship", 5, "Battleship.png", 20.0, 600.0),
    CRUISER("Cruiser", 4, "Cruise.png", 300.0, 600.0),
    DESTROYER_1("Destroyer 1", 3, "Destroyer.png", 20.0, 650.0),
    DESTROYER_2("Destroyer 2", 3, "Destroyer.png", 200.0, 650.0),
    SUBMARINE("Submarine", 2, "Submarine.png", 370.0, 650.0);

    private final String name;
    private final int size;
    private final String imageName;
    private final double startX;
    private final double startY;

    ShipType(String name, int size, String imageName, double startX, double startY) {
        this.name = name;
        this.size = size;
        this.imageName = imageName;
        this.startX = startX;
        this.startY = startY;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public String getImageName() {
        return imageName;
    }

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }
}
