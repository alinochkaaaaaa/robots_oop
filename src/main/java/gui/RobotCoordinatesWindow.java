package gui;

import model.MultiRobotModel;
import model.RobotModel;
import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

public class RobotCoordinatesWindow extends JInternalFrame implements PropertyChangeListener {
    private final MultiRobotModel multiModel;
    private final Map<Integer, JPanel> robotPanels = new HashMap<>();
    private final JPanel mainPanel;

    public RobotCoordinatesWindow(MultiRobotModel multiModel) {
        super("Координаты роботов", true, true, true, true);
        this.multiModel = multiModel;

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        multiModel.addPropertyChangeListener(this);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        getContentPane().add(scrollPane);

        setSize(300, 400);
        setLocation(320, 10);

        updateRobotPanels();
    }

    private void updateRobotPanels() {
        mainPanel.removeAll();
        robotPanels.clear();

        for (RobotModel robot : multiModel.getRobots()) {
            JPanel robotPanel = createRobotPanel(robot);
            robotPanels.put(robot.getRobotId(), robotPanel);
            mainPanel.add(robotPanel);
            mainPanel.add(Box.createVerticalStrut(10));
        }

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private JPanel createRobotPanel(RobotModel robot) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(robot.getRobotName()));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel xLabel = new JLabel();
        JLabel yLabel = new JLabel();
        JLabel dirLabel = new JLabel();
        JLabel targetXLabel = new JLabel();
        JLabel targetYLabel = new JLabel();

        // Сохраняем ссылки на метки для обновления
        robot.addPropertyChangeListener(evt -> {
            SwingUtilities.invokeLater(() -> {
                xLabel.setText(String.format("%.2f", robot.getRobotPositionX()));
                yLabel.setText(String.format("%.2f", robot.getRobotPositionY()));
                dirLabel.setText(String.format("%.2f°", Math.toDegrees(robot.getRobotDirection())));
                targetXLabel.setText(String.valueOf(robot.getTargetPositionX()));
                targetYLabel.setText(String.valueOf(robot.getTargetPositionY()));
            });
        });

        addLabelPair(panel, gbc, 0, "Позиция X:", xLabel);
        addLabelPair(panel, gbc, 1, "Позиция Y:", yLabel);
        addLabelPair(panel, gbc, 2, "Направление:", dirLabel);
        addLabelPair(panel, gbc, 3, "Цель X:", targetXLabel);
        addLabelPair(panel, gbc, 4, "Цель Y:", targetYLabel);

        // Инициализация значений
        xLabel.setText(String.format("%.2f", robot.getRobotPositionX()));
        yLabel.setText(String.format("%.2f", robot.getRobotPositionY()));
        dirLabel.setText(String.format("%.2f°", Math.toDegrees(robot.getRobotDirection())));
        targetXLabel.setText(String.valueOf(robot.getTargetPositionX()));
        targetYLabel.setText(String.valueOf(robot.getTargetPositionY()));

        return panel;
    }

    private void addLabelPair(JPanel panel, GridBagConstraints gbc,
                              int row, String labelText, JLabel valueLabel) {
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        valueLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(valueLabel, gbc);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        SwingUtilities.invokeLater(this::updateRobotPanels);
    }
}