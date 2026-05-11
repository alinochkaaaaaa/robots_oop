package gui;

import log.Logger;
import model.MultiRobotModel;
import model.RobotModel;
import model.Waypoint;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import model.RobotModelWrapper;
import plugin.RobotInstance;
import javax.swing.JPanel;

public class GameVisualizer extends JPanel implements PropertyChangeListener {
    private final Timer m_timer = initTimer();
    private final MultiRobotModel multiModel;
    private RobotModel selectedRobotForTarget = null;
    private final Map<Integer, Color> robotColors = new HashMap<>();
    private int nextColorIndex = 0;
    private final Color[] colors = {
            Color.MAGENTA, Color.BLUE, Color.RED, Color.ORANGE, Color.CYAN, Color.PINK
    };

    private volatile boolean needsRepaint = false;
    private final TrailManager trailManager = new TrailManager(2000); // Увеличил размер следа
    private boolean showTrail = true;

    private static Timer initTimer() {
        return new Timer("events generator", true);
    }

    public GameVisualizer(MultiRobotModel multiModel) {
        this.multiModel = multiModel;

        // Подписываемся на существующих роботов
        for (RobotModel robot : multiModel.getRobots()) {
            robot.addPropertyChangeListener(this);
        }

        // Слушаем добавление новых роботов
        multiModel.addPropertyChangeListener("robots", evt -> {
            for (RobotModel robot : multiModel.getRobots()) {
                // Добавляем слушателя только если его ещё нет
                robot.removePropertyChangeListener(this);
                robot.addPropertyChangeListener(this);
            }
            // Обновляем выбранного робота если нужно
            if (selectedRobotForTarget == null && !multiModel.getRobots().isEmpty()) {
                selectedRobotForTarget = multiModel.getRobots().get(0);
                Logger.debug("Выбран робот 1 по умолчанию");
            }
            repaint();
        });

        if (!multiModel.getRobots().isEmpty()) {
            selectedRobotForTarget = multiModel.getRobots().get(0);
            Logger.debug("Выбран робот 1 по умолчанию");
        }

        m_timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (needsRepaint) {
                    EventQueue.invokeLater(() -> {
                        repaint();
                        needsRepaint = false;
                    });
                }
            }
        }, 0, 33);

        m_timer.schedule(new TimerTask() {
            @Override
            public void run() {
                multiModel.updateAllModels();
            }
        }, 0, 16);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point clickPoint = e.getPoint();
                Logger.debug("Клик в точке: " + clickPoint);
                setTargetForSelectedRobot(clickPoint);
                repaint();
            }
        });

        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });

        setDoubleBuffered(true);
        setBackground(Color.WHITE);
    }

    private void handleKeyPress(KeyEvent e) {
        int keyCode = e.getKeyCode();
        int robotCount = multiModel.getRobotCount();

        if (robotCount == 0) return;

        RobotModel newSelectedRobot = null;

        if (keyCode == KeyEvent.VK_1) {
            if (robotCount >= 1) {
                newSelectedRobot = multiModel.getRobot(0);
                Logger.debug("Выбран робот 1");
            }
        } else if (keyCode == KeyEvent.VK_2) {
            if (robotCount >= 2) {
                newSelectedRobot = multiModel.getRobot(1);
                Logger.debug("Выбран робот 2");
            } else {
                Logger.debug("Робот 2 не существует");
            }
        }

        if (newSelectedRobot != null && newSelectedRobot != selectedRobotForTarget) {
            selectedRobotForTarget = newSelectedRobot;
            repaint();
            requestFocusInWindow();
        }
    }

    private void setTargetForSelectedRobot(Point p) {
        if (selectedRobotForTarget != null) {
            Logger.debug("Установка цели для робота " + selectedRobotForTarget.getRobotId() +
                    " в точку (" + p.x + "," + p.y + ")");
            selectedRobotForTarget.setTargetPosition(p.x, p.y);
        } else {
            Logger.debug("Нет выбранного робота");
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();

        // Добавляем точку в след при изменении позиции
        if ("position".equals(propertyName) && evt.getSource() instanceof RobotModel) {
            RobotModel robot = (RobotModel) evt.getSource();
            trailManager.addPoint(robot.getRobotPositionX(), robot.getRobotPositionY());
        }

        needsRepaint = true;
    }

    private Color getRobotColor(int robotId) {
        return robotColors.computeIfAbsent(robotId,
                _ -> colors[nextColorIndex++ % colors.length]);
    }

    public void clearTrail() {
        trailManager.clear();
        repaint();
        Logger.debug("След очищен");
    }

    public void setShowTrail(boolean show) {
        this.showTrail = show;
        repaint();
    }

    public void startTracingOnSelectedRobot(List<Waypoint> path) {
        if (selectedRobotForTarget != null && path != null && !path.isEmpty()) {
            // Очищаем след перед новой трассировкой
            clearTrail();
            selectedRobotForTarget.stopTracing();
            selectedRobotForTarget.startTracing(path);
            Logger.debug("Запущена трассировка для робота " + selectedRobotForTarget.getRobotId() +
                    " с маршрутом из " + path.size() + " точек");
        } else {
            Logger.debug("Не удалось запустить трассировку: нет выбранного робота или пустой путь");
        }
    }

    public void stopTracingOnSelectedRobot() {
        if (selectedRobotForTarget != null) {
            selectedRobotForTarget.stopTracing();
            Logger.debug("Остановлена трассировка для робота " + selectedRobotForTarget.getRobotId());
        }
    }

    public boolean isSelectedRobotTracing() {
        return selectedRobotForTarget != null && selectedRobotForTarget.isTracingMode();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // Рисуем след ДО рисования роботов, чтобы след был под ними
        if (showTrail) {
            trailManager.draw(g2d);
        }

        // Рисуем цели
        for (RobotModel robot : multiModel.getRobots()) {
            drawTarget(g2d, robot.getTargetPositionX(), robot.getTargetPositionY());
        }

        // Рисуем роботов
        for (RobotModel robot : multiModel.getRobots()) {
            int robotX = (int) robot.getRobotPositionX();
            int robotY = (int) robot.getRobotPositionY();

            boolean isSelected = (robot == selectedRobotForTarget);

            if (robot instanceof RobotModelWrapper) {
                RobotModelWrapper wrapper = (RobotModelWrapper) robot;
                RobotInstance instance = wrapper.getPluginInstance();
                instance.draw(g2d, robotX, robotY, isSelected);
            } else {
                drawRobot(g2d, robotX, robotY, robot.getRobotDirection(),
                        getRobotColor(robot.getRobotId()), isSelected);
            }

            drawRobotId(g2d, robotX, robotY, robot.getRobotId());
        }

        drawHint(g2d);
    }

    private void drawRobotId(Graphics2D g, int x, int y, int id) {
        AffineTransform old = g.getTransform();
        g.setTransform(new AffineTransform());
        g.setColor(Color.BLACK);
        g.setFont(g.getFont().deriveFont(12f));
        g.drawString("Робот " + id, x - 25, y - 15);
        g.setTransform(old);
    }

    private void drawHint(Graphics2D g) {
        AffineTransform old = g.getTransform();
        g.setTransform(new AffineTransform());
        g.setColor(Color.DARK_GRAY);
        g.setFont(g.getFont().deriveFont(11f));

        String hint1 = "Клик - установка цели для выбранного робота";
        String hint2 = "Клавиши 1 и 2 - выбор робота";
        String hint3 = "Текущий выбранный робот: " +
                (selectedRobotForTarget != null ?
                        selectedRobotForTarget.getRobotId() : "нет");

        g.drawString(hint1, 10, getHeight() - 45);
        g.drawString(hint2, 10, getHeight() - 30);
        g.drawString(hint3, 10, getHeight() - 15);

        if (isSelectedRobotTracing()) {
            g.setColor(new Color(0, 100, 200, 200));
            g.setFont(g.getFont().deriveFont(12f));
            g.drawString("РЕЖИМ ТРАССИРОВКИ АКТИВЕН", 10, getHeight() - 60);
        }

        g.setTransform(old);
    }

    private static void fillOval(Graphics g, int centerX, int centerY, int diam1, int diam2) {
        g.fillOval(centerX - diam1 / 2, centerY - diam2 / 2, diam1, diam2);
    }

    private static void drawOval(Graphics g, int centerX, int centerY, int diam1, int diam2) {
        g.drawOval(centerX - diam1 / 2, centerY - diam2 / 2, diam1, diam2);
    }

    private void drawRobot(Graphics2D g, int x, int y, double direction, Color color, boolean isSelected) {
        int robotCenterX = x;
        int robotCenterY = y;

        AffineTransform oldTransform = g.getTransform();
        AffineTransform t = AffineTransform.getRotateInstance(direction, robotCenterX, robotCenterY);
        g.setTransform(t);

        g.setColor(color);
        fillOval(g, robotCenterX, robotCenterY, 30, 10);
        g.setColor(Color.BLACK);
        drawOval(g, robotCenterX, robotCenterY, 30, 10);

        g.setColor(Color.WHITE);
        fillOval(g, robotCenterX + 10, robotCenterY, 5, 5);
        g.setColor(Color.BLACK);
        drawOval(g, robotCenterX + 10, robotCenterY, 5, 5);

        if (isSelected) {
            g.setColor(new Color(255, 255, 0, 100));
            fillOval(g, robotCenterX, robotCenterY, 40, 40);
        }

        g.setTransform(oldTransform);
    }

    private void drawTarget(Graphics2D g, double x, double y) {
        AffineTransform old = g.getTransform();
        g.setTransform(new AffineTransform());

        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);

        g.setColor(Color.GREEN);
        fillOval(g, ix, iy, 10, 10);
        g.setColor(Color.BLACK);
        drawOval(g, ix, iy, 10, 10);

        g.drawLine(ix - 4, iy, ix + 4, iy);
        g.drawLine(ix, iy - 4, ix, iy + 4);

        g.setTransform(old);
    }
}