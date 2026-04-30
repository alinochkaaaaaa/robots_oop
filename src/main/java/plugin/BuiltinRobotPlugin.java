package plugin;

public class BuiltinRobotPlugin implements RobotPlugin {

    private static final String TYPE_ID = "builtin";
    private static final String DISPLAY_NAME = "Стандартный робот";

    @Override
    public String getRobotTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public RobotInstance createInstance(double startX, double startY) {
        return new BuiltinRobotInstance(startX, startY);
    }

    private static class BuiltinRobotInstance implements RobotInstance {
        private double x, y, direction;
        private int targetX, targetY;

        BuiltinRobotInstance(double x, double y) {
            this.x = x;
            this.y = y;
            this.direction = 0;
            this.targetX = (int) x + 50;
            this.targetY = (int) y;
        }

        @Override
        public void update(double targetX, double targetY) {
            this.targetX = (int) targetX;
            this.targetY = (int) targetY;

            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < 1.0) {
                x = targetX;
                y = targetY;
                return;
            }

            double angleToTarget = Math.atan2(dy, dx);
            double angleDiff = angleToTarget - direction;

            while (angleDiff > Math.PI) angleDiff -= 2 * Math.PI;
            while (angleDiff < -Math.PI) angleDiff += 2 * Math.PI;

            double maxTurn = Math.toRadians(10);
            double turn = Math.max(-maxTurn, Math.min(maxTurn, angleDiff));
            direction += turn;

            double step = Math.min(5, distance);
            x += step * Math.cos(direction);
            y += step * Math.sin(direction);
        }

        @Override
        public void draw(java.awt.Graphics2D g, int x, int y, boolean isSelected) {
            // отрисовку делает GameVisualizer
        }

        @Override
        public double getX() { return x; }

        @Override
        public double getY() { return y; }

        @Override
        public double getDirection() { return direction; }

        @Override
        public void setPosition(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}