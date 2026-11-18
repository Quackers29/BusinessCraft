# Production and upgrades: a path to a working release

Review of `claude/prod-a1-a4` (draft PR #41). It builds on the overhaul review and on #41. It does not repeat them.

#41 already installs the bundled CSVs, validates them, shares one parser, tidies the toml comments, and adds the production tests. That work stands. This note is only about what still stops the loop being playable once production and research are switched on.

Nothing here treats an unapproved production-system draft as approved. No datapacks, no in-game config editor, and no new rule language. The CSV files stay. Pre-alpha: there is no legacy shim to keep. A retune can change the files in place.

## What was run

`./gradlew test`: 1068 tests, 0 failures, 0 skipped.

`bash tools/showcase-play.sh start --production` (Forge server only, port 25565). Four towns, watched from 19:02 to about 19:07, then that server was stopped. No client was joined, so the screens were not opened. The sync notes below are from the code plus the saved town data.

All four towns were plains. Starterton and Hungerford rolled Balanced Hamlet. Farmstead and Highmark rolled Farming Village.

## What the live towns did

The ticker treats a CSV minute as a real minute (progress moves by 1/1200 per tick). At the bundled times that is a debug speed, and the world behaved like one.

- Starterton started at pop 7, food 1100, wood 100. Farming (granted free by Basic Settlement) filled food to the 2000 cap in about a minute. Wood never rose. Planks stayed 0. Work units stayed 0. Pop climbed 7 to 15. The AI bought Tourism Basic (100 emeralds) and one Border Expansion, then sat idle (`research=null` for several minutes). Woodcutting costs 3000 food. The barn only holds 2000, so that research can never be paid.
- Farmstead and Highmark filled food, wood and planks to the cap inside two minutes, then sat on full. Pop climbed about one person every 40 seconds (the 0.6 minute growth recipe). Both spent the whole session researching Border Expansion (level 3 and still going).
- Hungerford started at pop 60, food 120, happiness shown as 100 only because happiness had not ticked yet. Five seconds later happiness was 55, which is the food formula exactly (120 food against a 60% min-stock of 1200, times 50, plus the +50 from Basic Settlement). Food hit 0 by one minute. Happiness then stuck at 50, not 0. Pop fell 60 to 50, about one death every 24 seconds, which is the 0.4 minute starvation cycle. The sawmill never made a plank (wood 240 is under the 1200 min-stock). Taxes still printed emeralds (100 to 664) and work units ran up to the cap of 1000. The AI bought border upgrades with the remaining wood while the town died.
- Every town queued 5 pending tourists and spawned none. This world has no platforms, so the tourism recipe fills `tourist_cap` and stalls. Arrivals stayed 0, so Pop Cap Upgrade (it requires 2 tourists arrived) can never be paid here.
- Highmark's saved border flat value went from the kit's 10 to 33. Pop was over 80% of the pop cap, so Expand Border kept firing. Border Expansion level 3 does not only add border: the unlock is stored as a number, and the x2 benefit makes that number 4, and the recipe time is base divided by that number. The expand recipe was running about four times faster than its 0.5 minute row.

Fed towns sat at happiness 100 the whole time. Growth never looked at the pop cap of 100. Pop was still under 100 when the server stopped, so the cap was not hit, but the code path does not check it.

## Blockers

1. **A new town cannot reach the next job.** Woodcutting costs 3000 food, the sawmill 4000, work units 3000 wood, research speed 2000 wood. Basic Settlement sets storage to 2000. Starterton showed the result: food full, wood stuck at 100, nothing left to research that builds an industry. Cap Upgrade would raise storage, but it costs 250 wood and the hamlet only started with 100.
2. **The bundled times are debug times.** Farming is 40 food every 1.2 seconds. A 2000 barn fills in about a minute. Research is 15 to 30 seconds. Starvation kills someone every 24 seconds. This matches #41's own note. It is not a release pace.
3. **Population ignores the pop cap, and the border recipe does not stop.** Growth adds 1 whenever happiness is over 60 and the theoretical food rate is a surplus. The surplus check uses the unlocked rate, not whether food is actually falling. Once pop is over 80% of the cap, Expand Border adds 1 forever, and repeat research makes that recipe faster (Highmark, 10 to 33). Two towns 120 apart will grow into each other.
4. **The player cannot choose research.** The server method `startResearch` is ready. There is no packet and no button. The screen says "Idle - Select Research". The AI is the only player, and in this world it bought border levels during a famine and on towns that were already full.
5. **The screens show the wrong numbers, and they do not show why a recipe stopped.** The production view model always says requirements are met. A locked or stalled recipe with no progress is labelled Ready. Amounts like `1*pop` are stored as 0, so taxes, food use and work units show 0 and "+0.0/hr". Cycle time is printed as hours with one decimal, so every bundled recipe shows "0.0 hours/cycle". The resources tab multiplies a per-minute rate by 3 and calls it per hour, which is 20 times too low. The production tab's hourly figure uses times 60, so the two tabs disagree. Speed upgrades are ignored in the cycle text.
6. **The view model is sent only when the town revision changes.** In-progress recipe time and research time do not bump that revision. Happiness writes do not either. At these debug speeds a stock change every second hides it. At a release cycle of several minutes the bar, the happiness line and the research percent stay put until some other stock change.

Smaller, same area:

- Happiness is a step, not a scale. Food fills a base of 0 to 50. Basic Settlement adds 50. Fed is always 100. Starving is always 50. Growth only needs more than 60, which is a small pile of food.
- Basic Settlement's effect list includes the node `farming_basic`, so every town starts with farming already researched. The row with a cost is dead.
- Iron and coal are in `items.csv` and in no recipe.
- Hot reload still runs on the file-watcher thread (the old A6 note). Do not add a reload command on top of it. It is not what makes the loop unplayable.

## Steps

Each one is its own PR, in this order. Effort is how much code moves, not a calendar guess. Risk is for a world that already has towns.

1. **Stop the town breaking itself.** In `TownProductionComponent`, do not grow pop past `pop_cap`, and do not treat a stalled recipe as a surplus. Gate Expand Border on crowding (the research AI already has a density check) so it stops when the town has room. Stop a repeatable unlock from speeding its recipe: an unlock of 1 must stay 1, not `1 * benefit^(level-1)`. Small change, a few methods and the tests that pin today's pop and border behaviour. Low risk. Do this before any retune, or the new times still run away.
2. **Retune the bundled CSVs, after the table below is accepted.** `productions.csv` and `upgrades.csv` only. Costs have to fit in the storage cap the town has at that point. Update the tests that assert the old costs and the 5760 storage figure. Small change, medium risk for any dev world already saved, which is acceptable in pre-alpha.
3. **Make the two screens true, and push progress.** One view-model fix: real minutes and seconds, effective cycle time after speed, per-hour = per-minute times 60 on both tabs, dynamic amounts filled in, status from the real condition check (locked, missing input, under min stock, barn full, visitor cap). Bump the town revision about once a second while a cycle or a research is running, so the bar and happiness move. Do not bump it every tick. Medium change in the two builders, `ProductionTab` is already a display of those strings, and `TownInterfaceEntity` sync can stay. Low risk.
4. **Let the player start research.** One client-to-server packet with the same reach and permission check as the other town packets. A button on each upgrades row. `research.mode` in the toml: `player`, `ai`, or `both`. For the release, default `player` when research is on. Wants then follow the chosen node instead of the AI pick. The "Select Research" line goes away. Medium change. Low risk if the default stays `ai` until Gareth picks; the live run is the reason to prefer `player`.
5. **Point the AI at food and caps, only if `ai` or `both` stays.** It should not buy an infinite border while food is at 0 or while every barn is full. Smaller follow-up. Skip it if research is player-only for the release.

Not in this pass: a typed effect language, datapacks, an in-game config editor. The #41 validator is enough to edit the CSVs safely.

## Suggested release numbers

These are a proposal for a 30 to 90 minute session. They are not approved. The unit is real minutes, because that is what the ticker does. Changing the unit to in-game days means changing the ticker, not just the CSV.

One cycle should be long enough to watch and short enough to matter in one sitting. The barn should take a sitting to fill, not a minute. A missed food supply should cost a person within about ten minutes, and the player can put bread into town storage to stop it.

### Recipe cycles

| Recipe | Now | Suggested | One cycle |
|---|---|---|---|
| Food consumption | 0.4 min, 1 per person | 8 min | eats 1 food per person |
| Population growth | 0.6 min, +1 | 12 min | +1 person, and it must stop at the pop cap |
| Tourism | 0.2 min, +1 | 3 min | one visitor attempt. Cap 5 fills in about 15 min |
| Farming | 0.02 min, 40 food | 4 min, 24 food | about 6 food a minute |
| Woodcutting | 0.05 min, 50 wood | 6 min, 24 wood | about 4 wood a minute |
| Sawmill | 0.01 min, 100 planks | 5 min, 16 planks from 8 wood and 8 work | a conversion |
| Taxes | 0.4 min, 1 emerald per person | 10 min | emeralds trickle, including during a famine, but slowly |
| Expand border | 0.5 min, +1, and it never stops | 30 min, +1, only while crowded | stops once the town has room |
| Work units | 0.1 min, 1 per person | 5 min | enough to run the sawmill, not a flood |

At pop 10, food use is about 1.25 a minute and farming makes about 6, so a small town eats and still stocks. The 2000 cap is then a ceiling, not something you hit before you have read the screen.

### Research

| Node | Now | Suggested time | Suggested cost | Why |
|---|---|---|---|---|
| Farming Basic | 0.5 min, 100 food | see decision 5 | 80 food if it becomes a real choice | settlement already grants it |
| Woodcutting | 0.5 min, 3000 food | 10 min | 400 food | 3000 does not fit in 2000 storage |
| Sawmill | 0.5 min, 4000 food | 12 min | 200 food and 40 wood | same |
| Cap upgrade | 0.5 min, 250 wood, benefit replaces | 15 min | 200 wood | each level should add storage. Today level 3 is 5760, not the sum of the levels |
| Farming Advanced | 0.5 min, 150 planks | 12 min | 40 planks | a speed boost, after the sawmill exists |
| Pop cap | 0.5 min, 200 wood and 2 arrivals | 15 min | 150 wood | the arrival cost needs a platform and another town, or it is a soft lock |
| Tourism Basic | 0.5 min, 100 emeralds | 8 min | 80 emeralds | the recipe is already on. This row is only a speed-up |
| Border Expansion | 0.5 min, 2 wood, cost and benefit x2 | 20 min | 40 wood, mild cost growth | must not speed the expand recipe up each level |
| Research Basic | 0.25 min, 2000 wood | 10 min | 200 wood | 2000 wood does not fit |
| Work units | 0.5 min, 3000 wood | 10 min | 200 wood | same |
| Work unit upgrade | 0.5 min, 200 wood | 15 min | 150 wood | adds cap |

## Decisions Gareth has to make

1. Keep CSV minutes as real minutes, and fix the screens to say so. The other option is to change the ticker to in-game days and retune again. Do not leave the two meanings in place.
2. Accept this table or mark up different times and costs. The CSV PR should not start before that.
3. Who may spend town goods on research: the person at the Town Centre, or anyone.
4. Release research mode. Recommendation: `player`, with the AI off, because the live AI bought border levels for a starving town and for towns that were already full. `both` is a later autopilot.
5. Does Basic Settlement keep granting farming for free? If yes, take Farming Basic off the list the player is told to choose. If no, it is the first real choice and the chain has to go.
6. Repeat maths: each level adds its effect (recommended), or the higher level replaces the lower one (what Highmark's storage and border do today).
7. Is there a dev world that must keep loading? If not, steps 1 and 2 do not need a migration.
