package log;

import java.util.ArrayList;
import java.util.Collections;

// volatile - переменная может меняться в разных потоках
// synchronized - защита от одновременного доступа из разных потоков

/**
 * Что починить:
 * 1. Этот класс порождает утечку ресурсов (связанные слушатели оказываются
 * удерживаемыми в памяти)
 * 2. Этот класс хранит активные сообщения лога, но в такой реализации он 
 * их лишь накапливает. Надо же, чтобы количество сообщений в логе было ограничено 
 * величиной m_iQueueLength (т.е. реально нужна очередь сообщений 
 * ограниченного размера) 
 */

public class LogWindowSource
{
    private int m_iQueueLength; // макс кол-во сообщений
    
    private ArrayList<LogEntry> m_messages; // список сообщений
    private final ArrayList<LogChangeListener> m_listeners; // список подписчиков
    private volatile LogChangeListener[] m_activeListeners; // кэш подписчиков

    public LogWindowSource(int iQueueLength) 
    {
        m_iQueueLength = iQueueLength;
        m_messages = new ArrayList<LogEntry>(iQueueLength);
        m_listeners = new ArrayList<LogChangeListener>();
    }

    //подписка на уведомления

    public void registerListener(LogChangeListener listener)
    {
        synchronized(m_listeners)
        {
            m_listeners.add(listener);
            m_activeListeners = null; // сброс кэша
        }
    }

    // отписка от уведомлений

    public void unregisterListener(LogChangeListener listener)
    {
        synchronized(m_listeners)
        {
            m_listeners.remove(listener);
            m_activeListeners = null; // сброс кэша
        }
    }

    // добавление нового сообщения

    public void append(LogLevel logLevel, String strMessage)
    {
        LogEntry entry = new LogEntry(logLevel, strMessage);
        m_messages.add(entry);

        // уведомление всех подписчиков
        LogChangeListener [] activeListeners = m_activeListeners;
        if (activeListeners == null)
        {
            synchronized (m_listeners)
            {
                if (m_activeListeners == null)
                {
                    activeListeners = m_listeners.toArray(new LogChangeListener [0]);
                    m_activeListeners = activeListeners;
                }
            }
        }
        for (LogChangeListener listener : activeListeners)
        {
            listener.onLogChanged(); // вызов метода подписчика
        }
    }
    
    public int size()
    {
        return m_messages.size();
    }

    public Iterable<LogEntry> range(int startFrom, int count)
    {
        if (startFrom < 0 || startFrom >= m_messages.size())
        {
            return Collections.emptyList();
        }
        int indexTo = Math.min(startFrom + count, m_messages.size());
        return m_messages.subList(startFrom, indexTo);
    }

    public Iterable<LogEntry> all()
    {
        return m_messages;
    }
}
