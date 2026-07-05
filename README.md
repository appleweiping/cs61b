# CS 61B — Data Structures & Algorithms (Spring 2021)

> From-skeleton implementations of every CS 61B project and the two map labs — a
> guitar-string synthesizer, a full **Gitlet** version-control system, and a
> **BYOW** procedural world engine — an independent build of
> **CS 61B — Data Structures** (UC Berkeley), part of a
> [csdiy.wiki](https://csdiy.wiki/) full-catalog build.

![status](https://img.shields.io/badge/status-complete-brightgreen)
![language](https://img.shields.io/badge/Java-21-informational)
![license](https://img.shields.io/badge/license-MIT-blue)

## Overview

CS 61B is UC Berkeley's second CS course, taught by Josh Hug: data structures,
algorithms, and Java engineering at scale. This repo contains complete,
runnable implementations of all four projects (Project 0–3) plus Labs 7 and 8,
built on top of the official [sp21 skeleton](https://sp21.datastructur.es/)
(imported as the first commit). Everything compiles and passes its own test
suite / autograder-style integration tests on JDK 21, CPU-only.

The centerpieces are **Gitlet** — a working Git clone with commits, branches,
merge (3-way, fast-forward, conflict), reset, checkout, and log — and **BYOW**,
a deterministic random-world generator with an interactive WASD explorer.

## Results (measured on Windows 11, JDK 21 / Temurin 21.0.7, CPU-only)

| Assignment | What it does | Result (measured) |
|---|---|---|
| **proj0 — 2048** | Game logic: tilt/merge, game-over & max-tile checks | JUnit **75/75** OK (5 test classes) |
| **proj1 — Deques + GuitarString** | `LinkedListDeque`, `ArrayDeque`, `MaxArrayDeque`, Karplus-Strong synth | JUnit **18/18** OK; synth stays bounded ≤0.5 and decays to ~0.006 after 1s |
| **proj2 — Gitlet** | Git-like VCS: init/add/commit/rm/log/status/checkout/branch/merge/reset | Staff integration tester **13/13 passed** |
| **proj3 — BYOW** | Deterministic explorable world engine + WASD driver | JUnit **6/6** OK (determinism, connectivity, wall collisions, save/load) |
| **lab7 — BSTMap** | Unbalanced BST-backed `Map61B` | JUnit **11/11** OK; 100k random inserts in 0.19s |
| **lab8 — MyHashMap** | Separate-chaining hash table, 5 bucket types, load-factor resize | JUnit **18/18** OK; 100k inserts in 0.13s (ArrayList buckets) |

### Sample BYOW world (seed `N2873123S`, 950 floor tiles)

Full ASCII render in [`results/proj3_byow_world_sample.txt`](results/proj3_byow_world_sample.txt).
Rooms connected by hallways, avatar `@` placed on a floor tile; BFS from the
avatar reaches every walkable tile (verified by `allFloorIsConnected`).

```
                                        ######                ######
                                        #·······# #########      #·······#
         ################################·······###·············########·······#
         #·······································································#
         #······#######··#······································########·······#
         ...  (walls #, floor ·, avatar @)  ...
       #·······#     #··········#·······@······#   #··········#·············
```

### Gitlet integration test output (`results/proj2_gitlet_tests.txt`)

```
test01-init: OK          test11-rm: OK              test16-errors: OK
test02-basic-checkout: OK test12-branch-checkout: OK test17-merge-noconflict: OK
test03-basic-log: OK      test13-merge-conflict: OK  test18-fastforward: OK
test04-prev-checkout: OK  test14-find-globallog: OK
test10-add-status: OK     test15-reset: OK
Ran 13 tests. All passed.
```

### Map lab speed comparison (real timings)

`lab7` shows why balance matters: inserting 10 000 **sorted** keys degenerates the
plain BST to an O(N)-deep spine (**1.22 s**) versus Java's balanced `TreeMap`
(**0.02 s**). `lab8` compares five bucket data structures inserting 100 000 random
keys — list-backed buckets (ArrayList/LinkedList, **0.13–0.14 s**) beat tree/heap
buckets (TreeSet/HashSet/PQ, **0.18–0.19 s**) because a bounded load factor keeps
buckets short. See [`results/lab7_bstmap_results.txt`](results/lab7_bstmap_results.txt)
and [`results/lab8_hashmap_results.txt`](results/lab8_hashmap_results.txt).

## Implemented assignments

- [x] **Project 0 — 2048** — game model: `tilt`, tile merging, `emptySpaceExists`, `maxTileExists`, `atLeastOneMoveExists`.
- [x] **Project 1 — Deques & GuitarString** — `Deque` interface, doubly-linked and circular-array deques, `MaxArrayDeque` with a comparator, and a Karplus-Strong `GuitarString` synthesizer.
- [x] **Project 2 — Gitlet** — a Git-like version-control system: `init`, `add`, `commit`, `rm`, `log`, `global-log`, `find`, `status`, three forms of `checkout`, `branch`, `rm-branch`, `reset`, and `merge` (clean 3-way, conflict, fast-forward, ancestor), plus every spec'd error message. Persistence via SHA-1 content-addressed blobs/commits.
- [x] **Project 3 — BYOW** — deterministic pseudo-random world generation (rooms + hallways, fully connected), a tile-rendering engine, an interactive WASD explorer, and quit/`:q` + load state persistence.
- [x] **Lab 7 — BSTMap** — an unbalanced binary-search-tree `Map61B` with `put`/`get`/`remove` (Hibbard deletion) and subtree-size tracking.
- [x] **Lab 8 — MyHashMap** — a separate-chaining hash table with load-factor resizing; the `createBucket()` factory lets subclasses swap in ArrayList/LinkedList/TreeSet/HashSet/PriorityQueue buckets.

## Project structure

```
cs61b/
├── proj0/game2048/        # 2048 game logic + JUnit tests
├── proj1/
│   ├── deque/             # LinkedListDeque, ArrayDeque, MaxArrayDeque + tests
│   └── gh2/               # Karplus-Strong GuitarString + Guitar Hero demo
├── proj2/
│   ├── gitlet/            # Gitlet VCS (Main, Repository, Commit, Utils, ...)
│   └── testing/           # staff tester.py + .in integration tests
├── proj3/byow/            # BYOW: Core (World/Engine/Driver) + TileEngine
├── lab7/bstmap/           # BSTMap + speed tests
├── lab8/hashmap/          # MyHashMap + 5 bucket subclasses + speed tests
├── library-sp21/javalib/  # course jars (JUnit, algs4, stdlib, jh61b)
└── results/               # captured test output + BYOW world sample
```

## How to run

All code targets **JDK 21** (`javac`/`java` on PATH) and the bundled jars in
`library-sp21/javalib/`. On Windows the classpath separator is `;` (use `:` on
macOS/Linux).

```bash
# --- proj0: 2048 ---
cd proj0
javac -cp 'javalib/*' game2048/*.java
java  -cp 'javalib/*;.' org.junit.runner.JUnitCore game2048.TestModel

# --- proj1: Deques + GuitarString ---
cd proj1
javac -cp '../library-sp21/javalib/*' deque/*.java gh2/*.java
java  -cp '../library-sp21/javalib/*;.' org.junit.runner.JUnitCore deque.ArrayDequeTest

# --- proj2: Gitlet (integration tester) ---
cd proj2
javac -d classes gitlet/*.java
cd testing
python tester.py --progdir="$(pwd)/../classes" samples/*.in student_tests/*.in
# You can also run gitlet directly:
#   java -cp ../classes gitlet.Main init

# --- proj3: BYOW ---
cd proj3
javac -cp '../library-sp21/javalib/*' byow/TileEngine/*.java byow/Core/*.java
java  -cp '../library-sp21/javalib/*;.' org.junit.runner.JUnitCore byow.Core.WorldGenerationTest
# Interactive explorer (opens a window):
#   java -cp '../library-sp21/javalib/*;.' byow.Core.Main

# --- lab7 / lab8 ---
cd lab7
javac -cp '../library-sp21/javalib/*' bstmap/*.java
java  -cp '../library-sp21/javalib/*;.' org.junit.runner.JUnitCore bstmap.TestBSTMap
cd ../lab8
javac -cp '../library-sp21/javalib/*' hashmap/*.java
java  -cp '../library-sp21/javalib/*;.' org.junit.runner.JUnitCore hashmap.TestMyHashMapBuckets
```

## Verification

- **Gitlet** was verified with the course's own staff integration tester
  (`proj2/testing/tester.py`), which drives `java gitlet.Main` through scripted
  `.in` files and diffs stdout against expected output. All 13 tests pass
  (4 staff samples + 9 additional tests covering rm, branches, merge conflicts,
  find/global-log, reset, error messages, no-conflict merge, fast-forward).
  A relative `--progdir` breaks because the tester chdirs into scratch dirs — use
  an **absolute** progdir path (shown above).
- **BYOW** is verified by `WorldGenerationTest` (6 tests): same seed ⇒ identical
  world (determinism), different seeds differ, the avatar starts on a floor tile,
  a BFS from the avatar reaches every walkable tile (full connectivity), WASD
  movement never passes through walls, and `...:q` + `L` reproduces the
  quit/reload state.
- **proj0/proj1/lab7/lab8** are verified by their JUnit suites; the map labs
  additionally run the course speed tests (real timings above).
- Raw captured output lives in [`results/`](results/): `proj0_2048_tests.txt`,
  `proj1_deque_tests.txt`, `proj2_gitlet_tests.txt`, `proj3_byow_tests.txt`,
  `proj3_byow_world_sample.txt`, `lab7_bstmap_results.txt`, `lab8_hashmap_results.txt`.

## Tech stack

- **Java 21** (Temurin), compiled and tested with JDK 21.
- **JUnit 4.12** + Hamcrest for unit tests; the CS 61B **`tester.py`** Python
  harness for Gitlet integration tests.
- Course libraries: `algs4`, `stdlib`, `jh61b`, `ucb` (in `library-sp21/javalib/`).

## Key ideas / what I learned

- **Content-addressed storage** — Gitlet keys blobs and commits by their SHA-1
  hash, exactly like Git; the commit DAG, staging area, and branch refs are all
  just serialized objects on disk.
- **Graph algorithms in anger** — Gitlet's `merge` walks the commit DAG to find
  the latest common ancestor; BYOW uses BFS to prove every floor tile is
  reachable and union-of-rooms hallway carving to guarantee connectivity.
- **Balanced vs. unbalanced structures** — lab7 makes the cost of an unbalanced
  BST on sorted input concrete (O(N) depth, 60× slower than `TreeMap`).
- **Hashing & amortized analysis** — lab8's load-factor-triggered resize keeps
  `put` amortized O(1); swapping the bucket data structure shows the constant
  factors that dominate when buckets stay short.
- **Deterministic procedural generation** — seeding a PRNG makes an entire world
  reproducible, which is what makes it testable.

## Credits & license

Based on the projects and labs of **CS 61B — Data Structures (Spring 2021)** by
**Josh Hug, UC Berkeley**. This repository is an independent educational
reimplementation; all course materials, skeleton code, specifications, and the
provided `library-sp21` jars belong to their original authors and the CS 61B
staff. My own implementation code is released under the [MIT License](LICENSE).
