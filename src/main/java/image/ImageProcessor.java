package image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ImageProcessor {

    public static List<Point> findLargestObjectContour(BufferedImage image) {
        if (image == null) return new ArrayList<>();

        image = scaleImageIfNeeded(image, 800, 800);

        BufferedImage gray = toGrayscale(image);
        int threshold = otsuThreshold(gray);
        BufferedImage binary = toBinary(gray, threshold);

        System.out.println("Threshold: " + threshold);

        binary = ensureWhiteObject(binary);

        // Находим все белые пиксели (линии)
        List<Point> allWhitePixels = new ArrayList<>();
        for (int y = 0; y < binary.getHeight(); y++) {
            for (int x = 0; x < binary.getWidth(); x++) {
                if (isWhite(binary, x, y)) {
                    allWhitePixels.add(new Point(x, y));
                }
            }
        }

        if (allWhitePixels.isEmpty()) {
            System.out.println("Нет белых пикселей");
            return new ArrayList<>();
        }

        System.out.println("Всего белых пикселей: " + allWhitePixels.size());

        // Для тонкой линии используем алгоритм поиска пути
        List<Point> contour = extractLineContour(binary, allWhitePixels);
        System.out.println("Извлечено точек контура: " + contour.size());

        // Упрощаем контур для сглаживания
        if (contour.size() > 10) {
            contour = simplifyContour(contour, 5.0);
            System.out.println("После упрощения: " + contour.size() + " точек");
        }

        return contour;
    }

    /**
     * Алгоритм поиска контура для тонкой линии (толщиной 1 пиксель)
     */
    private static List<Point> extractLineContour(BufferedImage binary, List<Point> whitePixels) {
        if (whitePixels.size() < 3) return whitePixels;

        // Находим точку с минимальным x (самую левую)
        Point start = whitePixels.get(0);
        for (Point p : whitePixels) {
            if (p.x < start.x || (p.x == start.x && p.y < start.y)) {
                start = p;
            }
        }

        List<Point> contour = new ArrayList<>();
        Point current = start;
        Point previous = null;

        int maxIterations = whitePixels.size() * 2;
        int iterations = 0;

        do {
            contour.add(new Point(current.x, current.y));
            iterations++;

            if (iterations > maxIterations) {
                System.out.println("Превышен лимит итераций");
                break;
            }

            // Ищем следующую точку (8 направлений)
            Point next = findNextPoint(binary, current, previous);
            if (next == null) break;

            previous = current;
            current = next;

        } while ((current.x != start.x || current.y != start.y) && iterations < maxIterations);

        // Замыкаем контур
        if (contour.size() > 0) {
            Point first = contour.get(0);
            Point last = contour.get(contour.size() - 1);
            if (first.x != last.x || first.y != last.y) {
                contour.add(first);
            }
        }

        return contour;
    }

    /**
     * Находит следующую точку в контуре, исключая предыдущую
     */
    private static Point findNextPoint(BufferedImage binary, Point current, Point previous) {
        // 8 направлений
        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dy = {0, 1, 1, 1, 0, -1, -1, -1};

        Point bestPoint = null;
        int bestPriority = -1;

        for (int i = 0; i < 8; i++) {
            int nx = current.x + dx[i];
            int ny = current.y + dy[i];

            if (nx < 0 || nx >= binary.getWidth() || ny < 0 || ny >= binary.getHeight()) {
                continue;
            }

            if (previous != null && nx == previous.x && ny == previous.y) {
                continue; // Не возвращаемся назад
            }

            if (isWhite(binary, nx, ny)) {
                // Приоритет: сначала продолжаем в том же направлении
                int priority = 8 - i;
                if (bestPoint == null || priority > bestPriority) {
                    bestPoint = new Point(nx, ny);
                    bestPriority = priority;
                }
            }
        }

        return bestPoint;
    }

    private static BufferedImage scaleImageIfNeeded(BufferedImage image, int maxWidth, int maxHeight) {
        int w = image.getWidth();
        int h = image.getHeight();

        if (w <= maxWidth && h <= maxHeight) {
            return image;
        }

        double scale = Math.min((double) maxWidth / w, (double) maxHeight / h);
        int newW = (int) (w * scale);
        int newH = (int) (h * scale);

        BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
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
            System.out.println("Инвертируем изображение (объект тёмный)");
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

    public static List<Point> simplifyContour(List<Point> points, double epsilon) {
        if (points == null || points.size() < 3) {
            return points == null ? new ArrayList<>() : new ArrayList<>(points);
        }

        List<Point> result = new ArrayList<>();
        simplifyRDP(points, 0, points.size() - 1, epsilon, result);

        return result;
    }

    private static void simplifyRDP(List<Point> points, int startIdx, int endIdx, double epsilon, List<Point> result) {
        if (startIdx >= endIdx) return;

        double maxDist = 0;
        int index = -1;
        Point start = points.get(startIdx);
        Point end = points.get(endIdx);

        for (int i = startIdx + 1; i < endIdx; i++) {
            double dist = perpendicularDistance(points.get(i), start, end);
            if (dist > maxDist) {
                maxDist = dist;
                index = i;
            }
        }

        if (maxDist > epsilon && index != -1) {
            simplifyRDP(points, startIdx, index, epsilon, result);
            result.add(points.get(index));
            simplifyRDP(points, index, endIdx, epsilon, result);
        } else {
            if (result.isEmpty()) {
                result.add(start);
            }
            result.add(end);
        }
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

        int centerX = offsetX + targetWidth / 2;
        int centerY = offsetY + targetHeight / 2;
        int objectCenterX = (minX + maxX) / 2;
        int objectCenterY = (minY + maxY) / 2;

        List<Point> normalized = new ArrayList<>();
        for (Point p : contour) {
            int newX = centerX + (int) ((p.x - objectCenterX) * scale);
            int newY = centerY + (int) ((p.y - objectCenterY) * scale);
            normalized.add(new Point(newX, newY));
        }
        return normalized;
    }
}