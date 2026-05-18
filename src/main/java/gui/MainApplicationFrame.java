package gui;

import image.ImageLoader;
import model.MultiRobotModel;
import model.Waypoint;
import plugin.RobotPlugin;
import plugin.RobotPluginManager;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
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

import log.Logger;

public class MainApplicationFrame extends JFrame {
    private final JDesktopPane desktopPane = new JDesktopPane();
    private final MultiRobotModel multiRobotModel = new MultiRobotModel();
    private RobotCoordinatesWindow coordinatesWindow;
    private LogWindow logWindow;
    private GameWindow gameWindow;

    // Контроллер трассировки (вынесен отдельно по требованию)
    private final TracingController tracingController;

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

        // Инициализация контроллера трассировки
        tracingController = new TracingController(this, multiRobotModel, gameWindow);

        setupExitHandler();
        loadPluginConfiguration();

        boolean hasSavedPositions = loadWindowPositionsIfExists();
        if (!hasSavedPositions) {
            setDefaultWindowPositions();
        }

        startStatusUpdater();
        setupHotkeys();

        SwingUtilities.invokeLater(this::ensureAllWindowsVisible);
    }

    private void ensureAllWindowsVisible() {
        if (logWindow == null || !logWindow.isVisible() || !isWindowInDesktopPane(logWindow)) {
            showLogWindow();
        }
        if (gameWindow == null || !gameWindow.isVisible() || !isWindowInDesktopPane(gameWindow)) {
            showGameWindow();
        }
        if (coordinatesWindow == null || !coordinatesWindow.isVisible() || !isWindowInDesktopPane(coordinatesWindow)) {
            showCoordinatesWindow();
        }

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
            if (isWindowInDesktopPane(logWindow)) desktopPane.remove(logWindow);
            logWindow.dispose();
        }
        logWindow = createLogWindow();
        desktopPane.add(logWindow);
        logWindow.setVisible(true);
        try { logWindow.setIcon(false); } catch (Exception ignored) {}
        logWindow.toFront();
    }

    private void showGameWindow() {
        if (gameWindow != null) {
            if (isWindowInDesktopPane(gameWindow)) desktopPane.remove(gameWindow);
            gameWindow.dispose();
        }
        gameWindow = createGameWindow();
        desktopPane.add(gameWindow);
        gameWindow.setVisible(true);
        try { gameWindow.setIcon(false); } catch (Exception ignored) {}
        gameWindow.toFront();
    }

    private void showCoordinatesWindow() {
        if (coordinatesWindow != null) {
            if (isWindowInDesktopPane(coordinatesWindow)) desktopPane.remove(coordinatesWindow);
            coordinatesWindow.dispose();
        }
        coordinatesWindow = createCoordinatesWindow();
        desktopPane.add(coordinatesWindow);
        coordinatesWindow.setVisible(true);
        try { coordinatesWindow.setIcon(false); } catch (Exception ignored) {}
        coordinatesWindow.toFront();
    }

    private boolean isWindowInDesktopPane(JInternalFrame window) {
        for (JInternalFrame frame : desktopPane.getAllFrames()) {
            if (frame == window) return true;
        }
        return false;
    }

    private void configureMainFrame() {
        int inset = 50;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(inset, inset, screenSize.width - inset * 2, screenSize.height - inset * 2);
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
        return new RobotCoordinatesWindow(multiRobotModel);
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

        if (tracingController.getLastLoadedFileName() != null) {
            leftText.append("Загружено: ").append(tracingController.getLastLoadedFileName());
            if (tracingController.getLastFoundContour() != null && !tracingController.getLastFoundContour().isEmpty()) {
                leftText.append(" | Найден объект: ")
                        .append(tracingController.getLastFoundContour().size())
                        .append(" точек");
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
        javax.swing.Timer timer = new javax.swing.Timer(500, e -> updateStatusBar());
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
        loadImageItem.addActionListener(e -> tracingController.onLoadImage());
        traceMenu.add(loadImageItem);

        JMenuItem startTraceItem = new JMenuItem("Начать обводку", KeyEvent.VK_S);
        startTraceItem.addActionListener(e -> tracingController.onStartTracing());
        traceMenu.add(startTraceItem);

        JMenuItem stopTraceItem = new JMenuItem("Остановить", KeyEvent.VK_T);
        stopTraceItem.addActionListener(e -> tracingController.onStopTracing());
        traceMenu.add(stopTraceItem);

        traceMenu.addSeparator();

        JMenuItem clearTrailItem = new JMenuItem("Очистить след", KeyEvent.VK_C);
        clearTrailItem.addActionListener(e -> tracingController.onClearTrail());
        traceMenu.add(clearTrailItem);

        JMenuItem showImageInfoItem = new JMenuItem("Информация об объекте", KeyEvent.VK_I);
        showImageInfoItem.addActionListener(e -> tracingController.onShowImageInfo());
        traceMenu.add(showImageInfoItem);

        return traceMenu;
    }

    public void setStatusMessage(String message) {
        if (statusLeftLabel != null) {
            statusLeftLabel.setText(" " + message);
        }
        Logger.debug(message);
    }

    private JMenu createFileMenu() {
        JMenu fileMenu = new JMenu("Файл");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem loadRobotItem = new JMenuItem("Загрузить робота из JAR...", KeyEvent.VK_L);
        loadRobotItem.addActionListener(e -> loadRobotFromJar());
        fileMenu.add(loadRobotItem);

        fileMenu.addSeparator();

        JMenuItem showCoordinatesItem = new JMenuItem("Показать координаты", KeyEvent.VK_C);
        showCoordinatesItem.addActionListener(e -> showCoordinatesWindow());
        fileMenu.add(showCoordinatesItem);

        JMenuItem showGameItem = new JMenuItem("Показать игровое поле", KeyEvent.VK_G);
        showGameItem.addActionListener(e -> showGameWindow());
        fileMenu.add(showGameItem);

        JMenuItem showLogItem = new JMenuItem("Показать протокол", KeyEvent.VK_L);
        showLogItem.addActionListener(e -> showLogWindow());
        fileMenu.add(showLogItem);

        JMenuItem restoreAllItem = new JMenuItem("Восстановить все окна (F5)", KeyEvent.VK_R);
        restoreAllItem.addActionListener(e -> restoreAllWindows());
        fileMenu.add(restoreAllItem);

        fileMenu.addSeparator();

        JMenuItem addRobotItem = new JMenuItem("Добавить робота", KeyEvent.VK_A);
        addRobotItem.addActionListener(e -> {
            multiRobotModel.addRobot();
            setStatusMessage("Добавлен новый робот. Всего роботов: " + multiRobotModel.getRobotCount());
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

        JMenuItem systemLookAndFeel = new JMenuItem("Системная схема", KeyEvent.VK_S);
        systemLookAndFeel.addActionListener(e -> {
            setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            setStatusMessage("Изменён внешний вид: системная схема");
        });
        lookAndFeelMenu.add(systemLookAndFeel);

        JMenuItem crossplatformLookAndFeel = new JMenuItem("Универсальная схема", KeyEvent.VK_S);
        crossplatformLookAndFeel.addActionListener(e -> {
            setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            setStatusMessage("Изменён внешний вид: универсальная схема");
        });
        lookAndFeelMenu.add(crossplatformLookAndFeel);

        return lookAndFeelMenu;
    }

    private JMenu createTestMenu() {
        JMenu testMenu = new JMenu("Тесты");
        testMenu.setMnemonic(KeyEvent.VK_T);

        JMenuItem addLogMessageItem = new JMenuItem("Сообщение в лог", KeyEvent.VK_S);
        addLogMessageItem.addActionListener(e -> Logger.debug("Новая строка"));
        testMenu.add(addLogMessageItem);

        return testMenu;
    }

    private void loadRobotFromJar() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите JAR-файл с роботом");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JAR файлы (*.jar)", "jar"));

        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File jarFile = fileChooser.getSelectedFile();
        setStatusMessage("Загрузка плагина: " + jarFile.getName());

        new Thread(() -> {
            try {
                RobotPlugin plugin = RobotPluginManager.getInstance().loadRobotFromJar(jarFile);
                SwingUtilities.invokeLater(() -> {
                    multiRobotModel.addRobotFromPlugin(plugin);
                    setStatusMessage("Робот-плагин загружен: " + plugin.getDisplayName());
                    savePluginConfiguration();
                });
            } catch (Exception e) {
                Logger.error("Ошибка загрузки плагина: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    setStatusMessage("Ошибка загрузки плагина");
                    JOptionPane.showMessageDialog(this, "Ошибка загрузки:\n" + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
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
        } catch (Exception e) {
            Logger.error("Ошибка сохранения конфигурации: " + e.getMessage());
        }
    }

    private void loadPluginConfiguration() {
        File configFile = new File(PLUGIN_CONFIG_FILE);
        if (!configFile.exists()) return;

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
                        RobotPluginManager.getInstance().loadRobotFromJar(jarFile);
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("Ошибка загрузки конфигурации: " + e.getMessage());
        }
    }

    private boolean loadWindowPositionsIfExists() {
        // ... (оставляем без изменений, как было в оригинале) ...
        try {
            String userHome = System.getProperty("user.home");
            File configFile = new File(userHome, ".robots_window_config.xml");
            if (!configFile.exists()) return false;

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
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void saveWindowPositions() {
        // ... (оставляем без изменений) ...
        try {
            String userHome = System.getProperty("user.home");
            File configFile = new File(userHome, ".robots_window_config.xml");
            Properties props = new Properties();
            JInternalFrame[] frames = desktopPane.getAllFrames();
            StringBuilder zOrder = new StringBuilder();

            for (JInternalFrame frame : frames) {
                String frameName = frame.getTitle();
                if (zOrder.length() > 0) zOrder.append(",");
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
        int result = JOptionPane.showOptionDialog(this,
                "Вы действительно хотите выйти?",
                "Подтверждение выхода",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"Да", "Нет"},
                "Нет");

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
        } catch (Exception ignored) {}
    }
}