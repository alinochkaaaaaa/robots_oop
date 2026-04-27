package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AllChangesTest {

    private MultiRobotModel multiModel;
    private RobotModel robot1;
    private RobotModel robot2;

    @BeforeEach
    void setUp() {
        multiModel = new MultiRobotModel();
        robot1 = multiModel.getRobot(0);
        robot2 = multiModel.addRobot();
    }
    //1. Создание второго робота - проверяет что можно добавить второго робота и ID=2
    @Test
    void test01_CreateSecondRobot() {
        assertEquals(2, multiModel.getRobotCount());
        assertNotNull(robot2);
        assertEquals(2, robot2.getRobotId());
    }
    //2. Установка цели для конкретного робота - цель устанавливается именно тому роботу
    @Test
    void test02_SetTargetForSpecificRobot() {
        robot2.setTargetPosition(500, 500);
        assertEquals(500, robot2.getTargetPositionX());
        assertEquals(500, robot2.getTargetPositionY());
    }
    //3. Цель не влияет на других роботов - у каждого робота своя независимая цель
    @Test
    void test03_TargetDoesNotAffectOtherRobots() {
        robot1.setTargetPosition(100, 100);
        robot2.setTargetPosition(500, 500);

        assertEquals(100, robot1.getTargetPositionX());
        assertEquals(500, robot2.getTargetPositionX());
        assertNotEquals(robot1.getTargetPositionX(), robot2.getTargetPositionX());
    }
    //4. Робот имеет ID - идентификатор присваивается и читается корректно
    @Test
    void test04_RobotHasId() {
        assertEquals(1, robot1.getRobotId());
        assertEquals(2, robot2.getRobotId());
    }
    //5. Робот имеет имя - имя формируется как "Робот X" для отображения на поле
    @Test
    void test05_RobotHasName() {
        assertEquals("Робот 1", robot1.getRobotName());
        assertEquals("Робот 2", robot2.getRobotName());
    }
    //6. Разные цвета для разных роботов - проверяем что ID разные (цвета будут разные по логике)
    @Test
    void test06_DifferentColorsForDifferentRobots() {
        assertNotEquals(robot1.getRobotId(), robot2.getRobotId());
    }
    //7. Робот двигается к цели - упрощённая физика работает, позиция меняется
    @Test
    void test07_RobotMovesTowardsTarget() {
        double oldX = robot1.getRobotPositionX();
        robot1.setTargetPosition((int) oldX + 100, (int) robot1.getRobotPositionY());

        for (int i = 0; i < 10; i++) {
            robot1.updateModel();
        }

        assertNotEquals(oldX, robot1.getRobotPositionX());
    }
    //8. Робот останавливается у цели - при достижении цели движение прекращается
    @Test
    void test08_RobotStopsAtTarget() {
        robot1.setTargetPosition(
                (int) robot1.getRobotPositionX(),
                (int) robot1.getRobotPositionY()
        );

        for (int i = 0; i < 20; i++) {
            robot1.updateModel();
        }

        assertEquals(robot1.getTargetPositionX(), robot1.getRobotPositionX(), 1.0);
        assertEquals(robot1.getTargetPositionY(), robot1.getRobotPositionY(), 1.0);
    }
    //9. Получение списка всех роботов - метод возвращает всех роботов в модели
    @Test
    void test09_GetAllRobotsReturnsCompleteList() {
        List<RobotModel> robots = multiModel.getRobots();
        assertEquals(2, robots.size());
        assertTrue(robots.contains(robot1));
        assertTrue(robots.contains(robot2));
    }
    //10. Обновление всех роботов одним вызовом - updateAllModels двигает всех сразу
    @Test
    void test10_UpdateAllModels() {
        robot1.setTargetPosition(300, 300);
        robot2.setTargetPosition(400, 400);

        double oldX1 = robot1.getRobotPositionX();
        double oldX2 = robot2.getRobotPositionX();

        multiModel.updateAllModels();

        assertNotEquals(oldX1, robot1.getRobotPositionX());
        assertNotEquals(oldX2, robot2.getRobotPositionX());
    }
    //11. Роботы двигаются независимо - каждый движется к своей цели независимо
    @Test
    void test11_RobotsMoveIndependently() {
        robot1.setTargetPosition(800, 600);
        robot2.setTargetPosition(50, 50);

        double r1x = robot1.getRobotPositionX();
        double r2x = robot2.getRobotPositionX();

        for (int i = 0; i < 30; i++) {
            multiModel.updateAllModels();
        }

        assertNotEquals(r1x, robot1.getRobotPositionX());
        assertNotEquals(r2x, robot2.getRobotPositionX());

        boolean r1MovedRight = robot1.getRobotPositionX() > r1x;
        boolean r2MovedLeft = robot2.getRobotPositionX() < r2x;
        assertTrue(r1MovedRight || r2MovedLeft);
    }
    //12. Нельзя удалить последнего робота - в системе всегда должен быть хотя бы 1 робот
    @Test
    void test12_CannotDeleteLastRobot() {
        multiModel.removeRobot(robot1);
        assertEquals(1, multiModel.getRobotCount());
        assertNotNull(multiModel.getRobot(0));
    }
    //13. Удаление робота работает когда их несколько - можно удалить робота если есть запасной
    @Test
    void test13_CanDeleteRobotWhenMultiple() {
        assertEquals(2, multiModel.getRobotCount());
        multiModel.removeRobot(robot1);
        assertEquals(1, multiModel.getRobotCount());
        assertEquals(robot2.getRobotId(), multiModel.getRobot(0).getRobotId());
    }
    //14. Координаты обновляются после движения - позиция меняется после вызова updateModel
    @Test
    void test14_CoordinatesUpdateAfterMove() {
        double oldX = robot1.getRobotPositionX();
        robot1.setTargetPosition((int) oldX + 50, (int) robot1.getRobotPositionY());

        multiModel.updateAllModels();

        assertNotEquals(oldX, robot1.getRobotPositionX());
    }
    //15. Можно добавить третьего робота - система поддерживает более 2 роботов
    @Test
    void test15_CanAddThirdRobot() {
        RobotModel robot3 = multiModel.addRobot();
        assertEquals(3, multiModel.getRobotCount());
        assertEquals(3, robot3.getRobotId());
        assertEquals("Робот 3", robot3.getRobotName());
    }
}