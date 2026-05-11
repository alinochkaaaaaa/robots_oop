package gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Управление следом робота.
 */
public class TrailManager {
    private final List<Point> trail = new ArrayList<>();
    private final int maxSize;

    public TrailManager(int maxSize) {
        this.maxSize = maxSize;
    }

    public void addPoint(double x, double y) {
        Point p = new Point((int) x, (int) y);
        trail.add(p);
        if (trail.size() > maxSize) {
            trail.remove(0);
        }
    }

    public void clear() {
        trail.clear();
    }

    public void draw(Graphics2D g) {
        if (trail.size() < 2) return;

        g.setColor(new Color(100, 150, 255, 180));
        g.setStroke(new BasicStroke(2.0f));

        for (int i = 1; i < trail.size(); i++) {
            Point p1 = trail.get(i - 1);
            Point p2 = trail.get(i);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        g.setStroke(new BasicStroke(1.0f));
    }

    public boolean isEmpty() {
        return trail.isEmpty();
    }
}