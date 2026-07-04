package deque;

import java.util.Iterator;

/**
 * A deque backed by a circular array. Ends operations run in amortized
 * constant time. The array starts at length 8, grows by doubling when full,
 * and — for arrays of length 16 or more — shrinks (halves) whenever the usage
 * factor would drop below 25%, keeping usage at least 25% for large arrays.
 *
 * @param <T> the element type
 * @author appleweiping
 */
public class ArrayDeque<T> implements Deque<T>, Iterable<T> {

    private static final int INIT_CAPACITY = 8;
    private static final int MIN_SHRINK_CAPACITY = 16;
    private static final double MIN_USAGE = 0.25;

    private T[] items;
    private int size;
    /** Index of the current front element (valid when size > 0). */
    private int nextFirst;
    /** Index at which the next addLast will write. */
    private int nextLast;

    /** Create an empty deque. */
    @SuppressWarnings("unchecked")
    public ArrayDeque() {
        items = (T[]) new Object[INIT_CAPACITY];
        size = 0;
        nextFirst = INIT_CAPACITY / 2;
        nextLast = nextFirst + 1;
    }

    private int minusOne(int index) {
        return (index - 1 + items.length) % items.length;
    }

    private int plusOne(int index) {
        return (index + 1) % items.length;
    }

    /** Copy all elements (front to back) into a fresh array of the given capacity. */
    @SuppressWarnings("unchecked")
    private void resize(int capacity) {
        T[] fresh = (T[]) new Object[capacity];
        int p = plusOne(nextFirst);
        for (int i = 0; i < size; i += 1) {
            fresh[i] = items[p];
            p = plusOne(p);
        }
        items = fresh;
        nextFirst = capacity - 1;
        nextLast = size;
    }

    @Override
    public void addFirst(T item) {
        if (size == items.length) {
            resize(items.length * 2);
        }
        items[nextFirst] = item;
        nextFirst = minusOne(nextFirst);
        size += 1;
    }

    @Override
    public void addLast(T item) {
        if (size == items.length) {
            resize(items.length * 2);
        }
        items[nextLast] = item;
        nextLast = plusOne(nextLast);
        size += 1;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void printDeque() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i += 1) {
            sb.append(get(i));
            if (i < size - 1) {
                sb.append(' ');
            }
        }
        System.out.println(sb);
    }

    /** Shrink the array if it is large and usage has fallen below 25%. */
    private void maybeShrink() {
        if (items.length >= MIN_SHRINK_CAPACITY
                && (double) size / items.length < MIN_USAGE) {
            resize(items.length / 2);
        }
    }

    @Override
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        int first = plusOne(nextFirst);
        T item = items[first];
        items[first] = null;
        nextFirst = first;
        size -= 1;
        maybeShrink();
        return item;
    }

    @Override
    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        int last = minusOne(nextLast);
        T item = items[last];
        items[last] = null;
        nextLast = last;
        size -= 1;
        maybeShrink();
        return item;
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        int p = (plusOne(nextFirst) + index) % items.length;
        return items[p];
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayDequeIterator();
    }

    private class ArrayDequeIterator implements Iterator<T> {
        private int pos = 0;

        @Override
        public boolean hasNext() {
            return pos < size;
        }

        @Override
        public T next() {
            T item = get(pos);
            pos += 1;
            return item;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Deque)) {
            return false;
        }
        Deque<?> other = (Deque<?>) o;
        if (other.size() != this.size) {
            return false;
        }
        for (int i = 0; i < size; i += 1) {
            Object a = get(i);
            Object b = other.get(i);
            if (a == null ? b != null : !a.equals(b)) {
                return false;
            }
        }
        return true;
    }
}
