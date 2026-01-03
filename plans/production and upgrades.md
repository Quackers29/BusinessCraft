Business Craft Mod - Final Design Summary (December 18, 2025)

Business Craft is a lightweight Minecraft mod focused on making resource and passenger transportation the core rewarding gameplay loop. Players build and use transport networks (rails, roads, planes, buses — compatible with mods like Create, Valkyrien Skies, etc.) to deliver cargo and tourists between towns for rewards with large time bonuses. The mod is deliberately lag-free: no entity spawning, no world-gen structures, no visual buildings or villagers — everything is data-driven and UI-only via a single Town Hub block.

Core Gameplay Loop

Towns are autonomous data entities represented only by a Town Hub block and its UI. Place a Town Hub in a biome to create a town with biome-specific starting specialization and resources ("starting kit"). Towns automatically run unlocked production recipes (only if all input conditions are met), generate surplus which is auctioned to other towns with deficits to create cargo contracts, and spawn tourists wanting to travel between towns.

Contracts and tourists: Players accept jobs at Town Hub UI for "express" delivery using their transport systems. Reward: base pay plus big time bonus for speed. If ignored, automated "snail mail" delivery happens slowly with no player reward. Delivered cargo boosts receiving town stockpiles (up to storage caps); tourists boost happiness and population.

Town System and Autonomy

Tracked core values:
- current_population
- pop_cap (maximum supported population / housing capacity)
- happiness (0–100%)
- resource stockpiles with per-item storage_cap

Production recipes only add output if storage space is available (per-item cap). Excess is wasted.

All progression (production unlocks, improvements, capacities, bonuses) is controlled via the upgrade/research tree.

Growth feels organic and fully configurable.

Player Interaction

Town Hub UI shows stockpiles (current / storage_cap), production rates (with condition status), current_population / pop_cap, happiness, active contracts/tourist requests, research progress, and delivery buttons. No player control over town decisions.

Config System (Highly Flexible and Admin-Friendly)

Everything data-driven via config/businesscraft/. Hot-reload supported. Excel/Google Sheets friendly.

Mod ships with simple/ and complex/ template folders for easy starting points.

File List

1. items.csv — Registry of all items/resources.
Columns: item_id,display_name,mc_item_id

Example rows:
food,Food,wheat
wood,Wood,oak_log
stone,Stone,cobblestone
iron,Iron Ingot,iron_ingot
planks,Planks,oak_planks
brass,Brass Ingot,create:brass_ingot
money,Emeralds,minecraft:emerald

2. productions.csv — Defines all production recipes.
Columns: prod_id,display_name,base_cycle_time_days,inputs,outputs

inputs and outputs: packed semicolon-separated list
- Resource consumption: item_id:amount
- Population scaling: pop*item_id:amount
- Condition checks: happiness:>60 ; pop:<pop_cap ; pop:<=95%pop_cap ; surplus:item_id

Recipe runs only if all inputs/conditions satisfied.

Example rows:
prod_id,display_name,base_cycle_time_days,inputs,outputs
population_maintenance,Food Consumption,1,pop*food:1,
population_growth,Natural Population Growth,10,surplus:food;happiness:>60;pop:<pop_cap,population:1
basic_farming,Basic Wheat Farming,1,,food:4
advanced_farming,Advanced Wheat Farming,1,,food:8
wood_to_planks,Wood to Planks,0.5,wood:4,planks:16
brass_production,Brass Smelting,2,copper:3;zinc:1,brass:4
passive_mining,Passive Stone Mining,1,,stone:5

3. upgrades.csv — Node definitions, prerequisites, description, and effects.
Columns: node_id,category,display_name,prereq_nodes,benefit_description,effects

effects format: semicolon-separated list of target:adjustment

Uniform adjustment rules for all targets:
- Plain number → flat additive (positive or negative)
  - e.g., pop_cap:20 → +20
  - basic_farming-time:1 → +1 day (slows production)
  - basic_farming-time:-0.5 → -0.5 day (speeds up)
- Number% → percentage multiplier
  - e.g., pop_cap:15% → ×1.15
  - storage_cap_food:20% → ×1.20 food storage
- prod_id alone (no colon/value) → unlocks the production recipe

Supported targets:
- prod_id (unlock)
- prod_id-time (flat or % cycle time)
- prod_id-input (% on resource inputs)
- prod_id-output (flat or % on outputs)
- happiness (flat or %)
- pop_cap (flat or %)
- storage_cap_item_id (flat or % for specific item)
- storage_cap_all (flat or % for all items)
- tourist_rate (% only)
- trade_value (% only)

Example rows:
basic_settlement,housing,Basic Settlement,,,pop_cap:10;storage_cap_all:200;happiness:50;population_maintenance;population_growth
farming_basic,farming,Basic Farming,basic_settlement,,basic_farming
farming_improved,farming,Improved Irrigation,farming_basic,,basic_farming-time:-30%;basic_farming-output:20%;happiness:10;pop_cap:15%;storage_cap_food:300
warehouse_construction,industry,Warehouse,basic_settlement,,storage_cap_all:10000;storage_cap_wood:50%;storage_cap_stone:1000
wood_processing,wood,Sawmill,basic_settlement,,wood_to_planks
sawmill_efficiency,industry,Sawmill Efficiency,wood_processing,,wood_to_planks-input:-25%;wood_to_planks-output:20%;wood_to_planks-time:-0.2
housing_expansion,housing,Medium Housing,basic_settlement,,pop_cap:25;tourist_rate:30%
brass_factory,industry,Brass Factory,,brass_production;brass_production-output:30%;storage_cap_brass:200;happiness:10;trade_value:15%

4. upgrade_requirements.csv — Costs and research time per node.
Columns: node_id,research_days,required_items

Example rows:
basic_settlement,0,
farming_improved,7,food:20;wood:20;stone:10

Special Requirements (Non-consumable):
- tourism_count:[number] -> Requires a total number of tourists to have arrived at the town.
- tourism_distance:[number] -> Requires a total distance (in blocks) traveled by all tourists.
Example: housing_advancement,5,wood:50;tourism_count:20;tourism_distance:2000

5. biomes.csv — Starting kits and initial values (now simplified and consistent).
Columns: biome_id,starting_nodes,starting_values

starting_values format: packed key:amount list (population, happiness, and all initial stockpiles)

Example rows:
minecraft:plains,basic_settlement;farming_basic,population:5;food:120;wood:60;money:60;happiness:70
minecraft:forest,basic_settlement;wood_processing,population:8;food:80;wood:150;money:50;happiness:65
minecraft:mountains,basic_settlement,population:6;food:40;wood:80;stone:80;iron:50;money:70;happiness:60
minecraft:nether_wastes,,population:3;coal:200;money:100;happiness:50

(Note: pop_cap and storage_caps start at 0 and are granted by starting_nodes effects, e.g., basic_settlement gives initial pop_cap and storage_cap_all.)

6. global.toml — Core settings.
daily_tick_interval = 2400
tourist_spawn_rate_per_pop = 0.01
snail_mail_speed_multiplier = 0.1
base_reward_per_item = 5
base_reward_per_tourist = 20
time_bonus_multiplier = 2.0
express_courier_cut = 0.3
research_choice_randomness = 0.3
production_tick_interval = 100
min_stock_percent = 60
excess_stock_percent = 80

Why This Design Works

- Completely uniform and intuitive effect syntax across every mechanic.
- Flat and percentage adjustments available everywhere (including time).
- Clean unlock with just the prod_id.
- Storage caps fully integrated with granular or global control.
- Production conditions powerful and readable.
- biomes.csv now fully generic and consistent with packed format (no separate columns for population/happiness/stockpiles).
- No hard-coded specials — everything configurable.
- Highly scalable for simple vanilla or massive modpack tech trees.
- Excel-friendly packed strings throughout.

This is the absolute final, consistent, and production-ready design.

7. Research & AI Prioritization (Standardized 0-100 Scale)
    
The Town AI uses a smart, continuous scoring system to select its next research project. All scores are standardized to a **0-100 priority scale** (where 100 is Max Urgency/Desire) to allow the AI to fairly weigh different needs against each other.
    
**Priority Drivers:**
    
1.  **Storage Urgency (Capacity Logic):**
    *   **Goal:** Expand capacity only when running out of space.
    *   **Logic:** Scores increase quadratically as storage fills.
    *   **Formula:** `Score = 100 * (Occupied / Cap)^2`
    *   **Effect:**
        - 0% Full = **0 Priority**.
        - 50% Full = **25 Priority**.
        - 90% Full = **81 Priority**.
        - 100% Full = **100 Priority** (Urgent).
    
2.  **Production Speed (Deficit Logic):**
    *   **Goal:** Speed up production to meet consumption demands.
    *   **Logic:** Compares Production Rate vs Consumption Rate.
    *   **Formula:** `Score = 50 * (2.0 - (Production / Consumption))` (Clamped 0-100).
    *   **Effect:**
        - **Critical Deficit** (0 Prod / Has Cons) = **100 Priority**.
        - **Balanced** (Prod == Cons) = **50 Priority**.
        - **Safe Surplus** (Prod >= 2x Cons) = **0 Priority**.
    
3.  **Accumulation Speed (Fill Rate Logic):**
    *   **Used For:** **Population** and **Tourists** (Resources that produce but don't consume).
    *   **Goal:** Fill the available capacity as fast as possible.
    *   **Logic:** Scores inversely to fullness (Empty = High Priority).
    *   **Formula:** `Score = 100 * (1.0 - (Current / Cap))`
    *   **Effect:**
        - 0/10 Tourists = **100 Priority** (Need Marketing/Spawns!).
        - 5/10 Tourists = **50 Priority**.
        - 10/10 Tourists = **0 Priority**.
    
4.  **Quality of Life:**
    *   **Goal:** Maintain high Happiness and Tourism stats.
    *   **Formula:** `Score = 100 * (1.0 - (Current / Max))`
    *   **Effect:** 0% Happiness = 100 Priority.
    
**AI Patience Logic:**
*   **Problem:** Low-cost upgrades could "snipe" resources while the AI was saving for a specific high-priority expensive upgrade.
*   **Solution:** The AI calculates a "Patience Threshold" based on the highest priority score available.
*   **Behavior:** It will idle/wait for up to 1 in-game day to afford the best upgrade. If the day passes (or if a cheap upgrade has a score > threshold), it buys what it can.

**How to Influence AI:**
*   **Force Storage:** Fill their inventory.
*   **Force Production:** Create a deficit (buy out their stock or add consumption).
*   **Force Marketing/Growth:** Raise their caps so they feel "empty" (e.g. build housing to urge population growth).