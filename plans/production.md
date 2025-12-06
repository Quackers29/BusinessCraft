Business Craft Mod Summary
We've been brainstorming a Minecraft mod called Business Craft, centered on resource and passenger transportation as the core gameplay loop. It's designed to incentivize players to build and use transport networks (rails, planes, buses, cargo wagons) without overwhelming micromanagement. The mod keeps everything lightweight—no entity spawning, no world-gen buildings—just a single Town Hub block with a UI for viewing stats, inventories, and ongoing bids. Players act primarily as couriers for faster deliveries, earning rewards with time bonuses, while an automated "snail mail" system handles the rest slowly.
Core Mechanics

Transportation Focus:
Tourists: Spawn automatically; players ferry them between towns for rewards. Delivered tourists boost town happiness/population.
Resources/Cargo: Towns generate excess resources (e.g., food, wood, iron) and bid on deficits from others. Winning bids create cargo contracts—players can intercept for quick delivery (express lane) or let automation handle it (snail mail, slower but hands-off).
Player Incentives: Base pay per delivery + bonuses for speed/on-time arrival. Can't handle everything solo, so it's optional but rewarding. High player reputation (future versions) could allow influencing town decisions.

Economy & Bidding:
Towns form a closed-loop economy: Produce > Consume > Sell excess via auctions.
Bidding wars: Towns auto-bid higher for critical needs (e.g., food shortages). Winners get a delivery window—player couriers speed it up, earning a cut.
Starts simple: New towns have ~20 coins budget, 2-10 population, basic stockpiles (e.g., 50 food).


Town System & Autonomy

Town Lifecycle: All towns are autonomous NPCs (UI-only). Place a Hub block in a biome to spawn one—biome influences starting specialization (e.g., mountains → mining, plains → farming).
Base State: Small population (number only), one basic workstation (icon in UI), minimal production/consumption.
Progression: Automated upgrade tree with three tracks (flexible starting point, diversifies over time):TrackFocusKey UpgradesBenefitsResourcesRaw materials (wood, stone, iron, coal, wheat)Level 1-3 chains (e.g., logs → planks)More cargo for transport; surplus for bidding/selling.ServicesSupport for growth (schools, hospitals, markets)Basic → Advanced (e.g., school covers 10-25% pop)Higher trade values, happiness boosts, more tourists.PopulationHousing & expansionSmall houses → Apartments → Festival plazasMore villagers (jobs/tourists), but higher needs (food/happiness).
Diversification: Starts specialized (e.g., coal mine in mountains), maxes that track, then auto-branches (e.g., mining town → trade hub). Each diversify step costs resources and slightly reduces prior output (~10%) for balance.

Automation Logic:
Daily/5-Day Checks: Algorithm evaluates needs via a success score (weighted: Food 50%, Happiness 30%, Population 20%).
Triggers: Upgrade if surplus (e.g., >7 days food) or deficit (e.g., low happiness → services). Random 10% chance for variety.
Costs/Trade-offs: Upgrades consume resources (e.g., 10 wood for house); over-specialize → stagnation, diversify → slower but broader growth.

No player input on upgrades (v1)—towns "decide" based on logic: Low food → industry; Low happiness → services; High pop/low housing → expansion.
Production Chains: Tiered outputs (e.g., Wheat Level 1: 1 food/day; Level 2: 2 food). UI shows surplus, current output, next goal.


Scoring & Balancing

Food: Absolute stockpile vs. daily need (1/villager). Surplus (>7 days) → growth focus; Deficit → emergency industry upgrade.
Happiness: 0-100% scale (starts 50%). Base 20/villager +2 per missing service. Low (<25%) → services push; High (>80%) → shift to pop/resources.
Other Resources: Surplus thresholds (e.g., > threshold = +2 score; below = -3). Tracked per type (wood, iron, etc.).
Services: % coverage (e.g., school: 10%). Average across types; <50% → underserved, triggers upgrades.
Population Growth: +1 every 10 days if happiness >50% and housing sufficient. Ties into everything—more people = more production/consumption/tourists/cargo.

Implementation Notes

UI-Only: Population/resources as numbers/icons; no lag from entities/blocks. Progress bars for tiers, buttons to pause/rush (costs extra resources).
Configurability: Use CSV for easy tweaking:textresource_type,level,cost_food,cost_wood,cost_iron,output_amount,output_item
wood,1,10,0,0,1,log
wood,2,0,15,0,2,log
iron,1,20,0,0,1,iron_ore
Separate file for weights: food_weight:50, iron_weight:30, happiness_weight:20.
Load on startup; no code changes for balances.


This setup creates a living economy where towns evolve organically, driving constant transport opportunities.
