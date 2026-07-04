package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * The BYOW engine. Interprets an input protocol of the form
 * <pre>
 *   N&lt;seed&gt;S            start a new world with the given long seed
 *   w a s d              move the avatar up/left/down/right
 *   :q                   quit and save the world to disk
 *   L                    load the most recently saved world (then continue
 *                        interpreting any remaining input)
 * </pre>
 * The same seed always produces the same world, and
 * {@code interactWithInputString("N..S:q")} followed by
 * {@code interactWithInputString("L..")} reproduces the same state as running
 * the combined input in one shot.
 *
 * @author appleweiping
 */
public class Engine {
    TERenderer ter = new TERenderer();
    /* Feel free to change the width and height. */
    public static final int WIDTH = 80;
    public static final int HEIGHT = 30;

    /** File used to persist the world across quit/load. */
    private static final File SAVE_FILE =
            new File(System.getProperty("user.dir"), "byow_save.ser");

    /**
     * Method used for exploring a fresh world. This method should handle all
     * inputs, including inputs from the main menu.
     */
    public void interactWithKeyboard() {
        ter.initialize(WIDTH, HEIGHT);
        InteractiveDriver driver = new InteractiveDriver(this, ter);
        driver.run();
    }

    /**
     * Method used for autograding and testing. Behaves exactly as if the user
     * typed {@code input} into {@link #interactWithKeyboard()}.
     *
     * @param input the input string to feed to the engine
     * @return the 2D tile representation of the resulting world
     */
    public TETile[][] interactWithInputString(String input) {
        World world = runInput(input);
        return world == null ? null : world.getTiles();
    }

    /**
     * Interpret INPUT and return the resulting {@link World}, or null if the
     * input never established a world.
     */
    World runInput(String input) {
        String s = input.trim();
        int i = 0;
        World world = null;
        int n = s.length();

        while (i < n) {
            char c = Character.toLowerCase(s.charAt(i));
            if (c == 'n') {
                // Parse the seed digits until 's'.
                int j = i + 1;
                StringBuilder digits = new StringBuilder();
                while (j < n && Character.isDigit(s.charAt(j))) {
                    digits.append(s.charAt(j));
                    j += 1;
                }
                long seed = digits.length() == 0 ? 0L
                        : Long.parseLong(digits.toString());
                // Skip the trailing 's' that terminates the seed, if present.
                if (j < n && Character.toLowerCase(s.charAt(j)) == 's') {
                    j += 1;
                }
                world = new World(WIDTH, HEIGHT, seed);
                i = j;
            } else if (c == 'l') {
                world = load();
                i += 1;
            } else if (c == ':') {
                // ":q" quits and saves.
                if (i + 1 < n && Character.toLowerCase(s.charAt(i + 1)) == 'q') {
                    if (world != null) {
                        save(world);
                    }
                    i += 2;
                    break;
                }
                i += 1;
            } else if (c == 'w' || c == 'a' || c == 's' || c == 'd') {
                if (world != null) {
                    world.moveAvatar(c);
                }
                i += 1;
            } else {
                // Ignore any unrecognised character.
                i += 1;
            }
        }
        return world;
    }

    /** Serialize WORLD to the save file. */
    void save(World world) {
        try (ObjectOutputStream out =
                     new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            out.writeObject(world);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save world", e);
        }
    }

    /** Load the most recently saved world, or null if none exists. */
    World load() {
        if (!SAVE_FILE.exists()) {
            return null;
        }
        try (ObjectInputStream in =
                     new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
            return (World) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to load world", e);
        }
    }
}
