package deque;

/**
 * A double-ended queue (deque): a sequence supporting constant-time insertion
 * and removal at both ends and indexed access.
 *
 * @param <T> the type of element stored
 * @author appleweiping
 */
public interface Deque<T> {

    /** Add {@code item} to the front of the deque. */
    void addFirst(T item);

    /** Add {@code item} to the back of the deque. */
    void addLast(T item);

    /** Return {@code true} iff the deque contains no elements. */
    default boolean isEmpty() {
        return size() == 0;
    }

    /** Return the number of elements in the deque. */
    int size();

    /** Print the deque from first to last, separated by spaces, then a newline. */
    void printDeque();

    /** Remove and return the front element, or {@code null} if empty. */
    T removeFirst();

    /** Remove and return the back element, or {@code null} if empty. */
    T removeLast();

    /**
     * Return the element at {@code index} (0 is the front), or {@code null}
     * if no such element exists. Must run in constant time for arrays and
     * be implemented iteratively for linked lists.
     */
    T get(int index);
}
