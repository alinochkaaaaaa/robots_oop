# Плагинная архитектура: динамическая загрузка роботов

## Описание задачи

Реализация возможности загружать роботов из внешних JAR-файлов без остановки и перекомпиляции программы.

**Ключевые технологии:**
- `ClassLoader` (`URLClassLoader`) — загрузка классов из JAR
- `Reflection` — поиск и создание экземпляров
- `WeakReference` — кэш с возможностью выгрузки
- `ConcurrentHashMap` — потокобезопасность
- `Singleton` — единый менеджер

---

## 📁 Структура пакета `plugin/`
src/main/java/plugin/

├── RobotPlugin.java # Интерфейс плагина

├── RobotInstance.java # Интерфейс экземпляра робота

└── RobotPluginManager.java # Синглтон-менеджер загрузки


---

## Интерфейсы

### RobotPlugin.java

```java
public interface RobotPlugin {
    String getRobotTypeId();              // уникальный ID типа
    String getDisplayName();              // отображаемое имя
    RobotInstance createInstance(double x, double y);
}
```

### RobotInstance.java

```java
public interface RobotInstance {
    void update(double targetX, double targetY);     // движение
    void draw(Graphics2D g, int x, int y, boolean isSelected); // отрисовка
    double getX(); double getY(); double getDirection();
    void setPosition(double x, double y);
}
```

### Главный класс: RobotPluginManager

```java
public class RobotPluginManager {
    private static final RobotPluginManager INSTANCE = new RobotPluginManager();
    public static RobotPluginManager getInstance() { return INSTANCE; }
    private RobotPluginManager() { loadPluginsFromCache(); }
    
    // Основной метод
    public RobotPlugin loadRobotFromJar(File jarFile) throws Exception;
    
    // Для UI и сериализации
    public List<RobotPlugin> getAllRobotTypes();
    public List<String> getLoadedJarPaths();
    public void unloadAllPlugins();
}
```

---

## Алгоритм загрузки JAR

- Проверка файла (существует? .jar? валидный?)
- Создание URLClassLoader
- Сканирование JAR в поиске .class файлов
- Рефлексия: проверка implements RobotPlugin
- Создание экземпляра через конструктор
- Сохранение в кэш (WeakReference)

---

## Сборка JAR

```
javac -cp "../../src/main/java" TestRobotPlugin.java
jar cf test_robot.jar TestRobotPlugin.class
```