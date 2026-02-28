package gui;

import model.RobotModel;

import javax.swing.*;
import java.awt.*;

public class RobotCoordinatesWindow extends JInternalFrame implements RobotModel.RobotModelListener
{
    private final RobotModel model;

    private final JLabel xCoordLabel;
    private final JLabel yCoordLabel;
    private final JLabel directionLabel;
    private final JLabel targetXLabel;
    private final JLabel targetYLabel;

    public RobotCoordinatesWindow(RobotModel model)
    {
        this.model = model;
        super("Координаты робота", true, true, true, true);
        model.addListener(this);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Создаем панель для отображения координат
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Текущие координаты"));

        // Заголовки
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Позиция X:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        xCoordLabel = new JLabel(String.format("%.2f", model.getRobotPositionX()));
        xCoordLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(xCoordLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Позиция Y:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        yCoordLabel = new JLabel(String.format("%.2f", model.getRobotPositionY()));
        yCoordLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(yCoordLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Направление:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        directionLabel = new JLabel(String.format("%.2f°", Math.toDegrees(model.getRobotDirection())));
        directionLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(directionLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Цель X:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        targetXLabel = new JLabel(String.valueOf(model.getTargetPositionX()));
        targetXLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(targetXLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Цель Y:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        targetYLabel = new JLabel(String.valueOf(model.getTargetPositionY()));
        targetYLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(targetYLabel, gbc);

        getContentPane().add(panel);
        setSize(250, 200);
        setLocation(320, 10);
    }

    @Override
    public void onRobotPositionChanged(double x, double y, double direction)
    {
        SwingUtilities.invokeLater(() -> {
            xCoordLabel.setText(String.format("%.2f", x));
            yCoordLabel.setText(String.format("%.2f", y));
            directionLabel.setText(String.format("%.2f°", Math.toDegrees(direction)));

            targetXLabel.setText(String.valueOf(model.getTargetPositionX()));
            targetYLabel.setText(String.valueOf(model.getTargetPositionY()));
        });
    }

    // метод для обновления цели
    public void updateTargetPosition(int x, int y)
    {
        SwingUtilities.invokeLater(() -> {
            targetXLabel.setText(String.valueOf(x));
            targetYLabel.setText(String.valueOf(y));
        });
    }
}