<div align="center">
  <a href="#english">🇬🇧 English</a> &nbsp; | &nbsp; <a href="#tiếng-việt">🇻🇳 Tiếng Việt</a>
</div>

---

<h1 id="english">Cyberpunk Gomoku AI (Caro NxN)</h1>

[![CI](https://github.com/Thien21112005/heuristic-caro-nxn/actions/workflows/ci.yml/badge.svg)](https://github.com/Thien21112005/heuristic-caro-nxn/actions/workflows/ci.yml)

A modern, highly optimized, and scalable implementation of the classic Gomoku (Caro/Tic-Tac-Toe) game built with Java Swing and [FlatLaf](https://www.formdev.com/flatlaf/). This project features a modern dark, neon-accented user interface, dynamic NxN board generation, and a highly competitive Artificial Intelligence opponent powered by a custom **Heuristic Pattern Matching** algorithm.

## Table of Contents
- [Features](#features)
- [In-Depth AI Algorithm](#in-depth-ai-algorithm)
- [Architecture & Design](#architecture--design)
- [Prerequisites & Installation](#prerequisites--installation)
- [Usage & Configuration](#usage--configuration)

## Features

### Gameplay
- **Dynamic Board Sizing:** Any NxN board from 3x3 to 20x20, with quick presets and the last size remembered.
- **Clear win rule:** 3x3 to 5x5 need a full row, column or diagonal; 6x6 and up need 5 in a row. The rule is shown before and during the game.
- **Zoom & Pan:** Mouse-wheel zoom anchored at the cursor, drag to pan, plus zoom/fit buttons; the board auto-fits the window.
- **Two Game Modes:** Local Two Players and VS AI, with a running score across rounds.
- **Undo (optional):** Enable "Allow undo" in Settings; against the AI it takes back your move and the AI's reply.

### User Interface (UI/UX)
- **Modern dark theme:** FlatLaf look and feel with the Inter font, rounded cards, animated buttons and toggle switches.
- **Custom-painted board:** Hover preview of your next piece, pop-in animation, last-move marker and a glowing winning line.
- **Player cards:** Highlight whose turn it is, show an animated "Thinking..." state for the AI and each side's wins.
- **Friendly dialogs:** Centered cards over a dimmed window; "Play again" or "View board" after a game.
- **Keyboard shortcuts:** `Ctrl+Z` undo, `R` restart, `F` fit board, `+`/`-` zoom, `Esc` back.
- **Audio settings:** Background music and sound effects can be switched off; clips load on a background thread.

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
By scanning only from empty cells and limiting the ray cast distance to the target winning length (max 5 steps per direction), the time complexity per AI turn is strictly $\mathcal{O}(N^2)$, where $N$ is the board dimension. This guarantees a stable `~1ms` calculation time even on a 100x100 grid. To prevent the AI from feeling overly robotic, a randomized artificial delay (300ms - 800ms) is injected to simulate "thinking" time.

## Architecture & Design

The project strictly adheres to the **Model-View-Controller (MVC)** pattern:
- **Model (`src/model`):** `GameModel` (board, turns, win/draw detection, move history for undo), `GameAI` (heuristic engine), `Settings` (persisted preferences).
- **Controller (`src/controller`):** `GameController` validates moves, schedules and cancels the AI reply, and handles undo/restart.
- **View (`src/view`):** `MainWindow` hosts the screens in a `CardLayout`; reusable components live in `view/components` and the design tokens in `view/theme/Theme`.
- **Tests (`test`):** JUnit 5 tests for the rules, the AI and the controller.

## Prerequisites & Installation

### Requirements
- **Java Development Kit (JDK):** Version 17 or higher.
- **Apache Maven:** 3.8 or higher (downloads FlatLaf automatically).
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
3. **Run the game directly:**
   ```bash
   mvn compile exec:java
   ```
4. **Or build a standalone jar and run it:**
   ```bash
   mvn package
   java -jar target/caro-nxn.jar
   ```
5. **Run the tests:**
   ```bash
   mvn test
   ```

### Running with IntelliJ IDEA
1. Open IntelliJ IDEA and select **Open**.
2. Choose the `pom.xml` in the cloned `heuristic-caro-nxn` folder and open it **as a Project**.
3. Wait for Maven to download the dependencies.
4. Open `src/main/RunGame.java` and click the green **Run** button next to `main`.

## Usage & Configuration
**Settings** (saved automatically between launches):
- **Background music** / **Sound effects:** turn audio on or off.
- **Allow undo:** off by default (no take-backs). When on, an Undo button (`Ctrl+Z`) appears in the game. Against the AI it reverts both your move and the AI's reply.

**Controls:** mouse wheel to zoom, drag to pan, `F` to fit the board, `R` to restart, `Esc` to go back.

---

<br><br>

<h1 id="tiếng-việt">Cyberpunk Gomoku AI (Caro NxN)</h1>

Một phiên bản mở rộng, hiện đại và tối ưu hóa cao của trò chơi cờ Caro (Gomoku) kinh điển, được xây dựng bằng Java Swing và [FlatLaf](https://www.formdev.com/flatlaf/). Dự án có giao diện tối hiện đại với điểm nhấn neon, hỗ trợ linh hoạt kích thước bàn cờ NxN và được tích hợp một hệ thống Trí tuệ Nhân tạo (AI) đáng gờm, vận hành bởi thuật toán **Heuristic Pattern Matching** (Đánh giá theo mẫu chẩn đoán) tự xây dựng.

## Mục lục
- [Tính năng nổi bật](#tính-năng-nổi-bật)
- [Phân tích sâu Thuật toán AI](#phân-tích-sâu-thuật-toán-ai)
- [Kiến trúc & Thiết kế](#kiến-trúc--thiết-kế)
- [Hướng dẫn cài đặt](#hướng-dẫn-cài-đặt)
- [Sử dụng & Cấu hình](#sử-dụng--cấu-hình)

## Tính năng nổi bật

### Gameplay
- **Kích thước bàn linh hoạt:** Mọi bàn NxN từ 3x3 đến 20x20, có nút chọn nhanh và nhớ kích thước lần trước.
- **Luật thắng rõ ràng:** Bàn 3x3 đến 5x5 phải đủ cả hàng/cột/đường chéo; từ 6x6 trở lên cần 5 quân liên tiếp. Luật được hiển thị trước và trong ván.
- **Zoom & kéo bàn cờ:** Lăn chuột để zoom quanh con trỏ, kéo để di chuyển, có nút zoom/vừa khung; bàn cờ tự co giãn theo cửa sổ.
- **Hai chế độ chơi:** Hai người trên cùng một máy và Đấu với AI, có tính tỉ số qua các ván.
- **Đi lại (tuỳ chọn):** Bật "Allow undo" trong Settings; khi đấu với AI sẽ lùi cả nước của bạn lẫn nước của AI.

### Giao diện & Trải nghiệm (UI/UX)
- **Giao diện tối hiện đại:** FlatLaf với font Inter, thẻ bo góc, nút có hiệu ứng chuyển động và công tắc bật/tắt.
- **Bàn cờ tự vẽ:** Xem trước quân khi rê chuột, hiệu ứng đặt quân, đánh dấu nước vừa đi và đường thắng phát sáng.
- **Thẻ người chơi:** Làm nổi bật người đến lượt, hiển thị "Thinking..." khi AI đang nghĩ và số ván thắng của mỗi bên.
- **Hộp thoại thân thiện:** Hiện giữa màn hình trên nền mờ; sau ván có "Play again" hoặc "View board".
- **Phím tắt:** `Ctrl+Z` đi lại, `R` chơi lại, `F` vừa khung, `+`/`-` zoom, `Esc` quay lại.
- **Cài đặt âm thanh:** Bật/tắt nhạc nền và hiệu ứng; âm thanh được nạp ở luồng riêng nên không làm giật giao diện.

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

*Lưu ý: Để tránh AI đánh quá nhanh gây cảm giác "máy móc", code đã cố tình chèn thêm độ trễ ngẫu nhiên (300ms - 800ms) để giả lập thời gian "suy nghĩ" của con người.*

## Kiến trúc & Thiết kế
Dự án tuân thủ nghiêm ngặt mô hình kiến trúc **Model-View-Controller (MVC)**:
- **Model (`src/model`):** `GameModel` (bàn cờ, lượt đi, kiểm tra thắng/hoà, lịch sử nước đi để Undo), `GameAI` (engine heuristic), `Settings` (lưu cài đặt).
- **Controller (`src/controller`):** `GameController` kiểm tra nước đi, hẹn giờ/huỷ nước của AI, xử lý Undo và chơi lại.
- **View (`src/view`):** `MainWindow` chứa các màn hình bằng `CardLayout`; component dùng chung nằm trong `view/components`, màu sắc/font ở `view/theme/Theme`.
- **Tests (`test`):** Unit test JUnit 5 cho luật chơi, AI và controller.

## Hướng dẫn cài đặt

### Yêu cầu hệ thống
- **Java Development Kit (JDK):** Phiên bản 17 trở lên.
- **Apache Maven:** 3.8 trở lên (tự tải thư viện FlatLaf).
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
3. **Chạy game trực tiếp:**
   ```bash
   mvn compile exec:java
   ```
4. **Hoặc đóng gói thành file jar rồi chạy:**
   ```bash
   mvn package
   java -jar target/caro-nxn.jar
   ```
5. **Chạy unit test:**
   ```bash
   mvn test
   ```

### Chạy bằng IntelliJ IDEA (Dành cho người mới)
1. Mở IntelliJ IDEA, chọn nút **Open**.
2. Chọn file `pom.xml` trong thư mục `heuristic-caro-nxn` và mở **as a Project**.
3. Chờ Maven tải thư viện xong.
4. Mở `src/main/RunGame.java` và nhấn nút **Run** (tam giác xanh) cạnh hàm `main`.

## Sử dụng & Cấu hình
**Settings** (tự lưu giữa các lần mở game):
- **Background music** / **Sound effects:** bật/tắt nhạc nền và hiệu ứng âm thanh.
- **Allow undo:** mặc định tắt (hạ thủ bất hoàn). Khi bật, nút Undo (`Ctrl+Z`) sẽ xuất hiện trong ván. Khi đánh với AI, Undo lùi cả lượt của bạn và lượt của AI.

**Điều khiển:** lăn chuột để zoom, kéo để di chuyển bàn cờ, `F` vừa khung, `R` chơi lại, `Esc` quay lại.
