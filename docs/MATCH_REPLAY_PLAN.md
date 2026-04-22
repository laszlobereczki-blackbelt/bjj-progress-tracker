# Match Replay and Analysis — Implementation Plan

A feature for the BJJ Progression Tracker that lets the user reconstruct a match step-by-step, record fatigue per step, and receive pattern-based analysis and suggestions. All BJJ domain knowledge (moves, transitions, recommendations) lives in an **external JSON file** — the app treats it as opaque data, so it can be refined without code changes.

> **Scope note.** BJJ correctness is explicitly out of scope. The app is a generic two-player sequence recorder with pattern analysis; the JSON catalog supplies meaning.

---

## 1. Phased Implementation Plan

Each phase is independently shippable and adds visible value.

### Phase 1 — Basic Replay (skeleton) ✅ DONE
**Goal:** Create, save, and play back a match as an ordered list of events (player + move + timestamp).

Key components:
- New feature package `hu.blackbelt.matchreplay` following the existing feature-package convention.
- Entities: `Match`, `MatchEvent`.
- Service: `MatchService` (CRUD, ordered event append).
- UI: `MatchReplayListView` (list of saved matches), `MatchEditorView` (create/edit), `MatchPlaybackView` (step-through).
- Menu entry (see §4).

**Implemented 2026-04-21.** Package `hu.blackbelt.matchreplay`. Menu item "Match Lab" at order 6 (`vaadin:film`). Editor supports add-event and undo-last. Playback is step-through with clickable event list.

### Phase 2 — Data Tracking (fatigue + context) ✅ DONE
**Goal:** Richer per-step data so analysis has signal to work with.

Key components:
- Extend `MatchEvent` with `fatigueA` (0–10), `fatigueB` (0–10), `positionAfter` (string key from JSON), optional `note`.
- Match-level fields: `opponentName`, `matchDate`, `durationSeconds`, `resultOutcome` (enum), `selfRole` (A or B — which side is the user).
- UI: fatigue sliders inline in the editor; compact display in playback.

**Implemented 2026-04-21.** `ResultOutcome` enum added. `Match` extended with `selfRole`, `durationSeconds`, `resultOutcome`. `MatchEvent` extended with `fatigueA`, `fatigueB`, `positionAfter`. New Match dialog includes all match-level fields. Editor shows fatigue step-fields (sticky from last event) and positionAfter input; actor auto-alternates after each add. Playback card shows positionAfter, fatigue (A/B), and note. List view shows "You are", Result, and Duration columns.

### Phase 3 — External Move Catalog + Smart Input ✅ DONE
**Goal:** Load move/transition/recommendation data from an external JSON file and use it to speed up input.

Key components:
- `MoveCatalog` loader: reads `moves.json` from a configurable path (`bjj.match-replay.catalog-path` in `application.properties`), watches for changes, exposes an in-memory read model.
- Input widget: searchable move picker, quick buttons for legal transitions from current position, "recent moves" row.
- Validation is **advisory only** — the UI warns on out-of-catalog transitions but never blocks, since the catalog will be incomplete for a long time.

**Implemented 2026-04-21.** Package `hu.blackbelt.matchreplay.catalog`. `MoveCatalogLoader` (`@Service`) reads `config/moves.json` on startup (copies bundled `moves-default.json` on first run), watches the directory via `WatchService` for live reloads, and exposes an `AtomicReference<MoveCatalogSnapshot>`. Record types: `CatalogPosition`, `CatalogMove`, `CatalogTransition`, `CatalogResponseEntry`, `CatalogRecommendedResponse`, `MoveCatalogSnapshot` (with `search()`, `transitionsFrom()`, `findMove()` helpers). Editor `MatchEditorView` upgraded: move `TextField` replaced with a `ComboBox<String>` with server-side label+tag+key filtering and `allowCustomValue=true`; "Quick:" chips show legal transitions from the current position with auto-fill of positionAfter; "Recent:" chips surface last 5 distinct moves; advisory warning shown for out-of-catalog keys. Timeline cards now display human-readable move labels and position labels from the catalog.

### Phase 4 — Pattern Detection and Frequencies ✅ DONE
**Goal:** Compute per-match statistics and detect recurring sequences.

Key components:
- `MatchAnalyzer` service (pure functions over a `Match` aggregate).
- Outputs: move frequencies per player, top N-grams (n=2,3) per player, fatigue-over-time series, time-in-position totals.
- `MatchAnalysisView` with charts (Vaadin Chart or simple CSS bars — decide during implementation).

**Implemented 2026-04-21.** Package `hu.blackbelt.matchreplay.analysis`. Records: `MoveFrequency`, `PatternOccurrence`, `FatiguePoint`, `PositionTime`, `MatchAnalysis`. `MatchAnalyzer` (`@Service`) is pure (no JPA/UI dependencies) and computes move frequencies per actor, n=2 and n=3 move N-grams (patterns with ≥2 occurrences), fatigue-over-time series, and time-in-position step counts. `MatchAnalysisView` at route `match-lab/analysis` shows three `TabSheet` panels: **Frequencies** (CSS horizontal bars for top moves per player + time-in-position block), **Patterns** (N-gram cards grouped by actor), **Fatigue** (per-step table with inline fill-bar indicators for both players). "Analyze" button added to `MatchReplayListView` grid and `MatchEditorView` header.

### Phase 5 — Recommendations and Mistake Flagging ✅ DONE
**Goal:** Compare user actions to catalog recommendations and surface actionable feedback.

Key components:
- `RecommendationEngine`: for each of the user's moves, looks up `recommendedResponses` for the opponent's preceding move in the JSON catalog. Flags "mistake" when the user's choice has a lower `score` than the top recommendation above a configurable gap.
- Feedback panel in the analysis view: mistake list with the recommended alternative and a short reason string pulled from JSON.

**Implemented 2026-04-22.** Package `hu.blackbelt.matchreplay.analysis`. `MistakeSeverity` enum (HIGH/MEDIUM/LOW). `MistakeFinding` record captures stepIndex, user/opponent move keys+labels, position, chosenScore, top recommendation (key, label, score, reason), scoreGap, and severity. `RecommendationEngine` (`@Service`) iterates events where actor==selfRole and the preceding event was the opponent's; looks up `recommendedResponses` by (opponentMoveKey, fromPositionKey); computes score gap (0 if user move absent from response list); severity: HIGH (gap≥5), MEDIUM (3–4), LOW (1–2). `MatchAnalysisView` extended: injects `RecommendationEngine`, computes findings alongside `MatchAnalysis`, adds a fourth **Feedback** tab showing: guidance when selfRole is unset; a list of HIGH/MEDIUM mistake cards (color-coded border) each with step number, severity badge, situation description, top recommendation, and catalog reason; LOW deviations collapsed behind a toggle button.

### Phase 6 — UI/UX Polish and Cross-Match Insights ✅ DONE
**Goal:** Improve ergonomics and aggregate insights across matches.

Key components:
- Keyboard shortcuts in the editor (number keys for quick transitions, `U` to undo last event).
- "Across matches" aggregates: most frequent mistakes, move preferences by opponent, fatigue curves averaged.
- Export a match to a shareable JSON file (same schema as the internal model, minus DB IDs).

**Implemented 2026-04-22.** Keyboard shortcuts in `MatchEditorView`: keys `1`–`9` select the numbered quick-transition chips (chips now display their shortcut number); `U` undoes the last event; a "Shortcuts:" hint line is shown in the input bar. Shortcut registrations are cleared and re-registered each time `buildUi()` is called to avoid duplicates on re-navigation. Export: `MatchExportService` serialises a `Match` to pretty-printed JSON (exportVersion, exportedAt, all match fields minus DB ID, full events array); an **Export JSON** anchor-button using Vaadin 25 `DownloadHandler` API is added to the editor header, triggering a direct browser file download. Cross-match insights: `MatchRepository.findAllWithEvents()` and `MatchService.listAllWithEvents()` added; new records `AggregatedMistake`, `OpponentMoveStats`, `AveragedFatiguePoint`, `CrossMatchAnalysis`; `CrossMatchAnalyzer` (`@Service`) aggregates `MistakeFinding`s across all matches (top 10 by frequency), user move preferences per opponent (top 5 moves per opponent), and average fatigue across 5 equal match-percentage buckets. `MatchReplayListView` converted to a `TabSheet` with **Matches** (existing grid + New Match button) and **Insights** (top mistakes with severity colours, move-preference bar charts grouped by opponent, average fatigue table) tabs.

---

## 2. Data Model (High Level)

Stored in H2 via JPA; shown here as JSON for clarity. Identifiers and audit fields omitted.

### Match

```json
{
  "id": 42,
  "matchDate": "2026-04-15",
  "opponentName": "Alex",
  "selfRole": "A",
  "durationSeconds": 360,
  "resultOutcome": "WIN_SUBMISSION",
  "notes": "Competition semifinal",
  "events": [ /* ordered */ ]
}
```

### MatchEvent (ordered within a Match)

```json
{
  "stepIndex": 0,
  "timestampSeconds": 12,
  "actor": "A",
  "moveKey": "double_leg_takedown",
  "positionAfterKey": "side_control_top_A",
  "fatigueA": 2,
  "fatigueB": 3,
  "note": null
}
```

Rules:
- `stepIndex` is dense and monotonic within a match.
- `moveKey` and `positionAfterKey` are free strings — the app does not enforce foreign keys to the JSON catalog. If the catalog is updated later, old matches still render.
- `actor` is `"A"` or `"B"`; `selfRole` on the match tells the UI which one is the user.

### Derived: TransitionObservation (not persisted; computed on demand)

```json
{
  "fromPositionKey": "closed_guard_bottom_A",
  "moveKey": "scissor_sweep",
  "toPositionKey": "mount_top_A",
  "actor": "A",
  "matchId": 42,
  "stepIndex": 5
}
```

### Derived: MoveFrequency

```json
{
  "actor": "A",
  "moveKey": "scissor_sweep",
  "count": 7,
  "fractionOfActorMoves": 0.18
}
```

### Derived: PatternOccurrence (n-gram of moves by the same actor)

```json
{
  "actor": "A",
  "sequence": ["knee_cut_pass", "side_control_establish", "kimura_attempt"],
  "occurrences": 3,
  "matches": [42, 51, 53]
}
```

---

## 3. External Data Source Design

A single JSON file, reloadable at runtime, with three top-level sections. The file is **data, not schema** — the app reads it defensively and ignores unknown fields.

### File: `moves.json`

```json
{
  "version": 1,
  "positions": [
    { "key": "closed_guard_bottom_A", "label": "Closed guard (A on bottom)" },
    { "key": "mount_top_A", "label": "Mount (A on top)" },
    { "key": "side_control_top_A", "label": "Side control (A on top)" }
  ],
  "moves": [
    {
      "key": "scissor_sweep",
      "label": "Scissor sweep",
      "category": "sweep",
      "tags": ["guard", "beginner"]
    },
    {
      "key": "knee_cut_pass",
      "label": "Knee cut pass",
      "category": "pass",
      "tags": ["passing"]
    },
    {
      "key": "kimura_attempt",
      "label": "Kimura attempt",
      "category": "submission",
      "tags": ["upper_body"]
    }
  ],
  "transitions": [
    {
      "fromPosition": "closed_guard_bottom_A",
      "move": "scissor_sweep",
      "toPosition": "mount_top_A",
      "difficulty": 2
    },
    {
      "fromPosition": "mount_top_A",
      "move": "kimura_attempt",
      "toPosition": "mount_top_A",
      "difficulty": 3
    }
  ],
  "recommendedResponses": [
    {
      "whenOpponentPlays": "knee_cut_pass",
      "fromPosition": "half_guard_bottom_A",
      "responses": [
        { "move": "underhook_recover_guard", "score": 9, "reason": "Highest-percentage recovery against knee cut." },
        { "move": "deep_half_entry",         "score": 7, "reason": "Strong counter if underhook is denied." },
        { "move": "turtle_up",               "score": 4, "reason": "Defensive fallback; concedes top position." }
      ]
    }
  ]
}
```

Design notes:
- `key` fields are stable identifiers; `label` is display text. Renaming a label is safe; changing a key breaks historical matches' display (they'll show the raw key).
- `score` is a 0–10 integer. The recommendation engine uses relative gaps, not absolute values.
- `recommendedResponses` is keyed by (opponent-move, position) so the same move can have different best responses depending on where it happens.
- File lives at a configurable path (default: `config/moves.json` next to the jar). A bundled starter file ships in `src/main/resources/moves-default.json` and is copied out on first run if the configured path is missing.

---

## 4. UI and UX Plan

### Menu entry

A new top-level nav item in `MainLayout`, marked as a special/highlighted feature (e.g. accent color, "new" badge, or a distinct icon such as `VaadinIcon.FILM` or `VaadinIcon.MOVIE`).

Suggested names (pick one during implementation):
- **Match Lab** — implies experimentation and analysis.
- **Replay & Review**
- **Match Studio**
- **Roll Replay** (BJJ vernacular: a "roll" is a sparring/match session).
- **Fight IQ** — emphasizes the analytical angle.

### User flow

1. **Entry point.** User clicks the highlighted menu item → `MatchReplayListView` shows existing matches (date, opponent, result) with a prominent **"New match"** button.
2. **Create match — header step.** Modal or top section: date, opponent name, which side is the user (A/B), optional duration and result. Save creates an empty `Match` and opens the editor.
3. **Editor — step-by-step entry.** Main area is a **timeline** of events. Below it, an **event input bar** with:
   - Actor toggle (A / B), defaults to alternating.
   - **Move picker**: search field (fuzzy match on label and tags), a row of **quick-select chips** showing the legal transitions from the current position per the catalog, and a **recent moves** row (last 5 distinct moves used in this match).
   - Fatigue sliders for A and B, sticky at their last values so the user only moves them when fatigue actually changes.
   - **Add event** button (or Enter key).
   - **Undo last** button (or `U` key).
4. **Timeline view.** Horizontal strip, one tile per event, color-coded by actor. Each tile shows move label, position after, and a small fatigue indicator (two tiny bars, one per player). Clicking a tile selects it for editing or deletion; right-click inserts a new event before it.
5. **Save & analyze.** "Analyze" button takes the user to `MatchAnalysisView`.
6. **Analysis view — four panels:**
   - **Frequencies**: bar chart of top N moves per player.
   - **Patterns**: list of repeated N-grams (e.g. "knee cut → side control → kimura, used 3×") with an option to jump to each occurrence.
   - **Fatigue curve**: line chart of fatigue vs. step for both players.
   - **Feedback**: numbered list of mistakes, each item showing:
     - What happened ("You played X from Y").
     - What the catalog recommended ("Top response: Z, score 9").
     - Short reason from JSON.
     - A "Show in timeline" link.
7. **Cross-match insights** (Phase 6): a separate tab on the list view showing aggregates.

### Input ergonomics — principles

- **Fast by default, detailed on demand.** Fatigue sliders are stickied; position-after auto-fills from the catalog if the chosen move has a single `toPosition` for the current `fromPosition`.
- **Never block.** Out-of-catalog moves are allowed via free text; they just don't get quick-select or recommendation coverage.
- **Keyboard-first.** Number keys 1–9 pick from the current quick-select chips; `A`/`B` swaps actor; `Enter` commits; `U` undoes.

### Feedback presentation

Keep it short, non-judgmental, and actionable:

> **Step 12.** Opponent played *knee cut pass*. You chose *turtle up* (score 4). Top recommendation: *underhook recover guard* (score 9) — highest-percentage recovery against knee cut.

One line per mistake, grouped by severity (score gap ≥ 5 = highlighted; 3–4 = normal; <3 = hidden by default, expandable).

---

## 5. Analysis Logic (No Code)

All analysis functions operate on a single `Match` aggregate (or a set of matches for cross-match insights). They are pure, deterministic, and independent of the UI.

### Repeated pattern detection

- For each actor, extract the ordered list of their moves.
- Slide a window of length n (n = 2 and n = 3) over the list; count occurrences of each window.
- Keep windows with `count ≥ 2`, sort by count descending, then by length descending.
- Optional: also count patterns that span both actors (alternating sequences) — treat as a separate category in the UI.

### Move frequency calculation

- Per actor: `count(move) / totalMovesByActor`.
- Expose both absolute count and fraction. Sort descending; keep top N for display, full list available on expand.

### Likely-reaction estimation

- For a given opponent move in a given position, aggregate historical responses across all the user's matches.
- `P(response | opponent_move, position) ≈ count(response after opponent_move from position) / count(opponent_move from position)`.
- Display as "When your opponent does X from Y, you usually do Z (60%)". Requires a minimum sample size (e.g. 3) before showing, to avoid misleading claims from tiny samples.

### Comparison to recommendations

- For each event where the **previous** event was by the other actor:
  - Look up `recommendedResponses` in the catalog keyed by (prevEvent.moveKey, prevEvent.positionAfterKey).
  - If no entry exists, skip (no opinion).
  - If the user's chosen move matches the top response, mark as **aligned**.
  - Otherwise compute the score gap `topScore − chosenScore`. If the chosen move is not in the response list, treat its score as 0.

### Mistake flagging

- A step is flagged as a **mistake** when:
  - A recommendation exists for its context, AND
  - The user's score gap is above a configurable threshold (default 3), AND
  - The actor is the user (the `selfRole` side) — we don't critique the opponent.
- Severity buckets: `HIGH` (gap ≥ 5), `MEDIUM` (3–4), `LOW` (1–2, hidden by default).
- Mistakes are derived on read; not stored. This means they automatically refresh when the catalog is updated.

---

## 6. System Overview

Four layers, loosely coupled:

```
 ┌──────────────────────────────────────────────────────────────┐
 │  UI (Vaadin views)                                          │
 │  MatchReplayListView · MatchEditorView · MatchPlaybackView  │
 │  MatchAnalysisView                                          │
 └───────────┬──────────────────────────────────────┬───────────┘
             │                                      │
             │ reads/writes matches                 │ requests analyses
             ▼                                      ▼
 ┌──────────────────────────┐         ┌──────────────────────────┐
 │  MatchService            │         │  MatchAnalyzer           │
 │  (JPA CRUD, event append)│         │  RecommendationEngine    │
 └───────────┬──────────────┘         └────────┬─────────────────┘
             │                                  │
             ▼                                  ▼
 ┌──────────────────────────┐         ┌──────────────────────────┐
 │  H2 / JPA                │         │  MoveCatalog (in-memory) │
 │  Match, MatchEvent tables│         │  loaded from moves.json  │
 └──────────────────────────┘         └────────┬─────────────────┘
                                               │
                                               ▼
                                      ┌──────────────────────────┐
                                      │  moves.json              │
                                      │  (editable, reloadable)  │
                                      └──────────────────────────┘
```

Interaction summary:
- **UI ↔ MatchService:** The editor and playback views read and write `Match` / `MatchEvent` rows. The service is the only writer; analyzers are read-only.
- **UI ↔ MoveCatalog:** The input widget asks the catalog for legal transitions from the current position, the move list for search, and labels for display. The UI treats missing entries gracefully.
- **MatchAnalyzer + RecommendationEngine ↔ MoveCatalog:** Analysis reads the catalog only for `recommendedResponses` and for labels in feedback messages. All pattern/frequency math is catalog-independent.
- **MoveCatalog ↔ moves.json:** Loaded at startup and on file change (watch service or polling). The catalog exposes an immutable snapshot; readers always see a consistent view.
- **Separation of concerns:** The DB knows nothing about BJJ semantics. The catalog knows nothing about matches. The analyzer knows both but is pure. The UI orchestrates.

This keeps the BJJ knowledge entirely in one editable file, and lets the app's behavior be improved by editing JSON instead of Java.
