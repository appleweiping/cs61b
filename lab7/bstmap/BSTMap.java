package bstmap;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * A map backed by an (unbalanced) binary search tree, keyed by any
 * {@link Comparable} type.
 *
 * @param <K> the comparable key type
 * @param <V> the value type
 * @author appleweiping
 */
public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V> {

    /** A BST node. */
    private class Node {
        private K key;
        private V value;
        private Node left;
        private Node right;
        private int size;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.size = 1;
        }
    }

    private Node root;

    public BSTMap() {
        root = null;
    }

    @Override
    public void clear() {
        root = null;
    }

    @Override
    public boolean containsKey(K key) {
        return get(root, key) != null;
    }

    @Override
    public V get(K key) {
        Node n = get(root, key);
        return n == null ? null : n.value;
    }

    private Node get(Node n, K key) {
        if (n == null) {
            return null;
        }
        int cmp = key.compareTo(n.key);
        if (cmp < 0) {
            return get(n.left, key);
        } else if (cmp > 0) {
            return get(n.right, key);
        }
        return n;
    }

    @Override
    public int size() {
        return size(root);
    }

    private int size(Node n) {
        return n == null ? 0 : n.size;
    }

    @Override
    public void put(K key, V value) {
        root = put(root, key, value);
    }

    private Node put(Node n, K key, V value) {
        if (n == null) {
            return new Node(key, value);
        }
        int cmp = key.compareTo(n.key);
        if (cmp < 0) {
            n.left = put(n.left, key, value);
        } else if (cmp > 0) {
            n.right = put(n.right, key, value);
        } else {
            n.value = value;
        }
        n.size = 1 + size(n.left) + size(n.right);
        return n;
    }

    /** Print the map's key-value pairs in sorted key order, one per line. */
    public void printInOrder() {
        printInOrder(root);
    }

    private void printInOrder(Node n) {
        if (n == null) {
            return;
        }
        printInOrder(n.left);
        System.out.println(n.key + " -> " + n.value);
        printInOrder(n.right);
    }

    @Override
    public Set<K> keySet() {
        Set<K> keys = new TreeSet<>();
        addKeys(root, keys);
        return keys;
    }

    private void addKeys(Node n, Set<K> keys) {
        if (n == null) {
            return;
        }
        addKeys(n.left, keys);
        keys.add(n.key);
        addKeys(n.right, keys);
    }

    @Override
    public V remove(K key) {
        Node target = get(root, key);
        if (target == null) {
            return null;
        }
        V old = target.value;
        root = remove(root, key);
        return old;
    }

    @Override
    public V remove(K key, V value) {
        Node target = get(root, key);
        if (target == null || !valueEquals(target.value, value)) {
            return null;
        }
        V old = target.value;
        root = remove(root, key);
        return old;
    }

    private boolean valueEquals(V a, V b) {
        return a == null ? b == null : a.equals(b);
    }

    /** Hibbard deletion of KEY rooted at N. */
    private Node remove(Node n, K key) {
        if (n == null) {
            return null;
        }
        int cmp = key.compareTo(n.key);
        if (cmp < 0) {
            n.left = remove(n.left, key);
        } else if (cmp > 0) {
            n.right = remove(n.right, key);
        } else {
            if (n.left == null) {
                return n.right;
            }
            if (n.right == null) {
                return n.left;
            }
            // Replace with the successor (min of right subtree).
            Node successor = min(n.right);
            n.key = successor.key;
            n.value = successor.value;
            n.right = removeMin(n.right);
        }
        n.size = 1 + size(n.left) + size(n.right);
        return n;
    }

    private Node min(Node n) {
        while (n.left != null) {
            n = n.left;
        }
        return n;
    }

    private Node removeMin(Node n) {
        if (n.left == null) {
            return n.right;
        }
        n.left = removeMin(n.left);
        n.size = 1 + size(n.left) + size(n.right);
        return n;
    }

    @Override
    public Iterator<K> iterator() {
        return keySet().iterator();
    }
}
