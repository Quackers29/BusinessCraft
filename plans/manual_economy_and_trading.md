# Economy & Trading Manual

Trade with other towns, play the market, and amass a fortune in Emeralds!

## 1. The Contract Board
The **Contract Board** is your gateway to the Global Market. Access it via the "Sales" tab in any Town Interface or a dedicated Contract Board block.

### Buying & Selling
-   **Auctions**: All trades are auctions. Towns list items for sale, and other towns bid on them.
-   **Selling**: If your town has excess resources (e.g., 1000 Wood), it may automatically list them for sale.
-   **Buying**: You can browse active auctions and place bids using your town's Emerald reserves.

### Escrow System (Safe Trading)
To prevent fraud and "ghost" items, BusinessCraft uses an **Escrow System**:
-   **Sellers**: When an auction is created, the items are **removed** from your Available storage and placed in "Escrow". They are safe, but you can't use them for production while they are for sale.
    -   *If the auction succeeds*: The items are released from your Escrow immediately (held in "transit" for delivery).
    -   *If the auction fails*: The items are returned to your Available storage.
-   **Buyers**: When you bid, the Emeralds are immediately deducted and held in Escrow.
    -   *If you win*: The Emeralds are paid to the seller immediately.
    -   *If you are outbid*: Your Emeralds are immediately refunded to your account.

*Tip: Hover over items in the Resources Tab to see how many are currently in Escrow.*

## 2. Global Price Index (GPI)
Prices in BusinessCraft are dynamic and driven by Supply and Demand.

### How it Works
-   **Starting Price**: Everything starts with a base value (e.g., 1.0 Emeralds per unit).
-   **Market Fluctuations**:
    -   **High Demand**: If an item is bought frequently and for high prices, its GPI goes UP.
    -   **Oversupply**: If auctions expire with no bids, the market "cools down", and the GPI goes DOWN.
    -   **Failed Auctions**: If you list an item and nobody buys it, the system lowers the estimated value of that item relative to the listing price.

### Wealth Calculation
Your town's "Total Wealth" is calculated by multiplying your resources by their current GPI.
`Wealth = (Wood * Wood_Price) + (Iron * Iron_Price) + ... + Emeralds`

## 3. Contracts
Contracts are distinct from simple resource trades. They represent **Missions** or **Orders**.

### Types of Contracts
-   **Sell Order**: "I am selling 64 Iron for 100 Emeralds." (Standard Auction)
-   **Buy Order**: "I want 64 Iron and will pay 100 Emeralds." (Coming Soon)
-   **Courier Contract**: "Deliver this package to Town X." (See Courier Manual)

## 4. Tips for Tycoons
-   **Watch the Market**: Buy low, sell high. If Wood is cheap, stockpile it. If Iron is expensive, focus your production on it!
-   **Don't Overextend**: Remember that items in Escrow don't count towards your production inputs. Don't sell all your seeds if you need them to grow next season's crop!
-   **Reputation**: Fulfilling contracts quickly and reliably improves your town's standing (Future Feature).
