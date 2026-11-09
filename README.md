# Ekagra

**Student Productivity & Digital Wellbeing Monitoring System**
*Ekagra means "one-pointed focus".*

A Windows desktop app that helps students study with focus and keep healthy screen habits. It tracks which apps and websites you use, runs Pomodoro focus sessions, warns you when you get distracted, manages tasks, goals and habits, logs mood and sleep, and turns all of it into a daily Productivity Score, charts and insights. Mentors can follow their students and spot who needs help.

Built for **CT-263 Object-Oriented Analysis & Design** with Java 21, JavaFX, SQLite and Maven.

**Viva preparation guide** (design patterns, OOP, architecture, algorithms, model answers): <https://claude.ai/artifact/4iURjJ6QUhFwVgk4AKsLJT>

---

## Contents
1. [Quick start (no Java needed)](#1-quick-start-no-java-needed)
2. [Set up from scratch (run from source code)](#2-set-up-from-scratch-run-from-source-code)
3. [Demo accounts](#3-demo-accounts)
4. [Features](#4-features)
5. [How the project is organised](#5-how-the-project-is-organised)
6. [Running the tests](#6-running-the-tests)
7. [Building the standalone Ekagra.exe](#7-building-the-standalone-ekagraexe)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Quick start (no Java needed)

If someone gives you the **`Ekagra` folder** (built in step 7), you don't need to install anything:

1. Copy the whole `Ekagra` folder anywhere (e.g. Desktop). Keep the files inside together.
2. Double-click **`Ekagra.exe`**.
3. On the login screen click **Explore as a student** to load sample data, or create your own account.

The folder contains its own Java runtime. Your data is saved in `Ekagra\data\ekagra.db`.

> Windows may show "Windows protected your PC" the first time because the app isn't signed. Click **More info → Run anyway**.

---

## 2. Set up from scratch (run from source code)

Do this once on a new PC. Everything is free.

### Step 1: Install Java 21 (JDK)

**Option A (easiest):** open **PowerShell** and run:
```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

**Option B:** download the **JDK 21 (LTS) for Windows x64, .msi** from <https://adoptium.net>, run it, and tick **"Set JAVA_HOME variable"** during setup.

Close and reopen PowerShell, then check:
```powershell
java -version
```
It should say `version "21...`.

### Step 2: Install IntelliJ IDEA (recommended editor)

```powershell
winget install JetBrains.IntelliJIDEA.Community
```
or download it from <https://www.jetbrains.com/idea/download/>. The free version is enough, and it already includes **Maven** (the tool that downloads the libraries and builds the app).

### Step 3: Get the project

Copy the `ooad-project` folder to your PC (or clone it if it's on GitHub).

### Step 4: Open it in IntelliJ

1. Start IntelliJ → **Open** → select the `ooad-project` folder (the one containing `pom.xml`).
2. If asked, click **Trust Project**.
3. Wait while IntelliJ downloads the libraries (the progress bar at the bottom; about 1–3 minutes the first time, internet needed).
4. If IntelliJ asks for a JDK: **File → Project Structure → SDK → choose `temurin-21`**.

### Step 5: Run it

- Open the **Maven** panel (the "m" icon on the right edge) → **ekagra → Plugins → javafx → double-click `javafx:run`**.

The login window opens. The database `data\ekagra.db` is created automatically on the first run.

### Running from a terminal instead (optional)

You need Maven on your PATH. Download the "Binary zip archive" from <https://maven.apache.org/download.cgi>, unzip it (e.g. to `C:\tools\apache-maven`), and add its `bin` folder to PATH. Then in the project folder:
```powershell
mvn javafx:run      # run the app
mvn test            # run the unit tests
mvn package         # build target\ekagra-1.0.0.jar
```

---

## 3. Demo accounts

The login screen has **Explore as a student** and **Explore as a mentor**. Each click (re)creates these accounts with **two weeks of realistic sample data counted back from today**, then logs you in. Your own accounts aren't touched.

| Role | Name | Email | Password |
|---|---|---|---|
| Student (main demo) | Ayesha Khan | `demo@ekagra.app` | `demo1234` |
| Student (very focused) | Hira Siddiqui | `hira.demo@ekagra.app` | `demo1234` |
| Student (at risk) | Bilal Raza | `bilal.demo@ekagra.app` | `demo1234` |
| Mentor | Dr. Sara Ahmed | `mentor.demo@ekagra.app` | `demo1234` |

You can also click **Create an account** and sign up as a Student or a Mentor.

---

## 4. Features

There are two kinds of users:
- **Students** are the people being helped.
- **Mentors** are teachers or advisors who follow linked students.

A student links to a mentor by entering the mentor's **invite code** (e.g. `EK-6NTTC`) at sign-up or on their Profile.

### Account & profile
| Feature | What it does |
|---|---|
| Sign up / Log in | Create a Student or Mentor account. Passwords are stored securely (hashed with PBKDF2 + salt), never as plain text. |
| Profile | Edit name, university, semester, daily study target and avatar colour; link or unlink a mentor; change password. |

### Dashboard (student home screen)
| Feature | What it does |
|---|---|
| Greeting & next step | Tells you how much focus time is left to reach today's target, with quick **New task** and **Start focus** buttons. |
| Productivity Score | A 0–100 score for today with its main ingredients and the change since yesterday. |
| Tiles | Focus time vs target, screen time (and how much was distracting), tasks done today, focus streak (days in a row with a focus session). |
| Weekly chart, insights, up next, goals | Score for the last 7 days, the top 3 insights, the next tasks by deadline (▶ starts focusing on one), and today's goals. |

### Analytics
| Feature | What it does |
|---|---|
| Period | View the last 7, 14 or 30 days. |
| Scoring mode | Choose how the score is calculated: **Balanced**, **Study-focused** (exam weeks) or **Wellbeing-focused** (recovery weeks). All charts update instantly. |
| How today's score is made | Each ingredient with its weight and the points it contributes. |
| Charts | Score trend, screen time by category per day, focus minutes per day, where the time went, top apps. Hover any bar or point for details. |
| Insights | Plain-language observations, e.g. "You're most productive between 9 AM and 11 AM", "Distracting apps are up 40% compared with last week", "YouTube was your biggest distraction". |

### Tasks
| Feature | What it does |
|---|---|
| Tasks | Add, edit and delete assignments with subject, priority (Low → Urgent), status, deadline, estimated time and notes. |
| Subtasks | A checklist inside each task, with a progress bar. |
| Filters & search | Filter by status, priority or subject; sort by deadline, priority, newest or title; search by text. |
| Deadline alerts | A banner and red/amber labels for overdue tasks and tasks due within 24 hours, plus a reminder notification. |
| Week view | A Monday–Sunday calendar with tasks placed on their deadline day. |
| Focus on a task | The ▶ button starts a focus session linked to that task; the time spent is added to the task. |

### Focus Timer (Pomodoro)
| Feature | What it does |
|---|---|
| Timer | Focus, short break and long break with a circular countdown. Pause, resume, stop, skip. Breaks start automatically; every 4th break is a long one. |
| Timer lengths | Adjustable, with presets: Classic 25/5, Deep 50/10 and **Demo 1/1** (1-minute sessions for presentations). |
| Keeps running | The timer continues when you switch screens; a small countdown appears in the top bar. |
| Session rating | When a focus session ends you get a beep and rate how focused you felt (1–5 stars). |
| History | Today's focus time, sessions and distractions, plus a list of recent sessions. |
| **Focus Guard** | If you open a distracting app or website (YouTube, Instagram, Discord…) during a focus session, a warning pops up **on top of whatever you're using** and a distraction is counted. **Back to focus** returns you to Ekagra; **It's for study** removes the distraction and stops warning about that app for the session. |

### Activity Monitor
| Feature | What it does |
|---|---|
| Automatic tracking | Every 2 seconds Ekagra notes which app (and window title) you're using. Each app or website is classified as **Productive**, **Neutral** or **Distracting**. |
| Idle detection | After 5 minutes without keyboard or mouse input you're marked "Away" and that time isn't counted. |
| Right now | Live card showing the current app, its category and how long you've been in it. |
| Today | Screen time, % productive, distracting time, a balance bar, an hour-by-hour chart and top apps. |
| Privacy | The top bar always shows Tracking / Paused / Away. **Pause tracking** stops recording. Only app names and window titles are stored, never keystrokes or screenshots, and everything stays on your computer. |

### Wellbeing
| Feature | What it does |
|---|---|
| Wellbeing Score | 0–100 from sleep, mood, screen time and distraction balance. Parts you haven't logged are simply left out. |
| Mood check-in | Once a day: how you feel (Awful → Great), stress level (Calm → Overwhelmed) and an optional note. |
| Screen-time limits | A daily limit with a warning at 80% and an alert at 100%, plus per-app limits (e.g. Instagram 30 min a day). Works for websites inside any browser. |
| Break reminders | A reminder after N minutes of continuous screen use (default 50; never interrupts a focus session) and 20-20-20 eye breaks every 20 minutes. Both can be switched off. |
| Sleep log | Bedtime and wake-up time, with a chart of the last 7 nights. |
| Trends | Mood and stress chart for the last 7 days. |

### Goals & Habits
| Feature | What it does |
|---|---|
| Goals | Daily or weekly targets measured automatically: focus time, focus sessions, tasks completed, or **limits** such as "distracting apps under 1h a day". Shows progress and Achieved / Under limit / Over limit. |
| Habits | Daily habits (e.g. "Drink 8 glasses of water") you tick for any of the last 7 days, with current and best streaks. |

### Achievements
| Feature | What it does |
|---|---|
| XP & levels | Earn XP for focus time (+10 per 5 min), finishing tasks (+30, +20 if before the deadline), habits (+10) and the daily mood check-in (+10). Every 500 XP is a new level, shown under your name. |
| Badges | 11 badges such as First Focus, 7-Day Streak, Deadline Crusher, Digital Detox, Early Bird and Night Owl. Locked badges show your progress towards them. |

### Notifications
| Feature | What it does |
|---|---|
| Notification centre | All reminders, alerts, badges and mentor messages in one list, with All / Unread filters, Mark all as read and Clear all. |
| Bell badge | The 🔔 in the top bar shows how many are unread. |
| Desktop pop-ups | Small pop-ups in the bottom-right corner, visible even over other apps. Can be turned off in Settings. |

### Reports
| Feature | What it does |
|---|---|
| Weekly report (PDF) | A formatted report for this or last week: summary, daily table, focus per day, where the time went, top apps, goals, wellbeing and insights. Saved to `Documents\Ekagra Reports` by default. |
| CSV exports | Your tasks, focus sessions and activity log as spreadsheets for Excel or Google Sheets. |

### Mentor features
| Feature | What it does |
|---|---|
| My Students | Your invite code, plus every linked student's average score, focus time, overdue tasks and last activity. |
| At-risk alerts | A student is flagged if their average score is under 40, they have 2+ overdue tasks, or they've been inactive for 3+ days. Flagged students are listed first, with the reasons. |
| Student details | Score and focus charts, overdue tasks, most-used apps, and a downloadable weekly PDF report. Mood notes, sleep times and window titles stay private. |
| Send a note | Message a student; it appears in their notifications. |
| App Rules | Decide which apps/websites count as Productive, Neutral or Distracting (by app name, words in the window title, or both). **Test a window** shows how something would be classified. Only mentors can change rules. |

### Settings
Theme (light/dark), scoring mode, Focus Guard on/off, start tracking at login, idle time, wellbeing reminders, eye breaks, desktop pop-ups, profile, log out, and delete your activity history. Changes are saved instantly.

---

## 5. How the project is organised

```
ooad-project/
├── pom.xml                      Maven build file (libraries + how to build/run)
├── packaging/                   scripts to build Ekagra.exe (+ the app icon)
├── data/ekagra.db               the database (created on first run)
└── src/
    ├── main/java/com/ekagra/
    │   ├── app/         start-up, navigation between screens, theme
    │   ├── model/       the domain classes: User, Student, Mentor, Task, FocusSession, Goal, Habit…
    │   ├── dao/         database access (one DAO per table group)
    │   ├── db/          database connection, table creation, demo data
    │   ├── service/     business logic (tasks, focus, tracking, wellbeing, analytics, reports…)
    │   ├── tracking/    reads the active window from Windows, classifies apps
    │   ├── pattern/     design patterns: state/, strategy/, observer/, factory/, builder/
    │   ├── export/      PDF and CSV writers
    │   ├── controller/  one controller per screen
    │   └── ui/          reusable UI pieces (dialogs, charts, timer ring, pop-ups, animations)
    ├── main/resources/com/ekagra/
    │   ├── view/        the screens (FXML files)
    │   └── css/app.css  all styling (colours, light/dark theme)
    └── test/java/       62 automated tests (JUnit 5)
```

**Architecture:** MVC with layers: **Controller (UI) → Service (rules) → DAO (database)**.

**Design patterns used:** Singleton, Factory, Observer, Strategy, State, Builder, DAO, MVC.

**Libraries:**
- JavaFX 21 with AtlantaFX (UI and themes)
- Ikonli Feather (icons)
- SQLite JDBC (database)
- JNA (reads the active window from Windows)
- Apache PDFBox (PDF reports)
- JUnit 5 (tests)

---

## 6. Running the tests

- **IntelliJ:** right-click `src/test/java` → **Run 'All Tests'**, or use Maven panel → Lifecycle → **test**.
- **Terminal:** `mvn test`

The tests use a temporary in-memory database and a fake "Windows" for activity tracking, so they never touch your real data.

---

## 7. Building the standalone Ekagra.exe

This creates the no-install version from step 1. It needs JDK 21 on the PC that builds it.

```powershell
powershell -ExecutionPolicy Bypass -File packaging\build-exe.ps1
```

The result is `dist\Ekagra\` (about 180 MB, includes Java). Zip that **whole folder** to share it.

If Maven isn't on your PATH, first build the jar in IntelliJ (Maven panel → Lifecycle → **package**), then run the script.

---

## 8. Troubleshooting

| Problem | Fix |
|---|---|
| `java` is not recognized | Java isn't installed or PowerShell was open during the install. Install JDK 21 (step 1) and open a new PowerShell window. |
| IntelliJ shows red errors everywhere | Libraries are still downloading, or the SDK isn't set. Wait, then **File → Project Structure → SDK → temurin-21**, then Maven panel → ⟳ Reload. |
| "JavaFX runtime components are missing" | Run with **javafx:run** from the Maven panel, not the green ▶ next to `MainApp`. (Or run `com.ekagra.app.Launcher`.) |
| Activity Monitor says "Unavailable" | Activity tracking only works on Windows. Everything else works anywhere. |
| No Focus Guard pop-up | Make sure tracking isn't paused (top bar shows "● Tracking"), a **focus** session (not a break) is running, and Focus Guard is on in Settings. |
| Charts are empty | New accounts start empty. Use the app for a while, or click **Explore as a student** on the login screen. |
| First PDF takes ~15 seconds | Normal: the PDF library builds a font cache once. Later reports take about a second. |
| Start completely fresh | Close the app and delete `data\ekagra.db` (or `Ekagra\data\ekagra.db` for the exe). It's recreated empty on the next start. |
