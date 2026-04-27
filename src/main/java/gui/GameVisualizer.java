package gui;

import log.Logger;
import model.MultiRobotModel;
import model.RobotModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.RenderingHints;
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
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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

    private static Timer initTimer() {
        return new Timer("events generator", true);
    }

    public GameVisualizer(MultiRobotModel multiModel) {
        this.multiModel = multiModel;
        this.multiModel.addPropertyChangeListener(this);

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

        // Обработка клавиатуры для смены робота (только цифры 1 и 2)
        setFocusable(true); // фокус клавиатуры
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });

        setDoubleBuffered(true);
        setBackground(Color.WHITE);
    }

    /**
     * Обработка нажатий клавиш для смены выбранного робота.
     * Поддерживаются только клавиши 1 и 2
     */
    private void handleKeyPress(KeyEvent e) {
        int keyCode = e.getKeyCode();
        int robotCount = multiModel.getRobotCount();

        if (robotCount == 0) return;

        RobotModel newSelectedRobot = null;

        // Клавиша 1 - выбор первого робота
        if (keyCode == KeyEvent.VK_1) {
            if (robotCount >= 1) {
                newSelectedRobot = multiModel.getRobot(0);
                Logger.debug("Выбран робот 1");
            }
        }
        // Клавиша 2 - выбор второго робота (если существует)
        else if (keyCode == KeyEvent.VK_2) {
            if (robotCount >= 2) {
                newSelectedRobot = multiModel.getRobot(1);
                Logger.debug("Выбран робот 2");
            } else {
                Logger.debug("Робот 2 не существует");
            }
        }

        if (newSelectedRobot != null && newSelectedRobot != selectedRobotForTarget) {
            selectedRobotForTarget = newSelectedRobot;
            System.out.println(">>> ВЫБРАН РОБОТ " + selectedRobotForTarget.getRobotId());
            repaint();
            // фокус для продолжения ввода с клавиатуры
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
        needsRepaint = true;
    }

    private Color getRobotColor(int robotId) {
        return robotColors.computeIfAbsent(robotId,
                _ -> colors[nextColorIndex++ % colors.length]);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D)g;

        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        for (RobotModel robot : multiModel.getRobots()) {
            int targetX = robot.getTargetPositionX();
            int targetY = robot.getTargetPositionY();
            drawTarget(g2d, targetX, targetY);
        }

        for (RobotModel robot : multiModel.getRobots()) {
            int robotX = (int) robot.getRobotPositionX();
            int robotY = (int) robot.getRobotPositionY();

            drawRobot(g2d, robotX, robotY, robot.getRobotDirection(),
                    getRobotColor(robot.getRobotId()),
                    robot == selectedRobotForTarget);

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

    private void drawTarget(Graphics2D g, int x, int y) {
        AffineTransform old = g.getTransform();
        g.setTransform(new AffineTransform());

        g.setColor(Color.GREEN);
        fillOval(g, x, y, 10, 10);
        g.setColor(Color.BLACK);
        drawOval(g, x, y, 10, 10);

        g.drawLine(x - 4, y, x + 4, y);
        g.drawLine(x, y - 4, x, y + 4);

        g.setTransform(old);
    }
}