package gui;

import model.MultiRobotModel;

import java.awt.BorderLayout;

import javax.swing.JInternalFrame;
import javax.swing.JPanel;

public class GameWindow extends JInternalFrame {
    private final GameVisualizer visualizer;

    public GameWindow(MultiRobotModel model) {
        super("Игровое поле", true, true, true, true);
        visualizer = new GameVisualizer(model);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(visualizer, BorderLayout.CENTER);
        getContentPane().add(panel);
        pack();

        visualizer.setToolTipText("Клик - установка цели для выбранного робота." +
                "Для выбора робота нажмите цифру 1 или 2.");
    }
}