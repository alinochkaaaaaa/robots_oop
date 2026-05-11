package image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ImageProcessor {

    // ========================
    // ГЛАВНЫЙ ПУБЛИЧНЫЙ МЕТОД
    // ========================

    public static List<Point> findLargestObjectContour(BufferedImage image) {
        if (image == null) return new ArrayList<>();

        // Ограничиваем размер изображения (чтобы не было OutOfMemory)
        image = scaleImageIfNeeded(image, 800, 800);

        BufferedImage gray = toGrayscale(image);
        int threshold = otsuThreshold(gray);
        BufferedImage binary = toBinary(gray, threshold);
        binary = ensureWhiteObject(binary);

        ComponentInfo largest = findLargestComponent(binary);
        if (largest == null || largest.pixels.isEmpty()) return new ArrayList<>();

        List<Point> contour = extractContour(binary, largest);
        return simplifyContour(contour, 2.0);
    }

    /**
     * Масштабирование слишком больших изображений
     */
    private static BufferedImage scaleImageIfNeeded(BufferedImage image, int maxWidth, int maxHeight) {
        int w = image.getWidth();
        int h = image.getHeight();

        if (w <= maxWidth && h <= maxHeight) {
            return image;
        }

        double scale = Math.min((double) maxWidth / w, (double) maxHeight / h);
        int newW = (int) (w * scale);
        int newH = (int) (h * scale);

        BufferedImage scaled = new BufferedImage(newW, newH, image.getType());
        Graphics2D g = scaled.createGraphics();
        g.drawImage(image, 0, 0, newW, newH, null);
        g.dispose();

        return scaled;
    }

    public static BufferedImage toGrayscale(BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = result.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    public static BufferedImage toBinary(BufferedImage image, int threshold) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int gray = image.getRGB(x, y) & 0xFF;
                int value = gray < threshold ? 0 : 255;
                result.setRGB(x, y, value == 0 ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return result;
    }

    public static BufferedImage invert(BufferedImage image) {
        BufferedImage inverted = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                inverted.setRGB(x, y, 0xFFFFFFFF ^ image.getRGB(x, y));
            }
        }
        return inverted;
    }

    private static BufferedImage ensureWhiteObject(BufferedImage binary) {
        int whiteCount = 0;
        for (int y = 0; y < binary.getHeight(); y++) {
            for (int x = 0; x < binary.getWidth(); x++) {
                if (isWhite(binary, x, y)) whiteCount++;
            }
        }
        int total = binary.getWidth() * binary.getHeight();
        if (whiteCount < total / 2) {
            return invert(binary);
        }
        return binary;
    }

    private static boolean isWhite(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0xFF) > 128;
    }

    public static int otsuThreshold(BufferedImage grayImage) {
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();

        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                histogram[grayImage.getRGB(x, y) & 0xFF]++;
            }
        }

        int totalPixels = width * height;
        float sum = 0;
        for (int i = 0; i < 256; i++) sum += i * histogram[i];

        float sumB = 0;
        int wB = 0, wF = 0;
        float varMax = 0;
        int threshold = 0;

        for (int i = 0; i < 256; i++) {
            wB += histogram[i];
            if (wB == 0) continue;
            wF = totalPixels - wB;
            if (wF == 0) break;

            sumB += i * histogram[i];
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;
            float varBetween = wB * wF * (mB - mF) * (mB - mF);

            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = i;
            }
        }
        return threshold;
    }

    private static class ComponentInfo {
        List<Point> pixels = new ArrayList<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        int getArea() { return pixels.size(); }
    }

    private static ComponentInfo findLargestComponent(BufferedImage binary) {
        int width = binary.getWidth();
        int height = binary.getHeight();
        boolean[][] visited = new boolean[height][width];

        ComponentInfo largest = null;
        int maxArea = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isWhite(binary, x, y) && !visited[y][x]) {
                    ComponentInfo component = floodFill(binary, visited, x, y);
                    if (component.getArea() > maxArea && component.getArea() > 100) {
                        maxArea = component.getArea();
                        largest = component;
                    }
                }
            }
        }
        return largest;
    }

    private static ComponentInfo floodFill(BufferedImage binary, boolean[][] visited, int startX, int startY) {
        int width = binary.getWidth();
        int height = binary.getHeight();
        ComponentInfo component = new ComponentInfo();
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(startX, startY));
        visited[startY][startX] = true;

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            component.pixels.add(p);
            component.minX = Math.min(component.minX, p.x);
            component.minY = Math.min(component.minY, p.y);
            component.maxX = Math.max(component.maxX, p.x);
            component.maxY = Math.max(component.maxY, p.y);

            for (int i = 0; i < 4; i++) {
                int nx = p.x + dx[i];
                int ny = p.y + dy[i];
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && !visited[ny][nx] && isWhite(binary, nx, ny)) {
                    visited[ny][nx] = true;
                    queue.add(new Point(nx, ny));
                }
            }
        }
        return component;
    }

    /**
     * ИСПРАВЛЕННЫЙ метод извлечения контура
     */
    private static List<Point> extractContour(BufferedImage binary, ComponentInfo component) {
        List<Point> contour = new ArrayList<>();

        // Находим стартовую точку
        Point start = null;
        for (Point p : component.pixels) {
            if (start == null || p.x < start.x || (p.x == start.x && p.y < start.y)) {
                start = p;
            }
        }
        if (start == null) return contour;

        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dy = {0, 1, 1, 1, 0, -1, -1, -1};

        Point current = start;
        int direction = 0;

        // Защита от зацикливания
        int maxPoints = component.pixels.size() * 2;
        int pointCount = 0;

        do {
            contour.add(new Point(current.x, current.y));
            pointCount++;

            if (pointCount > maxPoints) {
                System.err.println("Warning: contour extraction exceeded limit");
                break;
            }

            boolean found = false;
            for (int i = 0; i < 8; i++) {
                int newDir = (direction + 7 - i) % 8;
                int nx = current.x + dx[newDir];
                int ny = current.y + dy[newDir];

                if (nx >= 0 && nx < binary.getWidth() && ny >= 0 && ny < binary.getHeight()) {
                    if (isWhite(binary, nx, ny)) {
                        current = new Point(nx, ny);
                        direction = newDir;
                        found = true;
                        break;
                    }
                }
            }

            if (!found) break;
            if (current.x == start.x && current.y == start.y) break;

        } while (pointCount < maxPoints);

        return contour;
    }

    public static List<Point> simplifyContour(List<Point> points, double epsilon) {
        if (points == null || points.size() < 3) {
            return new ArrayList<>(points == null ? List.of() : points);
        }

        double maxDist = 0;
        int index = -1;
        Point start = points.get(0);
        Point end = points.get(points.size() - 1);

        for (int i = 1; i < points.size() - 1; i++) {
            double dist = perpendicularDistance(points.get(i), start, end);
            if (dist > maxDist) {
                maxDist = dist;
                index = i;
            }
        }

        List<Point> result = new ArrayList<>();
        if (maxDist > epsilon && index != -1) {
            List<Point> left = simplifyContour(points.subList(0, index + 1), epsilon);
            List<Point> right = simplifyContour(points.subList(index, points.size()), epsilon);
            result.addAll(left);
            result.remove(result.size() - 1);
            result.addAll(right);
        } else {
            result.add(start);
            result.add(end);
        }
        return result;
    }

    private static double perpendicularDistance(Point p, Point a, Point b) {
        double abX = b.x - a.x;
        double abY = b.y - a.y;
        double apX = p.x - a.x;
        double apY = p.y - a.y;
        double ab2 = abX * abX + abY * abY;
        if (ab2 == 0) return Math.hypot(apX, apY);
        double t = (apX * abX + apY * abY) / ab2;
        if (t < 0) return Math.hypot(apX, apY);
        if (t > 1) return Math.hypot(p.x - b.x, p.y - b.y);
        double projX = a.x + t * abX;
        double projY = a.y + t * abY;
        return Math.hypot(p.x - projX, p.y - projY);
    }

    public static double calculateArea(List<Point> contour) {
        if (contour == null || contour.size() < 3) return 0;
        double area = 0;
        int n = contour.size();
        for (int i = 0; i < n; i++) {
            Point p1 = contour.get(i);
            Point p2 = contour.get((i + 1) % n);
            area += (double) p1.x * p2.y - (double) p2.x * p1.y;
        }
        return Math.abs(area) / 2.0;
    }

    public static List<Point> normalizeContour(List<Point> contour, int targetWidth, int targetHeight, int offsetX, int offsetY) {
        if (contour == null || contour.isEmpty()) return new ArrayList<>();

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (Point p : contour) {
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }

        int width = maxX - minX;
        int height = maxY - minY;
        if (width == 0 || height == 0) return new ArrayList<>(contour);

        double scale = Math.min((double) targetWidth / width, (double) targetHeight / height);

        List<Point> normalized = new ArrayList<>();
        for (Point p : contour) {
            int newX = offsetX + (int) ((p.x - minX) * scale);
            int newY = offsetY + (int) ((p.y - minY) * scale);
            normalized.add(new Point(newX, newY));
        }
        return normalized;
    }
}