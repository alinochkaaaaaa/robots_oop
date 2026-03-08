package log;

import java.util.*;

public class LogWindowSource {
    private final int m_iQueueLength;
    private final Queue<LogEntry> m_messages; // Queue вместо List
    private final List<LogChangeListener> m_listeners;
    private volatile LogChangeListener[] m_activeListeners;

    public LogWindowSource(int iQueueLength) {
        m_iQueueLength = iQueueLength;
        m_messages = new LinkedList<>(); // LinkedList как очередь
        m_listeners = new ArrayList<>();
    }

    public void registerListener(LogChangeListener listener) {
        synchronized(m_listeners) {
            m_listeners.add(listener);
            m_activeListeners = null;
        }
    }

    public void unregisterListener(LogChangeListener listener) {
        synchronized(m_listeners) {
            m_listeners.remove(listener);
            m_activeListeners = null;
        }
    }

    public void append(LogLevel logLevel, String strMessage) {
        synchronized(m_messages) {
            LogEntry entry = new LogEntry(logLevel, strMessage);
            m_messages.add(entry);

            while (m_messages.size() > m_iQueueLength) {
                m_messages.poll(); // Удаляем самое старое сообщение
            }
        }

        notifyListeners();
    }

    private void notifyListeners() {
        LogChangeListener[] activeListeners = m_activeListeners;
        if (activeListeners == null) {
            synchronized (m_listeners) {
                if (m_activeListeners == null) {
                    activeListeners = m_listeners.toArray(new LogChangeListener[0]);
                    m_activeListeners = activeListeners;
                }
            }
        }
        for (LogChangeListener listener : activeListeners) {
            listener.onLogChanged();
        }
    }

    public int size() {
        synchronized(m_messages) {
            return m_messages.size();
        }
    }

    public Iterable<LogEntry> all() {
        synchronized(m_messages) {
            return new ArrayList<>(m_messages);
        }
    }
}