package gui;

import model.MultiRobotModel;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import javax.swing.JDesktopPane;
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

    public MainApplicationFrame() {
        configureMainFrame();
        createWindows();
        setJMenuBar(createMenuBar());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        setupExitHandler();
        loadWindowPositions();
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
        addWindow(createLogWindow());
        addWindow(createGameWindow());
        addWindow(createCoordinatesWindow());
    }

    private LogWindow createLogWindow() {
        LogWindow logWindow = new LogWindow(Logger.getDefaultLogSource());
        logWindow.setLocation(10, 10);
        logWindow.setSize(300, 800);
        logWindow.setTitle("Протокол работы");
        logWindow.setMinimumSize(logWindow.getSize());
        logWindow.pack();
        Logger.debug("Протокол работает");
        return logWindow;
    }

    private GameWindow createGameWindow() {
        GameWindow gameWindow = new GameWindow(multiRobotModel);
        gameWindow.setSize(400, 400);
        gameWindow.setTitle("Игровое поле");
        return gameWindow;
    }

    private RobotCoordinatesWindow createCoordinatesWindow() {
        coordinatesWindow = new RobotCoordinatesWindow(multiRobotModel);
        coordinatesWindow.setTitle("Координаты роботов");
        return coordinatesWindow;
    }

    private void addWindow(JInternalFrame frame) {
        desktopPane.add(frame);
        frame.setVisible(true);
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

        JMenuItem showCoordinatesItem = new JMenuItem("Показать координаты", KeyEvent.VK_C);
        showCoordinatesItem.addActionListener(e -> {
            if (coordinatesWindow != null && !coordinatesWindow.isVisible()) {
                coordinatesWindow.setVisible(true);
            }
        });
        fileMenu.add(showCoordinatesItem);

        // Добавляем пункт для создания второго робота
        JMenuItem addRobotItem = new JMenuItem("Добавить робота", KeyEvent.VK_A);
        addRobotItem.addActionListener(e -> {
            if (multiRobotModel.getRobotCount() < 2) {
                multiRobotModel.addRobot();
                Logger.debug("Добавлен второй робот");
            } else {
                Logger.debug("Максимум 2 робота уже создано");
            }
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