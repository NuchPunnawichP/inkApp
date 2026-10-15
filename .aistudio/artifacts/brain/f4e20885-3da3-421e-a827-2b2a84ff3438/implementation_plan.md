# Pen Stroke Width Slider Control (Completed)

Provide an intuitive, responsive stroke width slider control for the pen tool in InkNote, allowing users to fine-tune ink thickness from 1px to 30px with dynamic live preview and quick presets.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following design decisions were confirmed via interactive consultation:
> - **Placement**: Popover floating panel anchored directly above the Pen tool in the bottom dock, keeping the drawing canvas clean and uncluttered.
> - **Range & Features**: Continuous slider spanning **1px to 30px**, equipped with a real-time ink stroke preview matching the active ink color (Black, Blue, Red, White) and quick thickness preset pills (Fine, Regular, Medium, Broad, Marker).

---

### 1. Overview & Core Concept

- **What It Does**: Tapping the Pen tool when already active (or long-pressing / tapping a dedicated thickness indicator) toggles a sleek Material 3 popover card directly above the dock. Users can drag a smooth slider from 1px to 30px or tap fast presets, watching a live ink dot update in real time.
- **Target Experience**: Seamless, one-handed stylus drawing on the Vivo V23 5G without covering the sketching canvas.
- **Key Value**: Exact control over writing thickness for fine handwriting (1–3px), note-taking & sketching (4–8px), diagramming (10–16px), or bold markers & highlighting (20–30px).

---

### 2. User Experience & Visual Design

#### Key User Flow:
1. User taps the **Pen** button in the floating bottom dock.
2. If already selected, a secondary tap (or tapping the size badge) opens the **Stroke Width Popover** directly above the dock.
3. The popover displays:
   - **Live Ink Preview**: An animated circle displaying the exact stroke width in the user's selected primary color (Black, Blue, Red, or White) against a subtle contrasting background.
   - **Numerical Size Label**: Current pixel value (e.g. `6.0 px`).
   - **Continuous Slider**: 1.0px to 30.0px with tactile step snapping / smooth glide.
   - **Quick Presets**: 5 quick-select pills:
     - *Fine* (2 px)
     - *Regular* (5 px)
     - *Medium* (10 px)
     - *Bold* (18 px)
     - *Marker* (28 px)
4. Tapping outside the popover or tapping on the canvas instantly closes it so sketching resumes with zero delay.

#### Visual Styling:
- Surface: M3 `surfaceContainerHigh` with 24.dp rounded corners and soft drop shadow.
- Slider: Material 3 `Slider` with custom thumb and track styling matching the app's primary theme.
- Contrast: Live preview circle renders a subtle outline for White ink on light themes, or Black ink on dark themes.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Popover vs. Inline Dock Expansion**
  - *Chosen Approach*: Floating popover panel hovering above the dock.
  - *Why*: Keeps the minimalist bottom dock compact (never overflowing or cramming tool buttons together), while granting ample room for a wide touch slider and legible preview.
  - *Alternative Considered*: Expanding the dock horizontally would push tool buttons off-screen on compact portrait screens.

- **Decision 2: 1px to 30px Range with Stylus Pressure Scaling**
  - *Chosen Approach*: The slider sets the base stroke width (1px to 30px), which continues to dynamically scale with stylus pressure during sketching.
  - *Why*: Allows both delicate micro-writing and expressive calligraphy.

---

### 4. Technical Architecture & Component Mapping

```
┌────────────────────────────────────────────────────────┐
│ DrawingCanvasScreen                                    │
│                                                        │
│  ┌───────────────────────┐                             │
│  │ Canvas Drawing Area   │  (Listens for outside taps  │
│  │ (Strokes + Pressure)  │   to dismiss popover)       │
│  └───────────────────────┘                             │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ StrokeWidthPopover (Floating above dock)         │  │
│  │  • Live Ink Circle Preview (activeColor, width)  │  │
│  │  • Pixel Label ("6.0 px")                        │  │
│  │  • M3 Slider (valueRange = 1f..30f)              │  │
│  │  • Presets Row: [Fine, Reg, Med, Bold, Marker]   │  │
│  └──────────────────────────────────────────────────┘  │
│                           ▲                            │
│  ┌────────────────────────┴─────────────────────────┐  │
│  │ MinimalistToolDock                               │  │
│  │  [Pen (badge)]  [Eraser]  [Text] | [Colors] | ...│  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

#### Component & State Mapping:
- `NoteViewModel`:
  - `penWidth: StateFlow<Float>` (already supported, exposed to slider).
  - `setPenWidth(width: Float)` updates current pen width and auto-saves preferences.
- `DrawingCanvasScreen`:
  - `var showWidthPopover by remember { mutableStateOf(false) }`
  - `StrokeWidthPopover` Composable rendered conditionally using `AnimatedVisibility` (fade + slide up).
  - Tapping outside or resuming drawing dismisses the popover.
