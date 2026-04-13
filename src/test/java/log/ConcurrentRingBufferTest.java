package log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConcurrentRingBuffer Тесты")
class ConcurrentRingBufferTest {

    private ConcurrentRingBuffer<String> buffer;
    private static final int CAPACITY = 5;

    @BeforeEach
    void setUp() {
        buffer = new ConcurrentRingBuffer<>(CAPACITY);
    }

    @Test
    @DisplayName("1. Добавление элементов в буфер и проверка размера")
    void testAddAndSize() {
        assertEquals(0, buffer.size());
        assertEquals(CAPACITY, buffer.capacity());

        buffer.add("A");
        assertEquals(1, buffer.size());

        buffer.add("B");
        buffer.add("C");
        assertEquals(3, buffer.size());
    }

    @Test
    @DisplayName("2. Перезапись старых данных при заполнении буфера")
    void testOverwriteWhenFull() {
        // Заполняем буфер
        for (int i = 0; i < CAPACITY; i++) {
            buffer.add("Element" + i);
        }
        assertEquals(CAPACITY, buffer.size());

        // Добавляем еще один элемент - самый старый должен перезаписаться
        buffer.add("NewElement");
        assertEquals(CAPACITY, buffer.size()); // размер не изменился

        // Проверяем, что самые старые данные перезаписались
        ConcurrentRingBuffer.RingBufferSnapshot<String> snapshot = buffer.getAll();
        assertEquals("NewElement", snapshot.get(CAPACITY - 1));
    }

    @Test
    @DisplayName("3. Получение сегмента записей")
    void testGetSegment() {
        // Добавляем тестовые данные
        for (int i = 0; i < CAPACITY; i++) {
            buffer.add("Msg" + i);
        }

        // Получаем сегмент со 2 по 4 элемент (индексы 1, 2, 3)
        ConcurrentRingBuffer.RingBufferSnapshot<String> segment = buffer.getSegment(1, 4);

        assertEquals(3, segment.size());
        assertEquals("Msg1", segment.get(0));
        assertEquals("Msg2", segment.get(1));
        assertEquals("Msg3", segment.get(2));
    }

    @Test
    @DisplayName("4. Получение всех элементов (getAll)")
    void testGetAll() {
        for (int i = 0; i < CAPACITY; i++) {
            buffer.add("Msg" + i);
        }

        ConcurrentRingBuffer.RingBufferSnapshot<String> all = buffer.getAll();

        assertEquals(CAPACITY, all.size());
        assertEquals(CAPACITY, all.totalSize());

        for (int i = 0; i < CAPACITY; i++) {
            assertEquals("Msg" + i, all.get(i));
        }
    }

    @Test
    @DisplayName("5. Итератор должен корректно обходить элементы")
    void testIterator() {
        List<String> expected = List.of("A", "B", "C", "D", "E");
        for (String item : expected) {
            buffer.add(item);
        }

        List<String> actual = new ArrayList<>();
        for (String item : buffer) {
            actual.add(item);
        }

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("6. Итератор должен быть потокобезопасным (работа со снимком)")
    void testIteratorThreadSafety() throws InterruptedException {
        // Добавляем начальные данные
        for (int i = 0; i < CAPACITY; i++) {
            buffer.add("Initial" + i);
        }

        // Получаем итератор (создается снимок)
        Iterator<String> iterator = buffer.iterator();

        // Во время итерации добавляем новые элементы
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            for (int i = 0; i < 100; i++) {
                buffer.add("New" + i);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return null;
        });

        // Итерируемся по снимку - не должно быть ConcurrentModificationException
        List<String> snapshotElements = new ArrayList<>();
        while (iterator.hasNext()) {
            snapshotElements.add(iterator.next());
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        // Снимок должен содержать только начальные элементы
        assertEquals(CAPACITY, snapshotElements.size());
        for (int i = 0; i < CAPACITY; i++) {
            assertTrue(snapshotElements.get(i).startsWith("Initial"));
        }
    }

    @Test
    @DisplayName("7. Потокобезопасность при одновременной записи из нескольких потоков")
    void testConcurrentAdd() throws InterruptedException {
        int threadCount = 10;
        int addsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successfulAdds = new AtomicInteger(0);

        // Запускаем несколько потоков, которые одновременно добавляют элементы
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < addsPerThread; j++) {
                        buffer.add("Thread" + threadId + "_Msg" + j);
                        successfulAdds.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Размер буфера не может превышать capacity
        assertTrue(buffer.size() <= CAPACITY);

        // Проверяем, что все добавления прошли без ошибок
        assertEquals(threadCount * addsPerThread, successfulAdds.get());
    }

    @Test
    @DisplayName("8. Обработка некорректных индексов в getSegment")
    void testInvalidSegmentIndices() {
        buffer.add("A");
        buffer.add("B");
        buffer.add("C");

        // Отрицательный startIndex
        assertThrows(IndexOutOfBoundsException.class, () -> {
            buffer.getSegment(-1, 2);
        });

        // endIndex больше размера
        assertThrows(IndexOutOfBoundsException.class, () -> {
            buffer.getSegment(0, 10);
        });

        // startIndex > endIndex
        assertThrows(IndexOutOfBoundsException.class, () -> {
            buffer.getSegment(3, 1);
        });
    }

    @Test
    @DisplayName("9. Добавление null элемента должно выбрасывать исключение")
    void testAddNullElement() {
        assertThrows(IllegalArgumentException.class, () -> {
            buffer.add(null);
        });
    }

    @Test
    @DisplayName("10. Проверка работы с переполнением буфера (кольцевое поведение)")
    void testCircularBehavior() {
        // Добавляем больше элементов, чем capacity
        for (int i = 0; i < CAPACITY * 3; i++) {
            buffer.add("Item" + i);
        }

        // Размер должен оставаться равным capacity
        assertEquals(CAPACITY, buffer.size());

        // Проверяем, что последние добавленные элементы сохранились
        ConcurrentRingBuffer.RingBufferSnapshot<String> snapshot = buffer.getAll();
        for (int i = 0; i < CAPACITY; i++) {
            String expected = "Item" + ((CAPACITY * 2) + i);
            assertEquals(expected, snapshot.get(i));
        }
    }
    @Test
    @DisplayName("11. getSegment на пустом буфере возвращает пустой сегмент")
    void testGetSegmentOnEmptyBuffer() {
        // Буфер пустой, ничего не добавляли

        ConcurrentRingBuffer.RingBufferSnapshot<String> segment = buffer.getSegment(0, 0);

        assertNotNull(segment);
        assertEquals(0, segment.size());
        assertEquals(0, segment.totalSize());
    }

    @Test
    @DisplayName("12. getSegment возвращает весь буфер при start=0 и end=size")
    void testGetSegmentEntireBuffer() {
        // Добавляем элементы
        for (int i = 0; i < CAPACITY; i++) {
            buffer.add("Item" + i);
        }

        ConcurrentRingBuffer.RingBufferSnapshot<String> segment = buffer.getSegment(0, buffer.size());

        assertEquals(CAPACITY, segment.size());
        assertEquals("Item0", segment.get(0));
        assertEquals("Item1", segment.get(1));
        assertEquals("Item2", segment.get(2));
        assertEquals("Item3", segment.get(3));
        assertEquals("Item4", segment.get(4));
    }

    @Test
    @DisplayName("13. getSegment возвращает корректный сегмент из середины буфера")
    void testGetSegmentFromMiddle() {
        // Добавляем 5 элементов
        for (int i = 0; i < CAPACITY; i++) {
            buffer.add("Msg" + i);
        }

        // Берём сегмент с индекса 1 по 4 (элементы 1,2,3)
        ConcurrentRingBuffer.RingBufferSnapshot<String> segment = buffer.getSegment(1, 4);

        assertEquals(3, segment.size());
        assertEquals("Msg1", segment.get(0));
        assertEquals("Msg2", segment.get(1));
        assertEquals("Msg3", segment.get(2));
    }

    @Test
    @DisplayName("14. getSegment возвращает последний элемент при start = size-1, end = size")
    void testGetSegmentLastElement() {
        buffer.add("First");
        buffer.add("Second");
        buffer.add("Third");

        ConcurrentRingBuffer.RingBufferSnapshot<String> segment = buffer.getSegment(2, 3);

        assertEquals(1, segment.size());
        assertEquals("Third", segment.get(0));
    }

    @Test
    @DisplayName("15. getSegment работает корректно после переполнения буфера")
    void testGetSegmentAfterOverflow() {
        // Заполняем буфер с переполнением (добавляем 7 элементов при capacity=5)
        for (int i = 0; i < CAPACITY + 2; i++) {
            buffer.add("Element" + i);
        }

        // В буфере должны быть: Element2, Element3, Element4, Element5, Element6
        // (Element0 и Element1 перезаписались)
        ConcurrentRingBuffer.RingBufferSnapshot<String> segment = buffer.getSegment(0, buffer.size());

        assertEquals(CAPACITY, segment.size());
        assertEquals("Element2", segment.get(0));
        assertEquals("Element3", segment.get(1));
        assertEquals("Element4", segment.get(2));
        assertEquals("Element5", segment.get(3));
        assertEquals("Element6", segment.get(4));
    }
}
