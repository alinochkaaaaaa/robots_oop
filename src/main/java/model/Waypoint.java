package model;

/**
 * Точка маршрута для режима трассировки.
 */
public record Waypoint(
        double x,
        double y,
        double tolerance
) {
    public Waypoint(double x, double y) {
        this(x, y, 5.0);
    }
}