# Changelog

All notable changes, generated from the Conventional Commit history by `tools/Changelog.java`.

## 2.0.0 — 2026-10-30

### Features

- **net:** add a versioned line-based protocol (a409003)
- **net:** add Connection with reader thread, send queue and heartbeat (d4f94a3)
- **net:** add LanHost and LanClient (8e9b6f1)
- **net:** announce and discover rooms with UDP broadcast (b6eb1ce)
- **controller:** add NetworkGameController with move validation (00f6d13)
- **lobby:** add host tab with room settings and waiting state (ee5894a)
- **lobby:** add join tab with discovered rooms and manual IP (1694a31)
- **game:** support LAN mode with opponent name and turn status (d8ea371)
- **game:** add restart handshake for LAN games (ea6402d)
- **game:** handle opponent disconnect and resign (8858ae4)
- **sound:** apply volume via the clip's master gain control (1f3b520)
- **settings:** add music and effects volume sliders (d14cb32)
- **window:** toggle fullscreen with F11 and remember it (92704f2)
- **ui:** add a first-launch walkthrough of rules and controls (7906f72)
- **about:** add a button to replay the walkthrough (01de63f)

### Bug Fixes

- **net:** report a hang-up we started as local even if the peer reacts first (830fa26)
- **stats:** keep the statistics tiles from collapsing in a decorated window (c8d0972)

## 1.2.0 — 2026-10-28

### ⚠ Breaking Changes

- **model:** a game can now have a winner with no winning line; callers must not assume getWinningLine() is non-null when getWinner() is set. (f184dab)

### Features

- **board:** add a pulsing hint marker (bde7975)
- **game:** add a Hint button and H shortcut backed by the hard AI (3a82c7b)
- **settings:** add an option to disable hints (194f93a)
- **model:** add a persistent Stats store per mode and difficulty (0a42f58)
- **game:** record finished rounds in Stats (7c74366)
- **stats:** add a statistics screen with win rate and streaks (dc4f5fb)
- **stats:** allow resetting statistics with confirmation (8447aea)
- **model:** add end reasons and forfeits (timeout, resign) (f184dab)
- **controller:** add a per-turn clock with an injectable time source (8d5a0eb)
- **game:** show a countdown ring on the active player card (0f69964)
- **setup:** add a turn time option (fa85a11)
- **game:** add a Resign button with confirmation (e208b85)
- **model:** expose ordered move history and replay to a given move (b4f7cbd)
- **board:** add optional move-number badges (5ab6477)
- **game:** add a replay bar to step through a finished game (56081ca)
- **game:** add arrow-key and auto-play controls to replay (b298c39)

## 1.1.0 — 2026-10-28

### Features

- **window:** add an application icon (bb265ac)
- **i18n:** add I18n helper and English message bundle (f1411e7)
- **i18n:** add Vietnamese translation (fe478c3)
- **settings:** add a language picker that reloads screens instantly (102cfb3)
- **ai:** add an easy AI that sometimes plays sub-optimal moves (894ea11)
- **ai:** add a pattern evaluator for open/broken threes and fours (21a5b10)
- **ai:** add a hard AI using time-limited alpha-beta search (6958197)
- **setup:** add a difficulty selector for VS AI (c094aa9)
- **game:** show the AI difficulty on the player card and header (155a2f2)
- **settings:** remember the last chosen difficulty (18a2daa)
- **setup:** let the player choose X or O against the AI (9f773b7)
- **game:** add an option to alternate the starting player each round (c7e5f11)

### Performance

- **assets:** downsample background music to 22 kHz mono (b34ca21)

## 1.0.0 — 2026-10-28

### Features

- **ui:** switch to a FlatLaf dark theme with shared design tokens (652b604)
- **settings:** persist preferences and route audio through SoundManager (97bcaff)
- **ui:** add animated AppButton, RoundedPanel and cover-scaled background (9223594)
- **ui:** redesign dialogs as centered cards over a dimmed backdrop (3bb5939)
- **menu:** redesign the main menu and drop the extra New Game step (37d2099)
- **setup:** add a unified game setup screen with presets and win rule (fe7d83e)
- **settings:** redesign settings with toggle switches and sound controls (a74ba05)
- **about:** redesign the About screen with rules and controls (145e2c5)
- **board:** add a custom-painted BoardView component (cbfaf25)
- **game:** redesign the game screen around BoardView and player cards (bcaaae7)

### Bug Fixes

- **ai:** cancel pending AI move on undo, reset and leave (55bbb30)
- **ai:** only fall back to the center cell when it is empty (b3da1f7)
- **board:** repaint the grid after reset (e07362b)
- **sound:** close one-shot clips once they finish playing (5a55381)
- **menu:** replace the previous game board instead of stacking it (577e415)
- **two-player:** cap the board size at 20 like the VS AI mode (1e89ee4)
- **game:** keep spacing and bold the score in the game-over dialog (9779728)
