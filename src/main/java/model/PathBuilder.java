package model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class PathBuilder {

    public static List<Waypoint> buildPathFromContour(List<Point> contour, double stepSize) {
        if (contour == null || contour.size() < 3) {
            return new ArrayList<>();
        }

        // Сначала сильно упрощаем контур для уменьшения количества точек
        List<Point> simplified = simplifyContour(contour, 8.0); // epsilon для большего упрощения
        System.out.println("После упрощения контура: " + simplified.size() + " точек");

        List<Waypoint> path = new ArrayList<>();

        for (int i = 0; i < simplified.size(); i++) {
            Point current = simplified.get(i);
            Point next = simplified.get((i + 1) % simplified.size());

            double fromX = current.x;
            double fromY = current.y;
            double toX = next.x;
            double toY = next.y;

            double distance = Math.hypot(toX - fromX, toY - fromY);

            // Увеличиваем шаг для уменьшения количества точек маршрута
            double actualStepSize = Math.max(stepSize, 30.0); // Минимум 30 пикселей между точками

            if (distance < actualStepSize) {
                path.add(new Waypoint(toX, toY, 15.0)); // Увеличен допуск
                continue;
            }

            int steps = (int) Math.ceil(distance / actualStepSize);
            // Ограничиваем максимальное количество шагов
            steps = Math.min(steps, 10);

            for (int s = 0; s <= steps; s++) {
                double t = (double) s / steps;
                double ix = fromX + (toX - fromX) * t;
                double iy = fromY + (toY - fromY) * t;
                path.add(new Waypoint(ix, iy, 15.0));
            }
        }

        System.out.println("Построен маршрут из " + path.size() + " точек");
        return path;
    }

    public static List<Point> simplifyContour(List<Point> points, double epsilon) {
        if (points == null || points.size() < 3) {
            return points == null ? new ArrayList<>() : new ArrayList<>(points);
        }

        List<Point> result = new ArrayList<>();
        simplifyRDP(points, 0, points.size() - 1, epsilon, result);

        // Удаляем дубликаты
        List<Point> unique = new ArrayList<>();
        for (Point p : result) {
            if (unique.isEmpty() || (unique.get(unique.size() - 1).x != p.x || unique.get(unique.size() - 1).y != p.y)) {
                unique.add(p);
            }
        }

        return unique;
    }

    private static void simplifyRDP(List<Point> points, int startIdx, int endIdx, double epsilon, List<Point> result) {
        if (startIdx >= endIdx) return;

        double maxDist = 0;
        int index = -1;
        Point start = points.get(startIdx);
        Point end = points.get(endIdx);

        for (int i = startIdx + 1; i < endIdx; i++) {
            double dist = perpendicularDistance(points.get(i), start, end);
            if (dist > maxDist) {
                maxDist = dist;
                index = i;
            }
        }

        if (maxDist > epsilon && index != -1) {
            simplifyRDP(points, startIdx, index, epsilon, result);
            result.add(points.get(index));
            simplifyRDP(points, index, endIdx, epsilon, result);
        } else {
            if (result.isEmpty()) {
                result.add(start);
            }
            result.add(end);
        }
    }

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