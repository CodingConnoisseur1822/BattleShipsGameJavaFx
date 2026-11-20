package model;

public enum MarkerType {
    HIT("hit.png"),
    MISS("failure.png");

    private final String imageName;

    MarkerType(String imageName) {
        this.imageName = imageName;
    }

    public String getImageName() {
        return imageName;
    }

    public static MarkerType fromResult(boolean hit) {
        if (hit) {
            return HIT;
        }
        return MISS;
    }
}