package model;

import plugin.RobotInstance;
import plugin.RobotPlugin;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MultiRobotModel {
    private final List<RobotModel> robots = new CopyOnWriteArrayList<>(); // Потокобезопасный список
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final Map<Integer, String> robotPluginTypes = new ConcurrentHashMap<>(); // тип плагина для каждого робота
    private int nextRobotId = 1;
    private volatile boolean isUpdating = false;

    public MultiRobotModel() {
        // Первый робот создается автоматически (стандартный)
        addRobot();
    }

    public RobotModel addRobot() {
        RobotModel newRobot = new RobotModel();
        newRobot.setRobotId(nextRobotId++);

        // Добавляем слушателя для пересылки событий
        newRobot.addPropertyChangeListener(evt -> {
            if (!isUpdating) {
                isUpdating = true;
                pcs.firePropertyChange("robot_" + newRobot.getRobotId(), null, newRobot);
                pcs.firePropertyChange("robots", null, robots);
                isUpdating = false;
            }
        });

        robots.add(newRobot);
        robotPluginTypes.put(newRobot.getRobotId(), "builtin");
        pcs.firePropertyChange("robots", null, robots);
        return newRobot;
    }

    public void removeRobot(RobotModel robot) {
        if (robots.size() > 1) {
            robots.remove(robot);
            robotPluginTypes.remove(robot.getRobotId());  // Удаляем информацию о типе
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

    public void addPropertyChangeListener(String robots, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    /**
     * Добавить робота из плагина (случайная позиция)
     */
    public RobotModel addRobotFromPlugin(RobotPlugin plugin) {
        return addRobotFromPlugin(plugin, 100 + Math.random() * 200, 100 + Math.random() * 200);
    }

    /**
     * Добавить робота из плагина с указанной позицией
     */
    public RobotModel addRobotFromPlugin(RobotPlugin plugin, double startX, double startY) {
        RobotInstance instance = plugin.createInstance(startX, startY);
        RobotModelWrapper wrapper = new RobotModelWrapper(instance, plugin.getRobotTypeId(), nextRobotId);
        robots.add(wrapper);
        robotPluginTypes.put(wrapper.getRobotId(), plugin.getRobotTypeId());
        nextRobotId++;
        pcs.firePropertyChange("robots", null, robots);
        return wrapper;
    }

    /**
     * Получить тип плагина для робота по его ID
     */
    public String getRobotPluginType(int robotId) {
        return robotPluginTypes.get(robotId);
    }
}