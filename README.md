<div align="center">

# ◈ ALGORITHM LAB

### Interactive Algorithm Visualization Laboratory

**Understand algorithms by watching them work.**

<br>

[![Java](https://img.shields.io/badge/Java-25%20LTS-ED8B00?style=for-the-badge\&logo=openjdk\&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-25-4285F4?style=for-the-badge\&logo=java\&logoColor=white)](https://openjfx.io/)
[![Architecture](https://img.shields.io/badge/Architecture-Modular-7C3AED?style=for-the-badge)](#architecture)
[![License](https://img.shields.io/badge/License-MIT-22C55E?style=for-the-badge)](#license)

<br>

**A professional desktop environment for exploring algorithms, recursion, data structures, graphs, and pathfinding through real-time visual execution.**

</div>

---

# ◈ What is Algorithm Lab?

**Algorithm Lab** is an interactive desktop application built with **Java 25 + JavaFX** that turns algorithms into visual, inspectable processes.

Instead of only showing the final result, Algorithm Lab focuses on the execution itself:

```text
INPUT
  │
  ▼
ALGORITHM
  │
  ├── Operation
  ├── Comparison
  ├── State Change
  ├── Recursive Call
  ├── Data Structure Update
  │
  ▼
VISUALIZATION
  │
  ▼
METRICS + EXPLANATION
```

The goal is simple:

> **Don't just learn what an algorithm produces. Understand what it is doing.**

---

# ⚡ Core Experience

Algorithm Lab combines four ideas into one environment:

| Layer           | Purpose                                            |
| --------------- | -------------------------------------------------- |
| 🧠 Algorithms   | Real algorithm implementations                     |
| ◈ Visualization | Every important operation becomes visible          |
| 📊 Analysis     | Metrics, complexity, execution state               |
| 🎓 Learning     | Explanations, recursion, code and state inspection |

This makes the application feel less like a classroom demonstration and more like an **algorithm development laboratory**.

---

# 🧪 Laboratories

## 🗼 Tower of Hanoi

The flagship visualization.

Explore recursive execution through a highly interactive Tower of Hanoi environment.

### Includes

* Dynamic disk count
* Automatic solving
* Step-by-step execution
* Play / Pause
* Reset
* Speed control
* Move history
* State timeline
* Previous / next state
* Recursion tree
* Live call stack
* Code execution visualization
* Current executing line
* Mathematical model
* Dynamic complexity information
* Real execution metrics

### Mathematical model

The minimum number of moves is calculated dynamically:

```text
T(n) = 2T(n - 1) + 1

T(n) = 2ⁿ - 1
```

No hardcoded move counts.

---

# 📊 Sorting Laboratory

Explore sorting algorithms as they execute.

### Algorithms

* Bubble Sort
* Selection Sort
* Insertion Sort
* Merge Sort
* Quick Sort
* Heap Sort

Algorithms are visualized using dynamic bars representing the actual array.

The visualization can expose:

```text
Comparisons
Swaps
Array Size
Elapsed Time
Recursion Depth
Sorted Elements
Current Operation
```

---

# 🔎 Searching Laboratory

Explore how searching algorithms progressively reduce the problem space.

### Algorithms

* Linear Search
* Binary Search

Visual information includes:

```text
Target
Current Element
Search Interval
Eliminated Region
Comparisons
Search Progress
```

---

# 🕸 Graph Traversal Laboratory

Build and explore graphs interactively.

### Algorithms

* Breadth-First Search
* Depth-First Search

### Graph editor

Create graphs by:

* Adding nodes
* Removing nodes
* Connecting nodes
* Removing edges
* Moving nodes

During execution, internal algorithm structures become visible.

### BFS

```text
QUEUE

[B]
[C]
```

### DFS

```text
STACK

[D]
[B]
[A]
```

---

# 🧭 Pathfinding Laboratory

Explore how algorithms discover paths through a grid.

### Algorithms

* Breadth-First Search
* Dijkstra
* A*

### Interactive environment

Users can:

* Draw walls
* Remove walls
* Set start
* Set destination
* Clear the grid
* Generate mazes

### A* inspection

Selected nodes can expose:

```text
g(n)
h(n)
f(n)
```

alongside the algorithm's exploration state.

---

# 🔁 Recursion Playground

A dedicated environment for understanding recursive execution.

Explore:

* Factorial
* Fibonacci
* Tower of Hanoi
* Binary Search
* Merge Sort

The recursion model focuses on:

```text
FUNCTION CALL
      │
      ▼
PARAMETERS
      │
      ▼
CALL STACK
      │
      ▼
RECURSIVE EXECUTION
      │
      ▼
RETURN VALUE
```

---

# 🧱 Data Structure Playground

Interactive visualization of fundamental data structures.

### Structures

* Stack
* Queue
* Linked List
* Binary Search Tree
* Heap

### Operations

```text
Insert
Delete
Search
Clear
Randomize
```

Operations are reflected immediately in the visual state.

---

# ⚖ Algorithm Comparison

Compare algorithms using the **same input**.

For sorting algorithms, Algorithm Lab can execute multiple algorithms against identical data and collect statistics from their actual execution.

Example:

```text
┌─────────────────────────────────────────────────────┐
│                 ALGORITHM COMPARISON                 │
├────────────────┬────────────┬────────┬──────────────┤
│ Algorithm      │ Comparisons│ Swaps  │ Execution    │
├────────────────┼────────────┼────────┼──────────────┤
│ Bubble Sort    │ Dynamic    │ Dynamic│ Dynamic      │
│ Selection Sort │ Dynamic    │ Dynamic│ Dynamic      │
│ Insertion Sort │ Dynamic    │ Dynamic│ Dynamic      │
│ Merge Sort     │ Dynamic    │ Dynamic│ Dynamic      │
│ Quick Sort     │ Dynamic    │ Dynamic│ Dynamic      │
│ Heap Sort      │ Dynamic    │ Dynamic│ Dynamic      │
└────────────────┴────────────┴────────┴──────────────┘
```

### No fabricated statistics

Metrics are generated from actual algorithm execution.

---

# 📈 Live Performance Metrics

Applicable laboratories can expose real-time execution information.

```text
EXECUTION

Operations          182
Comparisons          97
Swaps                41
Recursion Depth       6
Elapsed Time      0.084 s
```

The exact values depend on the actual execution.

---

# 🧠 Code + Algorithm Visualization

Algorithm Lab connects algorithm execution with its conceptual implementation.

For recursive algorithms, the environment can expose the currently active logical operation and relate it to the corresponding algorithm step.

Example:

```java
solve(n - 1, source, auxiliary, target);

move(source, target);

solve(n - 1, auxiliary, target, source);
```

The goal is to connect:

```text
CODE
 ↓
LOGIC
 ↓
STATE
 ↓
VISUALIZATION
```

---

# 🌳 Recursion Tree

Recursive algorithms can be inspected as a live tree.

Example:

```text
solve(4, A, C, B)
│
├── solve(3, A, B, C)
│   ├── solve(2, A, C, B)
│   │
│   └── move
│
├── move disk 4
│
└── solve(3, B, C, A)
```

The active recursive call is highlighted during execution.

---

# 📚 Move & Operation History

Every important operation can be recorded in a chronological history.

Example:

```text
#01   Disk 1      A → C
#02   Disk 2      A → B
#03   Disk 1      C → B
#04   Disk 3      A → C
```

The history can be used to inspect previous algorithm states.

---

# ⏱ State Timeline

Algorithm execution can be represented as a sequence of states:

```text
START
  │
  ●────●────●────●────●────●────●
       01   02   03   04   05   06
```

Each state represents a real point in the algorithm's execution.

---

# 🎨 Modern Desktop Experience

Algorithm Lab is intentionally designed to avoid the appearance of a traditional Java university project.

### Design principles

* Modern desktop interface
* Dark mode
* Light mode
* Glass-inspired panels
* Subtle borders
* Soft shadows
* Responsive layouts
* Smooth transitions
* Custom icon system
* Algorithmic backgrounds
* Interactive controls
* Clear information hierarchy

### The visual direction

```text
Developer Tool
      +
Scientific Visualization
      +
Interactive Learning
      +
Algorithm Laboratory
```

---

# 🌗 Themes

Algorithm Lab supports both:

**Dark Mode**

Designed for long visualization sessions and technical environments.

**Light Mode**

Designed for high readability and bright working environments.

Theme changes are handled at runtime.

---

# ⌨ Keyboard Shortcuts

| Shortcut   | Action         |
| ---------- | -------------- |
| `Space`    | Play / Pause   |
| `→`        | Next Step      |
| `←`        | Previous State |
| `R`        | Reset          |
| `+`        | Increase Speed |
| `-`        | Decrease Speed |
| `Ctrl + K` | Global Search  |
| `Ctrl + D` | Toggle Theme   |

---

# 🔍 Global Search

Quickly locate laboratories and algorithms.

Searchable concepts include:

```text
Tower of Hanoi
Merge Sort
Quick Sort
Binary Search
BFS
DFS
Dijkstra
A*
Stack
Queue
Recursion
Heap
Binary Search Tree
```

---

# ⚙ Settings

The application includes configurable visualization preferences.

### Appearance

```text
Theme
Animation Intensity
Particle Effects
```

### Visualization

```text
Show Labels
Show Complexity
Show Code
Show Recursion
Show Metrics
```

### Performance

```text
Animation FPS
Reduced Motion
```

---

# 🏗 Architecture

Algorithm Lab separates algorithm logic from JavaFX rendering.

The fundamental design is:

```text
┌─────────────────────────┐
│     Algorithm Logic     │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│      State / Events     │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│ Visualization Engine    │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│ Animation Controller     │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│       JavaFX UI         │
└─────────────────────────┘
```

For example:

```text
HanoiSolver
     │
     ▼
MoveEvent
     │
     ▼
VisualizationEngine
     │
     ▼
AnimationController
     │
     ▼
JavaFX
```

This keeps algorithm implementations independent from the presentation layer.

---

# 📁 Project Structure

```text
AlgorithmVisualizationLaboratory/
│
├── src/
│   └── main/
│       └── java/
│           └── app/
│               │
│               ├── Main.java
│               ├── ApplicationController.java
│               │
│               ├── algorithms/
│               │   ├── hanoi/
│               │   ├── sorting/
│               │   ├── searching/
│               │   ├── graph/
│               │   └── pathfinding/
│               │
│               ├── visualization/
│               │   ├── VisualizationEngine.java
│               │   ├── AnimationController.java
│               │   └── TimelineManager.java
│               │
│               ├── ui/
│               │   ├── MainWindow.java
│               │   ├── Sidebar.java
│               │   ├── TopBar.java
│               │   ├── StatusBar.java
│               │   ├── DashboardView.java
│               │   ├── VisualizationView.java
│               │   ├── InspectorPanel.java
│               │   └── components/
│               │
│               ├── model/
│               ├── theme/
│               ├── icons/
│               └── utils/
│
├── lib/
├── .gitignore
├── LICENSE
└── README.md
```

---

# 🛠 Technology

| Technology            | Version   |
| --------------------- | --------- |
| Java                  | 25 LTS    |
| JavaFX                | 25        |
| Language              | Java      |
| UI                    | JavaFX    |
| Architecture          | Modular   |
| Build                 | Pure Java |
| FXML                  | ❌         |
| Scene Builder         | ❌         |
| Maven                 | ❌         |
| Gradle                | ❌         |
| External UI Framework | ❌         |

---

# 🚀 Running the Project

## Requirements

Install:

* Java 25 LTS
* JavaFX 25

Verify Java:

```bash
java --version
```

Verify the compiler:

```bash
javac --version
```

Expected:

```text
java 25
javac 25
```

---

## IDE

The project can be opened using a Java IDE such as:

* IntelliJ IDEA
* Eclipse

Configure the JavaFX SDK and launch:

```text
app.Main
```

The project intentionally avoids Maven and Gradle.

---

# ⚡ Performance Targets

The application is designed with practical visualization limits in mind.

| Laboratory     |             Target |
| -------------- | -----------------: |
| Tower of Hanoi |     Up to 15 disks |
| Sorting        | Up to 200 elements |
| Graphs         |    Dozens of nodes |
| Pathfinding    |      Up to 50 × 50 |

Animations should remain responsive and should not block the JavaFX Application Thread.

JavaFX animation mechanisms such as:

```text
Timeline
PauseTransition
Task
Platform.runLater
```

can be used where appropriate.

---

# 🧩 Design Principles

## 01 — Real Execution

Visualizations represent actual algorithm operations.

## 02 — No Fake Metrics

Comparisons, swaps, moves, recursion depth and other metrics come from real execution.

## 03 — Separation of Concerns

Algorithms should not depend directly on JavaFX rendering.

## 04 — Visual Feedback

Important state changes should have a visible representation.

## 05 — Educational Clarity

Complex concepts should be broken into observable steps.

## 06 — Responsive Interaction

Animation and computation should be handled without freezing the UI.

## 07 — Extensibility

New algorithms and visualization laboratories should be possible without rewriting the entire application.

---

# 🧪 Educational Philosophy

Most algorithm tools answer:

> **What is the result?**

Algorithm Lab asks a different question:

> **What is happening right now?**

For every meaningful operation:

```text
What happened?
      ↓
Why did it happen?
      ↓
What state changed?
      ↓
What will happen next?
      ↓
How does the algorithm evolve?
```

This makes the application useful not only for running algorithms, but for understanding them.

---

# 🗺 Roadmap

### Visualization

* [ ] Advanced algorithm state replay
* [ ] Visualization recording
* [ ] Execution export
* [ ] More mathematical visualizations
* [ ] Advanced timeline controls

### Algorithms

* [ ] More sorting algorithms
* [ ] More graph algorithms
* [ ] More pathfinding algorithms
* [ ] More tree algorithms
* [ ] Additional dynamic programming visualizations

### Data Structures

* [ ] AVL Tree
* [ ] Red-Black Tree
* [ ] Trie
* [ ] Hash Table
* [ ] Graph data structures

### Developer Tools

* [ ] Custom algorithm playground
* [ ] Pseudocode editor
* [ ] Algorithm execution profiler
* [ ] Custom input generators
* [ ] Algorithm benchmarking
* [ ] Execution state serialization

---

# 📜 License

This project is licensed under the **MIT License**.

See the [`LICENSE`](LICENSE) file for details.

---

# 👨‍💻 Author

<div align="center">

### Qusai Jaber

Java Developer · Backend Developer · Software Engineering

[![GitHub](https://img.shields.io/badge/GitHub-qusayjber-181717?style=for-the-badge\&logo=github)](https://github.com/qusayjber)

</div>

---

<div align="center">

### ◈ Algorithm Lab

**Algorithms shouldn't just be read. They should be observed.**

<br>

`Java 25` · `JavaFX` · `Algorithms` · `Visualization` · `Recursion` · `Graphs` · `Data Structures`

</div>
