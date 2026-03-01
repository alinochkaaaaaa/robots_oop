package gui;

import model.RobotModel;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JPanel;

public class GameVisualizer extends JPanel implements RobotModel.RobotModelListener
{
    private final Timer m_timer = initTimer();
    private final RobotModel model;

    private static Timer initTimer() 
    {
        Timer timer = new Timer("events generator", true);
        return timer;
    }

    public GameVisualizer(RobotModel model)
    {
        this.model = model;
        model.addListener(this);

        // Таймер для перерисовки (каждые 50 мс)
        m_timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                onRedrawEvent();
            }
        }, 0, 50);

        // Таймер для обновления модели (каждые 10 мс)
        m_timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                onModelUpdateEvent();
            }
        }, 0, 10);

        // Обработчик кликов мыши
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                setTargetPosition(e.getPoint());
                repaint();
            }
        });

        setDoubleBuffered(true);
    }

    protected void setTargetPosition(Point p)
    {
        model.setTargetPosition(p.x, p.y);
    }

    // Просим перерисовать окно (в потоке Swing)
    protected void onRedrawEvent()
    {
        EventQueue.invokeLater(this::repaint);
    }

    // Обновление положения робота (вызывается по таймеру)
    protected void onModelUpdateEvent()
    {
        model.updateModel();
    }
    
    @Override
    public void onRobotPositionChanged(double x, double y, double direction)
    {
        // запоминаем, что позиция изменилась - repaint будет вызван по таймеру
    }

    private static int round(double value)
    {
        return (int)(value + 0.5);
    }

    // Отрисовка всего содержимого
    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
        Graphics2D g2d = (Graphics2D)g;
        drawRobot(g2d, round(model.getRobotPositionX()), round(model.getRobotPositionY()), model.getRobotDirection());
        drawTarget(g2d, model.getTargetPositionX(), model.getTargetPositionY());
    }
    
    private static void fillOval(Graphics g, int centerX, int centerY, int diam1, int diam2)
    {
        g.fillOval(centerX - diam1 / 2, centerY - diam2 / 2, diam1, diam2);
    }
    
    private static void drawOval(Graphics g, int centerX, int centerY, int diam1, int diam2)
    {
        g.drawOval(centerX - diam1 / 2, centerY - diam2 / 2, diam1, diam2);
    }

    // отображает робота (розовый овал с "глазом") и цель (зеленую точку)
    private void drawRobot(Graphics2D g, int x, int y, double direction)
    {
        int robotCenterX = round(model.getRobotPositionX());
        int robotCenterY = round(model.getRobotPositionY());
        // Поворачиваем систему координат
        AffineTransform t = AffineTransform.getRotateInstance(direction, robotCenterX, robotCenterY); 
        g.setTransform(t);
        // Рисуем корпус
        g.setColor(Color.MAGENTA);
        fillOval(g, robotCenterX, robotCenterY, 30, 10);
        g.setColor(Color.BLACK);
        drawOval(g, robotCenterX, robotCenterY, 30, 10);
        // Рисуем "глаз" (передняя часть)
        g.setColor(Color.WHITE);
        fillOval(g, robotCenterX  + 10, robotCenterY, 5, 5);
        g.setColor(Color.BLACK);
        drawOval(g, robotCenterX  + 10, robotCenterY, 5, 5);
    }
    
    private void drawTarget(Graphics2D g, int x, int y)
    {
        // Сбрасываем трансформацию
        AffineTransform t = AffineTransform.getRotateInstance(0, 0, 0);
        g.setTransform(t);
        g.setColor(Color.GREEN);
        fillOval(g, x, y, 5, 5);
        g.setColor(Color.BLACK);
        drawOval(g, x, y, 5, 5);
    }
}
