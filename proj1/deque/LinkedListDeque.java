package deque;

import java.util.Iterator;

/**
 * A deque backed by a circular, sentinel-headed, doubly-linked list.
 * All ends operations ({@code addFirst}, {@code addLast}, {@code removeFirst},
 * {@code removeLast}, {@code size}, {@code isEmpty}) run in constant time and
 * use no loops or recursion. {@code get} runs in O(index) via iteration and
 * {@code getRecursive} via recursion.
 *
 * @param <T> the element type
 * @author appleweiping
 */
public class LinkedListDeque<T> implements Deque<T>, Iterable<T> {

    /** A single doubly-linked node. */
    private class Node {
        private T item;
        private Node prev;
        private Node next;

        Node(T item, Node prev, Node next) {
            this.item = item;
            this.prev = prev;
            this.next = next;
        }
    }

    /** Circular sentinel: sentinel.next is the first node, sentinel.prev the last. */
    private final Node sentinel;
    private int size;

    /** Create an empty deque. */
    public LinkedListDeque() {
        sentinel = new Node(null, null, null);
        sentinel.prev = sentinel;
        sentinel.next = sentinel;
        size = 0;
    }

    @Override
    public void addFirst(T item) {
        Node first = new Node(item, sentinel, sentinel.next);
        sentinel.next.prev = first;
        sentinel.next = first;
        size += 1;
    }

    @Override
    public void addLast(T item) {
        Node last = new Node(item, sentinel.prev, sentinel);
        sentinel.prev.next = last;
        sentinel.prev = last;
        size += 1;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void printDeque() {
        StringBuilder sb = new StringBuilder();
        Node p = sentinel.next;
        while (p != sentinel) {
            sb.append(p.item);
            if (p.next != sentinel) {
                sb.append(' ');
            }
            p = p.next;
        }
        System.out.println(sb);
    }

    @Override
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        Node first = sentinel.next;
        sentinel.next = first.next;
        first.next.prev = sentinel;
        first.prev = null;
        first.next = null;
        size -= 1;
        return first.item;
    }

    @Override
    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        Node last = sentinel.prev;
        sentinel.prev = last.prev;
        last.prev.next = sentinel;
        last.prev = null;
        last.next = null;
        size -= 1;
        return last.item;
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        Node p = sentinel.next;
        for (int i = 0; i < index; i += 1) {
            p = p.next;
        }
        return p.item;
    }

    /** Return the element at {@code index} using recursion, or null if out of range. */
    public T getRecursive(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        return getRecursive(sentinel.next, index);
    }

    private T getRecursive(Node p, int index) {
        if (index == 0) {
            return p.item;
        }
        return getRecursive(p.next, index - 1);
    }

    @Override
    public Iterator<T> iterator() {
        return new LinkedListDequeIterator();
    }

    private class LinkedListDequeIterator implements Iterator<T> {
        private Node cur = sentinel.next;

        @Override
        public boolean hasNext() {
            return cur != sentinel;
        }

        @Override
        public T next() {
            T item = cur.item;
            cur = cur.next;
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
        Node p = sentinel.next;
        for (int i = 0; i < size; i += 1) {
            Object a = p.item;
            Object b = other.get(i);
            if (a == null ? b != null : !a.equals(b)) {
                return false;
            }
            p = p.next;
        }
        return true;
    }
}
