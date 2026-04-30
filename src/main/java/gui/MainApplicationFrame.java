package gui;

import model.MultiRobotModel;
import plugin.RobotPlugin;
import plugin.RobotPluginManager;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Properties;

import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import log.Logger;

public class MainApplicationFrame extends JFrame
{
    private final JDesktopPane desktopPane = new JDesktopPane();
    private final MultiRobotModel multiRobotModel = new MultiRobotModel();
    private RobotCoordinatesWindow coordinatesWindow;
    private LogWindow logWindow;
    private GameWindow gameWindow;

    private static final String PLUGIN_CONFIG_FILE = System.getProperty("user.home") +
            File.separator + ".robots_plugins.xml";

    public MainApplicationFrame() {
        configureMainFrame();
        createWindows();
        setJMenuBar(createMenuBar());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        setupExitHandler();
        loadWindowPositions();
        loadPluginConfiguration();
    }

    private void configureMainFrame() {
        int inset = 50;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(inset, inset,
                screenSize.width  - inset*2,
                screenSize.height - inset*2);
        setContentPane(desktopPane);
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

        gameWindow.toFront();

        desktopPane.setComponentZOrder(logWindow, 2);
        desktopPane.setComponentZOrder(gameWindow, 1);
        desktopPane.setComponentZOrder(coordinatesWindow, 0);
    }

    private LogWindow createLogWindow() {
        LogWindow logWindow = new LogWindow(Logger.getDefaultLogSource());
        logWindow.setLocation(10, 10);
        logWindow.setSize(300, 800);
        logWindow.setTitle("Протокол работы");
        logWindow.setResizable(true);
        logWindow.setClosable(true);
        logWindow.setIconifiable(true);
        logWindow.setMaximizable(true);
        return logWindow;
    }

    private GameWindow createGameWindow() {
        GameWindow gameWindow = new GameWindow(multiRobotModel);
        gameWindow.setSize(600, 500);
        gameWindow.setLocation(320, 10);
        gameWindow.setTitle("Игровое поле");
        gameWindow.setDoubleBuffered(true);
        return gameWindow;
    }

    private RobotCoordinatesWindow createCoordinatesWindow() {
        RobotCoordinatesWindow coordinatesWindow = new RobotCoordinatesWindow(multiRobotModel);
        coordinatesWindow.setLocation(650, 10);
        coordinatesWindow.setSize(300, 400);
        coordinatesWindow.setTitle("Координаты роботов");
        return coordinatesWindow;
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
        menuBar.add(createLookAndFeelMenu());
        menuBar.add(createTestMenu());
        return menuBar;
    }

    private JMenu createFileMenu() {
        JMenu fileMenu = new JMenu("Файл");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem loadRobotItem = new JMenuItem("Загрузить робота из JAR...", KeyEvent.VK_L);
        loadRobotItem.addActionListener(e -> loadRobotFromJar());
        fileMenu.add(loadRobotItem);

        JMenuItem showCoordinatesItem = new JMenuItem("Показать координаты", KeyEvent.VK_C);
        showCoordinatesItem.addActionListener(e -> {
            if (coordinatesWindow != null && !coordinatesWindow.isVisible()) {
                coordinatesWindow.setVisible(true);
            }
        });
        fileMenu.add(showCoordinatesItem);

        JMenuItem addRobotItem = new JMenuItem("Добавить робота", KeyEvent.VK_A);
        addRobotItem.addActionListener(e -> {
            multiRobotModel.addRobot();
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
        });
        lookAndFeelMenu.add(systemLookAndFeel);

        JMenuItem crossplatformLookAndFeel = new JMenuItem("Универсальная схема", KeyEvent.VK_S);
        crossplatformLookAndFeel.addActionListener(e -> {
            setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            invalidate();
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
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "JAR файлы (*.jar)", "jar"));

        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File jarFile = fileChooser.getSelectedFile();
        Logger.debug("Выбран файл: " + jarFile.getAbsolutePath());

        new Thread(() -> {
            try {
                RobotPlugin plugin = RobotPluginManager.getInstance().loadRobotFromJar(jarFile);

                SwingUtilities.invokeLater(() -> {
                    multiRobotModel.addRobotFromPlugin(plugin);
                    Logger.debug("Робот добавлен: " + plugin.getDisplayName());
                    savePluginConfiguration();
                });

            } catch (Exception e) {
                Logger.error("Ошибка загрузки плагина: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
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

    private void loadWindowPositions() {
        try {
            String userHome = System.getProperty("user.home");
            File configFile = new File(userHome, ".robots_window_config.xml");

            if (!configFile.exists()) {
                return;
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}