# Kế hoạch triển khai Caro NxN v1.1 → v2.0

> Ngày lập: 2026-10-28 · Tiến độ theo dõi tại [PROGRESS.md](PROGRESS.md)
>
> Ngoài phạm vi (đã loại): luật chặn 2 đầu / "đúng 5", chơi online qua server.

## Tổng quan

| Giai đoạn | Nội dung | Commit | Ước lượng |
|---|---|---|---|
| 0 | Nền móng: CI, nén nhạc, icon cửa sổ | 4 | 0.5 ngày |
| 1 | Song ngữ Tiếng Việt / English | 7 | 1 ngày |
| 2 | Độ khó AI (Dễ / Thường / Khó) | 9 | 2–3 ngày |
| 3 | Chọn phe, ai đi trước, đổi lượt mỗi ván | 5 | 1 ngày |
| 4 | Gợi ý nước đi (Hint) | 3 | 0.5 ngày |
| 5 | Thống kê lâu dài | 5 | 1 ngày |
| 6 | Đồng hồ mỗi lượt + đầu hàng | 6 | 1–1.5 ngày |
| 7 | Đánh số nước đi + xem lại ván (Replay) | 6 | 1.5 ngày |
| 8 | Chơi qua chung wifi (LAN) | 14 | 4–5 ngày |
| 9 | Âm lượng, toàn màn hình, hướng dẫn lần đầu | 7 | 1.5 ngày |
| 10 | Đóng gói `.exe` + phát hành v2.0 | 6 | 1 ngày |
| | **Tổng** | **~72** | **~15–18 ngày** |

**Vì sao theo thứ tự này:**
- **Song ngữ làm sớm** để mọi tính năng sau viết chữ thẳng vào file ngôn ngữ ngay từ đầu, khỏi phải sửa lại hàng trăm chuỗi lúc cuối.
- **Đồng hồ (giai đoạn 6) làm trước LAN** vì nó tạo ra khái niệm "lý do kết thúc ván" (hết giờ, đầu hàng). LAN dùng lại khái niệm này cho trường hợp đối thủ đầu hàng hoặc mất kết nối.
- **Replay làm trước LAN** vì nó buộc bàn cờ phải vẽ được từ một model bất kỳ. Việc tách lớp đó giúp phần LAN gọn hơn.

---

## Quy ước chung

### Conventional Commits
```
<type>(<scope>): <mô tả ngắn, tiếng Anh, thể mệnh lệnh, ≤ 72 ký tự>

<thân: giải thích VÌ SAO>

<footer: BREAKING CHANGE: ..., Refs #issue>
```

| Type | Khi nào dùng |
|---|---|
| `feat` | Thêm tính năng mà người chơi thấy được |
| `fix` | Sửa lỗi |
| `refactor` | Đổi cấu trúc code, hành vi giữ nguyên |
| `perf` | Tối ưu hiệu năng hoặc dung lượng |
| `style` | Chỉnh giao diện (màu, kích thước, khoảng cách) |
| `test` | Thêm hoặc sửa test |
| `docs` | README, CHANGELOG, plan |
| `build` | `pom.xml`, đóng gói |
| `ci` | GitHub Actions |
| `chore` | Dọn dẹp, asset, cấu hình |

**Scope:** `ai`, `model`, `controller`, `board`, `game`, `setup`, `menu`, `settings`, `about`, `stats`, `net`, `lobby`, `i18n`, `sound`, `ui`, `window`, `release`, `plan`.

**Quy tắc:**
1. Mỗi commit là một thay đổi logic, và phải build được, `mvn verify` xanh.
2. Commit đổi hành vi thì đi kèm test, hoặc có commit `test:` ngay sau nó.
3. Thay đổi phá tương thích thì dùng `feat!:` và ghi `BREAKING CHANGE:` ở footer.

### Nhánh git
- Mỗi giai đoạn một nhánh: `feat/foundation`, `feat/i18n`, `feat/ai-difficulty`…
- Xong giai đoạn thì merge vào `master` bằng `--no-ff`, không squash, để giữ lịch sử commit nhỏ.
- Gắn tag `v1.1.0` (giai đoạn 0–3), `v1.2.0` (4–7), `v2.0.0` (8–10).

### Một tính năng được coi là "xong" khi
- Có unit test cho logic
- Chạy thật được và có ảnh chụp màn hình
- Có đủ chữ cho cả 2 ngôn ngữ (từ giai đoạn 1 trở đi)
- README và mục "How to play" trong About đã cập nhật

---

## Giai đoạn 0: Nền móng

**Mục tiêu:** mỗi lần push đều có CI kiểm tra, file jar nhẹ hơn, cửa sổ có icon riêng.

| # | Commit | Chi tiết |
|---|---|---|
| 0.1 | `ci: run Maven verify on push and pull requests` | `.github/workflows/ci.yml`: ubuntu-latest, Temurin 17, cache `~/.m2`, `mvn -B verify`. |
| 0.2 | `docs: add CI status badge to README` | |
| 0.3 | `perf(assets): downsample background music to 22 kHz mono` | 13MB còn khoảng 3MB, dùng tool Java `tools/AudioDownsample.java`. |
| 0.4 | `feat(window): add an application icon` | Icon 16/32/64/256px vẽ bằng Java2D, gọi `setIconImages` ở `MainWindow`. |

**Kiểm tra:** CI xanh, nhạc nghe vẫn ổn, taskbar hiện icon mới.

---

## Giai đoạn 1: Song ngữ Tiếng Việt / English

**Thiết kế:**
- `src/i18n/messages.properties` (tiếng Anh, mặc định) và `messages_vi.properties`. Từ Java 9, file `.properties` đọc được UTF-8 nên viết thẳng tiếng Việt có dấu.
- `utils/I18n`:
  - `I18n.t(key)` để lấy chuỗi
  - `I18n.t(key, args...)` để chèn tham số, dùng `MessageFormat`
  - `I18n.setLocale(...)`
- `Settings.language` (`en` / `vi`): mặc định lấy theo ngôn ngữ hệ điều hành.
- **Đổi ngôn ngữ lập tức:** `MainWindow.rebuildScreens()` tạo lại các màn hình rồi quay về Settings. Settings chỉ vào được từ menu chính, nên đổi ngôn ngữ không làm mất ván đang chơi.
- `pom.xml`: thêm `i18n/**` vào phần resources.

| # | Commit |
|---|---|
| 1.1 | `feat(i18n): add I18n helper and English message bundle` |
| 1.2 | `refactor(menu): load main menu and setup strings from the bundle` |
| 1.3 | `refactor(settings): load settings and about strings from the bundle` |
| 1.4 | `refactor(game): load game screen, status and dialog strings from the bundle` |
| 1.5 | `feat(i18n): add Vietnamese translation` |
| 1.6 | `feat(settings): add a language picker that reloads screens instantly` |
| 1.7 | `test(i18n): check both bundles define the same keys` |

**Kiểm tra:** chụp ảnh mọi màn hình ở cả 2 ngôn ngữ, không có chữ nào bị tràn khung (tiếng Việt thường dài hơn tiếng Anh 20–30%).

---

## Giai đoạn 2: Độ khó AI

**Thiết kế:**
```
model/ai/
  AiStrategy.java        interface: int[] chooseMove(int[][] board, int me, int opp)
  Difficulty.java        enum EASY, NORMAL, HARD
  HeuristicAi.java       = GameAI hiện tại (Normal)
  EasyAi.java            Heuristic + ngẫu nhiên
  PatternEvaluator.java  chấm điểm theo mẫu trên cửa sổ 5–6 ô
  SearchAi.java          Hard: alpha-beta có giới hạn thời gian
```
- **Dễ:** 35% số lần chọn ngẫu nhiên trong 6 nước có điểm cao nhất. Luôn đánh nước thắng ngay nếu có, nhưng chỉ chặn đường 4 của người chơi với xác suất 60%.
- **Thường:** giữ nguyên thuật toán hiện tại.
- **Khó:**
  - Nhận ra các mẫu: năm, bốn mở, bốn, ba mở, ba có ô trống giữa (`XX_X`), hai mở.
  - Thưởng điểm khi tạo nước đôi.
  - Negamax + alpha-beta, đào sâu dần, giới hạn khoảng 250ms.
  - Chỉ xét những ô trong bán kính 2 quanh các quân đã có, giữ khoảng 12 ô tốt nhất mỗi tầng.
- **Chạy AI ở luồng riêng:** `GameController` gửi việc tính vào executor kèm số hiệu ván (`generation`). Khi Undo hoặc Restart thì tăng số hiệu, nên kết quả tính cũ trả về muộn sẽ tự bị bỏ qua. Vẫn giữ độ trễ tối thiểu 300ms.

| # | Commit |
|---|---|
| 2.1 | `refactor(ai): extract AiStrategy and move the current engine to HeuristicAi` |
| 2.2 | `refactor(controller): compute AI moves off the EDT with stale-result guard` |
| 2.3 | `feat(ai): add an easy AI that sometimes plays sub-optimal moves` |
| 2.4 | `feat(ai): add a pattern evaluator for open/broken threes and fours` |
| 2.5 | `feat(ai): add a hard AI using time-limited alpha-beta search` |
| 2.6 | `test(ai): cover hard AI tactics and response time` |
| 2.7 | `feat(setup): add a difficulty selector for VS AI` |
| 2.8 | `feat(game): show the AI difficulty on the player card and header` |
| 2.9 | `feat(settings): remember the last chosen difficulty` |

**Test:**
- Khó chặn được ba mở và `XX_XX`, tự tạo bốn + ba khi có cơ hội, thấy được đường thắng sau 2 nước.
- Một nước của Khó trên bàn 20x20 mất dưới 400ms (ngưỡng trên CI rộng hơn).
- Khó thắng Thường ít nhất 70% số ván, cố định seed ngẫu nhiên.
- Dễ không bao giờ chọn ô đã có quân.

---

## Giai đoạn 3: Chọn phe và ai đi trước

- X luôn đi trước, nhưng người chơi được chọn cầm X hoặc O. Nếu người chơi cầm O thì AI cầm X và đánh ngay khi vào ván.
- `GameController`: thay hằng số `AI_PLAYER`/`HUMAN_PLAYER` bằng biến `humanSide`. Sửa logic Undo và thêm test cho trường hợp AI cầm X.
- Tùy chọn "Đổi lượt đi trước mỗi ván". Tỉ số tính theo người chơi, không tính theo quân.

| # | Commit |
|---|---|
| 3.1 | `refactor(controller): make the human side configurable instead of fixed X` |
| 3.2 | `test(controller): cover AI playing X and undo when the AI moved first` |
| 3.3 | `feat(setup): let the player choose X or O against the AI` |
| 3.4 | `feat(game): add an option to alternate the starting player each round` |
| 3.5 | `fix(game): track scores per player rather than per piece` |

---

## Giai đoạn 4: Gợi ý nước đi (Hint)

- Nút Hint (phím `H`) dùng AI mức Khó, tính cho người đang đến lượt, chạy ở luồng nền.
- `BoardView.flashHint(r, c)`: vòng sáng nhấp nháy ở ô được gợi ý, tự tắt sau 2.5 giây hoặc khi có người đánh.
- Settings thêm mục "Cho phép gợi ý". Hộp thoại kết thúc ván ghi số gợi ý đã dùng.

| # | Commit |
|---|---|
| 4.1 | `feat(board): add a pulsing hint marker` |
| 4.2 | `feat(game): add a Hint button and H shortcut backed by the hard AI` |
| 4.3 | `feat(settings): add an option to disable hints` |

---

## Giai đoạn 5: Thống kê lâu dài

- `model/Stats` (lưu bằng Preferences), tách theo chế độ và độ khó: `played`, `wins`, `losses`, `draws`, `currentStreak`, `bestStreak`, `fastestWinMoves`.
- Ghi ngay khi ván kết thúc. Nếu Undo nước thắng thì trừ lại.
- Màn hình Stats vào từ menu chính (`Settings | Stats | About`): tỉ lệ thắng cho từng độ khó, chuỗi thắng, nút xóa thống kê có xác nhận.

| # | Commit |
|---|---|
| 5.1 | `feat(model): add a persistent Stats store per mode and difficulty` |
| 5.2 | `test(model): cover stats recording, undo and streaks` |
| 5.3 | `feat(game): record finished rounds in Stats` |
| 5.4 | `feat(stats): add a statistics screen with win rate and streaks` |
| 5.5 | `feat(stats): allow resetting statistics with confirmation` |

---

## Giai đoạn 6: Đồng hồ mỗi lượt + đầu hàng

- `model/EndReason`: `LINE`, `DRAW`, `TIMEOUT`, `RESIGN`, `DISCONNECT`. `GameModel.forfeit(player, reason)` để xử thua.
- Bộ đếm giờ nằm trong `GameController`, thời gian lấy qua interface `Clock` để test dùng được đồng hồ giả. Chỉ đếm lượt của người chơi. Undo và Restart đặt lại đồng hồ.
- Lựa chọn: Tắt / 10s / 20s / 30s / 60s.
- Vòng đếm ngược trên PlayerCard, 5 giây cuối chuyển đỏ và có tiếng tích tắc.
- Nút Đầu hàng (Resign).

| # | Commit |
|---|---|
| 6.1 | `feat(model)!: add end reasons and forfeits (timeout, resign)` |
| 6.2 | `feat(controller): add a per-turn clock with an injectable time source` |
| 6.3 | `test(controller): cover timeout, undo and restart resetting the clock` |
| 6.4 | `feat(game): show a countdown ring on the active player card` |
| 6.5 | `feat(setup): add a turn time option` |
| 6.6 | `feat(game): add a Resign button with confirmation` |

---

## Giai đoạn 7: Đánh số nước đi và xem lại ván

- `GameModel.getMoves()` trả về danh sách nước đi theo thứ tự, và `GameModel.replay(moves, k)` dựng lại bàn cờ tại nước thứ k.
- `BoardView.setDisplayModel(...)` để vẽ được bàn cờ "ảnh chụp" bất kỳ. Khi đang xem lại thì không cho click.
- Nút bật/tắt số thứ tự nước đi trên quân (phím `N`).
- Thanh Replay sau khi chọn "View board": `⏮ ◀ ▶/⏸ ▶ ⏭` cùng thanh kéo, có thể dùng phím ←/→, tự phát 0.6 giây mỗi nước.

| # | Commit |
|---|---|
| 7.1 | `feat(model): expose ordered move history and replay to a given move` |
| 7.2 | `test(model): cover replay reconstruction` |
| 7.3 | `refactor(board): render from a swappable display model` |
| 7.4 | `feat(board): add optional move-number badges` |
| 7.5 | `feat(game): add a replay bar to step through a finished game` |
| 7.6 | `feat(game): add arrow-key and auto-play controls to replay` |

---

## Giai đoạn 8: Chơi qua chung wifi (LAN)

```
net/
  Protocol.java        mã hóa / giải mã các lệnh dạng dòng chữ
  Connection.java      bọc socket: luồng đọc, hàng đợi gửi, heartbeat, đẩy callback về EDT
  LanHost.java         ServerSocket, nhận đúng 1 người vào
  LanClient.java       kết nối tới host
  LanDiscovery.java    UDP broadcast để thông báo và tìm phòng
controller/
  GameSession.java              interface chung
  NetworkGameController.java
view/
  LanLobbyPanel.java   2 tab: Tạo phòng / Vào phòng
```

**Giao thức** (gửi từng dòng chữ qua TCP, có số phiên bản):
```
HELLO 1 <tên> <size> <turnSeconds>
START <firstPlayer>
MOVE <r> <c>
RESTART_REQ / RESTART_OK / RESTART_NO
RESIGN
PING / PONG          heartbeat 2 giây, quá 8 giây không phản hồi thì coi là mất kết nối
BYE
```

**Nguyên tắc:**
- Host quyết định luật (kích thước bàn, đồng hồ).
- Cả hai bên đều tự kiểm tra nước đi bằng `GameModel` của mình; nước đi không hợp lệ thì báo "mất đồng bộ" và kết thúc ván.
- Không có Undo. Chơi lại phải được cả hai bên đồng ý.
- Tìm phòng: host gửi gói UDP `CARO_HOST 1 <tên> <size> <port>` lên cổng 45679 mỗi giây. Vẫn có ô nhập IP thủ công.
- Mất kết nối thì tính là `EndReason.DISCONNECT`, không ghi vào thống kê.
- Giới hạn mỗi dòng tối đa 256 ký tự và kiểm tra tọa độ, tránh bị làm treo hoặc sập game.

| # | Commit |
|---|---|
| 8.1 | `refactor(controller): extract a GameSession interface from GameController` |
| 8.2 | `feat(net): add a versioned line-based protocol` |
| 8.3 | `test(net): cover protocol encoding, decoding and malformed input` |
| 8.4 | `feat(net): add Connection with reader thread, send queue and heartbeat` |
| 8.5 | `feat(net): add LanHost and LanClient` |
| 8.6 | `test(net): play a full game between host and client over loopback` |
| 8.7 | `feat(net): announce and discover rooms with UDP broadcast` |
| 8.8 | `feat(controller): add NetworkGameController with move validation` |
| 8.9 | `feat(lobby): add host tab with room settings and waiting state` |
| 8.10 | `feat(lobby): add join tab with discovered rooms and manual IP` |
| 8.11 | `feat(game): support LAN mode with opponent name and turn status` |
| 8.12 | `feat(game): add restart handshake for LAN games` |
| 8.13 | `feat(game): handle opponent disconnect and resign` |
| 8.14 | `docs: document LAN play and firewall setup` |

**Rủi ro:**
- **Windows Firewall** hỏi quyền lần đầu: hướng dẫn bấm "Allow" trong README và ngay trên màn hình Tạo phòng.
- **Wifi cô lập các máy** (AP isolation): một máy bật hotspot cho máy kia vào. Hiện gợi ý này khi vào phòng thất bại.

---

## Giai đoạn 9: Âm lượng, toàn màn hình, hướng dẫn lần đầu

| # | Commit |
|---|---|
| 9.1 | `feat(sound): apply volume via the clip's master gain control` |
| 9.2 | `feat(settings): add music and effects volume sliders` |
| 9.3 | `feat(window): toggle fullscreen with F11 and remember it` |
| 9.4 | `feat(ui): add a first-launch walkthrough of rules and controls` |
| 9.5 | `feat(about): add a button to replay the walkthrough` |
| 9.6 | `style(ui): add focus rings and tab order for keyboard navigation` |
| 9.7 | `docs: refresh screenshots in README` |

---

## Giai đoạn 10: Đóng gói `.exe` và phát hành v2.0

- Profile Maven `-Ppackage-win` gọi `jpackage --type app-image`: tạo thư mục `Caro/` có `Caro.exe` kèm sẵn Java, không cần WiX. Nén zip lại là gửi được.
- Bộ cài `.msi` cần WiX, để tùy chọn.
- `CHANGELOG.md` sinh từ lịch sử conventional commit.
- `release.yml`: khi push tag `v*` thì build trên Windows và đính kèm `caro-nxn.jar` cùng `Caro-windows.zip` vào GitHub Release.

| # | Commit |
|---|---|
| 10.1 | `build: add a jpackage profile producing a Windows app image` |
| 10.2 | `build: generate the Windows .ico from the app icon` |
| 10.3 | `ci: publish jar and Windows zip on version tags` |
| 10.4 | `docs: add CHANGELOG generated from conventional commits` |
| 10.5 | `chore(release): bump version to 2.0.0` |
| 10.6 | `docs: add download and installation instructions` |
