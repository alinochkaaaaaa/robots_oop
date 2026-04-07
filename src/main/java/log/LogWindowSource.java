package log;

import java.util.*;

public class LogWindowSource {
    private final ConcurrentRingBuffer<LogEntry> buffer;
    private final List<LogChangeListener> listeners;
    private volatile LogChangeListener[] activeListeners;

    public LogWindowSource(int queueLength) {
        this.buffer = new ConcurrentRingBuffer<>(queueLength);
        this.listeners = new ArrayList<>();
    }

    public void registerListener(LogChangeListener listener) {
        synchronized(listeners) {
            listeners.add(listener);
            activeListeners = null;
        }
    }

    public void unregisterListener(LogChangeListener listener) {
        synchronized(listeners) {
            listeners.remove(listener);
            activeListeners = null;
        }
    }

    public void append(LogLevel logLevel, String strMessage) {
        LogEntry entry = new LogEntry(logLevel, strMessage); // O(1)
        buffer.add(entry);
        Iterable<LogEntry> segment = getSegment(0, size());
        notifyListeners();
    }

    private void notifyListeners() {
        LogChangeListener[] active = activeListeners;
        if (active == null) {
            synchronized (listeners) {
                if (activeListeners == null) {
                    activeListeners = listeners.toArray(new LogChangeListener[0]);
                    active = activeListeners;
                }
            }
        }
        for (LogChangeListener listener : active) {
            listener.onLogChanged();
        }
    }

    public int size() {
        return buffer.size();
    }

    /**
     * Получить сегмент записей (для эффективной отрисовки окна).
     */
    public Iterable<LogEntry> getSegment(int startIndex, int endIndex) {
        ConcurrentRingBuffer.RingBufferSnapshot<LogEntry> snapshot =
                buffer.getSegment(startIndex, endIndex);
        return snapshot;
    }

    /**
     * Получить все записи.
     */
    public Iterable<LogEntry> all() {
        return buffer.getAll();
    }
}