package log;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Потокобезопасный кольцевой буфер фиксированного размера.
 * Поддерживает O(1) добавление и чтение сегментов без копирования.
 */
public class ConcurrentRingBuffer<T> implements Iterable<T> {
    private final Object[] buffer;
    private final int capacity;
    private final AtomicInteger size = new AtomicInteger(0); // текущий размер
    private volatile int head = 0; // индекс для чтения (самая старая запись)
    private volatile int tail = 0; // индекс для записи (следующая свободная позиция)

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ConcurrentRingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    /**
     * Добавляет элемент в буфер. Если буфер полон, перезаписывает самый старый.
     * @return true если элемент был добавлен
     */
    public boolean add(T element) {
        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }

        lock.writeLock().lock();
        try {
            buffer[tail] = element;
            tail = (tail + 1) % capacity;

            if (size.get() < capacity) {
                size.incrementAndGet();
            } else {
                // буфер полон, сдвигаем head
                head = (head + 1) % capacity;
            }
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Возвращает количество элементов в буфере.
     */
    public int size() {
        return size.get();
    }

    /**
     * Возвращает максимальную вместимость буфера.
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Получает сегмент записей из буфера.
     * @param startIndex индекс начала (от 0 до size-1)
     * @param endIndex индекс конца (не включая)
     * @return Snapshot элементов
     */
    @SuppressWarnings("unchecked")
    public RingBufferSnapshot<T> getSegment(int startIndex, int endIndex) {
        if (startIndex < 0 || endIndex > size.get() || startIndex > endIndex) {
            throw new IndexOutOfBoundsException(
                    String.format("Invalid segment: start=%d, end=%d, size=%d",
                            startIndex, endIndex, size.get()));
        }

        lock.readLock().lock();
        try {
            int count = endIndex - startIndex;
            Object[] segment = new Object[count];

            int startPos = (head + startIndex) % capacity;
            for (int i = 0; i < count; i++) {
                int pos = (startPos + i) % capacity;
                segment[i] = buffer[pos];
            }

            return new RingBufferSnapshot<>((T[]) segment, size.get());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Возвращает все элементы в виде снимка
     * @return Snapshot всех элементов
     */
    public RingBufferSnapshot<T> getAll() {
        return getSegment(0, size.get());
    }

    @Override
    public Iterator<T> iterator() {
        return new SafeIterator(getAll());
    }

    /**
     * Снимок буфера на момент времени. Потокобезопасный итератор.
     */
    public static class RingBufferSnapshot<T> implements Iterable<T> {
        private final T[] elements;
        private final int totalSize;

        @SuppressWarnings("unchecked")
        RingBufferSnapshot(T[] elements, int totalSize) {
            this.elements = elements.clone(); // защитная копия
            this.totalSize = totalSize;
        }

        public T get(int index) {
            if (index < 0 || index >= elements.length) {
                throw new IndexOutOfBoundsException();
            }
            return elements[index];
        }

        public int size() {
            return elements.length;
        }

        public int totalSize() {
            return totalSize;
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                private int currentIndex = 0;

                @Override
                public boolean hasNext() {
                    return currentIndex < elements.length;
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return elements[currentIndex++];
                }
            };
        }
    }

    /**
     * Безопасный итератор, работающий со снимком.
     */
    private class SafeIterator implements Iterator<T> {
        private final RingBufferSnapshot<T> snapshot;
        private int index = 0;

        SafeIterator(RingBufferSnapshot<T> snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public boolean hasNext() {
            return index < snapshot.size();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return snapshot.get(index++);
        }
    }
}