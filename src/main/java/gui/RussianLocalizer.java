package gui;

import java.util.Locale;
import javax.swing.UIManager;

public class RussianLocalizer {

    public void apply() {
        Locale.setDefault(Locale.of("ru", "RU"));

        // Диалоги подтверждения
        UIManager.put("OptionPane.yesButtonText", "Да");
        UIManager.put("OptionPane.noButtonText", "Нет");
        UIManager.put("OptionPane.cancelButtonText", "Отмена");
        UIManager.put("OptionPane.okButtonText", "OK");

        // Диалоги файлов
        UIManager.put("FileChooser.openButtonText", "Открыть");
        UIManager.put("FileChooser.cancelButtonText", "Отмена");
        UIManager.put("FileChooser.lookInLabelText", "Папка");
        UIManager.put("FileChooser.fileNameLabelText", "Имя файла");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Тип файлов");

        // ===== ВНУТРЕННИЕ ОКНА =====
        // Подсказки для кнопок в заголовке
        UIManager.put("InternalFrame.closeButtonToolTip", "Закрыть");
        UIManager.put("InternalFrame.iconButtonToolTip", "Свернуть");
        UIManager.put("InternalFrame.maxButtonToolTip", "Развернуть");
        UIManager.put("InternalFrame.restoreButtonToolTip", "Восстановить");

        // Названия кнопок в системном меню (правой кнопкой)
        UIManager.put("InternalFrameTitlePane.restoreButtonText", "Восстановить");
        UIManager.put("InternalFrameTitlePane.moveButtonText", "Переместить");
        UIManager.put("InternalFrameTitlePane.sizeButtonText", "Размер");
        UIManager.put("InternalFrameTitlePane.minimizeButtonText", "Свернуть");
        UIManager.put("InternalFrameTitlePane.maximizeButtonText", "Развернуть");
        UIManager.put("InternalFrameTitlePane.closeButtonText", "Закрыть");

        // Альтернативные ключи для некоторых версий Swing
        UIManager.put("InternalFrameTitlePane.restoreTitle", "Восстановить");
        UIManager.put("InternalFrameTitlePane.moveTitle", "Переместить");
        UIManager.put("InternalFrameTitlePane.sizeTitle", "Размер");
        UIManager.put("InternalFrameTitlePane.iconifyTitle", "Свернуть");
        UIManager.put("InternalFrameTitlePane.maximizeTitle", "Развернуть");
        UIManager.put("InternalFrameTitlePane.closeTitle", "Закрыть");
    }
}