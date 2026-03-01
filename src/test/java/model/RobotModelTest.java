package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RobotModelTest {

    private RobotModel robotModel;

    @BeforeEach
    void setUp() {
        // Создаем новую модель перед каждым тестом
        robotModel = new RobotModel();
    }

    @Test
    @DisplayName("Тест 1: Проверка начальных координат")
    void testInitialCoordinates() {
        assertEquals(100, robotModel.getRobotPositionX(), 0.001,
                "Начальная координата X должна быть 100");
        assertEquals(100, robotModel.getRobotPositionY(), 0.001,
                "Начальная координата Y должна быть 100");
        assertEquals(0, robotModel.getRobotDirection(), 0.001,
                "Начальное направление должно быть 0");
        assertEquals(150, robotModel.getTargetPositionX(),
                "Начальная цель X должна быть 150");
        assertEquals(100, robotModel.getTargetPositionY(),
                "Начальная цель Y должна быть 100");
    }

    @Test
    @DisplayName("Тест 2: Проверка установки новой цели")
    void testSetTargetPosition() {
        // Устанавливаем новую цель
        robotModel.setTargetPosition(300, 400);

        assertEquals(300, robotModel.getTargetPositionX(),
                "Цель X должна быть 300");
        assertEquals(400, robotModel.getTargetPositionY(),
                "Цель Y должна быть 400");

        // Проверяем, что позиция робота не изменилась
        assertEquals(100, robotModel.getRobotPositionX(), 0.001,
                "Позиция робота X не должна измениться");
        assertEquals(100, robotModel.getRobotPositionY(), 0.001,
                "Позиция робота Y не должна измениться");
    }

    @Test
    @DisplayName("Тест 3: Проверка движения робота к цели")
    void testRobotMovement() {
        // Устанавливаем цель справа от робота
        robotModel.setTargetPosition(200, 100);

        // Запоминаем начальную позицию
        double startX = robotModel.getRobotPositionX();
        double startY = robotModel.getRobotPositionY();

        // Обновляем модель несколько раз
        for (int i = 0; i < 100; i++) {
            robotModel.updateModel();
        }

        double newX = robotModel.getRobotPositionX();
        double newY = robotModel.getRobotPositionY();

        // Проверяем, что робот действительно двигался
        assertTrue(newX > startX, "Робот должен двигаться вправо (X должен увеличиться)");
        assertEquals(startY, newY, 5.0, "Y координата должна измениться незначительно");

        // Проверяем, что робот приблизился к цели
        double initialDistance = Math.sqrt(Math.pow(200 - startX, 2) + Math.pow(100 - startY, 2));
        double newDistance = Math.sqrt(Math.pow(200 - newX, 2) + Math.pow(100 - newY, 2));

        assertTrue(newDistance < initialDistance,
                "Расстояние до цели должно уменьшиться");
    }

    @Test
    @DisplayName("Тест 4: Проверка остановки у цели")
    void testRobotStopsAtTarget() {
        // Устанавливаем цель рядом с роботом
        robotModel.setTargetPosition(105, 100);  // Цель в 5 пикселях справа

        // Обновляем модель много раз
        for (int i = 0; i < 1000; i++) {
            robotModel.updateModel();
        }

        double finalX = robotModel.getRobotPositionX();
        double finalY = robotModel.getRobotPositionY();
        double distanceToTarget = Math.sqrt(
                Math.pow(robotModel.getTargetPositionX() - finalX, 2) +
                        Math.pow(robotModel.getTargetPositionY() - finalY, 2)
        );

        // Проверяем, что робот остановился рядом с целью
        assertTrue(distanceToTarget < 2.0,
                "Робот должен остановиться рядом с целью. Расстояние: " + distanceToTarget);

        // Проверяем, что робот перестал двигаться
        double posXBefore = robotModel.getRobotPositionX();
        double posYBefore = robotModel.getRobotPositionY();

        robotModel.updateModel();

        assertEquals(posXBefore, robotModel.getRobotPositionX(), 0.001,
                "После достижения цели робот не должен двигаться по X");
        assertEquals(posYBefore, robotModel.getRobotPositionY(), 0.001,
                "После достижения цели робот не должен двигаться по Y");
    }

    @Test
    @DisplayName("Тест 5: Проверка границ окна")
    void testBoundaries() {
        // Пытаемся отправить робота за границы
        robotModel.setTargetPosition(1000, 1000);

        // Много раз обновляем модель
        for (int i = 0; i < 10000; i++) {
            robotModel.updateModel();
        }

        double x = robotModel.getRobotPositionX();
        double y = robotModel.getRobotPositionY();

        // Проверяем, что робот не вышел за границы
        assertTrue(x >= 15 && x <= 785,
                "Робот не должен выходить за границы по X: " + x);
        assertTrue(y >= 15 && y <= 585,
                "Робот не должен выходить за границы по Y: " + y);
    }

    @Test
    @DisplayName("Тест 6: Проверка работы слушателей")
    void testListeners() {
        AtomicInteger notificationCount = new AtomicInteger(0);
        AtomicBoolean listenerNotified = new AtomicBoolean(false);

        // Создаем и добавляем слушателя
        RobotModel.RobotModelListener listener = (x, y, direction) -> {
            notificationCount.incrementAndGet();
            listenerNotified.set(true);
        };

        robotModel.addListener(listener);

        // Устанавливаем цель и обновляем модель
        robotModel.setTargetPosition(200, 200);
        robotModel.updateModel();

        // Проверяем, что слушатель был уведомлен
        assertTrue(listenerNotified.get(), "Слушатель должен быть уведомлен о движении");
        assertTrue(notificationCount.get() > 0, "Слушатель должен получить уведомление");

        // Удаляем слушателя
        robotModel.removeListener(listener);
        listenerNotified.set(false);

        // Снова обновляем модель
        robotModel.updateModel();

        // Проверяем, что слушатель больше не получает уведомления
        assertFalse(listenerNotified.get(),
                "После удаления слушатель не должен получать уведомления");
    }

    @RepeatedTest(3)
    @DisplayName("Тест 7: Проверка движения к случайной цели")
    void testRandomTarget() {
        // Генерируем случайную цель в пределах окна
        int targetX = 50 + (int)(Math.random() * 700);
        int targetY = 50 + (int)(Math.random() * 500);

        robotModel.setTargetPosition(targetX, targetY);

        // Запоминаем начальную позицию
        double startX = robotModel.getRobotPositionX();
        double startY = robotModel.getRobotPositionY();

        // Обновляем модель, пока не достигнем цели или не сделаем много шагов
        int maxSteps = 5000;
        int steps = 0;
        double distanceToTarget;

        do {
            robotModel.updateModel();
            steps++;

            distanceToTarget = Math.sqrt(
                    Math.pow(targetX - robotModel.getRobotPositionX(), 2) +
                            Math.pow(targetY - robotModel.getRobotPositionY(), 2)
            );

        } while (distanceToTarget > 2.0 && steps < maxSteps);

        // Проверяем, что робот достиг цели
        assertTrue(distanceToTarget <= 2.0,
                String.format("Робот должен достичь цели (%d, %d) за %d шагов. " +
                                "Финальная позиция: (%.2f, %.2f), расстояние: %.2f",
                        targetX, targetY, steps,
                        robotModel.getRobotPositionX(),
                        robotModel.getRobotPositionY(),
                        distanceToTarget));

        // Проверяем, что робот действительно двигался к цели
        double finalX = robotModel.getRobotPositionX();
        double finalY = robotModel.getRobotPositionY();

        // Проверяем, что финальная позиция ближе к цели, чем начальная
        double initialDistance = Math.sqrt(
                Math.pow(targetX - startX, 2) + Math.pow(targetY - startY, 2)
        );

        assertTrue(distanceToTarget < initialDistance,
                "Робот должен приблизиться к цели");
    }
}