package log;

//(enum) - специальный тип данных, который содержит набор констант
// оценка важности сообщений

public enum LogLevel
{
    Trace(0),
    Debug(1), //отладочные сообщения
    Info(2), //информационные сообщения
    Warning(3),
    Error(4),
    Fatal(5);
    
    private int m_iLevel;
    
    private LogLevel(int iLevel)
    {
        m_iLevel = iLevel;
    }
    
    public int level()
    {
        return m_iLevel;
    }
}

