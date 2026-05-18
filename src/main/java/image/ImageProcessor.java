package image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ImageProcessor {

    public List<Point> findLargestObjectContour(BufferedImage image) {
        if (image == null) return new ArrayList<>();

        // Масштабируем изображение
        image = scaleImageIfNeeded(image, 800, 800);

        BufferedImage gray = toGrayscale(image);
        int threshold = otsuThreshold(gray);
        BufferedImage binary = toBinary(gray, threshold);

        binary = ensureWhiteObject(binary);

        System.out.println("Threshold: " + threshold);

        // Основной поиск контура
        List<Point> contour = traceOuterBoundary(binary);

        System.out.println("Найдено точек контура: " + contour.size());

        // Упрощаем контур
        if (contour.size() > 35) {
            contour = simplifyContour(contour, 6.0);
            System.out.println("После упрощения: " + contour.size() + " точек");
        }

        return contour;
    }

    /**
     * Трассировка внешней границы
     */
    private List<Point> traceOuterBoundary(BufferedImage binary) {
        int width = binary.getWidth();
        int height = binary.getHeight();

        Point start = findStartPoint(binary);
        if (start == null) return new ArrayList<>();

        List<Point> contour = new ArrayList<>();
        Point current = start;
        Point prev = new Point(start.x - 1, start.y);

        int maxSteps = width * height * 4;
        int steps = 0;

        do {
            contour.add(new Point(current.x, current.y));
            steps++;

            Point next = getNextBoundaryPoint(binary, current, prev);
            if (next == null) break;

            prev = current;
            current = next;

        } while (!current.equals(start) && steps < maxSteps);

        if (contour.size() > 5) {
            contour.add(contour.get(0)); // замыкаем контур
        }

        return contour;
    }

    private Point findStartPoint(BufferedImage binary) {
        for (int y = 0; y < binary.getHeight(); y++) {
            for (int x = 0; x < binary.getWidth(); x++) {
                if (isWhite(binary, x, y)) {
                    return new Point(x, y);
                }
            }
        }
        return null;
    }

    private Point getNextBoundaryPoint(BufferedImage binary, Point curr, Point prev) {
        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dy = {0, 1, 1, 1, 0, -1, -1, -1};

        int startDir = 0;
        for (int i = 0; i < 8; i++) {
            if (curr.x + dx[i] == prev.x && curr.y + dy[i] == prev.y) {
                startDir = (i + 1) % 8;
                break;
            }
        }

        for (int i = 0; i < 8; i++) {
            int dir = (startDir + i) % 8;
            int nx = curr.x + dx[dir];
            int ny = curr.y + dy[dir];

            if (nx >= 0 && nx < binary.getWidth() && ny >= 0 && ny < binary.getHeight()) {
                if (isWhite(binary, nx, ny)) {
                    return new Point(nx, ny);
                }
            }
        }
        return null;
    }

    // ====================== Вспомогательные методы ======================

    private static BufferedImage scaleImageIfNeeded(BufferedImage image, int maxWidth, int maxHeight) {
        int w = image.getWidth();
        int h = image.getHeight();
        if (w <= maxWidth && h <= maxHeight) return image;

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
        if (whiteCount < binary.getWidth() * binary.getHeight() / 2) {
            System.out.println("Инвертируем изображение (объект был тёмным)");
            return invert(binary);
        }
        return binary;
    }

    private static boolean isWhite(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0xFF) > 110;
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

    public List<Point> simplifyContour(List<Point> points, double epsilon) {
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
            if (result.isEmpty()) result.add(start);
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