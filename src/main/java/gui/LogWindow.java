package gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.TextArea;

import javax.swing.JInternalFrame;
import javax.swing.JPanel;

import log.LogChangeListener;
import log.LogEntry;
import log.LogWindowSource;

public class LogWindow extends JInternalFrame implements LogChangeListener
{
    private LogWindowSource m_logSource; // источник логов
    private TextArea m_logContent; // текстовое поле для отображения

    public LogWindow(LogWindowSource logSource) 
    {
        super("Протокол работы", true, true, true, true);
        m_logSource = logSource;
        m_logSource.registerListener(this); // подписываемся на изменения
        m_logContent = new TextArea("");
        m_logContent.setSize(200, 500);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(m_logContent, BorderLayout.CENTER);
        getContentPane().add(panel);
        pack();
        updateLogContent();
    }

    // Обновление текста в окне
    private void updateLogContent()
    {
        StringBuilder content = new StringBuilder();
        for (LogEntry entry : m_logSource.all())
        {
            content.append(entry.getMessage()).append("\n");
        }
        m_logContent.setText(content.toString());
        m_logContent.invalidate();
    }

    // Вызывается при добавлении нового сообщения в лог
    @Override
    public void onLogChanged()
    {
        EventQueue.invokeLater(this::updateLogContent);
    }
}
