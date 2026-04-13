package gui;

import log.LogChangeListener;
import log.LogEntry;
import log.LogWindowSource;
import javax.swing.*;
import java.awt.*;

public class LogWindow extends JInternalFrame implements LogChangeListener {
    private final LogWindowSource logSource;
    private final JTextArea logContent;
    private boolean autoScroll = true;
    private volatile boolean needsUpdate = false;
    private String cachedContent = "";

    public LogWindow(LogWindowSource logSource) {
        super("Протокол работы", true, true, true, true);
        this.logSource = logSource;
        this.logSource.registerListener(this);

        logContent = new JTextArea();
        logContent.setEditable(false);
        logContent.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(logContent);
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            // Определяем, хочет ли пользователь автоскролл
            if (!e.getValueIsAdjusting()) {
                JScrollBar scrollBar = (JScrollBar) e.getSource();
                autoScroll = scrollBar.getValue() + scrollBar.getVisibleAmount()
                        >= scrollBar.getMaximum();
            }
        });

        getContentPane().add(scrollPane, BorderLayout.CENTER);
        setSize(300, 500);

        updateLogContent();
    }

    private void updateLogContent() {
        StringBuilder content = new StringBuilder();
        for (LogEntry entry : logSource.all()) {
            content.append(entry.getMessage()).append("\n");
        }
        String newContent = content.toString();

        // Обновляем только если содержимое изменилось
        if (!newContent.equals(cachedContent)) {
            cachedContent = newContent;
            SwingUtilities.invokeLater(() -> {
                logContent.setText(cachedContent);
                if (autoScroll) {
                    JScrollBar vertical = ((JScrollPane) logContent.getParent().getParent()).getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                }
            });
        }
    }

    @Override
    public void onLogChanged() {
        if (!needsUpdate) {
            needsUpdate = true;
            SwingUtilities.invokeLater(() -> {
                updateLogContent();
                needsUpdate = false;
            });
        }
    }
}