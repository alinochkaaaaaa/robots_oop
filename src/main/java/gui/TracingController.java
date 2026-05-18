package gui;

import image.ImageLoader;
import image.ImageProcessor;
import model.MultiRobotModel;
import model.PathBuilder;
import model.Waypoint;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class TracingController {

    private final MainApplicationFrame mainFrame;
    private final MultiRobotModel multiRobotModel;
    private final GameWindow gameWindow;

    private BufferedImage loadedImage;
    private List<Point> lastFoundContour;
    private List<Waypoint> lastBuiltPath;
    private String lastLoadedFileName;

    public TracingController(MainApplicationFrame mainFrame, MultiRobotModel multiRobotModel, GameWindow gameWindow) {
        this.mainFrame = mainFrame;
        this.multiRobotModel = multiRobotModel;
        this.gameWindow = gameWindow;
    }

    public void onLoadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите изображение для трассировки");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                ImageLoader.getFormatDescription(),
                ImageLoader.getSupportedFormats()
        ));

        int result = fileChooser.showOpenDialog(mainFrame);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File selectedFile = fileChooser.getSelectedFile();

        try {
            mainFrame.setStatusMessage("Загрузка изображения: " + selectedFile.getName());

            loadedImage = ImageLoader.loadImage(selectedFile);
            lastLoadedFileName = selectedFile.getName();

            mainFrame.setStatusMessage("Обработка изображения...");

            ImageProcessor processor = new ImageProcessor();
            lastFoundContour = processor.findLargestObjectContour(loadedImage);

            if (lastFoundContour == null || lastFoundContour.isEmpty()) {
                mainFrame.setStatusMessage("Ошибка: объект не найден на изображении");
                JOptionPane.showMessageDialog(mainFrame,
                        "Не удалось найти объект на изображении.\nПопробуйте другое изображение.",
                        "Объект не найден", JOptionPane.ERROR_MESSAGE);
                return;
            }

            GameVisualizer visualizer = gameWindow.getGameVisualizer();
            int fieldWidth = visualizer.getWidth() > 0 ? visualizer.getWidth() : 600;
            int fieldHeight = visualizer.getHeight() > 0 ? visualizer.getHeight() : 500;

            List<Point> normalizedContour = ImageProcessor.normalizeContour(
                    lastFoundContour, fieldWidth - 100, fieldHeight - 150, 50, 60);

            PathBuilder pathBuilder = new PathBuilder();
            lastBuiltPath = pathBuilder.buildPathFromContour(normalizedContour, 40.0);

            mainFrame.setStatusMessage(String.format("Загружено: %s | Маршрут: %d точек",
                    lastLoadedFileName, lastBuiltPath.size()));

            String message = String.format(
                    "Изображение загружено успешно!\n" +
                            "Файл: %s\n" +
                            "Контур: %d точек\n" +
                            "Маршрут: %d точек\n\n" +
                            "Нажмите 'Начать обводку' для запуска.",
                    lastLoadedFileName, lastFoundContour.size(), lastBuiltPath.size());

            JOptionPane.showMessageDialog(mainFrame, message, "Успех", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            mainFrame.setStatusMessage("Ошибка загрузки: " + e.getMessage());
            JOptionPane.showMessageDialog(mainFrame, "Ошибка загрузки:\n" + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            mainFrame.setStatusMessage("Ошибка обработки: " + e.getMessage());
            JOptionPane.showMessageDialog(mainFrame, "Ошибка обработки:\n" + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void onStartTracing() {
        if (lastBuiltPath == null || lastBuiltPath.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(mainFrame,
                    "Маршрут не построен. Загрузить изображение сейчас?",
                    "Нет маршрута", JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                onLoadImage();
            }
            return;
        }

        GameVisualizer visualizer = gameWindow.getGameVisualizer();
        if (visualizer == null) {
            JOptionPane.showMessageDialog(mainFrame, "Игровое поле недоступно", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (multiRobotModel.getRobotCount() == 0) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Нет роботов. Добавьте робота через меню Файл → Добавить робота",
                    "Нет роботов", JOptionPane.WARNING_MESSAGE);
            return;
        }

        visualizer.startTracingOnSelectedRobot(lastBuiltPath);
        mainFrame.setStatusMessage("Трассировка запущена");
    }

    public void onStopTracing() {
        GameVisualizer visualizer = gameWindow.getGameVisualizer();
        if (visualizer != null && visualizer.isSelectedRobotTracing()) {
            visualizer.stopTracingOnSelectedRobot();
            mainFrame.setStatusMessage("Трассировка остановлена");
        }
    }

    public void onClearTrail() {
        GameVisualizer visualizer = gameWindow.getGameVisualizer();
        if (visualizer != null) {
            visualizer.clearTrail();
            mainFrame.setStatusMessage("След очищен");
        }
    }

    public void onShowImageInfo() {
        if (lastFoundContour == null || lastFoundContour.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Нет загруженного изображения.\nСначала загрузите изображение.",
                    "Нет данных", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        double area = ImageProcessor.calculateArea(lastFoundContour);
        String info = String.format(
                "Файл: %s\n" +
                        "Контур: %d точек\n" +
                        "Площадь: %.1f px²\n" +
                        "Маршрут: %d точек",
                lastLoadedFileName != null ? lastLoadedFileName : "неизвестно",
                lastFoundContour.size(),
                area,
                lastBuiltPath != null ? lastBuiltPath.size() : 0);

        JOptionPane.showMessageDialog(mainFrame, info, "Информация об объекте", JOptionPane.INFORMATION_MESSAGE);
    }

    // Геттеры для обновления статус-бара
    public String getLastLoadedFileName() {
        return lastLoadedFileName;
    }

    public List<Point> getLastFoundContour() {
        return lastFoundContour;
    }

    public List<Waypoint> getLastBuiltPath() {
        return lastBuiltPath;
    }
}