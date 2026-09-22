\# ◈ Algorithm Lab



\### Interactive Algorithm Visualization Laboratory



> \*\*Understand algorithms by watching them work.\*\*



Algorithm Lab is a modern JavaFX desktop laboratory for exploring algorithms, recursion, data structures, graph theory, and pathfinding through interactive visualizations.



Instead of simply displaying the final answer, Algorithm Lab focuses on \*\*what the algorithm is doing right now\*\*.



Every important operation can be visualized:



\* Algorithm state transitions

\* Comparisons

\* Swaps

\* Recursive calls

\* Call-stack changes

\* Graph traversal

\* Path exploration

\* Data structure operations

\* Execution statistics

\* Complexity information



The flagship visualization is \*\*Tower of Hanoi\*\*, supported by a collection of additional algorithm laboratories.



\---



\## ✦ Features



\### 🗼 Tower of Hanoi



A detailed interactive visualization of the classic recursive puzzle.



Features include:



\* Dynamic disk count

\* Automatic solving

\* Step-by-step execution

\* Play / Pause

\* Reset

\* Speed control

\* Move history

\* State timeline

\* Recursion visualization

\* Live call stack

\* Algorithm code visualization

\* Current executing line

\* Mathematical model

\* Dynamic minimum-move calculation

\* Real execution statistics

\* Reverse/state navigation where applicable



The minimum number of moves is calculated dynamically:



```text

T(n) = 2ⁿ - 1

```



\---



\## 🔢 Sorting Laboratory



Interactive visualizations for:



\* Bubble Sort

\* Selection Sort

\* Insertion Sort

\* Merge Sort

\* Quick Sort

\* Heap Sort



The visualization represents real algorithm operations rather than simulated statistics.



Tracked metrics include:



```text

Comparisons

Swaps

Array Size

Elapsed Time

Recursion Depth

Sorted Elements

```



\---



\## 🔍 Searching Laboratory



Implemented searching algorithms:



\* Linear Search

\* Binary Search



The laboratory visualizes:



\* Current element

\* Search interval

\* Eliminated regions

\* Target value

\* Comparisons

\* Search progress



\---



\## 🕸 Graph Traversal Laboratory



Interactive graph exploration with:



\* Breadth-First Search

\* Depth-First Search



Graph editing capabilities include:



\* Add nodes

\* Delete nodes

\* Connect nodes

\* Delete edges

\* Move nodes



During execution, the application visualizes structures such as:



```text

QUEUE



\[B]

\[C]

```



or:



```text

STACK



\[D]

\[B]

\[A]

```



\---



\## 🧭 Pathfinding Laboratory



Pathfinding algorithms include:



\* Breadth-First Search

\* Dijkstra

\* A\*



Interactive grid functionality includes:



\* Draw walls

\* Set start node

\* Set destination

\* Clear grid

\* Generate maze



The visualization can expose:



```text

Visited Cells

Path Length

Path Cost

g(n)

h(n)

f(n)

```



\---



\## 🔁 Recursion Playground



A dedicated environment for understanding recursive execution.



Examples include:



\* Factorial

\* Fibonacci

\* Tower of Hanoi

\* Binary Search

\* Merge Sort



The visualization focuses on:



```text

FUNCTION CALL

&#x20;     ↓

PARAMETERS

&#x20;     ↓

CALL STACK

&#x20;     ↓

RETURN VALUE

```



\---



\## 🧱 Data Structure Playground



Interactive visualizations for:



\* Stack

\* Queue

\* Linked List

\* Binary Search Tree

\* Heap



Supported operations include:



\* Insert

\* Delete

\* Search

\* Clear

\* Randomize



Operations are represented visually through animations and state changes.



\---



\## ⚖ Algorithm Comparison



Sorting algorithms can be executed against identical input data and compared using statistics produced by their actual execution.



Tracked information can include:



| Algorithm      | Comparisons |   Swaps | Execution |

| -------------- | ----------: | ------: | --------: |

| Bubble Sort    |     Dynamic | Dynamic |   Dynamic |

| Selection Sort |     Dynamic | Dynamic |   Dynamic |

| Insertion Sort |     Dynamic | Dynamic |   Dynamic |

| Merge Sort     |     Dynamic | Dynamic |   Dynamic |

| Quick Sort     |     Dynamic | Dynamic |   Dynamic |

| Heap Sort      |     Dynamic | Dynamic |   Dynamic |



No fabricated statistics are used.



\---



\# 🎨 Interface



Algorithm Lab is designed to feel more like a professional visualization tool than a traditional university project.



The interface focuses on:



\* Modern desktop UI

\* Clean information hierarchy

\* Light and dark themes

\* Glass-inspired panels

\* Subtle borders

\* Smooth transitions

\* Algorithmic backgrounds

\* Interactive controls

\* Consistent custom icons

\* Responsive layouts

\* Educational explanations



The goal is:



```text

Algorithm Laboratory

&#x20;       +

Developer Tool

&#x20;       +

Scientific Visualization

&#x20;       +

Interactive Learning

```



\---



\# 🌓 Themes



Algorithm Lab supports:



\* Light Mode

\* Dark Mode



Theme switching is integrated into the application interface.



\---



\# ⌨ Keyboard Shortcuts



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



\---



\# 🧠 Architecture



Algorithm logic is intentionally separated from JavaFX rendering.



The intended execution pipeline is:



```text

Algorithm

&#x20;   │

&#x20;   ▼

Algorithm State / Event

&#x20;   │

&#x20;   ▼

Visualization Engine

&#x20;   │

&#x20;   ▼

Animation Controller

&#x20;   │

&#x20;   ▼

JavaFX UI

```



For example:



```text

HanoiSolver

&#x20;    │

&#x20;    ▼

MoveEvent

&#x20;    │

&#x20;    ▼

VisualizationEngine

&#x20;    │

&#x20;    ▼

JavaFX Animation

```



This separation makes the algorithms easier to understand, test, extend, and reuse.



\---



\# 📁 Project Structure



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

│               │   ├── pathfinding/

│               │   └── graph/

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

├── README.md

└── LICENSE

```



\---



\# 🛠 Technology Stack



| Technology    | Version            |

| ------------- | ------------------ |

| Java          | 25 LTS             |

| JavaFX        | 25                 |

| Language      | Java               |

| UI            | JavaFX             |

| Architecture  | Modular / Layered  |

| Build System  | Manual / Pure Java |

| FXML          | Not used           |

| Scene Builder | Not used           |

| Maven         | Not used           |

| Gradle        | Not used           |



\---



\# 🚫 Design Philosophy



Algorithm Lab intentionally avoids the appearance of a traditional classroom demo.



It does not aim to be:



```text

Simple JavaFX Assignment

&#x20;       ❌

Static Algorithm Demo

&#x20;       ❌

Tower of Hanoi Game

&#x20;       ❌

```



Instead:



```text

Interactive Algorithm Laboratory

&#x20;       ✓

Real Algorithm Execution

&#x20;       ✓

Visual State Inspection

&#x20;       ✓

Educational Tool

&#x20;       ✓

Developer-Oriented Interface

&#x20;       ✓

```



\---



\# ⚡ Performance Goals



The application is designed with the following targets in mind:



\* Tower of Hanoi: up to 15 disks

\* Sorting: arrays up to approximately 200 elements

\* Graphs: dozens of nodes

\* Pathfinding: grids up to 50 × 50



Animations should remain responsive without blocking the JavaFX Application Thread.



\---



\# 🔬 Educational Focus



Algorithm Lab is designed around one central question:



> \*\*What is the algorithm doing right now?\*\*



Instead of only showing:



```text

Result = 42

```



the application attempts to show:



```text

What operation happened?

&#x20;       ↓

Why did it happen?

&#x20;       ↓

What state changed?

&#x20;       ↓

What happens next?

&#x20;       ↓

How does recursion / data structure / graph state evolve?

```



\---



\# 🚀 Running the Application



\## Requirements



Install:



\* Java 25 LTS

\* JavaFX 25



Verify Java:



```bash

java --version

```



Expected:



```text

java 25

```



Verify the compiler:



```bash

javac --version

```



\---



\## Running from an IDE



Open the project using:



\* IntelliJ IDEA

\* Eclipse



Configure the JavaFX SDK and use the project's main class:



```text

app.Main

```



\---



\# 🧪 Development Principles



Algorithm Lab follows several important principles:



\### Real execution



Visual statistics should come from actual algorithm execution.



\### Separation of concerns



Algorithm logic should not depend directly on JavaFX rendering.



\### Reusable components



Common visualization and UI components should be reusable across laboratories.



\### Responsive animation



Animations should not block the JavaFX Application Thread.



\### Maintainability



The application should remain modular as additional algorithms are introduced.



\---



\# 🗺 Roadmap



Potential future improvements include:



\* \[ ] More graph algorithms

\* \[ ] More sorting algorithms

\* \[ ] More pathfinding algorithms

\* \[ ] Advanced graph editors

\* \[ ] Algorithm recording/export

\* \[ ] Visualization screenshots

\* \[ ] Execution replay

\* \[ ] Algorithm benchmarking

\* \[ ] Custom algorithm playground

\* \[ ] Interactive pseudocode

\* \[ ] More data structures

\* \[ ] Algorithm state serialization

\* \[ ] Additional mathematical visualizations



\---



\# 📜 License



This project is released under the MIT License.



See \[`LICENSE`](LICENSE) for details.



\---



\# 👨‍💻 Author



\*\*Qusai Jaber\*\*



GitHub:



`https://github.com/qusayjber`



\---



\## ⭐ Project Goal



Algorithm Lab is built around a simple idea:



> \*\*Don't just learn the algorithm. Watch the algorithm think.\*\*



