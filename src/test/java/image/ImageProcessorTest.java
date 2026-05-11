package image;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImageProcessorTest {

    @Test
    void toGrayscale_shouldReturnGrayImage() {
        BufferedImage colorImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        BufferedImage gray = ImageProcessor.toGrayscale(colorImage);
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, gray.getType());
    }

    @Test
    void toBinary_shouldReturnBinaryImage() {
        BufferedImage gray = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage binary = ImageProcessor.toBinary(gray, 128);
        assertEquals(BufferedImage.TYPE_BYTE_BINARY, binary.getType());
    }

    @Test
    void invert_shouldSwapBlackAndWhite() {
        BufferedImage binary = new BufferedImage(3, 3, BufferedImage.TYPE_BYTE_BINARY);
        binary.setRGB(1, 1, 0xFFFFFFFF);

        BufferedImage inverted = ImageProcessor.invert(binary);

        int centerPixel = inverted.getRGB(1, 1) & 0xFFFFFF;
        assertTrue(centerPixel == 0 || centerPixel == 0xFF000000);
    }


    @Test
    void findLargestObjectContour_shouldReturnEmptyForEmptyImage() {
        List<Point> contour = ImageProcessor.findLargestObjectContour(null);
        assertTrue(contour.isEmpty());
    }

    @Test
    void calculateArea_shouldComputeSquareArea() {
        List<Point> square = List.of(
                new Point(0, 0),
                new Point(10, 0),
                new Point(10, 10),
                new Point(0, 10)
        );

        double area = ImageProcessor.calculateArea(square);
        assertEquals(100.0, area, 0.01);
    }

    @Test
    void calculateArea_shouldHandleNull() {
        double area = ImageProcessor.calculateArea(null);
        assertEquals(0.0, area);
    }

    @Test
    void simplifyContour_shouldRemoveRedundantPoints() {
        List<Point> points = List.of(
                new Point(0, 0),
                new Point(2, 0),
                new Point(5, 0),
                new Point(10, 0),
                new Point(10, 5),
                new Point(10, 10),
                new Point(8, 10),
                new Point(0, 10)
        );

        List<Point> simplified = ImageProcessor.simplifyContour(points, 2.0);

        assertTrue(simplified.size() <= 5);
        assertTrue(simplified.size() >= 3);
    }

    @Test
    void simplifyContour_shouldHandleNull() {
        List<Point> result = ImageProcessor.simplifyContour(null, 1.0);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void normalizeContour_shouldScaleAndTranslate() {
        List<Point> contour = List.of(
                new Point(0, 0),
                new Point(10, 0),
                new Point(10, 10),
                new Point(0, 10)
        );

        List<Point> normalized = ImageProcessor.normalizeContour(contour, 100, 100, 50, 50);

        assertEquals(50, normalized.get(0).x);
        assertEquals(50, normalized.get(0).y);
        assertTrue(normalized.get(1).x >= 140);
    }

    @Test
    void normalizeContour_shouldHandleNull() {
        List<Point> result = ImageProcessor.normalizeContour(null, 100, 100, 0, 0);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}