package plugin;

import log.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Менеджер загрузки плагинов роботов.
 *
 * Паттерны и техники:
 * - Singleton: единственный экземпляр менеджера
 * - ClassLoader: динамическая загрузка классов из JAR
 * - WeakReference: кэш загруженных классов (позволяет GC выгружать классы)
 * - ConcurrentHashMap: потокобезопасное кэширование
 * - Рефлексия: поиск и создание экземпляров загруженных классов
 *
 */
public class RobotPluginManager {

    // ========== Singleton ==========
    private static final RobotPluginManager INSTANCE = new RobotPluginManager();

    public static RobotPluginManager getInstance() {
        return INSTANCE;
    }

    private RobotPluginManager() {
        // Приватный конструктор для синглтона
        loadPluginsFromCache();
    }

    // ========== Поля ==========

    /** Кэш загруженных классов (слабые ссылки — позволяют выгружать плагины) */
    private final Map<String, WeakReference<Class<? extends RobotPlugin>>> pluginClassCache
            = new ConcurrentHashMap<>();

    /** Загруженные экземпляры плагинов (по typeId) */
    private final Map<String, RobotPlugin> loadedPlugins
            = new ConcurrentHashMap<>();

    /** Пути к загруженным JAR-файлам (для сериализации) */
    private final List<String> loadedJarPaths = new ArrayList<>();

    /** ClassLoader для загрузки классов из JAR */
    private URLClassLoader currentClassLoader;

    // ========== Основные методы ==========

    /**
     * Загрузить робота из JAR-файла.
     *
     * Алгоритм:
     * 1. Проверка, что файл существует и является JAR
     * 2. Создание URLClassLoader для загрузки классов из JAR
     * 3. Поиск класса, реализующего RobotPlugin (через рефлексию)
     * 4. Создание экземпляра плагина
     * 5. Сохранение в кэш
     *
     * @param jarFile JAR-файл с плагином
     * @return загруженный плагин
     * @throws Exception если загрузка не удалась
     */
    public RobotPlugin loadRobotFromJar(File jarFile) throws Exception {
        Logger.debug("Начало загрузки плагина из: " + jarFile.getAbsolutePath());

        // 1. Валидация файла
        validateJarFile(jarFile);

        // 2. Проверка кэша (не загружали ли уже)
        String jarName = jarFile.getName();
        if (pluginClassCache.containsKey(jarName)) {
            WeakReference<Class<? extends RobotPlugin>> ref = pluginClassCache.get(jarName);
            if (ref.get() != null) {
                Logger.debug("Плагин уже загружен из кэша: " + jarName);
                // Возвращаем существующий плагин по typeId
                for (RobotPlugin plugin : loadedPlugins.values()) {
                    if (plugin.getRobotTypeId().equals(jarName.replace(".jar", ""))) {
                        return plugin;
                    }
                }
            }
        }

        // 3. Загрузка через URLClassLoader
        URL jarUrl = jarFile.toURI().toURL();
        currentClassLoader = new URLClassLoader(
                new URL[]{jarUrl},
                Thread.currentThread().getContextClassLoader()
        );

        // 4. Поиск класса, реализующего RobotPlugin
        Class<? extends RobotPlugin> pluginClass = findPluginClass(currentClassLoader);

        // 5. Создание экземпляра
        RobotPlugin plugin = pluginClass.getDeclaredConstructor().newInstance();

        // 6. Сохранение в кэш
        loadedPlugins.put(plugin.getRobotTypeId(), plugin);
        pluginClassCache.put(jarName, new WeakReference<>(pluginClass));
        loadedJarPaths.add(jarFile.getAbsolutePath());

        Logger.debug("Плагин успешно загружен: " + plugin.getDisplayName() +
                " (typeId: " + plugin.getRobotTypeId() + ")");

        return plugin;
    }

    /**
     * Проверка, что файл является валидным JAR.
     */
    private void validateJarFile(File jarFile) throws IOException {
        if (!jarFile.exists()) {
            throw new IOException("Файл не существует: " + jarFile.getAbsolutePath());
        }
        if (!jarFile.getName().toLowerCase().endsWith(".jar")) {
            throw new IOException("Файл не имеет расширение .jar: " + jarFile.getName());
        }
        // Проверяем, что это действительно JAR
        try (JarFile jf = new JarFile(jarFile)) {
            Manifest manifest = jf.getManifest();
            // Даже если манифеста нет, файл всё равно может быть валидным JAR
            Logger.debug("JAR-файл валиден: " + jarFile.getName());
        } catch (IOException e) {
            throw new IOException("Невалидный JAR-файл: " + e.getMessage(), e);
        }
    }

    /**
     * Поиск класса, реализующего интерфейс RobotPlugin, в загруженном JAR.
     * Сканирует все классы в JAR-файле.
     */
    @SuppressWarnings("unchecked")
    private Class<? extends RobotPlugin> findPluginClass(URLClassLoader classLoader)
            throws ClassNotFoundException, IOException {

        // Получаем URL JAR-файла
        URL jarUrl = classLoader.getURLs()[0];
        String jarPath = jarUrl.getPath();

        // Открываем JAR и сканируем все entry
        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(new File(jarPath))) {

            java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                // Ищем .class файлы
                if (entryName.endsWith(".class") && !entryName.contains("$")) { // игнорируем вложенные классы
                    String className = entryName.replace("/", ".").replace(".class", "");

                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        if (isRobotPluginClass(clazz)) {
                            Logger.debug("Найден класс-плагин: " + className);
                            return (Class<? extends RobotPlugin>) clazz;
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        // Игнорируем ошибки загрузки отдельных классов
                        Logger.debug("Не удалось загрузить класс " + className + ": " + e.getMessage());
                    }
                }
            }
        }

        throw new ClassNotFoundException(
                "Не найден класс, реализующий RobotPlugin. " +
                        "Убедитесь, что в JAR есть класс, реализующий интерфейс plugin.RobotPlugin"
        );
    }

    /**
     * Проверяет, реализует ли класс интерфейс RobotPlugin.
     * Используется рефлексия.
     */
    private boolean isRobotPluginClass(Class<?> clazz) {
        if (clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
            return false;
        }

        // Проверяем, реализует ли класс интерфейс RobotPlugin
        for (Class<?> iface : clazz.getInterfaces()) {
            if (iface.getName().equals("plugin.RobotPlugin")) {
                return true;
            }
        }

        // Проверяем суперклассы (на случай наследования)
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return isRobotPluginClass(superClass);
        }

        return false;
    }

    // ========== Методы для работы с кэшем и сериализацией ==========

    /**
     * Получить все загруженные типы роботов.
     * @return список всех доступных плагинов
     */
    public List<RobotPlugin> getAllRobotTypes() {
        return new ArrayList<>(loadedPlugins.values());
    }

    /**
     * Получить плагин по его typeId.
     * @param typeId уникальный идентификатор типа робота
     * @return плагин или null, если не найден
     */
    public RobotPlugin getPluginByTypeId(String typeId) {
        return loadedPlugins.get(typeId);
    }

    /**
     * Получить список путей к загруженным JAR.
     * Для сериализации (используется Разработчиком C).
     */
    public List<String> getLoadedJarPaths() {
        return new ArrayList<>(loadedJarPaths);
    }

    /**
     * Очистить кэш и выгрузить все плагины.
     * Удаление слабых ссылок + закрытие ClassLoader.
     */
    public void unloadAllPlugins() {
        loadedPlugins.clear();
        pluginClassCache.clear();
        loadedJarPaths.clear();

        if (currentClassLoader != null) {
            try {
                currentClassLoader.close();
            } catch (IOException e) {
                Logger.debug("Ошибка закрытия ClassLoader: " + e.getMessage());
            }
            currentClassLoader = null;
        }

        Logger.debug("Все плагины выгружены");
    }

    /**
     * Загрузить плагины из кэша (при запуске).
     * Вызывается в конструкторе.
     * Восстанавливает плагины из сохранённой конфигурации.
     */
    private void loadPluginsFromCache() {
        // Здесь будет загрузка из XML-файла
        // Реализуется после того, как Разработчик C сделает сериализацию
        // Пока просто заглушка
        Logger.debug("Загрузка плагинов из кэша... (пока пусто)");
    }

    /**
     * Получить кэшированный класс (для тестов).
     */
    WeakReference<Class<? extends RobotPlugin>> getCachedClass(String jarName) {
        return pluginClassCache.get(jarName);
    }
}