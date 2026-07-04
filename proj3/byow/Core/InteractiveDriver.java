package byow.Core;

import byow.TileEngine.TERenderer;
import edu.princeton.cs.introcs.StdDraw;

import java.awt.Color;
import java.awt.Font;

/**
 * Drives the interactive (keyboard) mode of BYOW: a simple main menu
 * (New / Load / Quit), then live avatar movement with WASD until the player
 * quits with ":Q". Delegates all world logic to {@link Engine} and
 * {@link World}.
 *
 * @author appleweiping
 */
class InteractiveDriver {

    private final Engine engine;
    private final TERenderer ter;

    InteractiveDriver(Engine engine, TERenderer ter) {
        this.engine = engine;
        this.ter = ter;
    }

    void run() {
        World world = mainMenu();
        if (world == null) {
            return;
        }
        play(world);
    }

    /** Show the main menu and return a world (new or loaded), or null on quit. */
    private World mainMenu() {
        while (true) {
            drawMenu();
            if (!StdDraw.hasNextKeyTyped()) {
                StdDraw.pause(20);
                continue;
            }
            char key = Character.toLowerCase(StdDraw.nextKeyTyped());
            if (key == 'n') {
                long seed = readSeed();
                return new World(Engine.WIDTH, Engine.HEIGHT, seed);
            } else if (key == 'l') {
                World loaded = engine.load();
                if (loaded != null) {
                    return loaded;
                }
            } else if (key == 'q') {
                return null;
            }
        }
    }

    /** Prompt for and read the seed digits, terminated by 's'/'S'. */
    private long readSeed() {
        StringBuilder sb = new StringBuilder();
        while (true) {
            drawSeedPrompt(sb.toString());
            if (!StdDraw.hasNextKeyTyped()) {
                StdDraw.pause(20);
                continue;
            }
            char key = StdDraw.nextKeyTyped();
            if (Character.toLowerCase(key) == 's') {
                break;
            }
            if (Character.isDigit(key)) {
                sb.append(key);
            }
        }
        return sb.length() == 0 ? 0L : Long.parseLong(sb.toString());
    }

    /** Main play loop: render the world and process WASD / :Q. */
    private void play(World world) {
        boolean colonPending = false;
        while (true) {
            ter.renderFrame(world.getTiles());
            if (!StdDraw.hasNextKeyTyped()) {
                StdDraw.pause(20);
                continue;
            }
            char key = Character.toLowerCase(StdDraw.nextKeyTyped());
            if (colonPending && key == 'q') {
                engine.save(world);
                return;
            }
            colonPending = (key == ':');
            if (key == 'w' || key == 'a' || key == 's' || key == 'd') {
                world.moveAvatar(key);
            }
        }
    }

    private void drawMenu() {
        StdDraw.clear(Color.black);
        StdDraw.setPenColor(Color.white);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 30));
        double cx = Engine.WIDTH / 2.0;
        StdDraw.text(cx, Engine.HEIGHT * 0.75, "CS61B: BYOW");
        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 20));
        StdDraw.text(cx, Engine.HEIGHT * 0.5, "New Game (N)");
        StdDraw.text(cx, Engine.HEIGHT * 0.5 - 2, "Load Game (L)");
        StdDraw.text(cx, Engine.HEIGHT * 0.5 - 4, "Quit (Q)");
        StdDraw.show();
    }

    private void drawSeedPrompt(String soFar) {
        StdDraw.clear(Color.black);
        StdDraw.setPenColor(Color.white);
        StdDraw.setFont(new Font("Monaco", Font.PLAIN, 20));
        double cx = Engine.WIDTH / 2.0;
        StdDraw.text(cx, Engine.HEIGHT * 0.6, "Enter a seed, then press S:");
        StdDraw.text(cx, Engine.HEIGHT * 0.5, soFar);
        StdDraw.show();
    }
}
