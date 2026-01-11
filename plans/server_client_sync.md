# Server-Authoritative Architecture Plan: The "View-Model" Pattern

## Core Philosophy: The Dumb Terminal
**Principle**: The Client is a "Dumb Terminal" (Renderer). It possesses **zero** game logic, **zero** formulas, and **zero** configuration awareness. It does not "sync rules"; it receives "what to show".

**The Golden Rule**: 
> "The Server calculates the View; The Client renders the View."

### 1. Unified Logic Flow (Singleplayer = Multiplayer)
- **Singleplayer**: The Integrated Server reads `upgrades.csv`, calculates the view model, and sends it to the local client.
- **Multiplayer**: The Dedicated Server reads `upgrades.csv`, calculates the view model, and sends it to the remote client.
- **Result**: The Client code is identical in both cases. It *never* reads a CSV file.

### 2. Client: The Renderer (Zero Config)
- **No Formulas**: The Client does not know that `Cost = Base * Multiplier`. It provides no inputs to the logic.
- **No Parsing**: The Client has no code to parse `.csv` files.
- **Pure Display**: The Client receives a **View Model** packet containing:
    - `"Cost Text": "75 Gold"`
    - `"Is Locked": true`
    - `"Tooltip": "Requires Town Level 2"`
- It renders these strings/booleans directly. It does not calculate them.

---

## The Plan: Implementing The View-Model

### Phase 1: Server-Side View Logic
1.  **Logic Centralization**: All "Business Logic" (e.g., specific Upgrade mechanics, Production formulas) moves exclusively to the Server.
2.  **View Model Generation**: The Server creates a `UpgradeViewModel` object for each player request/login.
    *   *Instead of sending*: `BaseCost: 50, Multiplier: 1.5`
    *   *Server Sends*: `CurrentCost: 75` (Pre-calculated).

### Phase 2: State Synchronization (The View Packet)
1.  **Initial Load**: On login, Server sends `SyncRegistryViewPacket`.
    *   Contains the *Visual Definition* of every item (Name, Icon, Description).
    *   Does *not* contain logic (no "production rates" or "multipliers").
2.  **Dynamic Updates**:
    *   Player buys Upgrade.
    *   Server recalculates cost for next level.
    *   Server sends `UpdateNodeViewPacket(ID: "farm", Cost: "115 Gold")`.
    *   Client updates the label.

### Phase 3: Interaction Flow
1.  **Render**: Client iterates `ClientViewCache`. Draws icons.
2.  **Input**: User clicks "Buy". Client sends `InteractionPacket(ID: "farm")`.
    *   *Note*: Client does NOT check `if (cash >= cost)`. It just sends the click.
3.  **Process**: Server receives click -> Checks funds -> Deducts funds -> Increments Level -> Recalculates View.
4.  **Feedback**: Server sends packet -> Client updates UI.

---

## Alternative Approaches (Why we rejected them)

### 1. Client-Side Simulation ("Thick Client")
- **Concept**: Server sends "Base Cost 50, Multiplier 1.5". Client calculates "75".
- **Flaw**: Requires Client to replicate Server logic formulas. Use of `upgrades.csv` on client leads to "Ghost Items" (Client thinks cost is 50, Server knows it's 500).
- **Verdict**: **Rejected**. Logic duplication is the root of desync bugs.

### 2. Hash Verification
- **Concept**: Check if Client's `upgrades.csv` matches Server's.
- **Flaw**: Forces file management on users. Singleplayer logic diverges from Multiplayer.
- **Verdict**: **Rejected**. Bad UX.

### 3. Shared Config Logic
- **Concept**: Client reads CSVs to "predict" state.
- **Flaw**: See "Trust the Client". If Client config differs, prediction fails.
- **Verdict**: **Rejected**.

---

## Industry Comparison: How others do it

### 1. The "Quest Book" Model (FTB Quests, Better Questing)
*   **Similarity**: **High**
*   **Approach**: The Server holds all quest progress, rewards, and logic. The Client purely renders the "Quest Tree" UI. The Client cannot "complete" a quest; it can only ask the Server "Check my requirements".
*   **Verdict**: This is the closest industry standard to our **BusinessCraft** plan. It is highly robust against cheating and desyncs.

### 2. The "Simulation" Model (Create, Mekanism, Thermal)
*   **Similarity**: **Low (Thick Client)**
*   **Approach**: These technical mods sync raw config values (e.g., `machineSpeed = 100`) to the Client. The Client then *simulates* the machine locally to render smooth animations (gears spinning).
*   **Reason**: They need 60FPS smooth animations which requires local math.
*   **Why we differ**: We are a Tycoon/Economy mod. We don't need to predict gear rotation; we display discrete numbers (Money, Stock). Protocol security is more important than interpolation.

### 3. The "Battle" Model (Pixelmon, Cobblemon)
*   **Similarity**: **Medium**
*   **Approach**: Turn-based battles are handled 100% Server-side. The Client UI waits for a packet saying "Show Move Selection". The Client doesn't calculate damage; it just receives "Health: 50%".
*   **Verdict**: This mirrors our "View-Model" purchase flow. The client is a state machine viewer.

### 4. The "Vanilla" Model (Data Packs)
*   **Similarity**: **None**
*   **Approach**: Recipes are synced via JSON/Datapacks. Validation is shared.
*   **Why we differ**: Vanilla crafting is stateless (Input -> Output). Our Town Upgrade system is highly stateful (Level, Population, Previous Upgrades, Time), making purely static JSON sync insufficient.

**Conclusion**: By adopting the **View-Model** (FTB Quests / Pixelmon) approach, BusinessCraft avoids the complexity of Tech Mods (Simulation) and the fragility of Vanilla (Stateless JSON), resulting in a stable, cheat-proof economy.

---

## Static Assets & Optimization (The "Compromise")

While the **Logic** and **Values** are dynamic (Server-driven), we do not need to send *everything*. To save bandwidth and complexity, we rely on the Client behaving like a standard Minecraft client for static assets.

### 1. Resource References (IDs not Textures)
*   **Strategy**: The Server tells the Client *which* icon to draw, not *how* to draw it.
*   **Example**:
    *   Server sends: `"icon": "minecraft:iron_ingot"`
    *   Client resolves: Looks up local texture for Iron Ingot.
*   **Benefit**: Packet size is bytes (string), not megabytes (image data). The Client never needs "server textures"; it just needs to know what to point to.

### 2. Localization Keys (Text vs Keys)
*   **Strategy**: Where possible, send translation keys instead of raw text.
*   **Example**:
    *   Server sends: `"label": "item.minecraft.iron_ingot"`
    *   Client resolves: "Iron Ingot" (in English) or "Eisenbarren" (in German).
*   **Benefit**: Multi-language support works automatically on the client side without the Server needing to know the player's language.

### 3. Static UI Layouts
*   **Strategy**: The *structure* of the window (tabs, grid size, background image) is hardcoded or "static" in the Client code.
*   **Why**: Sending the entire UI definition (XML/JSON) every login is overkill (like HTML).
*   **Compromise**: The Server fills the *content* of the grid, but the Client decides the *shape* of the grid.
    *   *Server*: "Here is a list of 5 Upgrades."
    *   *Client*: "I will arrange them in a 3x2 grid."

**Summary**: The **View-Model** contains *Data*, not *Assets*. The Client provides the "Shell" (Textures, Fonts, Layouts), and the Server provides the "ghost" within the shell.



