package deque;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Tests for {@link LinkedListDeque}. */
public class LinkedListDequeTest {

    @Test
    public void addIsEmptySizeTest() {
        LinkedListDeque<String> lld = new LinkedListDeque<>();
        assertTrue(lld.isEmpty());
        lld.addFirst("front");
        assertEquals(1, lld.size());
        assertFalse(lld.isEmpty());
        lld.addLast("middle");
        assertEquals(2, lld.size());
        lld.addLast("back");
        assertEquals(3, lld.size());
    }

    @Test
    public void addRemoveTest() {
        LinkedListDeque<Integer> lld = new LinkedListDeque<>();
        assertTrue(lld.isEmpty());
        lld.addFirst(10);
        assertFalse(lld.isEmpty());
        lld.removeFirst();
        assertTrue(lld.isEmpty());
    }

    @Test
    public void removeEmptyReturnsNull() {
        LinkedListDeque<Integer> lld = new LinkedListDeque<>();
        assertNull(lld.removeFirst());
        assertNull(lld.removeLast());
        lld.addFirst(3);
        lld.removeLast();
        assertNull(lld.removeFirst());
    }

    @Test
    public void orderFirstLastTest() {
        LinkedListDeque<Integer> lld = new LinkedListDeque<>();
        for (int i = 0; i < 5; i += 1) {
            lld.addLast(i);      // 0 1 2 3 4
        }
        for (int i = 0; i < 5; i += 1) {
            assertEquals((Integer) i, lld.removeFirst());
        }
        for (int i = 0; i < 5; i += 1) {
            lld.addFirst(i);     // 4 3 2 1 0
        }
        for (int i = 0; i < 5; i += 1) {
            assertEquals((Integer) i, lld.removeLast());
        }
    }

    @Test
    public void getAndGetRecursiveTest() {
        LinkedListDeque<Integer> lld = new LinkedListDeque<>();
        for (int i = 0; i < 10; i += 1) {
            lld.addLast(i * i);
        }
        for (int i = 0; i < 10; i += 1) {
            assertEquals((Integer) (i * i), lld.get(i));
            assertEquals((Integer) (i * i), lld.getRecursive(i));
        }
        assertNull(lld.get(-1));
        assertNull(lld.get(10));
        assertNull(lld.getRecursive(100));
    }

    @Test
    public void iteratorTest() {
        LinkedListDeque<Integer> lld = new LinkedListDeque<>();
        for (int i = 0; i < 6; i += 1) {
            lld.addLast(i);
        }
        int expected = 0;
        for (int x : lld) {
            assertEquals(expected, x);
            expected += 1;
        }
        assertEquals(6, expected);
        Iterator<Integer> it = lld.iterator();
        assertTrue(it.hasNext());
    }

    @Test
    public void equalsTest() {
        LinkedListDeque<String> a = new LinkedListDeque<>();
        LinkedListDeque<String> b = new LinkedListDeque<>();
        assertEquals(a, b);
        a.addLast("x");
        a.addLast("y");
        b.addLast("x");
        b.addLast("y");
        assertEquals(a, b);
        b.addLast("z");
        assertNotEquals(a, b);

        // Cross-implementation equality with an ArrayDeque of same contents.
        ArrayDeque<String> ad = new ArrayDeque<>();
        ad.addLast("x");
        ad.addLast("y");
        assertEquals(a, ad);
    }

    @Test
    public void bigDequeTest() {
        LinkedListDeque<Integer> lld = new LinkedListDeque<>();
        for (int i = 0; i < 100000; i += 1) {
            lld.addLast(i);
        }
        for (int i = 0; i < 50000; i += 1) {
            assertEquals((Integer) i, lld.removeFirst());
        }
        for (int i = 99999; i >= 50000; i -= 1) {
            assertEquals((Integer) i, lld.removeLast());
        }
        assertTrue(lld.isEmpty());
    }
}
