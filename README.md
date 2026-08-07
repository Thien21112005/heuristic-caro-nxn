# Cyberpunk Tic-Tac-Toe (Caro NxN)

A modern, highly optimized, and scalable implementation of the classic Gomoku (Caro/Tic-Tac-Toe) game built with Java Swing. This project features a stunning Cyberpunk-themed user interface, dynamic NxN board generation, and a highly competitive Artificial Intelligence opponent powered by a custom Heuristic Pattern Matching algorithm.

## Table of Contents
- [Features](#features)
- [Architecture & Design](#architecture--design)
- [Artificial Intelligence Algorithm](#artificial-intelligence-algorithm)
- [Prerequisites & Installation](#prerequisites--installation)
- [Usage & Configuration](#usage--configuration)
- [Project Structure](#project-structure)
- [License](#license)

## Features

### Gameplay
- **Dynamic Board Sizing:** Support for NxN grid configurations (from classic 3x3 up to massive 20x20 boards).
- **Infinite Canvas Navigation:** Integrated drag-to-pan scrolling and mouse-wheel zooming, allowing players to comfortably navigate enormous boards without losing track of the game state.
- **Two Game Modes:** 
  - **Two Players (Local Co-op):** Play against a friend on the same machine.
  - **VS AI:** Challenge the built-in smart AI opponent.
- **Dynamic Turn Indicators:** Visual cues that dynamically highlight the active player's avatar while dimming the waiting player.

### User Interface (UI/UX)
- **Cyberpunk Aesthetic:** A cohesive, premium dark theme with neon accents, custom glowing dialogs, and a high-tech ambient background.
- **Custom Components:** Re-engineered Swing components including `CustomDialog`, `AvatarPanel`, and `CustomButton` with hover animations and sound effects to overcome the limitations of the default Java look-and-feel.

## Architecture & Design

The project strictly adheres to the **Model-View-Controller (MVC)** architectural pattern to ensure clean separation of concerns, scalability, and maintainability.

- **Model (`src/model`):** Manages the internal game state, board matrix data, turn logic, and algorithmic computations (Win detection & AI heuristic engine).
- **View (`src/view`):** Contains all GUI components. Views observe state passively and delegate user interactions to the controllers.
- **Controller (`src/controller`):** Handles business logic triggered by the UI, acts as the bridge between the View and the Model, and manages application routing (e.g., via `CardLayout`).

## Artificial Intelligence Algorithm

The single-player mode relies on a **Heuristic Pattern Matching** algorithm. Unlike traditional Minimax (which scales poorly on large NxN boards due to exponential time complexity `O(b^d)`), this heuristic approach evaluates the board locally and responds in sub-milliseconds, regardless of board size.

### Core Mechanics
1. **Directional Scanning:** For every empty cell on the board, the AI scans in 4 axes: Horizontal, Vertical, Primary Diagonal, and Secondary Diagonal.
2. **Dual Scoring System:** Each empty cell receives two distinct scores based on its surrounding patterns:
   - **Attack Score:** Evaluates how much this cell contributes to the AI's own winning patterns.
   - **Defense Score:** Evaluates how dangerous this cell is if the human player were to place their piece there.
3. **Pattern Weighting:** The algorithm assigns exponentially increasing weights to contiguous piece counts (e.g., 2-in-a-row, 3-in-a-row, 4-in-a-row). Open-ended patterns (unblocked on both sides) receive massive score multipliers compared to half-blocked patterns.
4. **Threat Prioritization:** If a human player establishes an "Open-3" or a "Half-blocked-4", the defense score for the blocking cells skyrockets, forcing the AI to immediately block the threat.

By summing the Attack and Defense scores for every valid move, the AI selects the coordinate with the highest aggregate score, ensuring a balance between aggressive expansion and impenetrable defense.

### Humanization (Thinking Delay)
To prevent the AI from feeling excessively robotic, a randomized asynchronous delay (ranging from 400ms to 1500ms) is injected before the AI registers its move. During this processing window, the board is locked to prevent input collision.

## Prerequisites & Installation

### Requirements
- Java Development Kit (JDK) 8 or higher.
- A modern IDE (IntelliJ IDEA, Eclipse, or VS Code) is recommended for compiling and running the project.

### Installation
1. Clone the repository:
   ```bash
   git clone <repository_url>
   ```
2. Navigate to the project root directory.
3. Compile the source code:
   ```bash
   javac -d out/production/TicTacToe -sourcepath src src/main/RunGame.java
   ```
4. Run the application:
   ```bash
   java -cp out/production/TicTacToe main.RunGame
   ```

## Usage & Configuration

### Cheat Mode (Undo Feature)
By default, the game enforces strict rules with no take-backs. However, for testing or casual play, a "Cheat Mode" can be enabled.
1. Navigate to **Settings** from the Main Menu.
2. Toggle **Enable Cheat Mode (Undo)** to `ON`.
3. During gameplay, an `Undo` button will now be visible, allowing you to revert moves (in VS AI mode, this automatically reverts both the AI's and the human's last move).

## Project Structure

```text
src/
├── assets/                  # Audio files, background images, and visual assets
├── controller/              # Event listeners and MVC controllers
├── main/
│   └── RunGame.java         # Application entry point
├── model/
│   ├── GameAI.java          # Heuristic engine logic
│   └── GameModel.java       # Core game state and win-checking logic
├── utils/
│   ├── ResourceUtils.java   # Utility for loading images smoothly
│   └── SoundPlayer.java     # Asynchronous audio playback utility
└── view/                    # Custom UI panels (GameMenu, TwoPlayerNxnPanel, CustomDialog, etc.)
```

## License
Distributed under the MIT License. See `LICENSE` for more information.
