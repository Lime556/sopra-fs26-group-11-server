# Settlers of Catan: Game Explanation

This project implements the core gameplay flow of **Settlers of Catan** in a digital multiplayer format.
The goal is to reach the configured victory point target (default: **10 VP**) before all other players.

## Core Objective

Players earn victory points by expanding their presence on the board and using strategic advantages.

| Source | Victory Points |
| --- | --- |
| Settlement | 1 VP |
| City | 2 VP |
| Development card (victory point type) | 1 VP |
| Longest Road bonus | 2 VP |
| Largest Army bonus | 2 VP |

When a player reaches or exceeds the target, the game ends and that player is declared the winner.

## Turn Flow

Each turn follows the same high-level sequence:

1. **Roll dice** to produce resources for players with adjacent settlements/cities.
2. **Trade** with the bank or other players to optimize resources.
3. **Build or upgrade** structures (roads, settlements, cities).
4. **Play development cards** where applicable.
5. **End turn** and pass control to the next player.

## Board and Player Systems

- **Hex tiles** represent resource regions (wood, brick, wool, grain, ore, desert).
- **Ports** improve trade rates for specific resources or generic trades.
- **Robber** blocks production on its tile and can influence player resource economy.
- **Road/settlement/city placement** controls expansion and scoring opportunities.

## Endgame and Leaderboard

The game state tracks victory points dynamically for every player.

- The backend recalculates and synchronizes points, winner, and finished state.
- The frontend displays the winner panel and final leaderboard ranking.
- All clients observe the same game-end result and order of players.

---
