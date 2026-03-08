package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class RobotModelTest {

    private RobotModel robotModel;

    @BeforeEach
    void setUp() {
        robotModel = new RobotModel();
    }

    @Test
    @DisplayName("Тест 1: Проверка начальных координат")
    void testInitialCoordinates() {
        assertEquals(100, robotModel.getRobotPositionX(), 0.001, "Начальная X должна быть 100");
        assertEquals(100, robotModel.getRobotPositionY(), 0.001, "Начальная Y должна быть 100");
        assertEquals(0, robotModel.getRobotDirection(), 0.001, "Начальное направление должно быть 0");
        assertEquals(150, robotModel.getTargetPositionX(), "Начальная цель X должна быть 150");
        assertEquals(100, robotModel.getTargetPositionY(), "Начальная цель Y должна быть 100");
    }

    @Test
    @DisplayName("Тест 2: Установка новой цели")
    void testSetTargetPosition() {
        robotModel.setTargetPosition(300, 400);

        assertEquals(300, robotModel.getTargetPositionX(), "Цель X должна быть 300");
        assertEquals(400, robotModel.getTargetPositionY(), "Цель Y должна быть 400");
    }

    @Test
    @DisplayName("Тест 3: PropertyChangeListener получает уведомление при изменении цели")
    void testPropertyChangeListenerOnTargetChange() {
        AtomicBoolean listenerNotified = new AtomicBoolean(false);
        AtomicInteger propertyCount = new AtomicInteger(0);

        PropertyChangeListener listener = evt -> {
            propertyCount.incrementAndGet();
            listenerNotified.set(true);

            assertEquals("targetPosition", evt.getPropertyName(),
                    "Имя свойства должно быть targetPosition");
            assertNotNull(evt.getNewValue(), "Новое значение не должно быть null");
        };

        robotModel.addPropertyChangeListener(listener);

        robotModel.setTargetPosition(250, 350);

        assertTrue(listenerNotified.get(), "Слушатель должен быть уведомлен");
        assertEquals(1, propertyCount.get(), "Должно быть ровно одно уведомление");
    }

    @Test
    @DisplayName("Тест 4: PropertyChangeListener получает уведомление при движении робота")
    void testPropertyChangeListenerOnRobotMove() throws InterruptedException {
        AtomicInteger notificationCount = new AtomicInteger(0);
        AtomicBoolean positionChanged = new AtomicBoolean(false);

        PropertyChangeListener listener = evt -> {
            if ("robotPosition".equals(evt.getPropertyName())) {
                notificationCount.incrementAndGet();
                positionChanged.set(true);

                Object newValue = evt.getNewValue();
                assertInstanceOf(double[].class, newValue, "Новое значение должно быть массивом double");

                double[] pos = (double[]) newValue;
                assertEquals(3, pos.length, "Массив должен содержать 3 элемента");
            }
        };

        robotModel.addPropertyChangeListener(listener);

        // Устанавливаем цель и делаем несколько шагов
        robotModel.setTargetPosition(200, 200);

        for (int i = 0; i < 10; i++) {
            robotModel.updateModel();
            Thread.sleep(5); // Небольшая задержка
        }

        assertTrue(positionChanged.get(), "Слушатель должен получить уведомление о движении");
        assertTrue(notificationCount.get() > 0, "Должно быть хотя бы одно уведомление");
    }

    @Test
    @DisplayName("Тест 5: Удаление PropertyChangeListener")
    void testRemovePropertyChangeListener() {
        AtomicInteger notificationCount = new AtomicInteger(0);

        PropertyChangeListener listener = evt -> notificationCount.incrementAndGet();

        robotModel.addPropertyChangeListener(listener);
        robotModel.removePropertyChangeListener(listener);

        robotModel.setTargetPosition(250, 350);
        robotModel.updateModel();

        assertEquals(0, notificationCount.get(),
                "После удаления слушатель не должен получать уведомления");
    }

    @Test
    @DisplayName("Тест 6: Проверка движения к цели")
    void testRobotMovement() {
        // Цель справа
        robotModel.setTargetPosition(200, 100);

        double startX = robotModel.getRobotPositionX();

        for (int i = 0; i < 100; i++) {
            robotModel.updateModel();
        }

        double newX = robotModel.getRobotPositionX();

        assertTrue(newX > startX, "Робот должен двигаться вправо (X должен увеличиться)");

        double initialDistance = Math.hypot(200 - startX, 100 - robotModel.getRobotPositionY());
        double newDistance = Math.hypot(200 - newX, 100 - robotModel.getRobotPositionY());

        assertTrue(newDistance < initialDistance, "Расстояние до цели должно уменьшиться");
    }

    @Test
    @DisplayName("Тест 7: Проверка границ окна")
    void testBoundaries() {
        robotModel.setTargetPosition(1000, 1000);

        for (int i = 0; i < 10000; i++) {
            robotModel.updateModel();
        }

        double x = robotModel.getRobotPositionX();
        double y = robotModel.getRobotPositionY();

        assertTrue(x >= 15 && x <= 785, "Робот не должен выходить за границы по X: " + x);
        assertTrue(y >= 15 && y <= 585, "Робот не должен выходить за границы по Y: " + y);
    }

    @Test
    @DisplayName("Тест 8: Множественные слушатели получают уведомления")
    void testMultipleListeners() {
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger count2 = new AtomicInteger(0);

        PropertyChangeListener listener1 = evt -> count1.incrementAndGet();
        PropertyChangeListener listener2 = evt -> count2.incrementAndGet();

        robotModel.addPropertyChangeListener(listener1);
        robotModel.addPropertyChangeListener(listener2);

        robotModel.setTargetPosition(250, 350);

        assertEquals(1, count1.get(), "Первый слушатель должен получить уведомление");
        assertEquals(1, count2.get(), "Второй слушатель должен получить уведомление");
    }
}