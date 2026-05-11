package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PathBuilder - построение маршрута из контура")
class PathBuilderTest {


    @Test
    @DisplayName("simplifyContour: null контур -> пустой список")
    void testSimplifyContourNull() {
        List<Point> result = PathBuilder.simplifyContour(null, 1.0);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("simplifyContour: пустой контур -> пустой список")
    void testSimplifyContourEmpty() {
        List<Point> result = PathBuilder.simplifyContour(new ArrayList<>(), 1.0);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("simplifyContour: контур с 2 точками -> те же 2 точки")
    void testSimplifyContourTwoPoints() {
        List<Point> points = List.of(
                new Point(0, 0),
                new Point(10, 10)
        );
        List<Point> result = PathBuilder.simplifyContour(points, 1.0);
        assertEquals(2, result.size());
        assertEquals(0, result.get(0).x);
        assertEquals(10, result.get(1).x);
    }

    @Test
    @DisplayName("simplifyContour: прямая линия из 3 точек -> 2 точки (средняя удаляется)")
    void testSimplifyContourStraightLine() {
        List<Point> points = List.of(
                new Point(0, 0),
                new Point(5, 5),
                new Point(10, 10)
        );
        List<Point> result = PathBuilder.simplifyContour(points, 1.0);
        // Средняя точка лежит на прямой, должна удалиться
        assertTrue(result.size() <= 2);
    }

    @Test
    @DisplayName("simplifyContour: квадрат с промежуточными точками -> упрощается до 4 углов")
    void testSimplifyContourSquare() {
        List<Point> square = new ArrayList<>();
        for (int x = 0; x <= 100; x += 20) {
            square.add(new Point(x, 0));
        }
        for (int y = 20; y <= 100; y += 20) {
            square.add(new Point(100, y));
        }
        for (int x = 80; x >= 0; x -= 20) {
            square.add(new Point(x, 100));
        }
        for (int y = 80; y >= 20; y -= 20) {
            square.add(new Point(0, y));
        }

        List<Point> simplified = PathBuilder.simplifyContour(square, 10.0);
        assertTrue(simplified.size() <= 8);
        assertTrue(simplified.size() >= 4);
    }


    @Test
    @DisplayName("buildPathFromContour: null контур -> пустой маршрут")
    void testBuildPathNull() {
        List<Waypoint> path = PathBuilder.buildPathFromContour(null, 10.0);
        assertNotNull(path);
        assertTrue(path.isEmpty());
    }

    @Test
    @DisplayName("buildPathFromContour: пустой контур -> пустой маршрут")
    void testBuildPathEmpty() {
        List<Waypoint> path = PathBuilder.buildPathFromContour(new ArrayList<>(), 10.0);
        assertTrue(path.isEmpty());
    }

    @Test
    @DisplayName("buildPathFromContour: треугольник -> маршрут из интерполированных точек")
    void testBuildPathTriangle() {
        List<Point> triangle = List.of(
                new Point(0, 0),
                new Point(100, 0),
                new Point(50, 100),
                new Point(0, 0)
        );

        List<Waypoint> path = PathBuilder.buildPathFromContour(triangle, 20.0);

        assertNotNull(path);
        assertTrue(path.size() >= 3);

        Waypoint first = path.get(0);
        Waypoint last = path.get(path.size() - 1);
        double distance = Math.hypot(first.getX() - last.getX(), first.getY() - last.getY());
        assertTrue(distance < 30.0);
    }

    @Test
    @DisplayName("buildPathFromContour: квадрат с шагом 10 -> много точек")
    void testBuildPathSquareSmallStep() {
        List<Point> square = List.of(
                new Point(0, 0),
                new Point(100, 0),
                new Point(100, 100),
                new Point(0, 100),
                new Point(0, 0)
        );

        List<Waypoint> path = PathBuilder.buildPathFromContour(square, 10.0);

        assertTrue(path.size() >= 30);
        assertTrue(path.size() <= 50);
    }

    @Test
    @DisplayName("buildPathFromContour: квадрат с большим шагом -> маршрут содержит все вершины")
    void testBuildPathSquareLargeStep() {
        List<Point> square = List.of(
                new Point(0, 0),
                new Point(100, 0),
                new Point(100, 100),
                new Point(0, 100),
                new Point(0, 0)
        );

        List<Waypoint> path = PathBuilder.buildPathFromContour(square, 50.0);

        assertTrue(path.size() >= 4, "Должны быть все 4 вершины");
    }

    @Test
    @DisplayName("buildPathFromContour: проверка, что точки маршрута лежат вдоль контура")
    void testBuildPathPointsOnContour() {
        List<Point> square = List.of(
                new Point(0, 0),
                new Point(100, 0),
                new Point(100, 100),
                new Point(0, 100),
                new Point(0, 0)
        );

        List<Waypoint> path = PathBuilder.buildPathFromContour(square, 20.0);

        for (Waypoint wp : path) {
            double x = wp.getX();
            double y = wp.getY();
            boolean onBorder = (Math.abs(x - 0) < 1 || Math.abs(x - 100) < 1 ||
                    Math.abs(y - 0) < 1 || Math.abs(y - 100) < 1);
            assertTrue(onBorder, "Точка (" + x + ", " + y + ") не на границе квадрата");
        }
    }

    @Test
    @DisplayName("buildPathFromContour: большой контур не должен падать по времени")
    void testBuildPathPerformance() {
        List<Point> bigContour = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            double angle = 2 * Math.PI * i / 500;
            bigContour.add(new Point(
                    (int) (200 + 100 * Math.cos(angle)),
                    (int) (200 + 100 * Math.sin(angle))
            ));
        }

        long start = System.nanoTime();
        List<Waypoint> path = PathBuilder.buildPathFromContour(bigContour, 10.0);
        long duration = System.nanoTime() - start;

        assertNotNull(path);
        assertTrue(duration < 100_000_000, "Слишком долго: " + duration / 1_000_000 + " мс");
    }
}