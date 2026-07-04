package gh2;

import deque.ArrayDeque;
import deque.Deque;

/**
 * Models a vibrating guitar string using the Karplus-Strong algorithm over a
 * ring buffer of samples, implemented on top of a {@link Deque}.
 *
 * @author appleweiping
 */
public class GuitarString {
    /** Sampling Rate. */
    private static final int SR = 44100;
    /** Energy decay factor. */
    private static final double DECAY = .996;

    /** Buffer for storing sound data. */
    private Deque<Double> buffer;

    /** Create a guitar string of the given frequency. */
    public GuitarString(double frequency) {
        int capacity = (int) Math.round(SR / frequency);
        buffer = new ArrayDeque<>();
        for (int i = 0; i < capacity; i += 1) {
            buffer.addLast(0.0);
        }
    }

    /** Pluck the guitar string by replacing the buffer with white noise. */
    public void pluck() {
        int capacity = buffer.size();
        for (int i = 0; i < capacity; i += 1) {
            buffer.removeFirst();
            double r = Math.random() - 0.5;
            buffer.addLast(r);
        }
    }

    /**
     * Advance the simulation one time step by performing one iteration of the
     * Karplus-Strong algorithm: dequeue the front sample and enqueue a new
     * sample equal to the decayed average of the first two samples.
     */
    public void tic() {
        double first = buffer.removeFirst();
        double second = buffer.get(0);
        double newSample = (first + second) / 2.0 * DECAY;
        buffer.addLast(newSample);
    }

    /** Return the double at the front of the buffer. */
    public double sample() {
        return buffer.get(0);
    }
}
