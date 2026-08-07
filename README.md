<div align="center">
  <a href="#english">🇬🇧 English</a> &nbsp; | &nbsp; <a href="#tiếng-việt">🇻🇳 Tiếng Việt</a>
</div>

---

<h1 id="english">Cyberpunk Gomoku AI (Caro NxN)</h1>

A modern, highly optimized, and scalable implementation of the classic Gomoku (Caro/Tic-Tac-Toe) game built with Java Swing. This project features a stunning Cyberpunk-themed user interface, dynamic NxN board generation, and a highly competitive Artificial Intelligence opponent powered by a custom **Heuristic Pattern Matching** algorithm.

## Table of Contents
- [Features](#features)
- [In-Depth AI Algorithm](#in-depth-ai-algorithm)
- [Architecture & Design](#architecture--design)
- [Prerequisites & Installation](#prerequisites--installation)
- [Usage & Configuration](#usage--configuration)

## Features

### Gameplay
- **Dynamic Board Sizing:** Support for NxN grid configurations (from classic 3x3 up to massive 20x20+ boards).
- **Infinite Canvas Navigation:** Integrated drag-to-pan scrolling and mouse-wheel zooming, allowing players to comfortably navigate enormous boards without losing track of the game state.
- **Two Game Modes:** Local Co-op (Two Players) and VS AI.

### User Interface (UI/UX)
- **Cyberpunk Aesthetic:** A cohesive, premium dark theme with glowing dialogs and a high-tech background.
- **Dynamic Turn Indicators:** Visual cues that dynamically highlight the active player's avatar while dimming the waiting player (with custom default profiles for Player and AI).
- **Asynchronous Audio:** Hover effects, selection chimes, and victory/defeat sound effects processed asynchronously without blocking the UI thread.

## In-Depth AI Algorithm

The single-player mode relies on a **Heuristic Pattern Matching** algorithm. While traditional Minimax algorithms (even with Alpha-Beta pruning) scale poorly on large NxN boards due to their exponential time complexity $\mathcal{O}(b^d)$, this heuristic approach evaluates the board locally and responds in sub-milliseconds, regardless of the board size.

### 1. 4-Directional Ray Casting
For every empty cell `(x, y)` on the board, the AI casts rays in 4 axes:
- Horizontal `(0, 1)`
- Vertical `(1, 0)`
- Primary Diagonal `(1, 1)`
- Secondary Diagonal `(1, -1)`

It counts contiguous pieces belonging to a specific player and identifies how many ends are blocked (by the board edge or the opponent's pieces).

### 2. Dual Evaluation Mechanism
Every empty cell receives two distinct evaluations:
- **Attack Score:** "If I (AI) place my piece here, how close does it get me to winning?"
- **Defense Score:** "If the Human places their piece here, how dangerous is it?"

The final value for an empty cell is: `TotalScore(x, y) = AttackScore + DefenseScore`. 

### 3. Base Scoring Mathematical Function
The algorithm applies an exponential weight based on the contiguous piece count:
```java
long base = (long) Math.pow(10, count + 1);
```
- 1 piece in a row $\rightarrow$ 100 points
- 2 pieces in a row $\rightarrow$ 1,000 points
- 3 pieces in a row $\rightarrow$ 10,000 points
- 4 pieces in a row $\rightarrow$ 100,000 points

### 4. Strategic Multipliers & Blocking Mechanics
- **Aggressive Multiplier:** Attack scores are multiplied by `1.2x` to slightly favor proactive expansion over purely passive defending.
- **Half-blocked Penalty:** If a pattern is blocked on one end (`blocks == 1`), the base score is strictly halved (`base / 2`).
- **Dead-end Nullification:** If a pattern is blocked on both ends (`blocks == 2`) and hasn't reached the winning length, the score is forced to `0` because it's a dead pattern.

### 5. Winning Priorities (Lethal Threat Handling)
When a pattern reaches the target winning length (e.g., 5 in a row), exponential scaling is overridden by absolute priority constants:
- **AI Win (Attack):** `1,000,000,000` (First priority: If the AI can win immediately, it takes the move).
- **Human Win (Defense):** `500,000,000` (Second priority: If the AI cannot win immediately but the human is about to win, it MUST block).

### 6. Complexity Analysis
By scanning only from empty cells and limiting the ray cast distance to the target winning length (max 5 steps per direction), the time complexity per AI turn is strictly $\mathcal{O}(N^2)$, where $N$ is the board dimension. This guarantees a stable `~1ms` calculation time even on a 100x100 grid. To prevent the AI from feeling overly robotic, a randomized artificial delay (400ms - 1500ms) is injected to simulate "thinking" time.

## Architecture & Design

The project strictly adheres to the **Model-View-Controller (MVC)** pattern:
- **Model (`src/model`):** Manages internal game state, board matrix data, and the Heuristic Engine.
- **View (`src/view`):** Contains all GUI components (drawn with Java 2D Graphics and Swing).
- **Controller (`src/controller`):** Handles business logic triggered by the UI and manages application routing (`CardLayout`).

## Prerequisites & Installation

### Requirements
- **Java Development Kit (JDK):** Version 8 or higher (Java 11 or 17 recommended).
- **Environment Variables:** Ensure that your `JAVA_HOME` is set and the `java` and `javac` commands are accessible in your system's PATH.
- **IDE (Optional but recommended):** IntelliJ IDEA, Eclipse, or Visual Studio Code with the Java Extension Pack.

### Installation & Compilation (Command Line)
1. **Clone the repository:**
   ```bash
   git clone https://github.com/Thien21112005/heuristic-caro-nxn.git
   ```
2. **Navigate to the project directory:**
   ```bash
   cd heuristic-caro-nxn
   ```
3. **Compile the source code:**
   Ensure you are in the root directory of the project, then run the compiler to build the classes into the `out/production/TicTacToe` folder:
   ```bash
   javac -d out/production/TicTacToe -sourcepath src src/main/RunGame.java
   ```
4. **Launch the game:**
   ```bash
   java -cp out/production/TicTacToe main.RunGame
   ```

### Running with IntelliJ IDEA
1. Open IntelliJ IDEA and select **Open**.
2. Navigate to the cloned `heuristic-caro-nxn` folder and click **OK**.
3. Right-click on the `src/` directory and select **Mark Directory as > Sources Root**.
4. Open `src/main/RunGame.java`.
5. Click the green **Run** button next to the `public static void main` method to start the game.

## Usage & Configuration
**Cheat Mode (Undo Feature):**
By default, the game enforces strict rules (no take-backs). For testing purposes, you can enable "Cheat Mode" in the **Settings** menu. This will reveal the Undo button during gameplay, allowing you to revert moves endlessly.

---

<br><br>

<h1 id="tiếng-việt">Cyberpunk Gomoku AI (Caro NxN)</h1>

Một phiên bản mở rộng, hiện đại và tối ưu hóa cao của trò chơi cờ Caro (Gomoku) kinh điển, được xây dựng bằng Java Swing. Dự án mang đậm phong cách giao diện Cyberpunk, hỗ trợ linh hoạt kích thước bàn cờ NxN và được tích hợp một hệ thống Trí tuệ Nhân tạo (AI) đáng gờm, vận hành bởi thuật toán **Heuristic Pattern Matching** (Đánh giá theo mẫu chẩn đoán) tự xây dựng.

## Mục lục
- [Tính năng nổi bật](#tính-năng-nổi-bật)
- [Phân tích sâu Thuật toán AI](#phân-tích-sâu-thuật-toán-ai)
- [Kiến trúc & Thiết kế](#kiến-trúc--thiết-kế)
- [Hướng dẫn cài đặt](#hướng-dẫn-cài-đặt)
- [Sử dụng & Cấu hình](#sử-dụng--cấu-hình)

## Tính năng nổi bật

### Gameplay
- **Kích thước bảng linh hoạt:** Hỗ trợ mọi cấu hình NxN (từ 3x3 truyền thống cho đến bàn cờ khổng lồ 20x20+).
- **Điều hướng vô hạn (Infinite Canvas):** Tích hợp tính năng lăn chuột để Zoom (phóng to/thu nhỏ) và nhấn giữ kéo thả để di chuyển góc nhìn. Điều này giúp người chơi không bao giờ bị lạc lối trên các bàn cờ cỡ bự.
- **Hai chế độ chơi:** Đấu đôi (hai người chơi trên cùng một máy) và Đấu với AI.

### Giao diện & Trải nghiệm (UI/UX)
- **Phong cách Cyberpunk:** Sử dụng tông màu tối sang trọng, các hộp thoại (dialog) phát sáng viền Neon, cùng hình nền công nghệ cao.
- **Chỉ báo lượt đi động:** Các Avatar được vẽ tự động bằng Graphics2D sẽ phát sáng viền tương ứng với người đang đến lượt và làm tối người chờ.
- **Âm thanh bất đồng bộ:** Các hiệu ứng âm thanh (hover chuột, click, thắng/thua) được xử lý luồng riêng biệt, không làm giật lag giao diện.

## Phân tích sâu Thuật toán AI

Chế độ chơi đơn sử dụng thuật toán **Heuristic Pattern Matching** (Đánh giá Heuristic theo mẫu). Trong khi thuật toán Minimax truyền thống (ngay cả khi có Cắt tỉa Alpha-Beta) gặp bế tắc và giật lag trên các bàn cờ NxN lớn do độ phức tạp thời gian tăng theo cấp số mũ $\mathcal{O}(b^d)$, phương pháp Heuristic này chỉ đánh giá cục bộ tại các điểm trống, cho ra quyết định trong chưa tới 1 mili-giây bất chấp kích thước bàn cờ.

### 1. Quét tia 4 hướng (4-Directional Ray Casting)
Tại mỗi ô trống `(x, y)` trên bàn cờ, AI sẽ phóng các tia quét theo 4 trục:
- Ngang `(0, 1)`
- Dọc `(1, 0)`
- Chéo chính `(1, 1)`
- Chéo phụ `(1, -1)`

Nó sẽ đếm số lượng các quân cờ liên tiếp của một phe cụ thể, đồng thời kiểm tra xem chuỗi đó bị chặn mấy đầu (do rìa bàn cờ hoặc do quân địch chặn).

### 2. Cơ chế Chấm điểm Kép (Dual Evaluation)
Mỗi ô trống sẽ nhận được hai bài kiểm tra điểm số biệt lập:
- **Điểm Tấn công (Attack Score):** "Nếu AI đánh vào đây, nó sẽ hỗ trợ tạo ra chuỗi chiến thắng cho AI tốt đến mức nào?"
- **Điểm Phòng thủ (Defense Score):** "Nếu con người đánh vào đây, nó có gây nguy hiểm và tạo thành chuỗi thắng cho con người không?"

Tổng điểm của một nước đi sẽ là: `TotalScore(x, y) = AttackScore + DefenseScore`. Nước đi có tổng điểm cao nhất sẽ được AI chọn.

### 3. Hàm Toán học Tính điểm Nền tảng
Thuật toán sử dụng hàm số mũ dựa trên số lượng quân cờ liên tiếp:
```java
long base = (long) Math.pow(10, count + 1);
```
- 1 quân liên tiếp $\rightarrow$ 100 điểm
- 2 quân liên tiếp $\rightarrow$ 1,000 điểm
- 3 quân liên tiếp $\rightarrow$ 10,000 điểm
- 4 quân liên tiếp $\rightarrow$ 100,000 điểm

### 4. Hệ số Chiến thuật & Xử lý bị Chặn
- **Hệ số Hổ báo (Aggressive Multiplier):** Các điểm tấn công sẽ được nhân với `1.2x`. Điều này giúp AI ưu tiên chủ động mở rộng thế trận của mình hơn là chỉ chăm chăm đi chặn phòng thủ một cách thụ động.
- **Phạt chặn một đầu (Half-blocked Penalty):** Nếu một chuỗi cờ bị chặn một đầu (`blocks == 1`), số điểm gốc sẽ bị chia đôi ngay lập tức (`base / 2`).
- **Xóa sổ thế cờ chết (Dead-end Nullification):** Nếu một chuỗi cờ bị chặn cả hai đầu (`blocks == 2`) nhưng chưa đủ số lượng quân để thắng, số điểm sẽ bị ép về `0` vì đánh vào đó hoàn toàn vô nghĩa.

### 5. Ưu tiên Kết liễu (Xử lý mối đe dọa chí mạng)
Khi một mẫu đạt đủ độ dài chiến thắng (ví dụ: 5 quân liên tiếp), hàm số mũ sẽ bị ghi đè bởi các hằng số tuyệt đối:
- **AI Thắng (Attack):** `1,000,000,000` (Ưu tiên số 1: Nếu AI phát hiện có thể thắng ngay lập tức, nó sẽ đánh nước đó để chốt ván).
- **Người Thắng (Defense):** `500,000,000` (Ưu tiên số 2: Nếu AI chưa thể thắng, nhưng phát hiện con người chuẩn bị thắng, nó BẮT BUỘC phải đánh vào đó để chặn lại).

### 6. Phân tích Độ phức tạp Thời gian
Bằng cách chỉ quét từ các ô trống và giới hạn khoảng cách quét bằng với số quân cờ cần để thắng (tối đa 5 bước quét mỗi hướng), độ phức tạp thời gian cho mỗi lượt suy nghĩ của AI bị ép xuống chuẩn $\mathcal{O}(N^2)$ ($N$ là kích thước cạnh bàn cờ). Điều này đảm bảo AI luôn xử lý xong trong `~1ms` kể cả trên bàn cờ khổng lồ 100x100. 

*Lưu ý: Để tránh AI đánh quá nhanh gây cảm giác "máy móc", code đã cố tình chèn thêm độ trễ ngẫu nhiên (400ms - 1500ms) để giả lập thời gian "suy nghĩ" của con người.*

## Kiến trúc & Thiết kế
Dự án tuân thủ nghiêm ngặt mô hình kiến trúc **Model-View-Controller (MVC)**:
- **Model (`src/model`):** Quản lý trạng thái logic của ván cờ, ma trận điểm và Engine AI Heuristic.
- **View (`src/view`):** Chứa các component giao diện (được vẽ tùy chỉnh bằng Java 2D Graphics và Swing).
- **Controller (`src/controller`):** Xử lý các logic khi người dùng tương tác, luân chuyển các màn hình (`CardLayout`).

## Hướng dẫn cài đặt

### Yêu cầu hệ thống
- **Java Development Kit (JDK):** Yêu cầu phiên bản 8 trở lên (Khuyến khích dùng Java 11 hoặc 17).
- **Biến môi trường:** Đảm bảo bạn đã cài đặt biến `JAVA_HOME` và các lệnh `java`, `javac` có thể chạy được từ Terminal/Command Prompt.
- **IDE (Tùy chọn nhưng khuyên dùng):** IntelliJ IDEA, Eclipse, hoặc Visual Studio Code (kèm gói Java Extension).

### Cài đặt & Chạy bằng Command Line (Terminal)
1. **Clone mã nguồn về máy:**
   ```bash
   git clone https://github.com/Thien21112005/heuristic-caro-nxn.git
   ```
2. **Di chuyển vào thư mục dự án:**
   ```bash
   cd heuristic-caro-nxn
   ```
3. **Biên dịch code (Compile):**
   Đứng tại thư mục gốc của dự án, chạy lệnh sau để build code vào thư mục `out/production/TicTacToe`:
   ```bash
   javac -d out/production/TicTacToe -sourcepath src src/main/RunGame.java
   ```
4. **Khởi chạy Game:**
   ```bash
   java -cp out/production/TicTacToe main.RunGame
   ```

### Chạy bằng IntelliJ IDEA (Dành cho người mới)
1. Mở IntelliJ IDEA, chọn nút **Open**.
2. Tìm đến thư mục `heuristic-caro-nxn` vừa tải về và ấn **OK**.
3. Nhấn chuột phải vào thư mục `src/`, chọn **Mark Directory as > Sources Root**.
4. Mở file `src/main/RunGame.java`.
5. Nhấn vào nút **Run** (hình tam giác màu xanh lá cây) ở đầu file để khởi chạy game.

## Sử dụng & Cấu hình
**Chế độ Cheat (Nút Undo):**
Mặc định game áp dụng luật thi đấu nghiêm ngặt (hạ thủ bất hoàn - không được đi lại). Tuy nhiên, bạn có thể vào menu **Settings** và bật tính năng **"Enable Cheat Mode (Undo)"**. Khi vào game, nút Undo sẽ xuất hiện cho phép bạn quay ngược thời gian bao nhiêu lần tùy thích. (Khi đánh với AI, bấm Undo sẽ lùi lại cả lượt của bạn và lượt của AI).
