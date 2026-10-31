---
name: Caro
description: Caro on a page of a Vietnamese school notebook, in ink, ballpoint, red pen and highlighter.
colors:
  paper: "#F7F8FC"
  sheet: "#FFFFFF"
  sheet-tint: "#EBEDF8"
  printed-square: "#E2E5F4"
  printed-square-fine: "#F0F1F9"
  margin-line: "#E88C9A"
  pencil-line: "#7C85B3"
  ink: "#1E2A5A"
  ink-soft: "#4E5A86"
  ink-faded: "#8A91B3"
  ballpoint: "#4B2FB5"
  ballpoint-hover: "#5C3FCB"
  ballpoint-pressed: "#3D2596"
  piece-x: "#0B7285"
  piece-o: "#C2410C"
  highlighter: "#FFE45C"
  red-pen: "#C62828"
  red-pen-pressed: "#A31F1F"
  scrim: "#1E2A5A6E"
typography:
  display:
    fontFamily: "Mali, Inter, sans-serif"
    fontSize: "88px"
    fontWeight: 700
  number:
    fontFamily: "Mali, Inter, sans-serif"
    fontSize: "48px"
    fontWeight: 700
  headline:
    fontFamily: "Mali, Inter, sans-serif"
    fontSize: "28px"
    fontWeight: 700
  title:
    fontFamily: "Mali, Inter, sans-serif"
    fontSize: "20px"
    fontWeight: 700
  lead:
    fontFamily: "Mali, Inter, sans-serif"
    fontSize: "17px"
    fontWeight: 600
  label:
    fontFamily: "Mali, Inter, sans-serif"
    fontSize: "15px"
    fontWeight: 600
  body:
    fontFamily: "Inter, sans-serif"
    fontSize: "15px"
    fontWeight: 400
  small:
    fontFamily: "Inter, sans-serif"
    fontSize: "13px"
    fontWeight: 400
  small-strong:
    fontFamily: "Inter, sans-serif"
    fontSize: "13px"
    fontWeight: 600
rounded:
  field: "5px"
  sheet: "7px"
  md: "10px"
  full: "999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "20px"
  xl: "32px"
  page-inset: "48px"
components:
  button-primary:
    backgroundColor: "{colors.ballpoint}"
    textColor: "{colors.sheet}"
    typography: "{typography.lead}"
    rounded: "{rounded.md}"
    padding: "9px 20px"
    height: "56px"
  button-primary-hover:
    backgroundColor: "{colors.ballpoint-hover}"
  button-primary-pressed:
    backgroundColor: "{colors.ballpoint-pressed}"
  button-secondary:
    backgroundColor: "{colors.sheet}"
    textColor: "{colors.ink}"
    typography: "{typography.label}"
    rounded: "{rounded.md}"
    padding: "9px 20px"
    height: "42px"
  button-secondary-hover:
    backgroundColor: "{colors.sheet-tint}"
  button-secondary-pressed:
    backgroundColor: "{colors.printed-square}"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.ink-soft}"
    typography: "{typography.label}"
    rounded: "{rounded.md}"
    padding: "4px 12px"
  button-ghost-hover:
    backgroundColor: "{colors.sheet-tint}"
    textColor: "{colors.ink}"
  button-danger:
    backgroundColor: "{colors.red-pen}"
    textColor: "{colors.sheet}"
    typography: "{typography.label}"
    rounded: "{rounded.md}"
    padding: "9px 20px"
    height: "42px"
  button-danger-pressed:
    backgroundColor: "{colors.red-pen-pressed}"
  option-plain:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    typography: "{typography.label}"
    padding: "4px"
    height: "42px"
  option-circled:
    backgroundColor: "transparent"
    textColor: "{colors.ballpoint}"
    typography: "{typography.label}"
    padding: "4px"
    height: "42px"
  sheet-panel:
    backgroundColor: "{colors.sheet}"
    rounded: "{rounded.sheet}"
    padding: "24px 32px"
  board-sheet:
    backgroundColor: "{colors.sheet}"
    rounded: "{rounded.md}"
  dialog-card:
    backgroundColor: "{colors.sheet}"
    rounded: "{rounded.sheet}"
    padding: "28px 32px 24px"
  text-field:
    backgroundColor: "{colors.sheet}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.field}"
    height: "44px"
  toggle-off:
    backgroundColor: "{colors.sheet}"
    rounded: "{rounded.full}"
    width: "52px"
    height: "30px"
  toggle-on:
    backgroundColor: "{colors.ink}"
    rounded: "{rounded.full}"
    width: "52px"
    height: "30px"
  tooltip:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.sheet}"
    padding: "6px 10px"
---

# Design System: Caro

## Overview

**Creative North Star: "The Back Page of the Notebook"**

Caro is what you played in the back of a school notebook, so the app is that page: a Vietnamese vở ô ly with faint violet-blue squares, a pink-red margin line, ink, a purple ballpoint, the teacher's red pen and a yellow highlighter. Two ridiculous faces are the only loud things on it.

The system is a real desktop control set that borrows its hand from the page. Buttons, sliders and fields behave like standard controls, while their outlines, grid lines and marks wobble slightly as a pen does. Density is moderate and everything fits a 980×680 window. The board is the largest thing on any screen it appears on.

Choices are circled, wins are looped and highlighted, and scores are kept as tally strokes, because that is how each of those things is marked on paper. The rejected looks are the old dark neon treatment (wallpaper, gradient title, glowing lines), notebook costume (ring binders, torn edges, coffee stains), grey WinForms panels, cream paper and serif type.

**Key Characteristics:**
- Cool paper with printed squares and a double margin line on every full screen
- Mali handwriting for headings, numbers and buttons; Inter for small reading text
- One solid button per screen, and a pen circle for whatever is selected
- Seeded pen wobble that holds still on every repaint
- Flat paper layers with hairline edges and no shadows
- Large raster faces that are never covered or crossed out

## Colors

A restrained cool paper-and-ink palette, with four pens that each have one job.

### Primary
- **Purple Ballpoint** (#4B2FB5): the fill of the main action, the pen circle on a selected option, the title underline, the hint loop, the spinner, the keyboard cursor, and the focus ring. White on it reads at 8.85:1. It lightens to **Ballpoint Hover** (#5C3FCB) and darkens to **Ballpoint Pressed** (#3D2596).

### Secondary
- **Teacher's Red Pen** (#C62828): the loop around a winning line, the winner's ring and label, a running-out clock, the losses in a results bar, error messages, and the destructive dialog button. It darkens to **Red Pen Pressed** (#A31F1F).
- **Yellow Highlighter** (#FFE45C): the swipe under a winning line (85% opacity), the marker behind the winner's name and behind "your turn", and the hinted cell. It never carries text in its own colour; ink on it reads at 10.76:1.

### Tertiary
- **Piece Teal** (#0B7285) and **Piece Orange** (#C2410C): the marks belonging to each player: the turn ring, the turn clock, the last-move circle, move numbers and the status line. They identify a side and are always paired with a face or a written "X" or "O". The face artwork itself is raster and keeps its own brighter colours.

### Neutral
- **Cool Paper** (#F7F8FC): the page and window background.
- **Plain Sheet** (#FFFFFF): sheets laid on the page, the board, secondary buttons and fields.
- **Sheet Tint** (#EBEDF8): hover fill on buttons and empty board cells, and the tiles in the walkthrough drawings.
- **Printed Square** (#E2E5F4) and **Fine Printed Square** (#F0F1F9): the 24px and 8px page grid. Printed Square also draws row dividers and empty results bars. Purely decorative.
- **Margin Pink** (#E88C9A): the double margin line, nothing else. Decorative at 2.29:1.
- **Pencil Line** (#7C85B3): control outlines, board grid lines, slider track and scrollbar thumb. 3.37:1 on paper and 3.58:1 on a sheet, so it passes for UI parts and never for text.
- **Ink** (#1E2A5A): main text (12.9:1 on paper), toggles, sliders, the active pager dot, wins in a results bar, and tooltips.
- **Soft Ink** (#4E5A86): secondary text and ghost buttons (6.32:1 on paper), and the outline of a board sheet.
- **Faded Ink** (#8A91B3): disabled text and controls only (3.1:1 on a sheet).
- **Scrim** (Ink at 43%): the tint behind a dialog.

### Named Rules
**The One Solid Action Rule.** Each screen has at most one button filled in violet: the thing you came here to do. Everything else is outlined or plain, and a game in progress has none at all.

**The Pen Circle Rule.** A selected option is circled in ballpoint on a transparent ground, with its text turned violet. No option ever becomes a filled button.

**The Ink Controls Rule.** Toggles, sliders and pagers are ink, not violet. They show a setting or a position; only the main action and the pen circle may spend the ballpoint.

**The Red Pen Rule.** Red marks what a teacher would mark: the win, the error, the time running out, the thing you can't undo. A win is red because it is marked, not because it is bad.

## Typography

**Display Font:** Mali (bundled in Bold and SemiBold, with Inter as the fallback if a file can't load)
**Body Font:** Inter (bundled through FlatLaf)

**Character:** Mali is a Vietnamese school handwriting with round, friendly forms and full diacritics, so the voice of the app is a student's. Inter handles anything small enough to be read rather than glanced at.

### Hierarchy
- **Display** (Mali Bold, 88px): the "Caro" title on the main menu only, with its pen underline.
- **Number** (Mali Bold, 48px): the board size in setup.
- **Headline** (Mali Bold, 28px): screen titles, the result title beside the board, and the big figure in a results row.
- **Title** (Mali Bold, 20px): the game header, dialog titles, player names, column headings in the LAN lobby. Mali SemiBold at this size labels the setup start button.
- **Lead** (Mali SemiBold, 17px): main-action labels, the status line, results row names.
- **Label** (Mali SemiBold, 15px): buttons, options, field and section labels, a player's status.
- **Body** (Inter Regular, 15px): subtitles, explanations, dialog messages, field text. Long lines are wrapped to widths of 250 to 330px, and emphasis inside them is bold.
- **Small** (Inter Regular, 13px): captions, helper text, errors, game counts. **Small Strong** (Inter SemiBold, 13px) sets win counts and the replay position.

Line height comes from each font's own metrics; none is set by hand.

### Named Rules
**The Two Hands Rule.** If you would write it with a pen, it is Mali: titles, numbers, buttons, labels. If it is a sentence you need to read, it is Inter.

**The Spare Width Rule.** Every label is measured 4px wider than Swing reports, because Vietnamese text draws a little wider than it measures and the last letter would otherwise be cut off. Never set a label to an exact computed width.

## Layout

Each full screen is a page with a fixed grid: 24px printed squares divided into 8px fine squares, with a double margin line at 36 and 39px from the left. Content clears the margin with a 48px left inset and keeps 20px on the right. The window never goes below 980×680; anything taller than the window scrolls inside it rather than squashing.

- **Main menu:** a 340px column at a 68px inset, holding the title, one 58px primary button, three 46px secondary buttons and a row of ghost links. A mini board fills the remaining space.
- **Game:** a header row, then a 168px player card on each side of the board with 20px gaps, and a fixed 84px footer. The footer swaps between controls, the result and replay transport without changing height, so the board never jumps. Board cells size to fit between 22 and 120px, and the player can zoom and pan within that range.
- **Secondary screens:** a ghost back link on the page, then one centred sheet: 520px for settings, 560px for about, 816px for results, 864px for setup and the LAN lobby, with two 380px columns and a 40px gap on the wider ones. Setup pins its 56px primary button below the sheet and anchors the sheet to the top, so opening its extra options never moves the card's top edge.

Sheets use 32px side padding and 22 to 28px at the top. Inside them, gaps step through 4, 6, 8, 10, 12, 14 and 16px, tight between a label and its control and wider between groups.

## Elevation & Depth

Flat by design: there are no shadows anywhere in the app. Depth is paper lying on paper. The page sits at the back, a white sheet with a hairline Pencil Line edge at 59% opacity sits on it, and a dialog lifts only by dimming everything behind it with the scrim and taking a 1px edge in violet, or in red if it is destructive.

### Named Rules
**The One Sheet Rule.** A secondary screen keeps one white sheet on the paper, and the board keeps its own white sheet, because small text and grid lines read better without the printed squares behind them. The main menu's column and the game's header, player cards and footer stay directly on the page, because they are short labels rather than reading text.

**The Flat Paper Rule.** Nothing casts a shadow or glows. If something needs to stand out, mark it with a pen.

## Shapes

Shapes look drawn, not constructed. `Sketch` produces every signature line: boxes with slightly bowed sides, straight lines with a slow wander, loops that overshoot their own start as a quick hand does. Each wobble comes from a fixed seed (usually the text or cell index), so a shape is identical on every repaint and never shimmers.

Corner radii on screen are gently curved on hand-drawn shapes (buttons and the board sheet, 10px), tighter on sheets and dialogs (7px), tighter still on text fields (5px), and fully round on toggles, scrollbar thumbs and pager dots. Pen strokes use round caps and joins throughout, from 1.2px for dense board grids to 4px for the title underline.

Icons are small vector drawings painted at their size: the back arrow and chevron with a 2px pen stroke, the replay transport as filled shapes.

## Components

### Buttons
Quick to read and a little uneven, like a box drawn on the page. All buttons are at least 42px tall, darken or tint on hover over about 200ms, and play a soft click.
- **Shape:** a hand-drawn box with a 10px radius, seeded from its label.
- **Primary:** solid ballpoint with white Mali. 42px at its smallest, and usually taller: 56 to 58px where it is the screen's main action.
- **Secondary:** a white fill and a 1.4px pencil outline with ink text. The outline moves toward ink on hover, and the fill tints.
- **Ghost:** text alone in soft ink for back links, menu links and minor actions, with a tinted box on hover. Resign and clear-results turn red on hover.
- **Danger:** solid red pen, used only as the confirming button of a destructive dialog. It takes the role of the main action there.
- **Focus:** a 2px rounded ring inside the button, violet on outlined buttons and white on solid ones, shown only while navigating by keyboard.
- **Disabled:** Primary becomes a tinted fill with a faded outline, and Secondary a paper fill with a Printed Square outline. Text goes to faded ink.

### Choice rows
The pen circle in use, and the system's most recognisable control.
- **Style:** a row of plain options with equal widths and 4px gaps, 42px tall, each with ink text on a transparent ground. Options may carry a 28px face.
- **Selected:** a 2.2px ballpoint loop around the option, with its text in violet. Wide options get a squared-off loop and a square one gets a true circle. The state is also exposed to assistive tech.
- **Keyboard:** left and right arrows move the choice while the row has focus.
- **Also circled:** the board-size presets, and the move-numbers button when it is switched on.

### Sheets
- **Corner Style:** 7px.
- **Background:** Plain Sheet.
- **Shadow Strategy:** none; see Elevation & Depth.
- **Border:** a hairline of Pencil Line at 59% opacity.
- **Internal Padding:** 32px at the sides, 22 to 28px at the top.

Results inside a sheet are a table of rows rather than nested cards: 80px tall, divided by a Printed Square line, with a 10px rounded bar of ink, pencil and red for wins, draws and losses. The figures are always written out beside it.

### Inputs / Fields
- **Style:** 44px tall, white, with a 5px radius and a pencil outline, ink Inter text, and placeholder text in soft ink.
- **Focus:** the outline turns ballpoint.
- **Error:** a 13px line in red pen beneath the field group.

### Toggles and sliders
- **Toggle:** 52×30px and fully round. Off is an empty white track with a pencil outline and a soft ink knob; on is a filled ink track with a white knob at the other end, so the state reads without colour. Space toggles it, and the knob slides over about 170ms.
- **Slider:** a pencil track with its filled part and thumb in ink; when disabled, a Printed Square track with a faded thumb.
- **Pager:** 8px pencil dots with the active one stretched to a 22×8px ink bar.
- **Scrollbar:** a 10px pencil thumb on a transparent track.

### Navigation
A ghost back link with a pen-drawn arrow sits at the top left of every screen below the menu and answers to Esc. There is no persistent nav bar; the main menu's buttons and links are the hub.

### Dialogs
A white card with a 7px radius, 1px violet or red edge and 28/32/24px padding, centred on the scrim. Mali title, Inter message, and a 12px gap between equal-width buttons with the confirming one on the right. Enter confirms and Esc cancels. They confirm an action or deliver a notice, and never announce the result of a game.

### The Board
The signature component and the same drawing at every size, including the mini board on the menu.
- **Sheet:** a hand-drawn white box with a soft ink outline (1.8px) and pencil grid lines (2px up to 5×5, 1.2px above that).
- **Pieces:** raster faces inset 6% in their cell. They pop in over 220ms with a slight overshoot. Below 48px the O zooms into its face and takes a thin ring, so the face stays visible.
- **Hover and cursor:** an empty cell shows a tint and a 45% ghost of the piece. The keyboard cursor is a violet rounded square, only while using the keyboard.
- **Last move:** a small pen circle in the player's colour.
- **Hint:** highlighter fill, 55% ghost and a pulsing violet loop for 2.5 seconds.
- **The win:** a highlighter swipe under the grid, then a red stadium loop drawn stroke by stroke over 450ms, under the pieces and clear of them. The winning faces hop by 14% as the loop closes, and all other pieces fade to 55%.

### Player card
168×270px, with no container. An 84px face sits inside a 112px pen ring, drawn in the player's colour on their turn and red for the winner. With a turn clock, the ring itself runs out as time does, turning red for the last 5 seconds beside a written count. Waiting faces rest at 70%. Below the face are the name, the side, a tally of four strokes and a slash for wins, and the status line with a highlighter marker on "your turn".

### Title
Mali Bold with a single 4px ballpoint underline rising slightly to the right. The main menu title is the only use at display size.

### Spinner
An 18px pen circle, 70% drawn, in ballpoint, turning every 1.4 seconds.

## Do's and Don'ts

### Do:
- **Do** take every colour, size and radius from `Theme`; add a token there before using a new value.
- **Do** draw new outlines, lines and loops through `Sketch` with a fixed seed, so they never shimmer between repaints.
- **Do** keep to one solid violet button per screen, and name what it does.
- **Do** show a selected option with the pen circle, and keep toggles, sliders and pagers in ink.
- **Do** give each secondary screen one white sheet, and keep the board on its own.
- **Do** put game-end results beside the board or in the 84px footer, with the winner's face large and in view.
- **Do** draw new icons as vector shapes painted at their size.
- **Do** keep text at 4.5:1 and UI parts at 3:1 on paper, and pair colour with a shape, a word or a mark.
- **Do** paint focus rings and the board cursor only during keyboard use, through `FocusRing`.
- **Do** check every new string in Vietnamese at the 980×680 minimum.

### Don't:
- **Don't** set forms, tables, paragraphs or board lines directly on the printed squares.
- **Don't** fill a selected option or add a second solid violet button to a screen.
- **Don't** strike through or cover a face, or bring up a modal over the board at the end of a round.
- **Don't** resize the board when the footer changes state.
- **Don't** use the highlighter beyond the win, the turn marker and the hint, or Margin Pink for anything but the margin.
- **Don't** add ring binders, torn edges, coffee stains, cream paper or serif type.
- **Don't** bring back wallpaper behind the board, gradient titles, glowing lines or violet everywhere.
- **Don't** use grey WinForms panels, or drop shadows of any kind.
