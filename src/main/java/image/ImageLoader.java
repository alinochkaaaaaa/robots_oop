package image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Загрузчик изображений для режима трассировки.
 *
 * Задача: загрузить изображение с диска и проверить, что формат поддерживается.
 */
public class ImageLoader {

    /** Поддерживаемые форматы изображений */
    private static final String[] SUPPORTED_FORMATS = {"png", "jpg", "jpeg", "bmp", "gif"};

    /**
     * Загрузить изображение из файла.
     */
    public static BufferedImage loadImage(File file) throws IOException {
        // Проверка: существует ли файл
        if (file == null || !file.exists()) {
            throw new IOException("Файл не существует: " + file);
        }

        // Пытаемся прочитать изображение через стандартную Java-библиотеку
        BufferedImage image = ImageIO.read(file);

        // Если ImageIO вернул null — значит формат не распознан
        if (image == null) {
            throw new IOException("Не удалось загрузить изображение. " +
                    "Поддерживаемые форматы: PNG, JPG, JPEG, BMP, GIF");
        }

        // заменяем прозрачный фон на белый
        if (image.getTransparency() != BufferedImage.OPAQUE) {
            BufferedImage withBackground = new BufferedImage(
                    image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = withBackground.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.drawImage(image, 0, 0, null);
            g.dispose();
            image = withBackground;
            System.out.println("Прозрачный фон заменён на белый");
        }

        return image;
    }

    /**
     * Проверить, поддерживается ли формат файла (по расширению).
     */
    public static boolean isSupportedFormat(File file) {
        String name = file.getName().toLowerCase();
        for (String format : SUPPORTED_FORMATS) {
            if (name.endsWith("." + format)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Получить массив поддерживаемых форматов (для фильтра JFileChooser).
     */
    public static String[] getSupportedFormats() {
        return SUPPORTED_FORMATS.clone();
    }

    /**
     * Получить описание форматов для отображения в UI.
     */
    public static String getFormatDescription() {
        return "Изображения (" + String.join(", ", SUPPORTED_FORMATS) + ")";
    }
}