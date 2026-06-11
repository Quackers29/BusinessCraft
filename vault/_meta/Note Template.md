---
tags:
  - detail
  - <area-tag e.g. economy, trade, town, tourists, platforms, production, config>
---
# <Process Name>

**Breadcrumb**: System > Subsystem > Process (e.g. Trade > Town-to-Town Trades > Bidding Process > Bid Calculation)
**TL;DR**: One sentence capturing the rule/behavior. (Greppable: `grep -r "TL;DR:" vault/` summarizes the whole vault.)

## What it does
2–5 sentences in plain language, readable by someone who has never seen the code. What problem does this solve for the player/game?

## How it works (process view)
The rules in words, not code: what triggers it, what the player sees, a worked example with real numbers. This section plus the two above should stand alone as "human documentation" — a reader can stop here.

---
> [!info]- Deep reference
> Everything below is implementation detail for developers and AI agents.

## Key classes & methods
| Class / Method | File | Role |
|---|---|---|
| `ClassName.methodName()` | `common/src/main/java/.../ClassName.java` | what it does |

## Rules & formulas (exact)
Exact behavior as implemented in code. Include units, rounding behavior, defaults, and config values that feed in. Use code snippets where helpful. Write from what the CODE does, not what comments claim.

## Edge cases & behaviors
- What happens at zero / negative / empty / max / tie?
- Any surprising behaviors discovered while reading the code.

## Test coverage
- Test file: `common/src/test/java/.../ClassNameTest.java`
- What is covered, what intentionally isn't.

## Open questions
- Discrepancies between code and comments/docs, quirks pinned by tests, suspected bugs, unclear intent. (Empty section is fine.)

## Related
- [[wikilinks to related notes]]
