package byow.Core;

import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A pseudorandom 2D world of rooms connected by hallways, generated
 * deterministically from a seed. Every floor tile is reachable from every
 * other, because hallways are drawn between the centres of consecutively
 * generated rooms. The world also tracks a movable avatar.
 *
 * @author appleweiping
 */
public class World implements Serializable {

    private static final long serialVersionUID = 1L;

    /** A rectangular room, in tile coordinates (lower-left inclusive). */
    private static final class Room implements Serializable {
        private static final long serialVersionUID = 1L;
        final int x;
        final int y;
        final int w;
        final int h;

        Room(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        int centerX() {
            return x + w / 2;
        }

        int centerY() {
            return y + h / 2;
        }
    }

    private final int width;
    private final int height;
    private final long seed;
    /** The transient tile grid, rebuilt from (seed, moves) on load. */
    private transient TETile[][] tiles;
    private int avatarX;
    private int avatarY;
    /** History of applied avatar moves, for deterministic save/replay. */
    private final StringBuilder moveHistory = new StringBuilder();

    /** Generate a new world of the given size from SEED. */
    public World(int width, int height, long seed) {
        this.width = width;
        this.height = height;
        this.seed = seed;
        this.tiles = new TETile[width][height];
        fill(Tileset.NOTHING);
        generate(new Random(seed));
    }

    /** Rebuild the transient tile grid after deserialization by regenerating
     *  from the seed and replaying the recorded move history. */
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.tiles = new TETile[width][height];
        fill(Tileset.NOTHING);
        generate(new Random(seed));
        // Replay moves without re-recording them.
        String moves = moveHistory.toString();
        moveHistory.setLength(0);
        for (int i = 0; i < moves.length(); i += 1) {
            moveAvatar(moves.charAt(i));
        }
    }

    private void fill(TETile t) {
        for (int x = 0; x < width; x += 1) {
            for (int y = 0; y < height; y += 1) {
                tiles[x][y] = t;
            }
        }
    }

    /** Build rooms, connect them with hallways, wall everything, place avatar. */
    private void generate(Random rand) {
        int roomCount = RandomUtils.uniform(rand, 12, 22);
        List<Room> rooms = new ArrayList<>();
        int attempts = 0;
        while (rooms.size() < roomCount && attempts < roomCount * 20) {
            attempts += 1;
            int rw = RandomUtils.uniform(rand, 4, 9);
            int rh = RandomUtils.uniform(rand, 4, 8);
            int rx = RandomUtils.uniform(rand, 1, Math.max(2, width - rw - 1));
            int ry = RandomUtils.uniform(rand, 1, Math.max(2, height - rh - 1));
            Room room = new Room(rx, ry, rw, rh);
            if (overlaps(room, rooms)) {
                continue;
            }
            carveRoom(room);
            rooms.add(room);
        }

        // Connect consecutive room centres with L-shaped hallways, guaranteeing
        // the whole floor graph is connected.
        for (int i = 1; i < rooms.size(); i += 1) {
            connect(rooms.get(i - 1), rooms.get(i), rand);
        }

        addWalls();

        // Place the avatar in the first room's centre (a guaranteed floor tile).
        Room start = rooms.get(0);
        avatarX = start.centerX();
        avatarY = start.centerY();
        tiles[avatarX][avatarY] = Tileset.AVATAR;
    }

    private boolean overlaps(Room r, List<Room> rooms) {
        // Keep a 1-tile gap so rooms don't merge into blobs.
        for (Room o : rooms) {
            if (r.x - 1 < o.x + o.w && r.x + r.w + 1 > o.x
                    && r.y - 1 < o.y + o.h && r.y + r.h + 1 > o.y) {
                return true;
            }
        }
        return false;
    }

    private void carveRoom(Room r) {
        for (int x = r.x; x < r.x + r.w; x += 1) {
            for (int y = r.y; y < r.y + r.h; y += 1) {
                if (inBounds(x, y)) {
                    tiles[x][y] = Tileset.FLOOR;
                }
            }
        }
    }

    /** Draw an L-shaped hallway between the centres of A and B. */
    private void connect(Room a, Room b, Random rand) {
        int x1 = a.centerX();
        int y1 = a.centerY();
        int x2 = b.centerX();
        int y2 = b.centerY();
        if (RandomUtils.bernoulli(rand)) {
            carveHallH(x1, x2, y1);
            carveHallV(y1, y2, x2);
        } else {
            carveHallV(y1, y2, x1);
            carveHallH(x1, x2, y2);
        }
    }

    private void carveHallH(int xa, int xb, int y) {
        int lo = Math.min(xa, xb);
        int hi = Math.max(xa, xb);
        for (int x = lo; x <= hi; x += 1) {
            if (inBounds(x, y)) {
                tiles[x][y] = Tileset.FLOOR;
            }
        }
    }

    private void carveHallV(int ya, int yb, int x) {
        int lo = Math.min(ya, yb);
        int hi = Math.max(ya, yb);
        for (int y = lo; y <= hi; y += 1) {
            if (inBounds(x, y)) {
                tiles[x][y] = Tileset.FLOOR;
            }
        }
    }

    /** Surround every floor tile with walls wherever there is currently nothing. */
    private void addWalls() {
        for (int x = 0; x < width; x += 1) {
            for (int y = 0; y < height; y += 1) {
                if (tiles[x][y] != Tileset.FLOOR) {
                    continue;
                }
                for (int dx = -1; dx <= 1; dx += 1) {
                    for (int dy = -1; dy <= 1; dy += 1) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (inBounds(nx, ny)
                                && tiles[nx][ny] == Tileset.NOTHING) {
                            tiles[nx][ny] = Tileset.WALL;
                        }
                    }
                }
            }
        }
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /** Move the avatar one tile in direction DIR (w/a/s/d) if the target is
     *  floor. Returns true iff the avatar moved. */
    public boolean moveAvatar(char dir) {
        int nx = avatarX;
        int ny = avatarY;
        switch (Character.toLowerCase(dir)) {
            case 'w': ny += 1; break;
            case 's': ny -= 1; break;
            case 'a': nx -= 1; break;
            case 'd': nx += 1; break;
            default: return false;
        }
        moveHistory.append(Character.toLowerCase(dir));
        if (!inBounds(nx, ny) || tiles[nx][ny] != Tileset.FLOOR) {
            // Record the attempt (blocked moves are part of history but the
            // outcome is identical on replay) and report no movement.
            return false;
        }
        tiles[avatarX][avatarY] = Tileset.FLOOR;
        avatarX = nx;
        avatarY = ny;
        tiles[avatarX][avatarY] = Tileset.AVATAR;
        return true;
    }

    public TETile[][] getTiles() {
        return tiles;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int avatarX() {
        return avatarX;
    }

    public int avatarY() {
        return avatarY;
    }

    public long getSeed() {
        return seed;
    }

    /** Count the number of FLOOR tiles (used in tests/verification). */
    public int floorCount() {
        int count = 0;
        for (int x = 0; x < width; x += 1) {
            for (int y = 0; y < height; y += 1) {
                if (tiles[x][y] == Tileset.FLOOR) {
                    count += 1;
                }
            }
        }
        return count;
    }
}
