---
tags:
  - overview
  - core
---
# Core Overview

**TL;DR**: Small set of cross-cutting pure utilities (time formatting, result monad, etc.) used by economy, trade, town, and UI layers to keep string output, error handling, and other helpers consistent without duplicating logic.

## Processes in this area
- **[[Core/Time/Time Display Formatting|Time Display Formatting]]** (T-017) — single source of truth for turning epoch millis into the duration strings ("2d 5h", "Expired"), "X ago" labels, and zoned date/time displays shown in every contract list, payment history row, and detail view. Integer-breakdown rules, configurable timezone (defaults UTC), and the isExpired / raw-remaining helpers all live here.
- **[[Core/Util/Result Type|Result Type]]** (T-018) — a uniform carrier that lets any part of the game say "this worked and here is the answer" or "this failed for this exact reason" without throwing exceptions or returning null. Callers are forced to handle both outcomes; supports chaining changes to the answer or the error. Used for configuration problems, placement validation, and many internal checks so errors stay visible in the type system.

## How it connects
Contract viewmodels and RewardEntry (payment board) call the formatting methods with server-captured "now" values so countdowns and history labels are consistent. ConfigLoader wires the `displayTimezone` TOML value into BCTimeUtils at load/hot-reload time. Because the utilities are pure (or static with save/restore in tests), they are fully unit-testable without Minecraft bootstrap.
