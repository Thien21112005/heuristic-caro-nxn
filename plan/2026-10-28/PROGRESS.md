# Tiến độ — kế hoạch 2026-10-28

Xem chi tiết từng mục tại [ROADMAP.md](ROADMAP.md). Cập nhật mỗi khi xong một commit.

**Chú thích:** ⬜ chưa làm · ✅ xong · ⏭️ bỏ qua / đổi phương án (có ghi chú)

## Giai đoạn 0: Nền móng (`feat/foundation`)
- ✅ 0.1 CI chạy `mvn verify`
- ✅ 0.2 Badge CI trong README
- ✅ 0.3 Nén nhạc nền 22 kHz mono
- ✅ 0.4 Icon ứng dụng

## Giai đoạn 1: Song ngữ (`feat/i18n`)
- ✅ 1.1 I18n helper + bundle tiếng Anh
- ✅ 1.2 Chuỗi Main menu + Setup
- ✅ 1.3 Chuỗi Settings + About
- ✅ 1.4 Chuỗi màn hình chơi + hộp thoại
- ✅ 1.5 Bản dịch tiếng Việt
- ✅ 1.6 Chọn ngôn ngữ, đổi ngay
- ✅ 1.7 Test hai bundle đủ key

## Giai đoạn 2: Độ khó AI (`feat/ai-difficulty`)
- ✅ 2.1 Tách `AiStrategy`, `HeuristicAi`
- ✅ 2.2 AI chạy ở luồng nền
- ✅ 2.3 AI Dễ
- ✅ 2.4 `PatternEvaluator`
- ✅ 2.5 AI Khó (alpha-beta)
- ✅ 2.6 Test AI Khó
- ✅ 2.7 Chọn độ khó ở Setup
- ✅ 2.8 Hiện độ khó trong game
- ✅ 2.9 Nhớ độ khó

## Giai đoạn 3: Chọn phe (`feat/sides`)
- ✅ 3.1 `humanSide` trong controller
- ✅ 3.2 Test AI cầm X
- ✅ 3.3 Chọn X/O khi đấu AI
- ✅ 3.4 Đổi lượt đi trước mỗi ván
- ✅ 3.5 Tỉ số theo người chơi — làm trước 3.4 và đổi thành `refactor`, để không có commit nào chứa lỗi tỉ số khi đổi lượt

## Giai đoạn 4: Gợi ý (`feat/hint`)
- ✅ 4.1 Marker gợi ý trên bàn
- ✅ 4.2 Nút Hint + phím H
- ✅ 4.3 Tắt gợi ý trong Settings

## Giai đoạn 5: Thống kê (`feat/stats`)
- ✅ 5.1 `Stats` store
- ✅ 5.2 Test Stats
- ✅ 5.3 Ghi kết quả ván
- ✅ 5.4 Màn hình Stats
- ✅ 5.5 Xóa thống kê

## Giai đoạn 6: Đồng hồ (`feat/turn-clock`)
- ✅ 6.1 `EndReason` + forfeit
- ✅ 6.2 Đồng hồ trong controller
- ✅ 6.3 Test đồng hồ
- ✅ 6.4 Vòng đếm ngược trên PlayerCard
- ✅ 6.5 Chọn thời gian ở Setup
- ✅ 6.6 Nút Đầu hàng

## Giai đoạn 7: Replay (`feat/replay`)
- ✅ 7.1 Lịch sử nước đi + replay trong model
- ✅ 7.2 Test replay
- ✅ 7.3 BoardView vẽ từ display model
- ✅ 7.4 Số thứ tự nước đi
- ✅ 7.5 Thanh Replay
- ✅ 7.6 Phím ←/→ + tự phát

## Giai đoạn 8: LAN (`feat/lan`)
- ✅ 8.1 `GameSession` interface
- ✅ 8.2 Protocol — bỏ lệnh `START`: phe mỗi ván suy ra từ số ván (host cầm X ở ván lẻ) trên cả hai máy
- ✅ 8.3 Test protocol
- ✅ 8.4 Connection + heartbeat
- ✅ 8.5 LanHost / LanClient
- ✅ 8.6 Test loopback cả ván
- ✅ 8.7 UDP discovery
- ⬜ 8.8 NetworkGameController
- ⬜ 8.9 Lobby: tab Tạo phòng
- ⬜ 8.10 Lobby: tab Vào phòng
- ⬜ 8.11 GamePanel chế độ LAN
- ⬜ 8.12 Bắt tay chơi lại
- ⬜ 8.13 Mất kết nối / đầu hàng
- ⬜ 8.14 Tài liệu LAN + firewall

## Giai đoạn 9: Tiện ích (`feat/polish`)
- ⬜ 9.1 Âm lượng qua master gain
- ⬜ 9.2 Thanh trượt âm lượng
- ⬜ 9.3 Toàn màn hình F11
- ⬜ 9.4 Hướng dẫn lần đầu
- ⬜ 9.5 Mở lại hướng dẫn từ About
- ⬜ 9.6 Focus ring + tab order
- ⬜ 9.7 Ảnh chụp màn hình README

## Giai đoạn 10: Phát hành (`release/v2.0`)
- ⬜ 10.1 Profile jpackage
- ⬜ 10.2 Icon .ico
- ⬜ 10.3 Workflow release
- ⬜ 10.4 CHANGELOG
- ⬜ 10.5 Version 2.0.0
- ⬜ 10.6 Hướng dẫn tải và cài
