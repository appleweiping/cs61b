package byow.Core;

import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import org.junit.Test;

import java.util.ArrayDeque;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the core BYOW guarantees: determinism, floor connectivity,
 * avatar placement, movement, and save/load round-tripping.
 */
public class WorldGenerationTest {

    private static String render(TETile[][] w) {
        return TETile.toString(w);
    }

    @Test
    public void sameSeedProducesSameWorld() {
        Engine e1 = new Engine();
        Engine e2 = new Engine();
        TETile[][] a = e1.interactWithInputString("N1234567890123S");
        TETile[][] b = e2.interactWithInputString("N1234567890123S");
        assertNotNull(a);
        assertEquals("Same seed must yield identical worlds",
                render(a), render(b));
    }

    @Test
    public void differentSeedsDiffer() {
        Engine e = new Engine();
        String a = render(e.interactWithInputString("N1S"));
        String b = render(e.interactWithInputString("N99999S"));
        assertTrue("Different seeds should (almost surely) differ",
                !a.equals(b));
    }

    @Test
    public void avatarIsPlacedOnFloorNeighbourhood() {
        Engine e = new Engine();
        World w = e.runInput("N42S");
        assertNotNull(w);
        TETile[][] tiles = w.getTiles();
        assertEquals(Tileset.AVATAR, tiles[w.avatarX()][w.avatarY()]);
    }

    @Test
    public void allFloorIsConnected() {
        // Every FLOOR (or AVATAR) tile must be reachable from the avatar via
        // 4-directional movement over floor tiles.
        Engine e = new Engine();
        World w = e.runInput("N20218S");
        assertNotNull(w);
        TETile[][] tiles = w.getTiles();
        int width = w.getWidth();
        int height = w.getHeight();

        boolean[][] visited = new boolean[width][height];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{w.avatarX(), w.avatarY()});
        visited[w.avatarX()][w.avatarY()] = true;
        int reached = 0;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            reached += 1;
            for (int[] d : dirs) {
                int nx = cur[0] + d[0];
                int ny = cur[1] + d[1];
                if (nx < 0 || nx >= width || ny < 0 || ny >= height
                        || visited[nx][ny]) {
                    continue;
                }
                TETile t = tiles[nx][ny];
                if (t == Tileset.FLOOR || t == Tileset.AVATAR) {
                    visited[nx][ny] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }

        int totalWalkable = 0;
        for (int x = 0; x < width; x += 1) {
            for (int y = 0; y < height; y += 1) {
                if (tiles[x][y] == Tileset.FLOOR
                        || tiles[x][y] == Tileset.AVATAR) {
                    totalWalkable += 1;
                }
            }
        }
        assertTrue("World should contain a substantial floor area",
                totalWalkable > 100);
        assertEquals("Every walkable tile must be reachable from the avatar",
                totalWalkable, reached);
    }

    @Test
    public void movementRespectsWalls() {
        Engine e = new Engine();
        World w = e.runInput("N7S");
        assertNotNull(w);
        int startX = w.avatarX();
        int startY = w.avatarY();
        // Attempt many moves; the avatar must always remain on a floor cell
        // that is currently marked AVATAR.
        for (char c : "wasdwasdwasdddddwwww".toCharArray()) {
            w.moveAvatar(c);
            assertEquals(Tileset.AVATAR,
                    w.getTiles()[w.avatarX()][w.avatarY()]);
        }
        // Avatar count in the grid is always exactly one.
        int avatars = 0;
        for (int x = 0; x < w.getWidth(); x += 1) {
            for (int y = 0; y < w.getHeight(); y += 1) {
                if (w.getTiles()[x][y] == Tileset.AVATAR) {
                    avatars += 1;
                }
            }
        }
        assertEquals(1, avatars);
        // Sanity: start position was a valid coordinate.
        assertTrue(startX >= 0 && startY >= 0);
    }

    @Test
    public void quitAndLoadReproduceState() {
        // Playing "N..S<moves>:q" then "L" must equal "N..S<moves>".
        Engine e1 = new Engine();
        e1.interactWithInputString("N999S dddwww :q".replace(" ", ""));
        Engine e2 = new Engine();
        TETile[][] loaded = e2.interactWithInputString("L");

        Engine e3 = new Engine();
        TETile[][] direct = e3.interactWithInputString("N999Sdddwww");

        assertNotNull(loaded);
        assertEquals(render(direct), render(loaded));
    }
}
