package deque;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Tests for {@link ArrayDeque}, including circular-buffer and resize behaviour. */
public class ArrayDequeTest {

    @Test
    public void addIsEmptySizeTest() {
        ArrayDeque<Integer> ad = new ArrayDeque<>();
        assertTrue(ad.isEmpty());
        ad.addFirst(1);
        ad.addLast(2);
        assertEquals(2, ad.size());
        assertFalse(ad.isEmpty());
    }

    @Test
    public void removeEmptyReturnsNull() {
        ArrayDeque<Integer> ad = new ArrayDeque<>();
        assertNull(ad.removeFirst());
        assertNull(ad.removeLast());
    }

    @Test
    public void orderTest() {
        ArrayDeque<Integer> ad = new ArrayDeque<>();
        // Interleave front/back adds to exercise wraparound.
        ad.addLast(0);   // [0]
        ad.addFirst(-1); // [-1, 0]
        ad.addLast(1);   // [-1, 0, 1]
        ad.addFirst(-2); // [-2, -1, 0, 1]
        assertEquals((Integer) (-2), ad.get(0));
        assertEquals((Integer) (-1), ad.get(1));
        assertEquals((Integer) 0, ad.get(2));
        assertEquals((Integer) 1, ad.get(3));
        assertEquals((Integer) (-2), ad.removeFirst());
        assertEquals((Integer) 1, ad.removeLast());
        assertEquals(2, ad.size());
    }

    @Test
    public void growAndShrinkTest() {
        ArrayDeque<Integer> ad = new ArrayDeque<>();
        // Grow well past the initial capacity of 8.
        for (int i = 0; i < 1000; i += 1) {
            ad.addLast(i);
        }
        assertEquals(1000, ad.size());
        for (int i = 0; i < 1000; i += 1) {
            assertEquals((Integer) i, ad.get(i));
        }
        // Remove almost everything; the backing array must shrink (verified
        // indirectly by continued correctness plus no OOM on repeated cycles).
        for (int i = 0; i < 999; i += 1) {
            assertEquals((Integer) i, ad.removeFirst());
        }
        assertEquals(1, ad.size());
        assertEquals((Integer) 999, ad.get(0));

        // Repeated add/remove cycles should not corrupt state or leak.
        for (int cycle = 0; cycle < 5000; cycle += 1) {
            ad.addLast(cycle);
            assertEquals((Integer) 999, ad.removeFirst());
            ad.addFirst(999);
            assertEquals((Integer) cycle, ad.removeLast());
        }
        assertEquals(1, ad.size());
    }

    @Test
    public void getOutOfBoundsTest() {
        ArrayDeque<Integer> ad = new ArrayDeque<>();
        ad.addLast(5);
        assertNull(ad.get(-1));
        assertNull(ad.get(1));
        assertNull(ad.get(100));
    }

    @Test
    public void iteratorTest() {
        ArrayDeque<Integer> ad = new ArrayDeque<>();
        for (int i = 0; i < 20; i += 1) {
            ad.addLast(i);
        }
        int expected = 0;
        for (int x : ad) {
            assertEquals(expected, x);
            expected += 1;
        }
        assertEquals(20, expected);
    }

    @Test
    public void equalsTest() {
        ArrayDeque<Integer> a = new ArrayDeque<>();
        ArrayDeque<Integer> b = new ArrayDeque<>();
        assertTrue(a.equals(b));
        for (int i = 0; i < 10; i += 1) {
            a.addLast(i);
            b.addLast(i);
        }
        assertTrue(a.equals(b));
        b.addLast(99);
        assertFalse(a.equals(b));
        assertFalse(a.equals("not a deque"));
    }
}
