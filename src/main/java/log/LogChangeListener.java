package log;

// интерфейс для классов, которые отслеживают изменения в логе
// часть паттерна Observer

public interface LogChangeListener
{
    public void onLogChanged(); 
}
