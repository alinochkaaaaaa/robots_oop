package model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class RobotModel {
    private volatile double robotPositionX = 100;
    private volatile double robotPositionY = 100;
    private volatile double robotDirection = 0;
    private volatile double targetPositionX = 150;
    private volatile double targetPositionY = 100;
    private int robotId = 0;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void setRobotId(int id) {
        this.robotId = id;
    }

    public int getRobotId() {
        return robotId;
    }

    public String getRobotName() {
        return "Робот " + robotId;
    }

    public void setTargetPosition(double x, double y) {   // ← int → double
        this.targetPositionX = x;
        this.targetPositionY = y;
        pcs.firePropertyChange("target", null, this);
    }

    public void updateModel() {
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
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double angleToTarget() {
        double dx = targetPositionX - robotPositionX;
        double dy = targetPositionY - robotPositionY;
        return Math.atan2(dy, dx);
    }

    public double getRobotPositionX() {
        return robotPositionX;
    }

    public double getRobotPositionY() {
        return robotPositionY;
    }

    public double getRobotDirection() {
        return robotDirection;
    }

    public double getTargetPositionX() {
        return targetPositionX;
    }

    public double getTargetPositionY() {
        return targetPositionY;
    }

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