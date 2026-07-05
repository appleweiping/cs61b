package hashmap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Collisions are resolved by separate chaining: each slot of the backing
 *  array holds a bucket (a {@link Collection} of nodes). The concrete bucket
 *  type is chosen by subclasses through {@link #createBucket()}. When the load
 *  factor (# items / # buckets) exceeds {@code maxLoad}, the table doubles and
 *  every entry is rehashed.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author appleweiping
 */
public class MyHashMap<K, V> implements Map61B<K, V> {

    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }

    /* Default number of buckets when none is specified. */
    private static final int DEFAULT_INITIAL_SIZE = 16;
    /* Default maximum load factor when none is specified. */
    private static final double DEFAULT_MAX_LOAD = 0.75;
    /* Factor by which the table grows when the load factor is exceeded. */
    private static final int RESIZE_FACTOR = 2;

    /* Instance Variables */
    private Collection<Node>[] buckets;
    private int size;
    private final double maxLoad;

    /** Constructors */
    public MyHashMap() {
        this(DEFAULT_INITIAL_SIZE, DEFAULT_MAX_LOAD);
    }

    public MyHashMap(int initialSize) {
        this(initialSize, DEFAULT_MAX_LOAD);
    }

    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    public MyHashMap(int initialSize, double maxLoad) {
        this.maxLoad = maxLoad;
        this.buckets = createTable(initialSize);
        this.size = 0;
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key, value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     */
    protected Collection<Node> createBucket() {
        return new LinkedList<>();
    }

    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    @SuppressWarnings("unchecked")
    private Collection<Node>[] createTable(int tableSize) {
        Collection<Node>[] table = new Collection[tableSize];
        for (int i = 0; i < tableSize; i++) {
            table[i] = createBucket();
        }
        return table;
    }

    /** Maps a key to a bucket index in [0, buckets.length). */
    private int bucketIndexOf(K key, int numBuckets) {
        return Math.floorMod(key.hashCode(), numBuckets);
    }

    /** Returns the node holding KEY in its bucket, or null if absent. */
    private Node nodeFor(K key) {
        Collection<Node> bucket = buckets[bucketIndexOf(key, buckets.length)];
        for (Node node : bucket) {
            if (node.key.equals(key)) {
                return node;
            }
        }
        return null;
    }

    @Override
    public void clear() {
        this.buckets = createTable(DEFAULT_INITIAL_SIZE);
        this.size = 0;
    }

    @Override
    public boolean containsKey(K key) {
        return nodeFor(key) != null;
    }

    @Override
    public V get(K key) {
        Node node = nodeFor(key);
        return node == null ? null : node.value;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void put(K key, V value) {
        Node existing = nodeFor(key);
        if (existing != null) {
            // Update in place. Bucket ordering keyed on node.key is unaffected.
            existing.value = value;
            return;
        }
        buckets[bucketIndexOf(key, buckets.length)].add(createNode(key, value));
        size += 1;
        if ((double) size / buckets.length > maxLoad) {
            resize(buckets.length * RESIZE_FACTOR);
        }
    }

    /** Grows the backing array to NEWSIZE buckets and rehashes every entry. */
    private void resize(int newSize) {
        Collection<Node>[] newBuckets = createTable(newSize);
        for (Collection<Node> bucket : buckets) {
            for (Node node : bucket) {
                newBuckets[bucketIndexOf(node.key, newSize)].add(node);
            }
        }
        buckets = newBuckets;
    }

    @Override
    public Set<K> keySet() {
        Set<K> keys = new HashSet<>();
        for (Collection<Node> bucket : buckets) {
            for (Node node : bucket) {
                keys.add(node.key);
            }
        }
        return keys;
    }

    @Override
    public V remove(K key) {
        Collection<Node> bucket = buckets[bucketIndexOf(key, buckets.length)];
        Iterator<Node> it = bucket.iterator();
        while (it.hasNext()) {
            Node node = it.next();
            if (node.key.equals(key)) {
                V old = node.value;
                it.remove();
                size -= 1;
                return old;
            }
        }
        return null;
    }

    @Override
    public V remove(K key, V value) {
        Node node = nodeFor(key);
        if (node == null || !valueEquals(node.value, value)) {
            return null;
        }
        return remove(key);
    }

    private boolean valueEquals(V a, V b) {
        return a == null ? b == null : a.equals(b);
    }

    @Override
    public Iterator<K> iterator() {
        return keySet().iterator();
    }
}
