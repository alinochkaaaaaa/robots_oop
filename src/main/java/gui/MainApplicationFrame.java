package gui;

import image.ImageLoader;
import image.ImageProcessor;
import model.MultiRobotModel;
import model.PathBuilder;
import model.Waypoint;
import plugin.RobotPlugin;
import plugin.RobotPluginManager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import log.Logger;

public class MainApplicationFrame extends JFrame
{
    private final JDesktopPane desktopPane = new JDesktopPane();
    private final MultiRobotModel multiRobotModel = new MultiRobotModel();
    private RobotCoordinatesWindow coordinatesWindow;
    private LogWindow logWindow;
    private GameWindow gameWindow;

    // Поля для трассировки
    private BufferedImage loadedImage;
    private List<Point> lastFoundContour;
    private List<Waypoint> lastBuiltPath;
    private String lastLoadedFileName = null;

    // Компоненты статус-бара
    private JPanel statusBar;
    private JLabel statusLeftLabel;
    private JLabel statusRightLabel;

    private static final String PLUGIN_CONFIG_FILE = System.getProperty("user.home") +
            File.separator + ".robots_plugins.xml";

    public MainApplicationFrame() {
        configureMainFrame();
        createWindows();
        createStatusBar();
        setJMenuBar(createMenuBar());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        setupExitHandler();
        loadPluginConfiguration();

        // Загружаем позиции окон только если они были сохранены
        // Иначе используем позиции по умолчанию
        boolean hasSavedPositions = loadWindowPositionsIfExists();

        if (!hasSavedPositions) {
            // Устанавливаем позиции по умолчанию
            setDefaultWindowPositions();
        }

        startStatusUpdater();
        setupHotkeys();

        // Принудительно показываем все окна
        SwingUtilities.invokeLater(() -> {
            ensureAllWindowsVisible();
        });
    }

    private void ensureAllWindowsVisible() {
        // Проверяем и восстанавливаем каждое окно
        if (logWindow == null || !logWindow.isVisible() || !isWindowInDesktopPane(logWindow)) {
            showLogWindow();
        }
        if (gameWindow == null || !gameWindow.isVisible() || !isWindowInDesktopPane(gameWindow)) {
            showGameWindow();
        }
        if (coordinatesWindow == null || !coordinatesWindow.isVisible() || !isWindowInDesktopPane(coordinatesWindow)) {
            showCoordinatesWindow();
        }

        // Игровое поле на передний план
        if (gameWindow != null) {
            gameWindow.toFront();
        }
    }

    private void setDefaultWindowPositions() {
        if (logWindow != null) {
            logWindow.setLocation(10, 10);
            logWindow.setSize(300, 800);
        }
        if (gameWindow != null) {
            gameWindow.setLocation(320, 10);
            gameWindow.setSize(600, 500);
        }
        if (coordinatesWindow != null) {
            coordinatesWindow.setLocation(930, 10);
            coordinatesWindow.setSize(300, 400);
        }
    }

    private void setupHotkeys() {
        javax.swing.Action showAllAction = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                restoreAllWindows();
            }
        };

        getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(
                javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "showAllWindows");
        getRootPane().getActionMap().put("showAllWindows", showAllAction);
    }

    private void restoreAllWindows() {
        showLogWindow();
        showGameWindow();
        showCoordinatesWindow();
        setStatusMessage("Все окна восстановлены");
    }

    private void showLogWindow() {
        if (logWindow != null) {
            if (isWindowInDesktopPane(logWindow)) {
                desktopPane.remove(logWindow);
            }
            logWindow.dispose();
        }
        logWindow = createLogWindow();
        desktopPane.add(logWindow);
        logWindow.setVisible(true);
        try {
            logWindow.setIcon(false);
        } catch (Exception ex) {}
        logWindow.toFront();
    }

    private void showGameWindow() {
        if (gameWindow != null) {
            if (isWindowInDesktopPane(gameWindow)) {
                desktopPane.remove(gameWindow);
            }
            gameWindow.dispose();
        }
        gameWindow = createGameWindow();
        desktopPane.add(gameWindow);
        gameWindow.setVisible(true);
        try {
            gameWindow.setIcon(false);
        } catch (Exception ex) {}
        gameWindow.toFront();
    }

    private void showCoordinatesWindow() {
        if (coordinatesWindow != null) {
            if (isWindowInDesktopPane(coordinatesWindow)) {
                desktopPane.remove(coordinatesWindow);
            }
            coordinatesWindow.dispose();
        }
        coordinatesWindow = createCoordinatesWindow();
        desktopPane.add(coordinatesWindow);
        coordinatesWindow.setVisible(true);
        try {
            coordinatesWindow.setIcon(false);
        } catch (Exception ex) {}
        coordinatesWindow.toFront();
    }

    private boolean isWindowInDesktopPane(JInternalFrame window) {
        for (JInternalFrame frame : desktopPane.getAllFrames()) {
            if (frame == window) {
                return true;
            }
        }
        return false;
    }

    private void configureMainFrame() {
        int inset = 50;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(inset, inset,
                screenSize.width  - inset*2,
                screenSize.height - inset*2);

        setLayout(new BorderLayout());
        add(desktopPane, BorderLayout.CENTER);
    }

    private void createWindows() {
        logWindow = createLogWindow();
        gameWindow = createGameWindow();
        coordinatesWindow = createCoordinatesWindow();

        desktopPane.add(logWindow);
        desktopPane.add(gameWindow);
        desktopPane.add(coordinatesWindow);

        logWindow.setVisible(true);
        gameWindow.setVisible(true);
        coordinatesWindow.setVisible(true);
    }

    private LogWindow createLogWindow() {
        LogWindow logWindow = new LogWindow(Logger.getDefaultLogSource());
        logWindow.setResizable(true);
        logWindow.setClosable(true);
        logWindow.setIconifiable(true);
        logWindow.setMaximizable(true);
        return logWindow;
    }

    private GameWindow createGameWindow() {
        GameWindow gameWindow = new GameWindow(multiRobotModel);
        gameWindow.setDoubleBuffered(true);
        return gameWindow;
    }

    private RobotCoordinatesWindow createCoordinatesWindow() {
        RobotCoordinatesWindow coordinatesWindow = new RobotCoordinatesWindow(multiRobotModel);
        return coordinatesWindow;
    }

    private void createStatusBar() {
        statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        statusBar.setPreferredSize(new Dimension(getWidth(), 24));

        statusLeftLabel = new JLabel(" Готов");
        statusLeftLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusLeftLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        statusRightLabel = new JLabel("Роботов: 0");
        statusRightLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusRightLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

        statusBar.add(statusLeftLabel, BorderLayout.WEST);
        statusBar.add(statusRightLabel, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);
    }

    private void updateStatusBar() {
        StringBuilder leftText = new StringBuilder(" ");

        if (lastLoadedFileName != null) {
            leftText.append("Загружено: ").append(lastLoadedFileName);
            if (lastFoundContour != null && !lastFoundContour.isEmpty()) {
                leftText.append(" | Найден объект: ").append(lastFoundContour.size()).append(" точек");
            }
        } else {
            leftText.append("Готов");
        }

        statusLeftLabel.setText(leftText.toString());

        StringBuilder rightText = new StringBuilder();
        rightText.append("Роботов: ").append(multiRobotModel.getRobotCount());

        GameVisualizer visualizer = gameWindow != null ? gameWindow.getGameVisualizer() : null;
        if (visualizer != null && visualizer.isSelectedRobotTracing()) {
            rightText.append(" | Трассировка: АКТИВНА");
        }

        statusRightLabel.setText(rightText.toString());
    }

    private void startStatusUpdater() {
        javax.swing.Timer timer = new javax.swing.Timer(500, e -> {
            updateStatusBar();
        });
        timer.start();
    }

    private void setupExitHandler() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createTraceMenu());
        menuBar.add(createLookAndFeelMenu());
        menuBar.add(createTestMenu());
        return menuBar;
    }

    private JMenu createTraceMenu() {
        JMenu traceMenu = new JMenu("Трассировка");
        traceMenu.setMnemonic(KeyEvent.VK_R);

        JMenuItem loadImageItem = new JMenuItem("Загрузить изображение...", KeyEvent.VK_L);
        loadImageItem.addActionListener(e -> onLoadImage());
        traceMenu.add(loadImageItem);

        JMenuItem startTraceItem = new JMenuItem("Начать обводку", KeyEvent.VK_S);
        startTraceItem.addActionListener(e -> onStartTracing());
        traceMenu.add(startTraceItem);

        JMenuItem stopTraceItem = new JMenuItem("Остановить", KeyEvent.VK_T);
        stopTraceItem.addActionListener(e -> onStopTracing());
        traceMenu.add(stopTraceItem);

        traceMenu.addSeparator();

        JMenuItem clearTrailItem = new JMenuItem("Очистить след", KeyEvent.VK_C);
        clearTrailItem.addActionListener(e -> onClearTrail());
        traceMenu.add(clearTrailItem);

        JMenuItem showImageInfoItem = new JMenuItem("Информация об объекте", KeyEvent.VK_I);
        showImageInfoItem.addActionListener(e -> onShowImageInfo());
        traceMenu.add(showImageInfoItem);

        return traceMenu;
    }

    private void setStatusMessage(String message) {
        if (statusLeftLabel != null) {
            statusLeftLabel.setText(" " + message);
        }
        Logger.debug(message);
    }

    private void onLoadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите изображение для трассировки");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                ImageLoader.getFormatDescription(),
                ImageLoader.getSupportedFormats()
        ));

        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();

        try {
            setStatusMessage("Загрузка изображения: " + selectedFile.getName());

            loadedImage = ImageLoader.loadImage(selectedFile);
            lastLoadedFileName = selectedFile.getName();
            Logger.debug("Изображение загружено: " + lastLoadedFileName);

            setStatusMessage("Обработка изображения...");

            lastFoundContour = ImageProcessor.findLargestObjectContour(loadedImage);

            if (lastFoundContour == null || lastFoundContour.isEmpty()) {
                setStatusMessage("Ошибка: объект не найден на изображении");
                JOptionPane.showMessageDialog(this,
                        "Не удалось найти объект на изображении.\n" +
                                "Попробуйте другое изображение с чёткими контурами.",
                        "Объект не найден",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            GameVisualizer visualizer = gameWindow.getGameVisualizer();
            if (visualizer == null) {
                throw new IllegalStateException("GameVisualizer не доступен");
            }

            int fieldWidth = visualizer.getWidth();
            int fieldHeight = visualizer.getHeight();

            if (fieldWidth <= 0 || fieldHeight <= 0) {
                fieldWidth = 600;
                fieldHeight = 500;
            }

            List<Point> normalizedContour = ImageProcessor.normalizeContour(
                    lastFoundContour,
                    fieldWidth - 100,
                    fieldHeight - 150,
                    50,
                    60
            );

            lastBuiltPath = PathBuilder.buildPathFromContour(normalizedContour, 40.0);

            System.out.println("Нормализованный контур: " + normalizedContour.size() + " точек");
            System.out.println("Маршрут: " + lastBuiltPath.size() + " точек");
            Logger.debug("Найден контур из " + lastFoundContour.size() + " точек");
            Logger.debug("Построен маршрут из " + lastBuiltPath.size() + " точек");

            setStatusMessage(String.format("Загружено: %s | Объект: %d точек | Маршрут: %d точек",
                    lastLoadedFileName, lastFoundContour.size(), lastBuiltPath.size()));

            String message = String.format(
                    "Изображение загружено успешно!\n" +
                            "Файл: %s\n" +
                            "Найден объект: %d точек контура\n" +
                            "Построен маршрут: %d точек\n\n" +
                            "Нажмите 'Начать обводку' для запуска робота.",
                    lastLoadedFileName,
                    lastFoundContour.size(),
                    lastBuiltPath.size()
            );

            JOptionPane.showMessageDialog(this,
                    message,
                    "Изображение загружено",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            setStatusMessage("Ошибка загрузки: " + e.getMessage());
            Logger.error("Ошибка загрузки изображения: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Ошибка загрузки изображения:\n" + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            setStatusMessage("Ошибка обработки: " + e.getMessage());
            Logger.error("Ошибка при обработке изображения: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Ошибка при обработке изображения:\n" + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onStartTracing() {
        if (lastBuiltPath == null || lastBuiltPath.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Маршрут не построен. Загрузить изображение сейчас?",
                    "Нет маршрута",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                onLoadImage();
            }
            return;
        }

        GameVisualizer visualizer = gameWindow.getGameVisualizer();
        if (visualizer == null) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка: игровое поле не доступно",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (multiRobotModel.getRobotCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Нет доступных роботов. Добавьте робота через меню Файл → Добавить робота",
                    "Нет роботов",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        visualizer.startTracingOnSelectedRobot(lastBuiltPath);
        setStatusMessage("Трассировка запущена | Робот движется по контуру");
        Logger.debug("Трассировка запущена для робота");
    }

    private void onStopTracing() {
        GameVisualizer visualizer = gameWindow.getGameVisualizer();
        if (visualizer != null) {
            if (visualizer.isSelectedRobotTracing()) {
                visualizer.stopTracingOnSelectedRobot();
                setStatusMessage("Трассировка остановлена");
                Logger.debug("Трассировка остановлена пользователем");
                JOptionPane.showMessageDialog(this,
                        "Трассировка остановлена",
                        "Остановлено",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                setStatusMessage("Трассировка не активна");
                Logger.debug("Трассировка не была активна");
            }
        }
    }

    private void onClearTrail() {
        GameVisualizer visualizer = gameWindow.getGameVisualizer();
        if (visualizer != null) {
            visualizer.clearTrail();
            setStatusMessage("След очищен");
            Logger.debug("След очищен");
        }
    }

    private void onShowImageInfo() {
        if (lastFoundContour == null || lastFoundContour.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Нет загруженного изображения.\n" +
                            "Сначала загрузите изображение через меню Трассировка → Загрузить изображение",
                    "Нет данных",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        double area = ImageProcessor.calculateArea(lastFoundContour);
        String info = String.format(
                "Информация о найденном объекте:\n\n" +
                        "Файл: %s\n" +
                        "Размер изображения: %d x %d пикселей\n" +
                        "Количество точек контура: %d\n" +
                        "Площадь объекта: %.1f пикселей²\n" +
                        "Точек в маршруте: %d",
                lastLoadedFileName != null ? lastLoadedFileName : "неизвестно",
                loadedImage != null ? loadedImage.getWidth() : 0,
                loadedImage != null ? loadedImage.getHeight() : 0,
                lastFoundContour.size(),
                area,
                lastBuiltPath != null ? lastBuiltPath.size() : 0
        );

        JOptionPane.showMessageDialog(this,
                info,
                "Информация об объекте",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private JMenu createFileMenu() {
        JMenu fileMenu = new JMenu("Файл");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem loadRobotItem = new JMenuItem("Загрузить робота из JAR...", KeyEvent.VK_L);
        loadRobotItem.addActionListener(e -> loadRobotFromJar());
        fileMenu.add(loadRobotItem);

        JMenuItem showCoordinatesItem = new JMenuItem("Показать координаты", KeyEvent.VK_C);
        showCoordinatesItem.addActionListener(e -> {
            showCoordinatesWindow();
        });
        fileMenu.add(showCoordinatesItem);

        JMenuItem showGameItem = new JMenuItem("Показать игровое поле", KeyEvent.VK_G);
        showGameItem.addActionListener(e -> {
            showGameWindow();
        });
        fileMenu.add(showGameItem);

        JMenuItem showLogItem = new JMenuItem("Показать протокол", KeyEvent.VK_L);
        showLogItem.addActionListener(e -> {
            showLogWindow();
        });
        fileMenu.add(showLogItem);

        JMenuItem restoreAllItem = new JMenuItem("Восстановить все окна (F5)", KeyEvent.VK_R);
        restoreAllItem.addActionListener(e -> restoreAllWindows());
        fileMenu.add(restoreAllItem);

        fileMenu.addSeparator();

        JMenuItem addRobotItem = new JMenuItem("Добавить робота", KeyEvent.VK_A);
        addRobotItem.addActionListener(e -> {
            multiRobotModel.addRobot();
            setStatusMessage("Добавлен новый робот. Всего роботов: " + multiRobotModel.getRobotCount());
            Logger.debug("Добавлен робот");
        });
        fileMenu.add(addRobotItem);

        fileMenu.addSeparator();

        JMenuItem exitMenuItem = new JMenuItem("Выход", KeyEvent.VK_Q);
        exitMenuItem.addActionListener(e -> exitApplication());
        fileMenu.add(exitMenuItem);

        return fileMenu;
    }

    private JMenu createLookAndFeelMenu() {
        JMenu lookAndFeelMenu = new JMenu("Режим отображения");
        lookAndFeelMenu.setMnemonic(KeyEvent.VK_V);
        lookAndFeelMenu.getAccessibleContext().setAccessibleDescription(
                "Управление режимом отображения приложения");

        JMenuItem systemLookAndFeel = new JMenuItem("Системная схема", KeyEvent.VK_S);
        systemLookAndFeel.addActionListener(e -> {
            setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            invalidate();
            setStatusMessage("Изменён внешний вид: системная схема");
        });
        lookAndFeelMenu.add(systemLookAndFeel);

        JMenuItem crossplatformLookAndFeel = new JMenuItem("Универсальная схема", KeyEvent.VK_S);
        crossplatformLookAndFeel.addActionListener(e -> {
            setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            invalidate();
            setStatusMessage("Изменён внешний вид: универсальная схема");
        });
        lookAndFeelMenu.add(crossplatformLookAndFeel);

        return lookAndFeelMenu;
    }

    private JMenu createTestMenu() {
        JMenu testMenu = new JMenu("Тесты");
        testMenu.setMnemonic(KeyEvent.VK_T);
        testMenu.getAccessibleContext().setAccessibleDescription(
                "Тестовые команды");

        JMenuItem addLogMessageItem = new JMenuItem("Сообщение в лог", KeyEvent.VK_S);
        addLogMessageItem.addActionListener(e -> Logger.debug("Новая строка"));
        testMenu.add(addLogMessageItem);

        return testMenu;
    }

    private void loadRobotFromJar() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите JAR-файл с роботом");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "JAR файлы (*.jar)", "jar"));

        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File jarFile = fileChooser.getSelectedFile();
        Logger.debug("Выбран файл: " + jarFile.getAbsolutePath());
        setStatusMessage("Загрузка плагина: " + jarFile.getName());

        new Thread(() -> {
            try {
                RobotPlugin plugin = RobotPluginManager.getInstance().loadRobotFromJar(jarFile);

                SwingUtilities.invokeLater(() -> {
                    multiRobotModel.addRobotFromPlugin(plugin);
                    setStatusMessage("Робот-плагин загружен: " + plugin.getDisplayName());
                    Logger.debug("Робот добавлен: " + plugin.getDisplayName());
                    savePluginConfiguration();
                });

            } catch (Exception e) {
                Logger.error("Ошибка загрузки плагина: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    setStatusMessage("Ошибка загрузки плагина");
                    JOptionPane.showMessageDialog(this,
                            "Ошибка загрузки:\n" + e.getMessage(),
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void savePluginConfiguration() {
        try {
            Properties props = new Properties();
            List<String> jarPaths = RobotPluginManager.getInstance().getLoadedJarPaths();

            props.setProperty("plugins.count", String.valueOf(jarPaths.size()));
            for (int i = 0; i < jarPaths.size(); i++) {
                props.setProperty("plugin." + i, jarPaths.get(i));
            }

            try (FileOutputStream out = new FileOutputStream(PLUGIN_CONFIG_FILE)) {
                props.storeToXML(out, "Loaded robot plugins");
            }
            Logger.debug("Конфигурация плагинов сохранена");
        } catch (Exception e) {
            Logger.error("Ошибка сохранения конфигурации: " + e.getMessage());
        }
    }

    private void loadPluginConfiguration() {
        File configFile = new File(PLUGIN_CONFIG_FILE);
        if (!configFile.exists()) {
            return;
        }

        try {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.loadFromXML(in);
            }

            int count = Integer.parseInt(props.getProperty("plugins.count", "0"));

            for (int i = 0; i < count; i++) {
                String jarPath = props.getProperty("plugin." + i);
                if (jarPath != null && !jarPath.isEmpty()) {
                    File jarFile = new File(jarPath);
                    if (jarFile.exists()) {
                        try {
                            RobotPluginManager.getInstance().loadRobotFromJar(jarFile);
                            Logger.debug("Загружен плагин: " + jarPath);
                        } catch (Exception e) {
                            Logger.error("Не удалось загрузить плагин: " + jarPath);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("Ошибка загрузки конфигурации: " + e.getMessage());
        }
    }

    private boolean loadWindowPositionsIfExists() {
        try {
            String userHome = System.getProperty("user.home");
            File configFile = new File(userHome, ".robots_window_config.xml");

            if (!configFile.exists()) {
                return false;
            }

            Properties props = new Properties();
            props.loadFromXML(new FileInputStream(configFile));

            JInternalFrame[] frames = desktopPane.getAllFrames();

            for (JInternalFrame frame : frames) {
                String frameName = frame.getTitle();

                String xStr = props.getProperty(frameName + ".x");
                String yStr = props.getProperty(frameName + ".y");
                if (xStr != null && yStr != null) {
                    frame.setLocation(Integer.parseInt(xStr), Integer.parseInt(yStr));
                }

                String wStr = props.getProperty(frameName + ".width");
                String hStr = props.getProperty(frameName + ".height");
                if (wStr != null && hStr != null) {
                    frame.setSize(Integer.parseInt(wStr), Integer.parseInt(hStr));
                }

                String maxStr = props.getProperty(frameName + ".maximum");
                if ("true".equals(maxStr)) {
                    try { frame.setMaximum(true); } catch (Exception e) {}
                }

                String iconStr = props.getProperty(frameName + ".icon");
                if ("true".equals(iconStr) && !"true".equals(maxStr)) {
                    try { frame.setIcon(true); } catch (Exception e) {}
                }
            }

            String zOrderStr = props.getProperty("window.zorder");
            if (zOrderStr != null && !zOrderStr.isEmpty()) {
                String[] frameTitles = zOrderStr.split(",");
                for (int i = frameTitles.length - 1; i >= 0; i--) {
                    for (JInternalFrame frame : frames) {
                        if (frame.getTitle().equals(frameTitles[i])) {
                            desktopPane.setComponentZOrder(frame, 0);
                            frame.toFront();
                            break;
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void saveWindowPositions() {
        try {
            String userHome = System.getProperty("user.home");
            File configFile = new File(userHome, ".robots_window_config.xml");
            Properties props = new Properties();

            JInternalFrame[] frames = desktopPane.getAllFrames();
            StringBuilder zOrder = new StringBuilder();

            for (JInternalFrame frame : frames) {
                String frameName = frame.getTitle();

                if (zOrder.length() > 0) {
                    zOrder.append(",");
                }
                zOrder.append(frameName);

                props.setProperty(frameName + ".x", String.valueOf(frame.getX()));
                props.setProperty(frameName + ".y", String.valueOf(frame.getY()));
                props.setProperty(frameName + ".width", String.valueOf(frame.getWidth()));
                props.setProperty(frameName + ".height", String.valueOf(frame.getHeight()));
                props.setProperty(frameName + ".icon", String.valueOf(frame.isIcon()));
                props.setProperty(frameName + ".maximum", String.valueOf(frame.isMaximum()));
            }

            props.setProperty("window.zorder", zOrder.toString());
            props.storeToXML(new FileOutputStream(configFile), "Robot Application Window Positions");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exitApplication() {
        int result = JOptionPane.showOptionDialog(
                this,
                "Вы действительно хотите выйти?",
                "Подтверждение выхода",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"Да", "Нет"},
                "Нет"
        );

        if (result == 0) {
            setStatusMessage("Завершение работы...");
            saveWindowPositions();
            savePluginConfiguration();
            System.exit(0);
        }
    }

    private void setLookAndFeel(String className) {
        try {
            UIManager.setLookAndFeel(className);
            SwingUtilities.updateComponentTreeUI(this);
        } catch (ClassNotFoundException | InstantiationException
                 | IllegalAccessException | UnsupportedLookAndFeelException e) {
            // ignore
        }
    }
}