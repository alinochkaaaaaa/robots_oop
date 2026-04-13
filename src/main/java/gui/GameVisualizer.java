package gui;

import log.Logger;
import model.MultiRobotModel;
import model.RobotModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class GameVisualizer extends JPanel implements PropertyChangeListener {
    private final Timer m_timer = initTimer();
    private final MultiRobotModel multiModel;
    private RobotModel selectedRobotForTarget = null;
    private final Map<Integer, Color> robotColors = new HashMap<>();
    private int nextColorIndex = 0;
    private final Color[] colors = {
            Color.MAGENTA, Color.BLUE, Color.RED, Color.ORANGE, Color.CYAN, Color.PINK
    };

    private static Timer initTimer() {
        return new Timer("events generator", true);
    }

    public GameVisualizer(MultiRobotModel multiModel) {
        this.multiModel = multiModel;
        this.multiModel.addPropertyChangeListener(this);

        // По умолчанию цель назначается первому роботу
        if (!multiModel.getRobots().isEmpty()) {
            selectedRobotForTarget = multiModel.getRobots().get(0);
            Logger.debug("Выбран робот 1 по умолчанию");
        }

        m_timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onRedrawEvent();
            }
        }, 0, 50);

        m_timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onModelUpdateEvent();
            }
        }, 0, 10);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point clickPoint = e.getPoint();

                if (e.isControlDown()) {
                    // Ctrl+клик - выбор робота для управления
                    Logger.debug("Ctrl+клик в точке: " + clickPoint);
                    selectRobotAtPosition(clickPoint);
                } else {
                    // Обычный клик - установка цели для выбранного робота
                    Logger.debug("Обычный клик в точке: " + clickPoint);
                    setTargetForSelectedRobot(clickPoint);
                }
                repaint();
            }
        });

        setDoubleBuffered(true);
    }

    private void selectRobotAtPosition(Point p) {
        // Ищем робота под курсором (с запасом 30 пикселей)
        RobotModel closestRobot = null;
        double minDistance = 60;

        System.out.println("=== ПОИСК РОБОТА ===");

        for (RobotModel robot : multiModel.getRobots()) {
            int rx = (int) robot.getRobotPositionX();
            int ry = (int) robot.getRobotPositionY();
            double dist = Math.hypot(p.x - rx, p.y - ry);

            System.out.println("Робот " + robot.getRobotId() +
                    " на (" + rx + "," + ry +
                    "), дистанция: " + (int)dist);

            if (dist < minDistance) {
                minDistance = dist;
                closestRobot = robot;
            }
        }

        if (closestRobot != null) {
            selectedRobotForTarget = closestRobot;
            System.out.println(">>> ВЫБРАН РОБОТ " + selectedRobotForTarget.getRobotId());
            Logger.debug("ВЫБРАН робот " + selectedRobotForTarget.getRobotId());
            repaint();
        } else {
            System.out.println(">>> РОБОТ НЕ НАЙДЕН");
            Logger.debug("Робот НЕ найден");
        }
    }

    private void setTargetForSelectedRobot(Point p) {
        if (selectedRobotForTarget != null) {
            Logger.debug("Установка цели для робота " + selectedRobotForTarget.getRobotId() +
                    " в точку (" + p.x + "," + p.y + ")");
            selectedRobotForTarget.setTargetPosition(p.x, p.y);
        } else {
            Logger.debug("Нет выбранного робота. Сначала выберите робота через Ctrl+Клик");
        }
    }

    protected void onRedrawEvent() {
        EventQueue.invokeLater(this::repaint);
    }

    protected void onModelUpdateEvent() {
        multiModel.updateAllModels();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        repaint();
    }

    private static int round(double value) {
        return (int)(value + 0.5);
    }

    private Color getRobotColor(int robotId) {
        return robotColors.computeIfAbsent(robotId,
                id -> colors[nextColorIndex++ % colors.length]);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D)g;

        // Рисуем цели (чтобы они были под роботами или над - не принципиально)
        for (RobotModel robot : multiModel.getRobots()) {
            int targetX = robot.getTargetPositionX();
            int targetY = robot.getTargetPositionY();
            drawTarget(g2d, targetX, targetY);
        }

        // Рисуем всех роботов
        for (RobotModel robot : multiModel.getRobots()) {
            int robotX = (int) robot.getRobotPositionX();
            int robotY = (int) robot.getRobotPositionY();

            drawRobot(g2d, robotX, robotY, robot.getRobotDirection(),
                    getRobotColor(robot.getRobotId()),
                    robot == selectedRobotForTarget);

            // Рисуем ID робота (без трансформации)
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
        String hint2 = "Ctrl+Клик на роботе - выбор робота";
        String hint3 = "Текущий выбранный робот: " +
                (selectedRobotForTarget != null ?
                        selectedRobotForTarget.getRobotId() : "нет");

        g.drawString(hint1, 10, getHeight() - 45);
        g.drawString(hint2, 10, getHeight() - 30);
        g.drawString(hint3, 10, getHeight() - 15);

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

        // Рисуем тело робота
        g.setColor(color);
        fillOval(g, robotCenterX, robotCenterY, 30, 10);
        g.setColor(Color.BLACK);
        drawOval(g, robotCenterX, robotCenterY, 30, 10);

        // Рисуем "глаз" робота (белая точка)
        g.setColor(Color.WHITE);
        fillOval(g, robotCenterX + 10, robotCenterY, 5, 5);
        g.setColor(Color.BLACK);
        drawOval(g, robotCenterX + 10, robotCenterY, 5, 5);

        // Восстанавливаем трансформацию
        g.setTransform(oldTransform);
    }

    private void drawTarget(Graphics2D g, int x, int y) {
        AffineTransform old = g.getTransform();
        g.setTransform(new AffineTransform());

        g.setColor(Color.GREEN);
        fillOval(g, x, y, 8, 8);
        g.setColor(Color.BLACK);
        drawOval(g, x, y, 8, 8);

        // Рисуем крестик внутри цели
        g.drawLine(x - 3, y, x + 3, y);
        g.drawLine(x, y - 3, x, y + 3);

        g.setTransform(old);
    }
}