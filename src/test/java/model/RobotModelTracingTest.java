package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RobotModel - режим трассировки")
class RobotModelTracingTest {

    private RobotModel robot;

    @BeforeEach
    void setUp() {
        robot = new RobotModel();
        robot.setRobotId(1);
        robot.setPosition(100, 100);
    }

    @Test
    @DisplayName("По умолчанию робот не в режиме трассировки")
    void testDefaultNotTracing() {
        assertFalse(robot.isTracingMode());
    }

    @Test
    @DisplayName("Запуск трассировки с корректным маршрутом")
    void testStartTracing() {
        List<Waypoint> path = List.of(
                new Waypoint(200, 100),
                new Waypoint(200, 200),
                new Waypoint(100, 200)
        );

        robot.startTracing(path);

        assertTrue(robot.isTracingMode());
    }

    @Test
    @DisplayName("Запуск трассировки с null маршрутом не меняет режим")
    void testStartTracingNullPath() {
        robot.startTracing(null);
        assertFalse(robot.isTracingMode());
    }

    @Test
    @DisplayName("Запуск трассировки с пустым маршрутом не меняет режим")
    void testStartTracingEmptyPath() {
        robot.startTracing(List.of());
        assertFalse(robot.isTracingMode());
    }

    @Test
    @DisplayName("Остановка трассировки")
    void testStopTracing() {
        List<Waypoint> path = List.of(new Waypoint(200, 100));
        robot.startTracing(path);
        assertTrue(robot.isTracingMode());

        robot.stopTracing();
        assertFalse(robot.isTracingMode());
    }

    @Test
    @DisplayName("В режиме трассировки робот движется к первой точке")
    void testTracingMovesToFirstWaypoint() {
        // Цель далеко, чтобы точно сдвинулся
        List<Waypoint> path = List.of(new Waypoint(500, 500, 5.0));
        robot.startTracing(path);

        double oldX = robot.getRobotPositionX();
        double oldY = robot.getRobotPositionY();

        // Делаем несколько обновлений, чтобы робот точно начал движение
        for (int i = 0; i < 10; i++) {
            robot.updateModel();
        }

        assertNotEquals(oldX, robot.getRobotPositionX(), 0.1);
        assertNotEquals(oldY, robot.getRobotPositionY(), 0.1);
    }

    @Test
    @DisplayName("После достижения всех точек режим трассировки выключается")
    void testTracingStopsAfterPathComplete() {
        // Цель очень близко к старту
        List<Waypoint> path = List.of(
                new Waypoint(105, 100, 5.0)
        );
        robot.startTracing(path);

        for (int i = 0; i < 50; i++) {
            robot.updateModel();
        }

        assertFalse(robot.isTracingMode());
    }

    @Test
    @DisplayName("Установка цели выключает режим трассировки")
    void testSetTargetDisablesTracing() {
        List<Waypoint> path = List.of(new Waypoint(200, 100));
        robot.startTracing(path);
        assertTrue(robot.isTracingMode());

        robot.setTargetPosition(300, 300);

        assertFalse(robot.isTracingMode());
        assertEquals(300, robot.getTargetPositionX());
        assertEquals(300, robot.getTargetPositionY());
    }

    @Test
    @DisplayName("Трассировка: робот движется по маршруту (проверка смены позиции)")
    void testTracingMovesAlongPath() {
        List<Waypoint> path = List.of(
                new Waypoint(200, 100, 10.0),
                new Waypoint(200, 200, 10.0),
                new Waypoint(100, 200, 10.0)
        );

        robot.startTracing(path);

        double lastX = robot.getRobotPositionX();
        double lastY = robot.getRobotPositionY();
        boolean hasMoved = false;

        for (int i = 0; i < 100; i++) {
            robot.updateModel();
            double currentX = robot.getRobotPositionX();
            double currentY = robot.getRobotPositionY();

            if (Math.abs(currentX - lastX) > 0.1 || Math.abs(currentY - lastY) > 0.1) {
                hasMoved = true;
                break;
            }
            lastX = currentX;
            lastY = currentY;
        }

        assertTrue(hasMoved, "Робот должен двигаться в режиме трассировки");
    }
    @Test
    @DisplayName("Трассировка не влияет на другие поля робота")
    void testTracingDoesNotBreakOtherFields() {
        int robotId = robot.getRobotId();
        assertEquals("Робот 1", robot.getRobotName());

        List<Waypoint> path = List.of(new Waypoint(200, 100));
        robot.startTracing(path);

        assertEquals(robotId, robot.getRobotId());
        assertEquals("Робот 1", robot.getRobotName());
    }
}