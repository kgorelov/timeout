# Time Until Next Event

## Wear OS Watch Face MVP

### Version

0.1 (MVP)

### Status

Design Specification and Execution Plan

---

# 1. Product Vision

Most watch faces display the current time. Users must mentally calculate how much time remains before their next commitment.

The goal of this application is to shift the user's focus from:

> "What time is it?"

to

> "How much time do I have left before the next important event?"

The watch face continuously displays a countdown to the next relevant event in the user's schedule, helping maintain discipline, routine, and time awareness with a single glance.

---

# 2. Problem Statement

People often lose track of available time between scheduled activities.

Examples:

* Leaving home for work
* Important meetings
* School pickup
* End of workday
* Bedtime
* Recurring daily routines

While calendars contain this information, they require multiple interactions to access.

The watch face should surface only the most relevant information:

* What is the next event?
* How much time remains?

---

# 3. MVP Goals

The MVP should:

* Display countdown to the next upcoming event.
* Work as a native Wear OS watch face.
* Read events from selected calendars.
* Support recurring routine events configured by the user.
* Update automatically.
* Require minimal interaction.

The MVP should NOT include:

* AI scheduling
* Task management
* Productivity analytics
* Complex notifications
* Calendar editing

---

# 4. User Stories

## User Story 1

As a user,

I want to see how much time remains until my next event,

so that I can manage my current activity without opening my calendar.

---

## User Story 2

As a user,

I want recurring daily routines (sleep, work start, work end),

so that the watch remains useful even when my calendar is empty.

---

## User Story 3

As a user,

I want the watch face to automatically select the most relevant upcoming event,

so that I never need to manually choose what is displayed.

---

# 5. Core Concepts

## Event

Represents any future point in time.

Attributes:

```text
id
title
startTime
endTime
source
isRoutine
priority
```

Sources:

* Google Calendar
* Outlook Calendar (via Android calendar provider)
* User-defined routines

---

## Routine Event

Recurring event defined by the user.

Examples:

* Wake up
* Leave home
* Start work
* Lunch
* End work
* Sleep

Configuration:

```text
Title
Time
Days of week
Enabled
```

---

# 6. Event Selection Algorithm

At any given moment:

1. Collect all future calendar events.
2. Collect all active routine events.
3. Remove events that already started.
4. Sort by start time.
5. Select the nearest upcoming event.

Pseudo-code:

```text
events = getCalendarEvents() + getRoutineEvents()

futureEvents =
    events.filter(event.startTime > now)

nextEvent =
    futureEvents.minBy(event.startTime)
```

---

# 7. Watch Face UI

## Main Screen

Large central countdown:

```text
01:42
```

Meaning:

```text
1 hour 42 minutes remaining
```

Secondary text:

```text
Until:
Leave Home
08:30
```

Example:

```text
00:17

Until:
Team Meeting
10:00
```

---

# 8. Countdown Display Rules

## More than 1 hour

```text
2h 34m
```

## Less than 1 hour

```text
34m
```

## Less than 10 minutes

```text
9m
```

## Less than 1 minute

```text
<1m
```

---

# 9. Progress Visualization

Optional but recommended.

Display progress ring showing elapsed time between:

Previous Event -> Next Event

Example:

```text
Leave Home ---- NOW ---- Meeting
```

Progress ring percentage:

```text
(now - previousEventTime)
/
(nextEventTime - previousEventTime)
```

---

# 10. Color States

Normal:

```text
Remaining > 60 min
```

Warning:

```text
Remaining <= 60 min
```

Urgent:

```text
Remaining <= 15 min
```

Critical:

```text
Remaining <= 5 min
```

Colors should remain subtle and battery-friendly.

---

# 11. Always-On Display

Requirements:

* Show countdown only.
* Remove animations.
* Update once per minute.
* Reduce rendering complexity.

Example:

```text
42m
Meeting
```

---

# 12. Settings Application

A companion Android app is required.

Functions:

### Calendar Selection

User can choose:

* Personal calendar
* Work calendar
* Family calendar

### Routine Management

Create/edit/delete routine events.

### Display Settings

* 12h / 24h
* Show progress ring
* Show event title
* Color alerts

---

# 13. Architecture

## Components

### Watch Face Module

Responsibilities:

* Rendering
* Countdown calculations
* Display updates

### Companion App

Responsibilities:

* Settings
* Calendar access
* Synchronization

### Shared Data Layer

Use:

```text
Jetpack DataStore
```

or

```text
Room
```

for local persistence.

---

# 14. Technology Stack

## Language

Kotlin

## UI

Jetpack Compose for Wear OS

## Watch Face

AndroidX Watch Face APIs

## Persistence

DataStore

## Synchronization

Wearable Data Layer API

## Minimum SDK

Wear OS 4+

---

# 15. Data Model

## Event

```kotlin
data class Event(
    val id: String,
    val title: String,
    val startTime: Instant,
    val endTime: Instant?,
    val source: EventSource,
    val isRoutine: Boolean
)
```

## Routine

```kotlin
data class Routine(
    val id: String,
    val title: String,
    val time: LocalTime,
    val daysOfWeek: Set<DayOfWeek>,
    val enabled: Boolean
)
```

---

# 16. MVP Milestones

## Milestone 1

Project Setup

Deliverables:

* Android project
* Wear module
* Companion app module

Estimated effort:

1 day

---

## Milestone 2

Watch Face Prototype

Deliverables:

* Static watch face
* Countdown rendering

Estimated effort:

2 days

---

## Milestone 3

Calendar Integration

Deliverables:

* Read Android calendar events
* Select next event

Estimated effort:

2–3 days

---

## Milestone 4

Routine Events

Deliverables:

* Routine storage
* Routine editor

Estimated effort:

2 days

---

## Milestone 5

Phone ↔ Watch Sync

Deliverables:

* Data Layer synchronization

Estimated effort:

2 days

---

## Milestone 6

Settings Screen

Deliverables:

* Calendar selection
* Routine management
* UI preferences

Estimated effort:

2–3 days

---

## Milestone 7

Polish & Testing

Deliverables:

* Battery optimization
* AOD support
* Edge-case handling

Estimated effort:

2 days

---

# 17. Success Criteria

The MVP is considered successful if:

* Countdown updates correctly.
* The next event is selected correctly.
* Calendar events sync reliably.
* Routine events work offline.
* Battery impact remains minimal.
* User can understand remaining time with a single glance.

---

# 18. Future Enhancements

Potential v2 features:

* Multiple upcoming events
* Timeline view
* Smart commute calculations
* Todoist integration
* Google Tasks integration
* Sleep API integration
* Focus sessions
* Statistics and habit tracking
* Complications support
* Custom watch face themes

---

# 19. Non-Goals

The application is NOT intended to become:

* A calendar replacement
* A task manager
* A habit tracker
* A scheduling assistant

Its primary purpose is:

> Display the remaining time until the next meaningful event with maximum clarity and minimum interaction.
