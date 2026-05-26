package gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TrailManager {
    private final List<Point> trail = new ArrayList<>();
    private final int maxSize;

    public TrailManager(int maxSize) {
        this.maxSize = maxSize;
    }

    public void addPoint(double x, double y) {
        Point p = new Point((int) x, (int) y);

        // Не добавляем дубликаты подряд
        if (!trail.isEmpty()) {
            Point last = trail.get(trail.size() - 1);
            if (last.x == p.x && last.y == p.y) {
                return;
            }
        }

        trail.add(p);
        if (trail.size() > maxSize) {
            trail.remove(0);
        }
    }

    public void clear() {
        trail.clear();
    }

    public void draw(Graphics2D g) {
        if (trail.size() < 2) {
            return;
        }

        // Градиентный след: от красного к синему
        for (int i = 1; i < trail.size(); i++) {
            Point p1 = trail.get(i - 1);
            Point p2 = trail.get(i);

            // Чем ближе к концу, тем ярче
            float intensity = (float) i / trail.size();
            Color trailColor = new Color(
                    255,
                    (int)(100 * (1 - intensity)),
                    (int)(100 * intensity),
                    180
            );

            g.setColor(trailColor);
            g.setStroke(new BasicStroke(3.0f));
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        g.setStroke(new BasicStroke(1.0f));
    }

    public boolean isEmpty() {
        return trail.isEmpty();
    }
}