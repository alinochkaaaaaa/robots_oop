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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class TracingController {

    private final MainApplicationFrame mainFrame;
    private final MultiRobotModel multiRobotModel;
    private final GameWindow gameWindow;

    private BufferedImage loadedImage;
    private List<Point> lastFoundContour;
    private List<Waypoint> lastBuiltPath;
    private String lastLoadedFileName;

    private static final String SAVES_DIR = "saves";
    private static final String PATH_FILE = SAVES_DIR + "/last_path.json";

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

            // размеры контура
            int minX = lastFoundContour.stream().mapToInt(p -> p.x).min().orElse(0);
            int maxX = lastFoundContour.stream().mapToInt(p -> p.x).max().orElse(0);
            int minY = lastFoundContour.stream().mapToInt(p -> p.y).min().orElse(0);
            int maxY = lastFoundContour.stream().mapToInt(p -> p.y).max().orElse(0);
            int contourWidth = maxX - minX;
            int contourHeight = maxY - minY;
            System.out.println("Размеры контура: " + contourWidth + "x" + contourHeight + " пикселей");

            // целевые размеры до 80% поля
            int targetWidth = (int) (fieldWidth * 0.8);
            int targetHeight = (int) (fieldHeight * 0.8);
            int offsetX = fieldWidth / 2;
            int offsetY = fieldHeight / 2;


            List<Point> normalizedContour = ImageProcessor.normalizeContour(
                    lastFoundContour, targetWidth, targetHeight, offsetX, offsetY);

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

    public void onSavePath() {
        if (lastBuiltPath == null || lastBuiltPath.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Нет маршрута для сохранения.", "Ошибка", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            File savesDir = new File(SAVES_DIR);
            if (!savesDir.exists()) {
                savesDir.mkdir();
            }

            File file = new File(PATH_FILE);
            FileWriter writer = new FileWriter(file);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(lastBuiltPath, writer);
            writer.close();

            mainFrame.setStatusMessage("Маршрут сохранён: " + file.getAbsolutePath());
            JOptionPane.showMessageDialog(mainFrame, "Маршрут успешно сохранён в:\n" + file.getAbsolutePath(), "Сохранено", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            mainFrame.setStatusMessage("Ошибка сохранения: " + e.getMessage());
            JOptionPane.showMessageDialog(mainFrame, "Ошибка сохранения:\n" + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void onLoadPath() {
        try {
            File file = new File(PATH_FILE);
            if (!file.exists()) {
                JOptionPane.showMessageDialog(mainFrame, "Файл маршрута не найден.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            FileReader reader = new FileReader(file);
            Gson gson = new Gson();
            java.lang.reflect.Type type = new TypeToken<List<Waypoint>>(){}.getType();
            List<Waypoint> path = gson.fromJson(reader, type);
            reader.close();

            if (path == null || path.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Маршрут пуст.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            this.lastBuiltPath = new ArrayList<>(path);
            mainFrame.setStatusMessage("Маршрут загружен: " + path.size() + " точек");
            JOptionPane.showMessageDialog(mainFrame, "Маршрут успешно загружен из:\n" + file.getAbsolutePath() + "\nТочек: " + path.size(), "Загружено", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            mainFrame.setStatusMessage("Ошибка загрузки: " + e.getMessage());
            JOptionPane.showMessageDialog(mainFrame, "Ошибка загрузки:\n" + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public List<Waypoint> getLastBuiltPath() {
        return lastBuiltPath;
    }
}