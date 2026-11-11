# Ekagra — Project Decision Log

This log records architectural, design, interaction, and implementation decisions made for the Ekagra desktop application.

---

## DEC-001: Chatbot Architecture & Naming — "Rafiq" (Study Companion)
- **Date**: 2026-11-10
- **Status**: Accepted
- **Context**: The project needed an intelligent companion to help students study with focus and maintain digital wellbeing without devolving into generic AI slop.
- **Alternatives Considered**:
  1. *Generic Floating Web Chatbot*: A bouncing popup bubble with freeform generic conversation. (Rejected: High distraction, low taste, classic AI slop).
  2. *Study Mitra / Coach*: Grounded in local data, but Hindi name was requested to be replaced.
  3. *Rafiq (Urdu: رفیق)*: Meaning "Kind Companion" / "Faithful Friend".
  4. *Rahnuma (Urdu: رہنما)*: Meaning "Guide / Path-finder".
  5. *Hamsafar (Urdu: ہمسفر)*: Meaning "Fellow Traveler".
- **Decision**: Name the companion **"Rafiq" (رفیق)**. Build it as an intentional, data-grounded study companion panel.
- **Rationale**:
  - Urdu name conveys empathy and companionship without corporate detachment.
  - Powered by an **offline-first heuristic rule engine** evaluating the student's actual SQLite database (today's score, top distracting apps, upcoming deadlines, sleep/mood).
  - Guarantees 100% reliability during offline academic vivas, presentations, and evaluations without needing external API keys or internet access.
  - Optional extensible LLM API integration if configured in Settings.

---

## DEC-002: Personalized Dynamic Theme Engine & Reset
- **Date**: 2026-11-10
- **Status**: Accepted
- **Context**: Students want personal aesthetic identity in a wellbeing app, moving beyond a basic binary light/dark switch.
- **Alternatives Considered**:
  1. *Hardcoded binary themes*: Only light and dark. (Rejected: insufficient personalization).
  2. *Freeform unconstrained styling*: Letting users edit raw CSS. (Rejected: breaks layout, high risk of unreadable contrast).
  3. *Curated Harmonic Palettes + Adaptive Custom Accent Picker*: 6 curated palettes with mathematical color derivation.
- **Decision**: Implement a dynamic color derivation engine in `ThemeManager` with:
  - 6 handcrafted aesthetic palettes: **Ocean Focus** (default), **Sage Matcha**, **Nordic Frost**, **Warm Terracotta**, **Dusk Amethyst**, and **Obsidian Minimal**.
  - A **Custom Accent ColorPicker** that automatically calculates harmonious shades (`accent-emphasis`, `accent-subtle`, `accent-muted`, `accent-fg`) while strictly maintaining WCAG AA contrast (≥ 4.5:1).
  - A prominent **"Reset to default theme"** action in Settings.
- **Rationale**: Guarantees visual harmony and accessibility while giving the student total aesthetic control over their workspace.

---

## DEC-003: Enter-Driven Interactive Demo Tour
- **Date**: 2026-11-10
- **Status**: Accepted
- **Context**: During academic vivas and presentations, live manual typing and clicking between screens is error-prone and clunky.
- **Alternatives Considered**:
  1. *Pre-recorded video*: Passive, non-interactive, cannot demonstrate real software.
  2. *Manual walkthrough*: Presenter has to remember which tabs to click, what sample data to enter, and navigate manually.
  3. *Keyboard-driven Interactive Tour*: Presenter presses `[Enter]` to advance through every major screen, auto-filling credentials, demonstrating focus timer, task completion, wellbeing rules, analytics, and theme customization.
- **Decision**: Implement `DemoTourManager` with keyboard listening (`[Enter]` / `[Space]` for next step, `[Backspace]` for previous, `[Esc]` to exit) and a floating non-intrusive status pill.
- **Rationale**: Elevates the viva experience into a polished, professional live demo where the student presenter can speak freely while advancing the showcase with a single keystroke.

---

## DEC-004: Ambient Focus Sounds with Play/Pause and Mute
- **Date**: 2026-11-10
- **Status**: Accepted
- **Context**: Students studying in noisy environments benefit from focus sounds during Pomodoro sessions.
- **Decision**: Integrate an ambient sound generator into the Focus Timer screen with:
  - Audio modes: *Gentle Rain*, *Forest Nature*, *Lofi Hum*, *Deep Binaural Focus*, and *White Noise*.
  - Full playback controls: Play/Pause toggle, dedicated Mute/Unmute button, and volume slider.
  - Built using standard Java sound APIs (`javax.sound.sampled` / procedural synthesis) with clean lifecycle management (auto-pauses when session ends or breaks begin).
- **Rationale**: Enhances actual focus utility without installing bulky third-party native binaries.

---

## DEC-005: Tasteful Background Imagery & Texture System
- **Date**: 2026-11-10
- **Status**: Accepted
- **Context**: The app felt flat and clinical in places (e.g. login pane, focus timer ground).
- **Decision**: Introduce subtle, high-taste background imagery and textures:
  - Brand pane on Login/Signup with a serene, minimalist study backdrop under a semi-transparent brand gradient.
  - Focus Timer with an optional serene zen background for flow-state immersion.
  - Follow anti-AI-slop rules from `design-taste-frontend` and `frontend-design`: no cheesy AI stock people, no surreal neon brains; only clean architectural, botanical, and calm study atmospheric textures.
- **Rationale**: Creates an inviting, peaceful digital environment that aligns with the philosophy of *Ekagra*.

---

## DEC-006: Real Photography via Unsplash (Anti-AI-Slop Visual Sourcing)
- **Date**: 2026-11-10
- **Status**: Accepted
- **Context**: The user explicitly requested fetching real photographs from Unsplash instead of using generative AI images.
- **Alternatives Considered**:
  1. *Generative AI images*: Often produce distorted hands, surreal lighting, and generic "AI-slop" looks. (Rejected).
  2. *Flat vector clipart*: Often looks corporate or childish. (Rejected).
  3. *Curated Real Photography from Unsplash*: High-taste, authentic photography of minimalist study environments, calm architecture, and serene botanicals.
- **Decision**: Fetch curated, high-quality photographs from Unsplash, optimize their file sizes, and store them locally under `src/main/resources/com/ekagra/images/` so the app is 100% offline-ready for university viva presentations.
- **Rationale**: Real photography grounds the app in reality, gives an authentic human touch, and completely avoids the AI-slop aesthetic.

---

## DEC-007: Implementation of `AGENTS.md` Grounding & Focus Directive
- **Date**: 2026-11-10
- **Status**: Accepted
- **Context**: Agent operations need strict guardrails to prevent hallucination, scope creep, and distraction.
- **Decision**: Create and adhere to `AGENTS.md` establishing prime directives: verify before asserting, no invented APIs, adherence to `pom.xml` constraints, continuous logging in `DECISIONS.md`, and strict verification before completion.
- **Rationale**: Ensures reliable pair-programming, deterministic execution, and prevents unauthorized or tangential code modifications.

---

## DEC-008: Chatbot Final Naming — "Rahnuma" (رہنما)
- **Date**: 2026-11-10
- **Status**: Accepted
- **Context**: The user reviewed the proposed Urdu names and selected **Rahnuma** (رہنما) as the official name for the study companion.
- **Alternatives Considered**:
  1. *Rafiq* (رفیق) - Kind Companion.
  2. *Hamsafar* (ہمسفر) - Fellow Traveler.
  3. *Rahnuma* (رہنما) - Guide / Path-Finder / Mentor.
- **Decision**: Finalized name as **Rahnuma (رہنما)** across all UI elements, tooltips, dialogue headers, and documentation.
- **Rationale**: In Urdu, "Rahnuma" evokes guidance, wisdom, and keeping a student on the right path—a natural match for a digital wellbeing and focus monitoring system.

---

## DEC-009: Full UI Polish, Offline Architecture & Viva Tour Integration
- **Date**: 2026-11-10
- **Status**: Implemented & Verified
- **Context**: The personalization engine, Rahnuma conversational guide, ambient sound engine, and keyboard-driven demo tour required verified integration and offline self-containment for student vivas.
- **Decision**:
  - Implemented ThemeManager with 6 handcrafted palettes + custom accent picker with automatic contrast derivation, backed by persistent SQLite user settings and instant one-click reset to default.
  - Implemented procedural ambient sound synthesis in AmbientSoundService with Play/Pause/Mute controls and clean lifecycle management.
  - Integrated RahnumaService with heuristic evaluation over real user study habits, distraction records, task deadlines, and sleep/wellbeing check-ins without external cloud APIs.
  - Integrated DemoTourManager enabling seamless step-by-step viva navigation via [Enter].
  - Stored real Unsplash photography locally in src/main/resources/com/ekagra/images/ for full offline presentation support.
- **Rationale**: Meets all student requirements for personalized styling, offline reliability during academic defense, anti-AI-slop visual taste, and zero external dependency risk.
---

## DEC-010: Responsive Dialog Viewport, Sound Synthesis Calibration & OOAD/SOLID Adherence
- **Date**: 2026-11-10
- **Status**: Implemented & Verified
- **Context**: 
  1. Student displays on varying laptop resolutions (768p, 1080p, display scaling 125%/150%) caused fixed-height dialogs to clip off-screen.
  2. Ambient sound synthesis buffer required native DirectSound buffer alignment and audible headroom.
  3. Strict adherence to Object-Oriented Analysis & Design (OOAD) and SOLID principles needed documentation.
- **Decision**:
  - **Responsive Dialog Viewport**: RahnumaDialog dynamically measures Navigator.getStage().getHeight(), scales its max height to 72% of the window (clamped between 380px and 500px), enables vertical priority on scrollPane, and invokes stage.centerOnScreen().
  - **Audio Engine Calibration**: AmbientSoundService now opens SourceDataLine with system default buffer (~88,200 bytes) rather than restricted 4KB buffer to eliminate underrun clipping; tuned procedural audio synthesis waveforms for Rain, Stream, Binaural, and White Noise to audible, soothing amplitude; synchronized play/pause automatically with PomodoroTimer state changes via TimerListener observer.
  - **OOAD & SOLID Principles Applied**:
    - **Single Responsibility Principle (SRP)**: View controllers (FocusController, RahnumaDialog) manage only UI state and presentation; business logic is delegated to service layer (RahnumaService, AmbientSoundService, TrackingService); persistence is isolated to DAOs (NotificationDao, ActivityDao, AppRuleDao).
    - **Open/Closed Principle (OCP)**: Timer events and tracking notifications use extensible listener interfaces (TimerListener, ActivityListener) without modifying the state machine.
    - **Liskov Substitution Principle (LSP)**: Polymorphic role hierarchy (User -> Student, Mentor) and session types (SessionType.FOCUS, SHORT_BREAK, LONG_BREAK).
    - **Interface Segregation Principle (ISP)**: Granular observer interfaces preventing unnecessary method couplings (Disposable, TimerListener, ActivityListener).
    - **Dependency Inversion Principle (DIP)**: Controllers rely on service abstractions and DAO factories rather than tight low-level couplings.
- **Rationale**: Ensures enterprise-grade software engineering rigor, rock-solid presentation reliability during university viva examinations, and zero layout overflow.