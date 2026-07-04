package deque;

import org.junit.Test;

import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** Tests for {@link MaxArrayDeque}. */
public class MaxArrayDequeTest {

    private static class IntComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer a, Integer b) {
            return a - b;
        }
    }

    private static class StringLengthComparator implements Comparator<String> {
        @Override
        public int compare(String a, String b) {
            return a.length() - b.length();
        }
    }

    @Test
    public void maxWithDefaultComparator() {
        MaxArrayDeque<Integer> mad = new MaxArrayDeque<>(new IntComparator());
        assertNull(mad.max());
        mad.addLast(3);
        mad.addLast(9);
        mad.addLast(-4);
        mad.addFirst(7);
        assertEquals((Integer) 9, mad.max());
    }

    @Test
    public void maxWithAdHocComparator() {
        MaxArrayDeque<String> mad =
                new MaxArrayDeque<String>(Comparator.<String>naturalOrder());
        mad.addLast("apple");
        mad.addLast("kiwi");
        mad.addLast("watermelon");
        mad.addLast("fig");
        // Natural (lexicographic) order: "watermelon" is the largest string.
        assertEquals("watermelon", mad.max());
        // By length, "watermelon" (10) is longest.
        assertEquals("watermelon", mad.max(new StringLengthComparator()));
        // By reverse length, the shortest ("fig", 3) wins.
        assertEquals("fig",
                mad.max(new StringLengthComparator().reversed()));
    }

    @Test
    public void inheritsDequeBehaviour() {
        MaxArrayDeque<Integer> mad = new MaxArrayDeque<>(new IntComparator());
        for (int i = 0; i < 100; i += 1) {
            mad.addLast(i);
        }
        assertEquals(100, mad.size());
        assertEquals((Integer) 99, mad.max());
        assertEquals((Integer) 0, mad.removeFirst());
    }
}
