package deque;

import java.util.Comparator;

/**
 * An {@link ArrayDeque} that can report its maximum element, either by a
 * default comparator supplied at construction or by an ad-hoc comparator.
 *
 * @param <T> the element type
 * @author appleweiping
 */
public class MaxArrayDeque<T> extends ArrayDeque<T> {

    private final Comparator<T> defaultComparator;

    /** Create a MaxArrayDeque using {@code c} as its default ordering. */
    public MaxArrayDeque(Comparator<T> c) {
        super();
        this.defaultComparator = c;
    }

    /** Return the maximum element by the default comparator, or null if empty. */
    public T max() {
        return max(defaultComparator);
    }

    /** Return the maximum element by comparator {@code c}, or null if empty. */
    public T max(Comparator<T> c) {
        if (isEmpty()) {
            return null;
        }
        T best = get(0);
        for (int i = 1; i < size(); i += 1) {
            T candidate = get(i);
            if (c.compare(candidate, best) > 0) {
                best = candidate;
            }
        }
        return best;
    }
}
