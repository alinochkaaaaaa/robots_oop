package gui;

import model.RobotModel;
import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class RobotCoordinatesWindow extends JInternalFrame implements PropertyChangeListener {
    private final RobotModel model;
    private JLabel xCoordLabel;
    private JLabel yCoordLabel;
    private JLabel directionLabel;
    private JLabel targetXLabel;
    private JLabel targetYLabel;

    public RobotCoordinatesWindow(RobotModel model) {
        super("Координаты робота", true, true, true, true);
        this.model = model;

        this.xCoordLabel = createCoordLabel();
        this.yCoordLabel = createCoordLabel();
        this.directionLabel = createCoordLabel();
        this.targetXLabel = createCoordLabel();
        this.targetYLabel = createCoordLabel();

        model.addPropertyChangeListener(this);

        JPanel mainPanel = createMainPanel();
        getContentPane().add(mainPanel);

        setSize(250, 200);
        setLocation(320, 10);
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Текущие координаты"));

        GridBagConstraints gbc = createGridBagConstraints();

        addLabelPair(panel, gbc, 0, "Позиция X:", xCoordLabel = createCoordLabel());
        addLabelPair(panel, gbc, 1, "Позиция Y:", yCoordLabel = createCoordLabel());
        addLabelPair(panel, gbc, 2, "Направление:", directionLabel = createCoordLabel());
        addLabelPair(panel, gbc, 3, "Цель X:", targetXLabel = createCoordLabel());
        addLabelPair(panel, gbc, 4, "Цель Y:", targetYLabel = createCoordLabel());

        updateLabels();

        return panel;
    }

    private GridBagConstraints createGridBagConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    private JLabel createCoordLabel() {
        JLabel label = new JLabel();
        label.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return label;
    }

    private void addLabelPair(JPanel panel, GridBagConstraints gbc,
                              int row, String labelText, JLabel valueLabel) {
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        panel.add(valueLabel, gbc);
    }

    private void updateLabels() {
        xCoordLabel.setText(String.format("%.2f", model.getRobotPositionX()));
        yCoordLabel.setText(String.format("%.2f", model.getRobotPositionY()));
        directionLabel.setText(String.format("%.2f°", Math.toDegrees(model.getRobotDirection())));
        targetXLabel.setText(String.valueOf(model.getTargetPositionX()));
        targetYLabel.setText(String.valueOf(model.getTargetPositionY()));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // Обновляем UI в потоке EDT
        SwingUtilities.invokeLater(this::updateLabels);
    }
}