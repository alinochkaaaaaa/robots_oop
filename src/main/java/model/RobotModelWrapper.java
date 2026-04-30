package model;

import plugin.RobotInstance;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class RobotModelWrapper extends RobotModel {

    private final RobotInstance instance;
    private final String pluginTypeId;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private volatile boolean isUpdatingFromInstance = false;
    private double cachedTargetX;
    private double cachedTargetY;

    public RobotModelWrapper(RobotInstance instance, String pluginTypeId, int robotId) {
        this.instance = instance;
        this.pluginTypeId = pluginTypeId;
        setRobotId(robotId);
        this.cachedTargetX = instance.getX();
        this.cachedTargetY = instance.getY();
    }

    @Override
    public double getRobotPositionX() {
        return instance.getX();
    }

    @Override
    public double getRobotPositionY() {
        return instance.getY();
    }

    @Override
    public double getRobotDirection() {
        return instance.getDirection();
    }

    @Override
    public void setTargetPosition(int x, int y) {
        this.cachedTargetX = x;   // int автоматически преобразуется в double
        this.cachedTargetY = y;
        pcs.firePropertyChange("target", null, this);
    }

    @Override
    public int getTargetPositionX() {
        return (int) Math.round(cachedTargetX);   // округление при возврате
    }

    @Override
    public int getTargetPositionY() {
        return (int) Math.round(cachedTargetY);
    }

    @Override
    public void updateModel() {
        instance.update(cachedTargetX, cachedTargetY);  // теперь передаём double

        if (!isUpdatingFromInstance) {
            isUpdatingFromInstance = true;
            pcs.firePropertyChange("position", null, this);
            isUpdatingFromInstance = false;
        }
    }

    public RobotInstance getPluginInstance() {
        return instance;
    }

    public String getPluginTypeId() {
        return pluginTypeId;
    }

    @Override
    public String getRobotName() {
        return "Робот " + getRobotId() + (pluginTypeId.equals("builtin") ? "" : " (" + pluginTypeId + ")");
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}