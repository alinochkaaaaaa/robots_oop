package model;

/**
 * Точка маршрута для режима трассировки.
 */
public class Waypoint {
    private final double x;
    private final double y;
    private final double tolerance;

    public Waypoint(double x, double y, double tolerance) {
        this.x = x;
        this.y = y;
        this.tolerance = tolerance;
    }

    public Waypoint(double x, double y) {
        this(x, y, 5.0);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getTolerance() { return tolerance; }
}