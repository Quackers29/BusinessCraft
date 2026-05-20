
# Global Market Pricing Logic

The Global Price Index (GPI) represents the estimated market value of resources across the entire server. Towns use this index to determine fair prices for buying and selling goods.

## 1. Calculating the GPI

The GPI is calculated using an **Alpha-Weighted Moving Average**. This ensures that the price evolves over time based on actual player trading activity, without being overly volatile from a single outlier transaction.

### Formula
```
NewPrice = (OldPrice * (1.0 - Alpha)) + (TransactionPrice * Alpha)
```
*   **OldPrice**: The current GPI for the resource (default 1.0).
*   **TransactionPrice**: The price per unit of the completed contract.
*   **Alpha**: The learning rate, currently set to **0.1** (10%).

### Example
*   Current GPI for Wood: `1.00`
*   A player completes a contract selling Wood for `5.00` per unit.
*   Calculation: `(1.00 * 0.9) + (5.00 * 0.1) = 0.9 + 0.5 = 1.40`
*   New GPI: `1.40`

## 2. When does the GPI Update?
The GPI updates in two scenarios:

### 1. Successful Auction (Winning Bid)
*   **Trigger**: When an auction closes and a winning bidder is found (after the listing duration expires).
*   **Calculation**: Weighted average using the **Winning Bid Price**.
    *   The price updates *before* delivery and payment finalization.

### 2. Failed Auction (Market Rejection)
*   **Trigger**: When a Sell Contract expires with **0 Bids**.
    *   This indicates the listing price was too high for the market.
*   **Calculation**: The system simulates a transaction at an "Implied Value" to drive prices down.
    *   `ImpliedValue = Min(currentGPI, ListingPrice) * 0.8` (80% of the lower of the two).
    *   This ensures that failed auctions *always* exert downward pressure on the market price, regardless of how high the listing price was.
    *   The GPI updates towards this lower value using the standard Alpha (10%).

## 3. Automated Town Pricing
NPC Towns automatically create contracts and place bids based on their needs:

### Selling (Supply)
*   Towns sell resources when their stock exceeds the **Excess Threshold** (default 150% of cap).
*   **Listing Price**: Calculated based on the current GPI ± random variation.
    *   If a town has vast excess, it may list closer to the GPI or slightly lower.
    *   Includes a small random factor (+/- 5%) to create market variety.

### Buying (Demand)
*   Towns bid on contracts when:
    *   Stock is below **Minimum Threshold** (default 20% of cap).
    *   **OR** The resource is explicitly **Wanted** for an active upgrade.
*   **Max Bid**:
    *   Standard: Up to **3x** the base price (GPI * Quantity).
    *   Wanted: Up to **5x** the base price (Aggressive bidding).
