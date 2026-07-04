package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static gitlet.Utils.join;
import static gitlet.Utils.message;
import static gitlet.Utils.plainFilenamesIn;
import static gitlet.Utils.readContents;
import static gitlet.Utils.readContentsAsString;
import static gitlet.Utils.readObject;
import static gitlet.Utils.restrictedDelete;
import static gitlet.Utils.sha1;
import static gitlet.Utils.writeContents;
import static gitlet.Utils.writeObject;

/**
 * The persistence layer and command implementations for gitlet.
 *
 * On-disk layout under CWD:
 * <pre>
 *   .gitlet/
 *     objects/          blob and commit objects, keyed by SHA-1
 *     refs/heads/       one file per branch, holding its head commit's SHA-1
 *     HEAD              name of the current branch (e.g. "master")
 *     index             serialized {@link Stage} (the staging area)
 * </pre>
 *
 * @author appleweiping
 */
public class Repository {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** Directory holding all blob and commit objects. */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    /** Directory holding branch reference pointers. */
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    /** Directory holding branch heads (refs/heads). */
    public static final File HEADS_DIR = join(REFS_DIR, "heads");
    /** File naming the current branch. */
    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");
    /** Serialized staging area. */
    public static final File INDEX_FILE = join(GITLET_DIR, "index");

    /** The staging area: files staged for addition (name -> blob SHA-1) and
     *  files staged for removal. */
    static class Stage implements Serializable {
        private static final long serialVersionUID = 1L;
        private final TreeMap<String, String> additions = new TreeMap<>();
        private final TreeSet<String> removals = new TreeSet<>();

        Map<String, String> additions() {
            return additions;
        }

        Set<String> removals() {
            return removals;
        }

        boolean isEmpty() {
            return additions.isEmpty() && removals.isEmpty();
        }

        void clear() {
            additions.clear();
            removals.clear();
        }
    }

    /* ------------------------------------------------------------------ */
    /* init                                                               */
    /* ------------------------------------------------------------------ */

    /** Create a new gitlet repository with a single initial commit on the
     *  branch "master". */
    public static void init() {
        if (GITLET_DIR.exists()) {
            message("A Gitlet version-control system "
                    + "already exists in the current directory.");
            return;
        }
        GITLET_DIR.mkdirs();
        OBJECTS_DIR.mkdirs();
        HEADS_DIR.mkdirs();

        Commit initial = new Commit();
        String id = writeCommit(initial);
        writeContents(join(HEADS_DIR, "master"), id);
        writeContents(HEAD_FILE, "master");
        writeStage(new Stage());
    }

    /* ------------------------------------------------------------------ */
    /* add                                                                */
    /* ------------------------------------------------------------------ */

    /** Stage FILENAME for addition. */
    public static void add(String fileName) {
        File file = join(CWD, fileName);
        if (!file.exists()) {
            message("File does not exist.");
            return;
        }
        byte[] contents = readContents(file);
        String blobId = sha1((Object) contents);
        Commit head = headCommit();
        Stage stage = readStage();

        if (blobId.equals(head.getBlob(fileName))) {
            // Identical to the committed version: nothing to stage, and undo
            // any prior staging (addition or removal) of this file.
            stage.additions().remove(fileName);
            stage.removals().remove(fileName);
        } else {
            writeBlob(blobId, contents);
            stage.additions().put(fileName, blobId);
            stage.removals().remove(fileName);
        }
        writeStage(stage);
    }

    /* ------------------------------------------------------------------ */
    /* commit                                                             */
    /* ------------------------------------------------------------------ */

    /** Commit the staged changes with the given MESSAGE. */
    public static void commit(String msg) {
        commit(msg, null);
    }

    /** Commit with an optional MERGEPARENT (second parent SHA-1). */
    private static void commit(String msg, String mergeParent) {
        if (msg == null || msg.isEmpty()) {
            message("Please enter a commit message.");
            return;
        }
        Stage stage = readStage();
        if (stage.isEmpty()) {
            message("No changes added to the commit.");
            return;
        }
        Commit parent = headCommit();
        String parentId = headCommitId();

        Map<String, String> tracked = new TreeMap<>(parent.getBlobs());
        for (Map.Entry<String, String> e : stage.additions().entrySet()) {
            tracked.put(e.getKey(), e.getValue());
        }
        for (String removed : stage.removals()) {
            tracked.remove(removed);
        }

        Commit newCommit = new Commit(msg, parentId, mergeParent, tracked);
        String id = writeCommit(newCommit);
        writeContents(join(HEADS_DIR, currentBranch()), id);
        stage.clear();
        writeStage(stage);
    }

    /* ------------------------------------------------------------------ */
    /* rm                                                                 */
    /* ------------------------------------------------------------------ */

    /** Unstage FILENAME if staged; if tracked by the head commit, stage it
     *  for removal and delete it from the working directory. */
    public static void rm(String fileName) {
        Stage stage = readStage();
        Commit head = headCommit();
        boolean staged = stage.additions().containsKey(fileName);
        boolean tracked = head.tracks(fileName);

        if (!staged && !tracked) {
            message("No reason to remove the file.");
            return;
        }
        if (staged) {
            stage.additions().remove(fileName);
        }
        if (tracked) {
            stage.removals().add(fileName);
            restrictedDelete(join(CWD, fileName));
        }
        writeStage(stage);
    }

    /* ------------------------------------------------------------------ */
    /* log / global-log / find                                            */
    /* ------------------------------------------------------------------ */

    /** Print the history of the current branch back to the initial commit. */
    public static void log() {
        String id = headCommitId();
        while (id != null) {
            Commit c = readCommit(id);
            System.out.print(logEntry(id, c));
            id = c.getParent();
        }
    }

    /** Print every commit ever made, in no particular order. */
    public static void globalLog() {
        for (String id : plainFilenamesIn(OBJECTS_DIR)) {
            Commit c = tryReadCommit(id);
            if (c != null) {
                System.out.print(logEntry(id, c));
            }
        }
    }

    /** Print the ids of all commits whose message equals MSG. */
    public static void find(String msg) {
        boolean found = false;
        for (String id : plainFilenamesIn(OBJECTS_DIR)) {
            Commit c = tryReadCommit(id);
            if (c != null && c.getMessage().equals(msg)) {
                System.out.println(id);
                found = true;
            }
        }
        if (!found) {
            message("Found no commit with that message.");
        }
    }

    /** Render one log entry (with trailing blank line) for commit ID/C. */
    private static String logEntry(String id, Commit c) {
        StringBuilder sb = new StringBuilder();
        sb.append("===\n");
        sb.append("commit ").append(id).append('\n');
        if (c.getSecondParent() != null) {
            sb.append("Merge: ")
                    .append(c.getParent(), 0, 7).append(' ')
                    .append(c.getSecondParent(), 0, 7).append('\n');
        }
        sb.append("Date: ").append(c.getTimestamp()).append('\n');
        sb.append(c.getMessage()).append('\n');
        sb.append('\n');
        return sb.toString();
    }

    /* ------------------------------------------------------------------ */
    /* status                                                             */
    /* ------------------------------------------------------------------ */

    /** Print the working-tree status. */
    public static void status() {
        StringBuilder sb = new StringBuilder();
        Stage stage = readStage();
        Commit head = headCommit();

        sb.append("=== Branches ===\n");
        String current = currentBranch();
        List<String> branches = plainFilenamesIn(HEADS_DIR);
        Collections.sort(branches);
        for (String b : branches) {
            sb.append(b.equals(current) ? "*" : "").append(b).append('\n');
        }
        sb.append('\n');

        sb.append("=== Staged Files ===\n");
        for (String f : stage.additions().keySet()) {
            sb.append(f).append('\n');
        }
        sb.append('\n');

        sb.append("=== Removed Files ===\n");
        for (String f : stage.removals()) {
            sb.append(f).append('\n');
        }
        sb.append('\n');

        sb.append("=== Modifications Not Staged For Commit ===\n");
        for (String line : modificationsNotStaged(head, stage)) {
            sb.append(line).append('\n');
        }
        sb.append('\n');

        sb.append("=== Untracked Files ===\n");
        for (String f : untrackedFiles(head, stage)) {
            sb.append(f).append('\n');
        }
        sb.append('\n');

        System.out.print(sb);
    }

    /** Compute the "Modifications Not Staged For Commit" section entries. */
    private static List<String> modificationsNotStaged(Commit head, Stage stage) {
        TreeSet<String> result = new TreeSet<>();
        Set<String> working = new HashSet<>(plainFilenamesIn(CWD));

        Set<String> candidates = new TreeSet<>();
        candidates.addAll(head.getBlobs().keySet());
        candidates.addAll(stage.additions().keySet());
        for (String f : candidates) {
            String stagedBlob = stage.additions().get(f);
            String committedBlob = head.getBlob(f);
            boolean inWorking = working.contains(f);
            if (inWorking) {
                String wBlob = sha1((Object) readContents(join(CWD, f)));
                if (stagedBlob != null) {
                    if (!wBlob.equals(stagedBlob)) {
                        result.add(f + " (modified)");
                    }
                } else if (committedBlob != null
                        && !wBlob.equals(committedBlob)) {
                    result.add(f + " (modified)");
                }
            } else {
                if (stagedBlob != null) {
                    result.add(f + " (deleted)");
                } else if (committedBlob != null
                        && !stage.removals().contains(f)) {
                    result.add(f + " (deleted)");
                }
            }
        }
        return new ArrayList<>(result);
    }

    /** Compute the "Untracked Files" section entries. */
    private static List<String> untrackedFiles(Commit head, Stage stage) {
        TreeSet<String> result = new TreeSet<>();
        for (String f : plainFilenamesIn(CWD)) {
            boolean tracked = head.tracks(f);
            boolean staged = stage.additions().containsKey(f);
            boolean removed = stage.removals().contains(f);
            if ((!tracked && !staged) || removed) {
                result.add(f);
            }
        }
        return new ArrayList<>(result);
    }

    /* ------------------------------------------------------------------ */
    /* checkout                                                           */
    /* ------------------------------------------------------------------ */

    /** checkout -- [file name] : restore file from the head commit. */
    public static void checkoutFile(String fileName) {
        checkoutFileFromCommit(headCommitId(), fileName);
    }

    /** checkout [commit id] -- [file name] : restore file from a commit. */
    public static void checkoutFileFromCommit(String commitId, String fileName) {
        String fullId = resolveCommitId(commitId);
        if (fullId == null) {
            message("No commit with that id exists.");
            return;
        }
        Commit c = readCommit(fullId);
        if (!c.tracks(fileName)) {
            message("File does not exist in that commit.");
            return;
        }
        writeWorkingFile(fileName, c.getBlob(fileName));
    }

    /** checkout [branch name] : switch to a branch. */
    public static void checkoutBranch(String branchName) {
        File branchFile = join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            message("No such branch exists.");
            return;
        }
        if (branchName.equals(currentBranch())) {
            message("No need to checkout the current branch.");
            return;
        }
        String targetId = readContentsAsString(branchFile);
        Commit target = readCommit(targetId);
        if (hasUntrackedConflict(target)) {
            message("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return;
        }
        checkoutCommitState(target);
        writeContents(HEAD_FILE, branchName);
        Stage stage = readStage();
        stage.clear();
        writeStage(stage);
    }

    /* ------------------------------------------------------------------ */
    /* branch / rm-branch                                                 */
    /* ------------------------------------------------------------------ */

    /** Create a new branch named BRANCHNAME at the current head. */
    public static void branch(String branchName) {
        File branchFile = join(HEADS_DIR, branchName);
        if (branchFile.exists()) {
            message("A branch with that name already exists.");
            return;
        }
        writeContents(branchFile, headCommitId());
    }

    /** Delete the branch named BRANCHNAME. */
    public static void rmBranch(String branchName) {
        File branchFile = join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            message("A branch with that name does not exist.");
            return;
        }
        if (branchName.equals(currentBranch())) {
            message("Cannot remove the current branch.");
            return;
        }
        branchFile.delete();
    }

    /* ------------------------------------------------------------------ */
    /* reset                                                              */
    /* ------------------------------------------------------------------ */

    /** Reset the current branch to COMMITID, updating the working directory. */
    public static void reset(String commitId) {
        String fullId = resolveCommitId(commitId);
        if (fullId == null) {
            message("No commit with that id exists.");
            return;
        }
        Commit target = readCommit(fullId);
        if (hasUntrackedConflict(target)) {
            message("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return;
        }
        checkoutCommitState(target);
        writeContents(join(HEADS_DIR, currentBranch()), fullId);
        Stage stage = readStage();
        stage.clear();
        writeStage(stage);
    }

    /* ------------------------------------------------------------------ */
    /* merge                                                              */
    /* ------------------------------------------------------------------ */

    /** Merge the branch BRANCHNAME into the current branch. */
    public static void merge(String branchName) {
        Stage stage = readStage();
        if (!stage.isEmpty()) {
            message("You have uncommitted changes.");
            return;
        }
        File branchFile = join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            message("A branch with that name does not exist.");
            return;
        }
        if (branchName.equals(currentBranch())) {
            message("Cannot merge a branch with itself.");
            return;
        }

        String headId = headCommitId();
        String otherId = readContentsAsString(branchFile);
        Commit otherCommit = readCommit(otherId);
        if (hasUntrackedConflict(otherCommit)) {
            message("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return;
        }

        String splitId = latestCommonAncestor(headId, otherId);
        if (splitId.equals(otherId)) {
            message("Given branch is an ancestor of the current branch.");
            return;
        }
        if (splitId.equals(headId)) {
            // Fast-forward: current branch simply moves to the other head.
            checkoutCommitState(otherCommit);
            writeContents(join(HEADS_DIR, currentBranch()), otherId);
            message("Current branch fast-forwarded.");
            return;
        }

        boolean conflict = doMerge(readCommit(splitId), headCommit(),
                otherCommit);
        String msg = "Merged " + branchName + " into " + currentBranch() + ".";
        commit(msg, otherId);
        if (conflict) {
            message("Encountered a merge conflict.");
        }
    }

    /**
     * Apply the three-way merge of SPLIT (ancestor), HEAD (current), and OTHER
     * (given), staging additions/removals. Returns true iff a conflict arose.
     */
    private static boolean doMerge(Commit split, Commit head, Commit other) {
        boolean conflict = false;
        Set<String> files = new TreeSet<>();
        files.addAll(split.getBlobs().keySet());
        files.addAll(head.getBlobs().keySet());
        files.addAll(other.getBlobs().keySet());

        for (String f : files) {
            String sBlob = split.getBlob(f);
            String hBlob = head.getBlob(f);
            String oBlob = other.getBlob(f);

            boolean headChanged = !eq(sBlob, hBlob);
            boolean otherChanged = !eq(sBlob, oBlob);

            if (!otherChanged) {
                // Other side unchanged since split: keep head's version.
                continue;
            }
            if (!headChanged) {
                // Only other changed: adopt other's version (or delete).
                if (oBlob == null) {
                    rm(f);
                } else {
                    writeWorkingFile(f, oBlob);
                    stageForMerge(f, oBlob);
                }
                continue;
            }
            // Both sides changed relative to split.
            if (eq(hBlob, oBlob)) {
                // Same change on both sides: nothing to do.
                continue;
            }
            // Conflict.
            conflict = true;
            String merged = conflictContents(hBlob, oBlob);
            byte[] bytes =
                    merged.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            String blobId = sha1((Object) bytes);
            writeBlob(blobId, bytes);
            writeContents(join(CWD, f), (Object) bytes);
            stageForMerge(f, blobId);
        }
        return conflict;
    }

    /** Build the conflict-marker file contents from the two blob ids
     *  (either may be null, meaning "absent"). */
    private static String conflictContents(String hBlob, String oBlob) {
        String headText = hBlob == null ? "" : blobString(hBlob);
        String otherText = oBlob == null ? "" : blobString(oBlob);
        return "<<<<<<< HEAD\n" + headText
                + "=======\n" + otherText
                + ">>>>>>>\n";
    }

    /** Stage FILENAME->BLOBID directly (used by merge, bypassing add()). */
    private static void stageForMerge(String fileName, String blobId) {
        Stage stage = readStage();
        stage.additions().put(fileName, blobId);
        stage.removals().remove(fileName);
        writeStage(stage);
    }

    /** Return the latest common ancestor of commits A and B. */
    private static String latestCommonAncestor(String aId, String bId) {
        Set<String> ancestorsOfA = ancestors(aId);
        ArrayDeque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(bId);
        visited.add(bId);
        while (!queue.isEmpty()) {
            String id = queue.poll();
            if (ancestorsOfA.contains(id)) {
                return id;
            }
            for (String p : readCommit(id).getParents()) {
                if (!visited.contains(p)) {
                    visited.add(p);
                    queue.add(p);
                }
            }
        }
        return null;
    }

    /** Return the set containing ID and all of its ancestors. */
    private static Set<String> ancestors(String id) {
        Set<String> seen = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(id);
        seen.add(id);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            for (String p : readCommit(cur).getParents()) {
                if (!seen.contains(p)) {
                    seen.add(p);
                    queue.add(p);
                }
            }
        }
        return seen;
    }

    /* ------------------------------------------------------------------ */
    /* Shared helpers                                                     */
    /* ------------------------------------------------------------------ */

    /** Replace the working directory with the state of commit TARGET:
     *  write its files, delete tracked files it does not contain. */
    private static void checkoutCommitState(Commit target) {
        Commit head = headCommit();
        for (String f : head.getBlobs().keySet()) {
            if (!target.tracks(f)) {
                restrictedDelete(join(CWD, f));
            }
        }
        for (Map.Entry<String, String> e : target.getBlobs().entrySet()) {
            writeWorkingFile(e.getKey(), e.getValue());
        }
    }

    /** Return true iff switching to TARGET would overwrite/delete a working
     *  file that is untracked by the current head. */
    private static boolean hasUntrackedConflict(Commit target) {
        Commit head = headCommit();
        Stage stage = readStage();
        for (String f : plainFilenamesIn(CWD)) {
            boolean trackedByHead = head.tracks(f);
            boolean staged = stage.additions().containsKey(f);
            if (trackedByHead || staged) {
                continue;
            }
            if (target.tracks(f)) {
                String wBlob = sha1((Object) readContents(join(CWD, f)));
                if (!wBlob.equals(target.getBlob(f))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Write blob BLOBID's contents into working file FILENAME. */
    private static void writeWorkingFile(String fileName, String blobId) {
        writeContents(join(CWD, fileName), (Object) readBlob(blobId));
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /* ---- object read/write ---- */

    private static String writeCommit(Commit c) {
        String id = c.id();
        writeObject(join(OBJECTS_DIR, id), c);
        return id;
    }

    private static Commit readCommit(String id) {
        return readObject(join(OBJECTS_DIR, id), Commit.class);
    }

    /** Read a commit, returning null if the object is not a Commit. */
    private static Commit tryReadCommit(String id) {
        try {
            return readObject(join(OBJECTS_DIR, id), Commit.class);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void writeBlob(String id, byte[] contents) {
        File f = join(OBJECTS_DIR, id);
        if (!f.exists()) {
            writeContents(f, (Object) contents);
        }
    }

    private static byte[] readBlob(String id) {
        return readContents(join(OBJECTS_DIR, id));
    }

    private static String blobString(String id) {
        return readContentsAsString(join(OBJECTS_DIR, id));
    }

    /* ---- refs / head ---- */

    private static String currentBranch() {
        return readContentsAsString(HEAD_FILE);
    }

    private static String headCommitId() {
        return readContentsAsString(join(HEADS_DIR, currentBranch()));
    }

    private static Commit headCommit() {
        return readCommit(headCommitId());
    }

    /** Resolve a possibly-abbreviated COMMITID to a full id, or null. */
    private static String resolveCommitId(String commitId) {
        if (commitId == null) {
            return null;
        }
        if (commitId.length() == Utils.UID_LENGTH) {
            return join(OBJECTS_DIR, commitId).exists() ? commitId : null;
        }
        for (String id : plainFilenamesIn(OBJECTS_DIR)) {
            if (id.startsWith(commitId) && tryReadCommit(id) != null) {
                return id;
            }
        }
        return null;
    }

    /* ---- staging area ---- */

    private static Stage readStage() {
        if (!INDEX_FILE.exists()) {
            return new Stage();
        }
        return readObject(INDEX_FILE, Stage.class);
    }

    private static void writeStage(Stage stage) {
        writeObject(INDEX_FILE, stage);
    }

    /** Return true iff a gitlet repository exists in the CWD. */
    public static boolean initialized() {
        return GITLET_DIR.exists();
    }
}
