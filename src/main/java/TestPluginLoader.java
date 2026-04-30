// src/main/java/TestPluginLoader.java (временный файл)

import plugin.RobotPlugin;
import plugin.RobotPluginManager;
import java.io.File;

public class TestPluginLoader {
    public static void main(String[] args) {
        try {
            // Путь к вашему JAR
            File jarFile = new File("build/plugins/test_robot.jar");

            System.out.println("Проверяем файл: " + jarFile.getAbsolutePath());
            System.out.println("Файл существует: " + jarFile.exists());

            // Загружаем плагин
            RobotPluginManager manager = RobotPluginManager.getInstance();
            RobotPlugin plugin = manager.loadRobotFromJar(jarFile);

            System.out.println(" Плагин загружен!");
            System.out.println("   Тип: " + plugin.getRobotTypeId());
            System.out.println("   Название: " + plugin.getDisplayName());

        } catch (Exception e) {
            System.err.println(" Ошибка загрузки: " + e.getMessage());
            e.printStackTrace();
        }
    }
}