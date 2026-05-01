# Implementation Plan: Multiple Alarms & Room Database Migration

## Overview
This document outlines the architectural changes made to support multiple alarms using Room Database, replacing the single-alarm SharedPreferences approach.

## Components

### 1. Data Layer (Room)
- **Entity**: `Alarm` (stores id, hours, minutes, repeat days, armed state, etc.)
- **DAO**: `AlarmDao` (CRUD operations)
- **Database**: `AppDatabase` (RoomDatabase, migration handling if needed)
- **Module**: `DatabaseModule` (Hilt provider for Database and Dao)
- **Repository**: `AlarmRepository` (Single source of truth for alarm data)

### 2. Logic Layer
- **AlarmScheduler**: 
    - Updated to accept `Alarm` objects.
    - Uses `alarm.id` for unique `PendingIntent` request codes.
    - `isCurrentTimeInWindow` exposed for checking active alarms.
- **LockOverlayService**:
    - Injected `AlarmRepository`.
    - `ACTION_STOP_LOCK`: Now checks for *other* active alarms before unlocking. If another alarm covers the current time, the lock persists.

### 3. UI Layer
- **Components**:
    - `HomeScreen`: Displays list of alarms from `AlarmRepository`. Toggle updates DB and Scheduler.
    - `LockdownScheduleScreen`: loads/saves alarms via `AlarmRepository`. Handles Insert (new) vs Update (existing).
- **Navigation**:
    - Pass `alarmId` (Int?) when navigating to edit. `null` = Create New.

### 4. Receivers
- **BootReceiver**:
    - Injected `AlarmRepository`.
    - Iterates all active alarms and reschedules them on boot.
- **LockAlarmReceiver**:
    - Forwards `ALARM_ID` and `TYPE` (START/STOP) to `LockOverlayService` (Conceptually, though currently Service handles generic START/STOP. The ID helps `AlarmScheduler` manage unique intents).

## Status
- [x] Database & Dao implementation
- [x] Repository implementation
- [x] Scheduler updates
- [x] UI Integration (Home, Schedule)
- [x] Service logic for overlapping alarms
- [x] Boot persistence
- [x] Compilation fixes

## Next Steps
- Implement "Delete" functionality in UI (Swipe to delete or Long press).
- Add stricter validations for overlapping times during creation (optional, current logic allows overlaps which just merge locks).
- Verify distinct ringtones per alarm (supported in DB/Scheduler, need to ensure Service plays correct one if multiple trigger? Currently Service plays generic or last config. Might need enhancement).
