# Crystal Power update

- Every player starts with exactly **2 free Crystals** for the complete Journey.
- Crystals are persisted in the same local DataStore wallet as other game settings.
- Resetting Adventure/Journey progress does **not** replenish Crystals.
- The Crystal counter is rendered immediately to the **left of the gold coin counter** on the Home screen and in the in-game compact wallet.
- The Journey Journal header also shows the Crystal balance beside coins.
- `Grid Shift` costs **1 Crystal only when entering a easier grid**.
- `Restore Grid` is always free and never refunds or consumes a Crystal.
- If the Crystal balance is 0, the current puzzle is left untouched and a reduced Grid Shift cannot start.
- A new crystal-shaped icon is drawn directly in Compose, so no extra image asset is required.
- `GameSettings.creditPurchasedCrystals(amount)` is included as the purchase-credit hook. It must only be called after Google Play Billing confirms a real purchase.
- Real-money checkout is intentionally not faked in this patch; Play Billing product IDs and pack/pricing configuration are still required before enabling purchases in production.
