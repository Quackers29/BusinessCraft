# Refinement Phase - Cleanup/Removal First (Human in the Loop)

## Core Direction
- [ ] Primary objective: clean up code and remove as much as safely possible.
- [ ] Default rule: do not add new code unless you explicitly request it.
- [ ] Keep behavior stable while simplifying and reducing code surface area.
- [ ] Work through checklists in order: `tasks/refinement_common.md` -> `tasks/refinement_forge.md` -> `tasks/refinement_fabric.md`.
- [ ] Start with isolated/pre-functionality files first; leave architecture-heavy files (for example networking/messaging) for later.

## Per-File Loop (Required Process)
- [ ] 1. Start.
- [ ] 2. Find the next file from the active refinement checklist.
- [ ] 3. Read that file fully.
- [ ] 4. Read associated files needed for context.
- [ ] 5. Make cleanup/refinement changes with a removal-first approach.
- [ ] 6. Mark off that file in the relevant refinement checklist.
- [ ] 7. Summarize the change and why for approval (short, direct, no waffling).
- [ ] 8. If approved, you commit.
- [ ] 9. End; wait for "next file" and repeat.

## Refinement Prioritization Rules
- [ ] First pass targets obvious isolated wins (unused code, dead branches, duplication cleanup, naming clarity, small simplifications).
- [ ] Avoid architecture rewrites unless explicitly approved for that file.
- [ ] Prefer deleting/reducing code over introducing new abstractions.
- [ ] Only proceed one file at a time under the approval loop above.


