package model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class RobotModel {
     private volatile double m_robotPositionX = 100;
    private volatile double m_robotPositionY = 100;
    private volatile double m_robotDirection = 0;

    private volatile int m_targetPositionX = 150;
    private volatile int m_targetPositionY = 100;

    private static final double maxVelocity = 0.1;
    private static final double maxAngularVelocity = 0.001;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    private void notifyPositionChanged() {
        pcs.firePropertyChange("robotPosition", null,
                new double[]{m_robotPositionX, m_robotPositionY, m_robotDirection});
    }

    private void notifyTargetChanged() {
        pcs.firePropertyChange("targetPosition", null,
                new int[]{m_targetPositionX, m_targetPositionY});
    }

    public void setTargetPosition(int x, int y) {
        m_targetPositionX = x;
        m_targetPositionY = y;

        notifyTargetChanged();
    }

    public double getRobotPositionX() {
        return m_robotPositionX;
    }

    public double getRobotPositionY() {
        return m_robotPositionY;
    }

    public double getRobotDirection() {
        return m_robotDirection;
    }

    public int getTargetPositionX() {
        return m_targetPositionX;
    }

    public int getTargetPositionY() {
        return m_targetPositionY;
    }

    private double distance(double x1, double y1, double x2, double y2) {
        double diffX = x1 - x2;
        double diffY = y1 - y2;
        return Math.sqrt(diffX * diffX + diffY * diffY);
    }

    private double angleTo(double fromX, double fromY, double toX, double toY) {
        double diffX = toX - fromX;
        double diffY = toY - fromY;
        return asNormalizedRadians(Math.atan2(diffY, diffX));
    }

    public void updateModel() {
        double distance = distance(m_targetPositionX, m_targetPositionY,
                m_robotPositionX, m_robotPositionY);

        double angleToTarget = angleTo(m_robotPositionX, m_robotPositionY,
                m_targetPositionX, m_targetPositionY);

        double angleDiff = angleToTarget - m_robotDirection;
        while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

        if (Math.abs(angleDiff) > 0.02) {
            double velocity = maxVelocity * 0.3;
            double angularVelocity = maxAngularVelocity * Math.signum(angleDiff);

            if (Math.abs(angleDiff) > 0.5) {
                angularVelocity = maxAngularVelocity * 2 * Math.signum(angleDiff);
            }

            moveRobot(velocity, angularVelocity, 10);
        } else {
            double velocity = maxVelocity;
            if (distance < 50) {
                velocity = maxVelocity * (distance / 50); // Замедление
            }
            moveRobot(velocity, 0, 10);
        }
    }

    private double applyLimits(double value, double min, double max) {
        if (value < min)
            return min;
        if (value > max)
            return max;
        return value;
    }

    private void moveRobot(double velocity, double angularVelocity, double duration) {
        velocity = applyLimits(velocity, 0, maxVelocity);
        angularVelocity = applyLimits(angularVelocity, -maxAngularVelocity, maxAngularVelocity);

        double newX, newY;
        double newDirection = m_robotDirection;

        if (Math.abs(angularVelocity) < 1e-8) {
            newX = m_robotPositionX + velocity * duration * Math.cos(m_robotDirection);
            newY = m_robotPositionY + velocity * duration * Math.sin(m_robotDirection);
        } else {
            double turnRadius = velocity / angularVelocity;
            double angleChange = angularVelocity * duration;

            newX = m_robotPositionX + turnRadius *
                    (Math.sin(m_robotDirection + angleChange) - Math.sin(m_robotDirection));
            newY = m_robotPositionY - turnRadius *
                    (Math.cos(m_robotDirection + angleChange) - Math.cos(m_robotDirection));

            newDirection = asNormalizedRadians(m_robotDirection + angleChange);
        }

        newX = Math.max(15, Math.min(785, newX));
        newY = Math.max(15, Math.min(585, newY));

        double distanceToTarget = distance(m_targetPositionX, m_targetPositionY, newX, newY);
        double currentDistance = distance(m_targetPositionX, m_targetPositionY,
                m_robotPositionX, m_robotPositionY);

        if (distanceToTarget > currentDistance && currentDistance < 3.0) {
            return;
        }

        m_robotPositionX = newX;
        m_robotPositionY = newY;
        m_robotDirection = newDirection;

        notifyPositionChanged();
    }

    private double asNormalizedRadians(double angle) {
        while (angle < 0) {
            angle += 2 * Math.PI;
        }
        while (angle >= 2 * Math.PI) {
            angle -= 2 * Math.PI;
        }
        return angle;
    }
}
