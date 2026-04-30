import plugin.RobotPlugin;
import plugin.RobotInstance;
import java.awt.Graphics2D;
import java.awt.Color;

public class SimpleRobotPlugin implements RobotPlugin {
    @Override
    public String getRobotTypeId() { return "simple_robot"; }
    @Override
    public String getDisplayName() { return "Квадратный робот"; }
    @Override
    public RobotInstance createInstance(double x, double y) { return new SimpleRobot(x, y); }
}

class SimpleRobot implements RobotInstance {
    private double x, y;

    SimpleRobot(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void update(double targetX, double targetY) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < 2.0) {  // Увеличил точность
            x = targetX;
            y = targetY;
            return;
        }

        double speed = 8.0;
        double step = Math.min(speed, distance);
        x += (dx / distance) * step;
        y += (dy / distance) * step;
    }

    @Override
    public void draw(Graphics2D g, int px, int py, boolean isSelected) {
        // ВАЖНО: рисуем по px, py (это robot.getRobotPositionX/Y)
        g.setColor(Color.GREEN);
        g.fillRect(px - 15, py - 15, 30, 30);
        g.setColor(Color.BLACK);
        g.drawRect(px - 15, py - 15, 30, 30);

        // Глаз для направления
        g.setColor(Color.WHITE);
        g.fillOval(px + 5, py - 10, 8, 8);
        g.setColor(Color.BLACK);
        g.fillOval(px + 7, py - 8, 4, 4);

        if (isSelected) {
            g.setColor(new Color(255, 255, 0, 100));
            g.fillRect(px - 20, py - 20, 40, 40);
        }
    }

    @Override
    public double getX() { return x; }
    @Override
    public double getY() { return y; }
    @Override
    public double getDirection() { return 0; }
    @Override
    public void setPosition(double x, double y) { this.x = x; this.y = y; }
}