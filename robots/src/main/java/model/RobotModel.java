package model;

import java.util.ArrayList;
import java.util.List;

public class RobotModel {
    // volatile - для многопоточного программирования
    // гарантия, что read/write будет происходить напрямую из основной памяти
    // чтобы все потоки всегда видели актуальное положение робота.
    private volatile double m_robotPositionX = 100;
    private volatile double m_robotPositionY = 100;
    private volatile double m_robotDirection = 0;

    private volatile int m_targetPositionX = 150;
    private volatile int m_targetPositionY = 100;

    private static final double maxVelocity = 0.1;
    private static final double maxAngularVelocity = 0.001;

    private final List<RobotModelListener> listeners = new ArrayList<>();

    // класс, который хочет следить за роботом обязан иметь этот метод
    // т.е. уметь реагировать на изменение позиции робота
    // при этом сообщаются координаты и направление

    public interface RobotModelListener {
        void onRobotPositionChanged(double x, double y, double direction);
    }

    public void addListener(RobotModelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RobotModelListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (RobotModelListener listener : listeners) {
            listener.onRobotPositionChanged(m_robotPositionX, m_robotPositionY, m_robotDirection);
        }
    }

    public void setTargetPosition(int x, int y) {
        m_targetPositionX = x;
        m_targetPositionY = y;
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

    private static double distance(double x1, double y1, double x2, double y2) {
        double diffX = x1 - x2;
        double diffY = y1 - y2;
        return Math.sqrt(diffX * diffX + diffY * diffY);
    }

    private static double angleTo(double fromX, double fromY, double toX, double toY) {
        double diffX = toX - fromX;
        double diffY = toY - fromY;
        return asNormalizedRadians(Math.atan2(diffY, diffX));
    }

    // улучшение поворота к цели!!
    public void updateModel() {
        double distance = distance(m_targetPositionX, m_targetPositionY,
                m_robotPositionX, m_robotPositionY);

        // Если робот достаточно близко к цели - останавливаемся
        if (distance < 1.0) {
            // Плавно останавливаем робота, уменьшая скорость до нуля
            // Но оставляем небольшую возможность доворота к цели
            double angleToTarget = angleTo(m_robotPositionX, m_robotPositionY,
                    m_targetPositionX, m_targetPositionY);
            double angleDiff = angleToTarget - m_robotDirection;
            while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
            while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

            // Если смотрим точно на цель - полностью останавливаемся
            if (Math.abs(angleDiff) < 0.05) {
                return;  // Полная остановка
            }

            // Если не смотрим на цель - только доворачиваемся
            double angularVelocity = maxAngularVelocity * 0.5 * Math.signum(angleDiff);
            // Не двигаемся вперед, только поворачиваемся
            moveRobot(0, angularVelocity, 10);
            return;
        }

        // Обычное движение к цели (когда далеко)
        double velocity = maxVelocity;
        double angleToTarget = angleTo(m_robotPositionX, m_robotPositionY,
                m_targetPositionX, m_targetPositionY);

        double angleDiff = angleToTarget - m_robotDirection;
        while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
        while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

        double angularVelocity = 0;
        if (Math.abs(angleDiff) > 0.01) {
            angularVelocity = maxAngularVelocity * Math.signum(angleDiff);
            // Приближаясь к цели, замедляемся
            double speedFactor = Math.min(1.0, distance / 50.0);
            velocity = maxVelocity * speedFactor;
        }

        moveRobot(velocity, angularVelocity, 10);
    }

    private static double applyLimits(double value, double min, double max) {
        if (value < min)
            return min;
        if (value > max)
            return max;
        return value;
    }

    // Физика движения робота(исправленная!)
    private void moveRobot(double velocity, double angularVelocity, double duration) {
        velocity = applyLimits(velocity, 0, maxVelocity);
        angularVelocity = applyLimits(angularVelocity, -maxAngularVelocity, maxAngularVelocity);

        double newX, newY;

        if (Math.abs(angularVelocity) < 1e-8) {
            // Движение по прямой
            newX = m_robotPositionX + velocity * duration * Math.cos(m_robotDirection);
            newY = m_robotPositionY + velocity * duration * Math.sin(m_robotDirection);
        } else {
            // Движение по дуге
            double turnRadius = velocity / angularVelocity;
            double angleChange = angularVelocity * duration;

            newX = m_robotPositionX + turnRadius *
                    (Math.sin(m_robotDirection + angleChange) - Math.sin(m_robotDirection));
            newY = m_robotPositionY - turnRadius *
                    (Math.cos(m_robotDirection + angleChange) - Math.cos(m_robotDirection));

            m_robotDirection = asNormalizedRadians(m_robotDirection + angleChange);
        }

        // Проверка границ окна
        newX = Math.max(15, Math.min(785, newX));
        newY = Math.max(15, Math.min(585, newY));

        // Дополнительная проверка: если мы уже очень близко к цели,
        // не даем роботу проскочить
        double distanceToTarget = distance(m_targetPositionX, m_targetPositionY, newX, newY);
        double currentDistance = distance(m_targetPositionX, m_targetPositionY,
                m_robotPositionX, m_robotPositionY);

        // Если новый шаг уводит от цели или проскакиваем цель
        if (distanceToTarget > currentDistance && currentDistance < 3.0) {
            // Останавливаемся на текущей позиции
            return;
        }

        m_robotPositionX = newX;
        m_robotPositionY = newY;

        notifyListeners();
    }

    // Приведение угла к диапазону [0, 2π]
    private static double asNormalizedRadians(double angle) {
        while (angle < 0) {
            angle += 2 * Math.PI;
        }
        while (angle >= 2 * Math.PI) {
            angle -= 2 * Math.PI;
        }
        return angle;
    }
}
