package log;

// класс-контейнер для одного сообщения в логе

public class LogEntry
{
    private LogLevel m_logLevel; // уровень важности сообщения
    private String m_strMessage; // текст сообщения
    
    public LogEntry(LogLevel logLevel, String strMessage)
    {
        m_strMessage = strMessage;
        m_logLevel = logLevel;
    }
    
    public String getMessage()
    {
        return m_strMessage;
    }
    
    public LogLevel getLevel()
    {
        return m_logLevel;
    }
}

