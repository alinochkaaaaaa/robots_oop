package model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public class RobotModel {
    private volatile double robotPositionX = 150;
    private volatile double robotPositionY = 100;
    private volatile double robotDirection = 0;
    private volatile double targetPositionX = 200;
    private volatile double targetPositionY = 100;
    private int robotId = 0;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private List<Waypoint> currentPath = null;
    private int currentPathIndex = 0;
    private boolean isTracingMode = false;

    public void setRobotId(int id) {
        this.robotId = id;
    }

    public int getRobotId() {
        return robotId;
    }

    public String getRobotName() {
        return "Робот " + robotId;
    }

    public void setTargetPosition(double x, double y) {
        if (isTracingMode) {
            stopTracing();
        }
        this.targetPositionX = x;
        this.targetPositionY = y;
        pcs.firePropertyChange("target", null, this);
    }

    public void startTracing(List<Waypoint> path) {
        if (path == null || path.isEmpty()) return;
        this.currentPath = new ArrayList<>(path);
        this.currentPathIndex = 0;
        this.isTracingMode = true;

        // Сразу ставим первую цель
        if (!path.isEmpty()) {
            Waypoint first = path.get(0);
            this.targetPositionX = first.x();
            this.targetPositionY = first.y();
        }
        System.out.println("Трассировка запущена, точек: " + path.size());
    }

    public void stopTracing() {
        this.isTracingMode = false;
        this.currentPath = null;
        this.currentPathIndex = 0;
    }

    public boolean isTracingMode() {
        return isTracingMode;
    }

    private void updateTracing() {
        if (!isTracingMode || currentPath == null || currentPathIndex >= currentPath.size()) {
            if (isTracingMode) {
                System.out.println("Трассировка завершена");
                stopTracing();
            }
            return;
        }

        Waypoint target = currentPath.get(currentPathIndex);
        double dx = target.x() - robotPositionX;
        double dy = target.y() - robotPositionY;
        double distance = Math.hypot(dx, dy);

        double tolerance = target.tolerance() * 2.0;  // Увеличенный допуск

        if (distance < tolerance) {
            currentPathIndex++;
            if (currentPathIndex % 25 == 0 || currentPathIndex > currentPath.size() - 5) {
                System.out.println("Перешли к точке " + currentPathIndex + "/" + currentPath.size());
            }
            if (currentPathIndex >= currentPath.size()) {
                System.out.println("Трассировка завершена!");
                stopTracing();
            }
            return;
        }

        double angleToTarget = Math.atan2(dy, dx);
        double angleDiff = angleToTarget - robotDirection;

        while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

        double maxTurn = Math.toRadians(15);
        double turn = Math.max(-maxTurn, Math.min(maxTurn, angleDiff));
        robotDirection += turn;

        double step = Math.min(7.0, distance * 0.65);
        robotPositionX += step * Math.cos(robotDirection);
        robotPositionY += step * Math.sin(robotDirection);

        pcs.firePropertyChange("position", null, this);
    }

    public void updateModel() {
        if (isTracingMode) {
            updateTracing();
            return;
        }

        double distance = distanceToTarget();
        if (distance < 1.0) {
            robotPositionX = targetPositionX;
            robotPositionY = targetPositionY;
            pcs.firePropertyChange("position", null, this);
            return;
        }

        double angleToTarget = angleToTarget();
        double angleDiff = angleToTarget - robotDirection;

        while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

        double maxTurn = Math.toRadians(10);
        double turn = Math.max(-maxTurn, Math.min(maxTurn, angleDiff));
        robotDirection += turn;

        double step = Math.min(5, distance);
        robotPositionX += step * Math.cos(robotDirection);
        robotPositionY += step * Math.sin(robotDirection);

        pcs.firePropertyChange("position", null, this);
    }

    private double distanceToTarget() {
        double dx = targetPositionX - robotPositionX;
        double dy = targetPositionY - robotPositionY;
        return Math.hypot(dx, dy);
    }

    private double angleToTarget() {
        double dx = targetPositionX - robotPositionX;
        double dy = targetPositionY - robotPositionY;
        return Math.atan2(dy, dx);
    }

    public double getRobotPositionX() { return robotPositionX; }
    public double getRobotPositionY() { return robotPositionY; }
    public double getRobotDirection() { return robotDirection; }
    public double getTargetPositionX() { return targetPositionX; }
    public double getTargetPositionY() { return targetPositionY; }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void setPosition(double x, double y) {
        this.robotPositionX = x;
        this.robotPositionY = y;
    }
}