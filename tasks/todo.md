# Refinement Phase - Full Pass (Human in the Loop)

## Goal
- [ ] Complete refinement across `common`, `forge`, and `fabric` using checklist-driven review and implementation.
- [ ] Prioritize obvious, isolated, pre-functionality files first; defer architecture-heavy areas (for example networking/messaging) until later passes.
- [ ] Keep decisions human-reviewed at each grouping checkpoint before moving on.

## Execution Order
- [ ] Phase R1: `tasks/refinement_common.md` (primary focus first)
- [ ] Phase R2: `tasks/refinement_forge.md`
- [ ] Phase R3: `tasks/refinement_fabric.md`
- [ ] Phase R4: Cross-platform parity pass and cleanup

## Phase R1 - Common First Strategy
- [ ] Segment `common` checklist into two buckets:
- [ ] Bucket A: Isolated/pre-functionality files (constants, config, utilities, small helpers, simple UI atoms, basic data holders).
- [ ] Bucket B: Architecture/core-flow files (network packets/messages, sync flows, managers, lifecycle orchestration).
- [ ] Start with Bucket A only and process top-to-bottom.
- [ ] For each completed isolated file, mark its line in `tasks/refinement_common.md`.
- [ ] After each small batch, pause for human review/approval before continuing.
- [ ] Only begin Bucket B after most obvious Bucket A files are complete and reviewed.

## Human-in-the-Loop Checkpoints
- [ ] Checkpoint 1: Approve Bucket A grouping for `common`.
- [ ] Checkpoint 2: Approve first isolated batch changes in `common`.
- [ ] Checkpoint 3: Approve transition from isolated files to architecture files in `common`.
- [ ] Checkpoint 4: Approve move from `common` to `forge`.
- [ ] Checkpoint 5: Approve move from `forge` to `fabric`.
- [ ] Checkpoint 6: Approve final parity/cleanup pass.

## Working Rules for This Phase
- [ ] Use checklist files as source of truth and mark progress continuously.
- [ ] Prefer low-risk, high-clarity refinements first (naming, small extraction, dead code cleanup, comments, consistency fixes).
- [ ] Defer large architectural rewrites unless explicitly approved at a checkpoint.
- [ ] Keep behavior stable unless a behavior change is explicitly requested and reviewed.


