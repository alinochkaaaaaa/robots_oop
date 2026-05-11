package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Построение маршрута из контура.
 */
public class PathBuilder {


    public static List<Waypoint> buildPathFromContour(List<Point> contour, double stepSize) {
        if (contour == null || contour.size() < 3) {
            return new ArrayList<>();
        }

        List<Point> simplified = simplifyContour(contour, 2.0);

        List<Waypoint> path = new ArrayList<>();

        for (int i = 0; i < simplified.size(); i++) {
            Point current = simplified.get(i);
            Point next = simplified.get((i + 1) % simplified.size());

            double fromX = current.x;
            double fromY = current.y;
            double toX = next.x;
            double toY = next.y;

            double distance = Math.hypot(toX - fromX, toY - fromY);

            if (distance < stepSize) {
                path.add(new Waypoint(toX, toY));
                continue;
            }

            int steps = (int) Math.ceil(distance / stepSize);
            for (int s = 0; s <= steps; s++) {
                double t = (double) s / steps;
                double ix = fromX + (toX - fromX) * t;
                double iy = fromY + (toY - fromY) * t;
                path.add(new Waypoint(ix, iy));
            }
        }

        return path;
    }

    public static List<Point> simplifyContour(List<Point> points, double epsilon) {
        if (points == null || points.size() < 3) {
            return new ArrayList<>(points == null ? List.of() : points);
        }

        double maxDist = 0;
        int index = -1;
        Point start = points.get(0);
        Point end = points.get(points.size() - 1);

        for (int i = 1; i < points.size() - 1; i++) {
            double dist = perpendicularDistance(points.get(i), start, end);
            if (dist > maxDist) {
                maxDist = dist;
                index = i;
            }
        }

        List<Point> result = new ArrayList<>();
        if (maxDist > epsilon && index != -1) {
            List<Point> left = simplifyContour(points.subList(0, index + 1), epsilon);
            List<Point> right = simplifyContour(points.subList(index, points.size()), epsilon);
            result.addAll(left);
            result.remove(result.size() - 1);
            result.addAll(right);
        } else {
            result.add(start);
            result.add(end);
        }
        return result;
    }

    /**
     * Расстояние от точки p до отрезка (a, b).
     */
    private static double perpendicularDistance(Point p, Point a, Point b) {
        double abX = b.x - a.x;
        double abY = b.y - a.y;
        double apX = p.x - a.x;
        double apY = p.y - a.y;
        double ab2 = abX * abX + abY * abY;
        if (ab2 == 0) return Math.hypot(apX, apY);
        double t = (apX * abX + apY * abY) / ab2;
        if (t < 0) return Math.hypot(apX, apY);
        if (t > 1) return Math.hypot(p.x - b.x, p.y - b.y);
        double projX = a.x + t * abX;
        double projY = a.y + t * abY;
        return Math.hypot(p.x - projX, p.y - projY);
    }
}