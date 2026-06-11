# Test + Docs Loop Protocol

**Purpose**: Incrementally build unit test coverage AND an Obsidian documentation vault for BusinessCraft, one small unit per iteration. Designed to be executed by any AI agent (including simpler/cheaper models) with no prior knowledge of the project.

**One iteration = one ledger item taken from TODO to DONE (or BLOCKED).** Do not batch. If the user asks for N iterations, complete them one at a time, fully finishing each before starting the next.

---

## Hard Rules — read before every iteration

1. **NEVER modify production code** (`src/main/**`). No refactors, no "small fixes", no visibility changes. If code cannot be tested as-is, mark the ledger item `BLOCKED` with a one-line reason and move to the next item.
2. **Only write files in these locations**:
   - Tests: `common/src/test/java/com/quackers29/businesscraft/...` (mirror the package of the class under test)
   - Docs: `vault/**`
   - Tracking: `vault/_meta/Coverage Ledger.md` and `vault/_meta/Loop Log.md`
3. **Only run these commands**:
   - `wsl ./gradlew :common:test` (all tests)
   - `wsl ./gradlew :common:test --tests "com.quackers29.businesscraft.SomeClassTest"` (filtered)
4. **Prefer pure-logic tests; use the bootstrap fixture only when needed.** Simple data classes like `BlockPos` or `CompoundTag` are safe with no setup (confirmed in T-001).
   - If the target needs `ItemStack`, `Items.*`, or other registry-backed objects: call `McBootstrap.init()` in a `@BeforeAll` (fixture: `common/src/test/java/com/quackers29/businesscraft/testutil/McBootstrap.java`; working example: `McBootstrapValidationTest.java`). Do NOT add the fixture to tests that don't need it.
   - Targets that need a live `Level`/world, real entities, networking, or a running server are still untestable — mark `NEEDS-MC`.
   - **Private pure methods MAY be tested via reflection** (`getDeclaredMethod` + `setAccessible(true)`) when the logic is important — see `VisitorProcessingHelperTest.java` for the working pattern. Note "tested via reflection" in the ledger row, and suggest extracting the method as a future refactor in the vault note's Open questions.
   - **Mutable public static config fields** (e.g. `ConfigLoader.metersPerEmerald`) may be set in `@BeforeEach` for determinism — ALWAYS save and restore the original value in `@AfterEach`.
5. **If a test reveals what looks like a real bug in production code**: do NOT fix the production code. Write the test, annotate it `@Disabled("BUG: <description> — see ledger")`, set ledger status to `BUG-FOUND` with details, and mention it clearly in your final summary to the user.
   - **Quirks are NOT bugs.** If you find suspicious-but-harmless behavior (e.g. an edge case that is unreachable through normal play, or behavior that is odd but plausibly intentional): do NOT escalate to `BUG-FOUND`. Instead write a **pinning test** that asserts CURRENT behavior, with a comment explaining the quirk and that the test pins it, and record the quirk under "Open questions" in the vault note. Reference example: `calculatePayment_zeroCountWithDistance_currentBehaviorPaysOne` in `VisitorProcessingHelperTest.java`. When unsure whether something is a quirk or a bug, treat it as a quirk and flag it in your final summary for human review.
6. **All committed-to tests must pass.** Never leave the build red. If you can't get green, revert your test file and mark the item `BLOCKED`.
7. Do not git commit unless the user explicitly asked for commits.
8. **Never run concurrent iterations.** The Coverage Ledger is the lock — one agent, one iteration at a time. If you find a stale `IN-PROGRESS` row from a previous session, reset it to `TODO` before picking your target.

---

## Iteration Steps

### Step 1 — Pick a target
- Open `vault/_meta/Coverage Ledger.md`.
- Pick the FIRST item with status `TODO` (top to bottom — order is priority).
- If no `TODO` items remain: find a new gap. Scan these packages (in priority order) for public methods containing calculations, validation, sorting, or data transformation that have no corresponding test class:
  1. `common/src/main/java/com/quackers29/businesscraft/economy/`
  2. `.../contract/`
  3. `.../town/components/` and `.../town/data/`
  4. `.../production/`
  5. `.../platform/` (the Platform class, not loader abstraction)
  6. `.../util/`
- Add the new gap as a ledger row, then proceed with it.
- Set the item's status to `IN-PROGRESS` and add today's date — get the REAL date by running `wsl date +%F`; never guess or increment dates.
- If the row is a RE-OPENED one (it already has a test file and vault note): EXTEND the existing files, don't rewrite them. Read them first.

### Step 2 — Understand the code
- Read the target class fully.
- Grep for usages of the target method(s) to understand how callers use it and what inputs are realistic.
- Identify: the formula/rules, the inputs, the outputs, the edge cases (zero, negative, empty collections, nulls, max values, ties).
- If at this point you discover the target is not pure logic (Rule 4), set status `NEEDS-MC` or `BLOCKED` with a reason, and return to Step 1 for the next item.

### Step 3 — Write the vault note (docs BEFORE tests)
- Create/update the note at the vault path given in the ledger (create folders as needed).
- Use the template in `vault/_meta/Note Template.md` EXACTLY — it has two layers:
  - **Human layer (top)**: frontmatter tags (`detail` + area tag), TL;DR one-liner, "What it does", "How it works (process view)" — plain language, worked example with real numbers, readable by a non-programmer. Reference example: `vault/Economy/Tourist Payments/Distance Payment Calculation.md`.
  - **Deep reference (below the `> [!info]- Deep reference` callout)**: key classes table, exact formulas, edge cases, test coverage, open questions.
- Write the formulas from what the CODE does, not what comments or docs claim. If they disagree, note the discrepancy under Open questions.
- Link related notes with `[[wikilinks]]` where they exist.
- **Maintain the navigation layer**: add/update a one-line entry for this note in the area's Overview note (e.g. `vault/Economy/Economy Overview.md` — TL;DR-style summary + wikilink). If the area has no Overview note yet, create one (frontmatter tag `overview`, copy the structure of Economy Overview) and add the area to the list in `vault/Mod Overview.md`.
- **Overview entries must be plain language**: readable by a non-programmer, NO method names, class names, or code symbols. One or two sentences about what the process does for the player/game, then the wikilink. The technical detail belongs ONLY in the detail note. Bad: "`addResource` with `Math.addExact` overflow → `Long.MAX_VALUE`". Good: "Towns store resources with safe limits so counts can never overflow or go negative."
- **Match the existing folder taxonomy**: place notes under the areas listed in `vault/Home.md`. Don't invent new top-level areas if an existing one fits.

### Step 4 — Write the test
- File: `common/src/test/java/<mirrored package>/<ClassName>Test.java`.
- Use JUnit 5 (`org.junit.jupiter.api`). One test class per production class. Method names: `methodName_condition_expectedResult`.
- Minimum coverage per target: happy path with realistic values, plus every edge case listed in the vault note.
- Every formula documented in Step 3 must have at least one test asserting it with hand-computed expected values (show the arithmetic in a comment).

### Step 5 — Run and verify
- Run: `wsl ./gradlew :common:test --tests "<your test class>"` — must pass.
- Run: `wsl ./gradlew :common:test` — full suite must pass.
- Add the test file link to the vault note's Test coverage section.

### Step 6 — Update tracking
- Ledger row: status `DONE`, fill Test file, Vault note, Date columns. Keep the Notes column to ONE short clause — details belong in the vault note and Loop Log, not the ledger table.
- Append ONE line to `vault/_meta/Loop Log.md`: `YYYY-MM-DD | <ledger ID> | <status> | <one-line summary>`.

### Step 7 — Report and stop
- Tell the user: what was covered, number of tests added, anything surprising (bugs, discrepancies, blocked items).
- STOP. Do not start the next item unless the user asked for multiple iterations.

---

## Status Definitions

| Status | Meaning |
|--------|---------|
| `TODO` | Identified gap, not started |
| `IN-PROGRESS` | Currently being worked (should never persist between sessions — if found stale, reset to TODO) |
| `DONE` | Vault note written + tests passing |
| `DOC-ONLY` | Vault note written; logic not unit-testable without MC bootstrap, documented instead |
| `NEEDS-MC` | Testable only with Minecraft bootstrap/GameTest — revisit if test infra improves |
| `BLOCKED` | Cannot proceed (reason required in Notes column) |
| `BUG-FOUND` | Test revealed probable production bug — needs human/senior-agent review |

## Notes for the supervising human/agent
- The one-time setup (JUnit in `common/build.gradle`, smoke test, vault skeleton) is tracked in `tasks/todo.md` under the Test + Docs Loop track and must be complete before the first loop iteration.
- The vault lives at `vault/` in the repo root. Open that folder as an Obsidian vault. If relocated, update paths in this file and the ledger.
- A capable agent should run the first 1–2 iterations to validate this protocol, then refine it before handing to cheaper models.
- Good iteration size: one class or one cohesive group of methods. If a class is huge (e.g. `Town.java`, 1,215 lines), split it across multiple ledger rows by concern (population math vs. resource storage vs. visit history).
