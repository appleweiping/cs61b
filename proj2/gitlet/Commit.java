package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a gitlet commit object: an immutable snapshot of tracked files
 * plus metadata (message, timestamp, and up to two parents). A commit is
 * identified by the SHA-1 of its serialized form.
 *
 * @author appleweiping
 */
public class Commit implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The log message of this commit. */
    private final String message;

    /** The commit time, formatted like Unix `date`, e.g.
     *  "Thu Jan 1 00:00:00 1970 +0000". */
    private final String timestamp;

    /** SHA-1 of the first parent, or null for the initial commit. */
    private final String parent;

    /** SHA-1 of the second parent for merge commits, or null otherwise. */
    private final String secondParent;

    /** Map from tracked file name to the SHA-1 of its blob. */
    private final TreeMap<String, String> blobs;

    /** Create the initial (root) commit with a fixed epoch timestamp. */
    public Commit() {
        this.message = "initial commit";
        this.timestamp = formatDate(new Date(0));
        this.parent = null;
        this.secondParent = null;
        this.blobs = new TreeMap<>();
    }

    /** Create a commit with the given metadata and tracked-file map. */
    public Commit(String message, String parent, String secondParent,
                  Map<String, String> blobs) {
        this.message = message;
        this.timestamp = formatDate(new Date());
        this.parent = parent;
        this.secondParent = secondParent;
        this.blobs = new TreeMap<>(blobs);
    }

    /** Format DATE as required by gitlet's log, e.g.
     *  "Thu Jan 1 00:00:00 1970 +0000". */
    private static String formatDate(Date date) {
        SimpleDateFormat fmt =
                new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return fmt.format(date);
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getParent() {
        return parent;
    }

    public String getSecondParent() {
        return secondParent;
    }

    /** Return the tracked file -> blob map. */
    public Map<String, String> getBlobs() {
        return blobs;
    }

    /** Return true iff FILENAME is tracked by this commit. */
    public boolean tracks(String fileName) {
        return blobs.containsKey(fileName);
    }

    /** Return the blob SHA-1 for FILENAME, or null if untracked. */
    public String getBlob(String fileName) {
        return blobs.get(fileName);
    }

    /** Return the list of parent SHA-1s (0, 1, or 2 entries). */
    public List<String> getParents() {
        List<String> parents = new ArrayList<>();
        if (parent != null) {
            parents.add(parent);
        }
        if (secondParent != null) {
            parents.add(secondParent);
        }
        return parents;
    }

    /** Compute this commit's SHA-1 id from its serialized bytes. */
    public String id() {
        return Utils.sha1((Object) Utils.serialize(this));
    }
}
