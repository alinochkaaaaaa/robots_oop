package image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImageLoaderTest {

    @TempDir
    File tempDir;

    @Test
    void loadImage_shouldLoadValidImage() throws IOException {
        // Создаём временный PNG файл
        File testImage = new File(tempDir, "test.png");
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "png", testImage);

        // Загружаем
        BufferedImage loaded = ImageLoader.loadImage(testImage);

        assertNotNull(loaded);
        assertEquals(10, loaded.getWidth());
        assertEquals(10, loaded.getHeight());
    }

    @Test
    void loadImage_shouldThrowExceptionForNonExistentFile() {
        File fakeFile = new File("/nonexistent/file.png");

        assertThrows(IOException.class, () -> {
            ImageLoader.loadImage(fakeFile);
        });
    }

    @Test
    void isSupportedFormat_shouldReturnTrueForPng() {
        File pngFile = new File("image.png");
        assertTrue(ImageLoader.isSupportedFormat(pngFile));
    }

    @Test
    void isSupportedFormat_shouldReturnFalseForTxt() {
        File txtFile = new File("document.txt");
        assertFalse(ImageLoader.isSupportedFormat(txtFile));
    }

    @Test
    void getSupportedFormats_shouldContainPng() {
        String[] formats = ImageLoader.getSupportedFormats();
        assertTrue(ArrayUtils.contains(formats, "png"));
    }
}

// Маленький вспомогательный класс, если нет Apache Commons
class ArrayUtils {
    static boolean contains(String[] array, String value) {
        for (String s : array) {
            if (s.equals(value)) return true;
        }
        return false;
    }
}