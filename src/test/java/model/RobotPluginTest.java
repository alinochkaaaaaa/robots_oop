package model;

import org.junit.jupiter.api.*;
import plugin.BuiltinRobotPlugin;
import plugin.RobotInstance;
import plugin.RobotPlugin;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;

public class RobotPluginTest {

    // ==================== RobotModelWrapper ТЕСТЫ ====================

    @Test
    @DisplayName("RobotModelWrapper: делегирование позиции")
    void testWrapperPosition() {
        MockRobotInstance instance = new MockRobotInstance(100, 150);
        RobotModelWrapper wrapper = new RobotModelWrapper(instance, "test", 1);

        assertEquals(100, wrapper.getRobotPositionX(), 0.001);
        assertEquals(150, wrapper.getRobotPositionY(), 0.001);
    }

    @Test
    @DisplayName("RobotModelWrapper: установка цели")
    void testWrapperTarget() {
        MockRobotInstance instance = new MockRobotInstance(0, 0);
        RobotModelWrapper wrapper = new RobotModelWrapper(instance, "test", 1);

        wrapper.setTargetPosition(200, 350);

        assertEquals(200, wrapper.getTargetPositionX());
        assertEquals(350, wrapper.getTargetPositionY());
    }

    @Test
    @DisplayName("RobotModelWrapper: updateModel вызывает instance.update()")
    void testWrapperUpdate() {
        MockRobotInstance instance = new MockRobotInstance(0, 0);
        RobotModelWrapper wrapper = new RobotModelWrapper(instance, "test", 1);

        wrapper.setTargetPosition(500, 600);
        wrapper.updateModel();

        assertTrue(instance.updateCalled);
        assertEquals(500, instance.lastTargetX);
        assertEquals(600, instance.lastTargetY);
    }

    @Test
    @DisplayName("RobotModelWrapper: ID и тип плагина")
    void testWrapperIdentity() {
        MockRobotInstance instance = new MockRobotInstance(0, 0);
        RobotModelWrapper wrapper = new RobotModelWrapper(instance, "super_robot", 99);

        assertEquals(99, wrapper.getRobotId());
        assertEquals("super_robot", wrapper.getPluginTypeId());
    }

    // ==================== BuiltinRobotPlugin ТЕСТЫ ====================

    @Test
    @DisplayName("BuiltinRobotPlugin: тип и название")
    void testBuiltinPluginBasics() {
        BuiltinRobotPlugin plugin = new BuiltinRobotPlugin();

        assertEquals("builtin", plugin.getRobotTypeId());
        assertEquals("Стандартный робот", plugin.getDisplayName());
    }

    @Test
    @DisplayName("BuiltinRobotPlugin: создание экземпляра и движение")
    void testBuiltinInstanceMovement() {
        BuiltinRobotPlugin plugin = new BuiltinRobotPlugin();
        RobotInstance instance = plugin.createInstance(0, 0);

        instance.update(100, 100);

        // Должен начать движение (координаты изменились)
        assertNotEquals(0, instance.getX());
        assertNotEquals(0, instance.getY());
    }

    // ==================== MultiRobotModel ТЕСТЫ ====================

    @Test
    @DisplayName("MultiRobotModel: добавление робота из плагина")
    void testAddPluginRobot() {
        MultiRobotModel model = new MultiRobotModel();
        int before = model.getRobotCount();
        TestPlugin testPlugin = new TestPlugin();

        RobotModel newRobot = model.addRobotFromPlugin(testPlugin);

        assertEquals(before + 1, model.getRobotCount());
        assertNotNull(newRobot);
    }

    @Test
    @DisplayName("MultiRobotModel: добавление робота с указанием позиции")
    void testAddPluginRobotWithPosition() {
        MultiRobotModel model = new MultiRobotModel();
        TestPlugin testPlugin = new TestPlugin();

        RobotModel newRobot = model.addRobotFromPlugin(testPlugin, 300, 400);

        assertEquals(300, newRobot.getRobotPositionX(), 0.001);
        assertEquals(400, newRobot.getRobotPositionY(), 0.001);
    }

    @Test
    @DisplayName("MultiRobotModel: удаление робота")
    void testRemovePluginRobot() {
        MultiRobotModel model = new MultiRobotModel();
        TestPlugin testPlugin = new TestPlugin();

        RobotModel pluginRobot = model.addRobotFromPlugin(testPlugin);
        int afterAdd = model.getRobotCount();

        model.removeRobot(pluginRobot);

        assertEquals(afterAdd - 1, model.getRobotCount());
    }

    @Test
    @DisplayName("MultiRobotModel: совместимость стандартных и плагин-роботов")
    void testMixedRobotsCompatibility() {
        MultiRobotModel model = new MultiRobotModel();
        TestPlugin testPlugin = new TestPlugin();

        RobotModel standard = model.getRobots().get(0);
        RobotModel pluginRobot = model.addRobotFromPlugin(testPlugin);

        assertFalse(standard instanceof RobotModelWrapper);
        assertTrue(pluginRobot instanceof RobotModelWrapper);
        assertEquals(2, model.getRobotCount());
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ ====================

    private static class MockRobotInstance implements RobotInstance {
        private double x, y;
        boolean updateCalled = false;
        double lastTargetX, lastTargetY;

        MockRobotInstance(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void update(double targetX, double targetY) {
            updateCalled = true;
            lastTargetX = targetX;
            lastTargetY = targetY;
            this.x = targetX;
            this.y = targetY;
        }

        @Override
        public void draw(Graphics2D g, int x, int y, boolean isSelected) {}

        @Override
        public double getX() { return x; }

        @Override
        public double getY() { return y; }

        @Override
        public double getDirection() { return 0; }

        @Override
        public void setPosition(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class TestPlugin implements RobotPlugin {
        @Override
        public String getRobotTypeId() { return "test_plugin"; }

        @Override
        public String getDisplayName() { return "Тестовый плагин"; }

        @Override
        public RobotInstance createInstance(double x, double y) {
            return new TestRobotInstance(x, y);
        }
    }

    private static class TestRobotInstance implements RobotInstance {
        private double x, y;

        TestRobotInstance(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void update(double targetX, double targetY) {
            this.x = targetX;
            this.y = targetY;
        }

        @Override
        public void draw(Graphics2D g, int x, int y, boolean isSelected) {}

        @Override
        public double getX() { return x; }

        @Override
        public double getY() { return y; }

        @Override
        public double getDirection() { return 0; }

        @Override
        public void setPosition(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}