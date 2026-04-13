package model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public class MultiRobotModel {
    private final List<RobotModel> robots = new ArrayList<>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private int nextRobotId = 1;

    public MultiRobotModel() {
        // Первый робот создается автоматически
        addRobot();
    }

    public RobotModel addRobot() {
        RobotModel newRobot = new RobotModel();
        newRobot.setRobotId(nextRobotId++);

        // Добавляем слушателя для пересылки событий
        newRobot.addPropertyChangeListener(evt -> {
            pcs.firePropertyChange("robot_" + newRobot.getRobotId(), null, newRobot);
            pcs.firePropertyChange("robots", null, robots);
        });

        robots.add(newRobot);
        pcs.firePropertyChange("robots", null, robots);
        return newRobot;
    }

    public void removeRobot(RobotModel robot) {
        if (robots.size() > 1) {
            robots.remove(robot);
            pcs.firePropertyChange("robots", null, robots);
        }
    }

    public List<RobotModel> getRobots() {
        return new ArrayList<>(robots);
    }

    public RobotModel getRobot(int index) {
        if (index >= 0 && index < robots.size()) {
            return robots.get(index);
        }
        return null;
    }

    public int getRobotCount() {
        return robots.size();
    }

    public void updateAllModels() {
        for (RobotModel robot : robots) {
            robot.updateModel();
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}