# CobbleRandomReward - Comprehensive Administrator & Developer Documentation

A high-performance, asynchronous weighted loot and reward distribution engine for Cobblemon and Minecraft server networks. CobbleRandomReward enables server administrators to construct modular, dynamic reward pools containing vanilla items, custom NBT gear, Cobblemon Pokémon with full property strings, economy currencies, and console commands—all processed off the main server thread to guarantee zero tick lag.

---

## Table of Contents
1. [Architectural Overview & Thread Safety](#1-architectural-overview--thread-safety)
2. [Loot Engine & Weighted Distribution Algorithm](#2-loot-engine--weighted-distribution-algorithm)
   - [2.1 Mathematical Probability Model](#21-mathematical-probability-model)
   - [2.2 Multi-Reward Roll Pipeline](#22-multi-reward-roll-pipeline)
3. [Reward Schema & Supported Formats (`ItemChance`)](#3-reward-schema--supported-formats-itemchance)
   - [3.1 Vanilla & Custom NBT Items](#31-vanilla--custom-nbt-items)
   - [3.2 Cobblemon Pokémon Rewards (`pokemon:`)](#32-cobblemon-pokemon-rewards-pokemon)
   - [3.3 Multi-Economy Cash Rewards (`money:`)](#33-multi-economy-cash-rewards-money)
   - [3.4 Administrative Commands & Placeholders](#34-administrative-commands--placeholders)
4. [Commands & Permissions Reference](#4-commands--permissions-reference)
   - [4.1 Command Matrix](#41-command-matrix)
   - [4.2 Auto-Completion & Dynamic Suggesters](#42-auto-completion--dynamic-suggesters)
5. [Directory Layout & Configuration Reference](#5-directory-layout--configuration-reference)
   - [5.1 `config.json` Specification](#51-configjson-specification)
   - [5.2 Localization Reference (`lang/<lang>.json`)](#52-localization-reference-langlangjson)
   - [5.3 Reward Pools Directory (`rewards/<id>.json`)](#53-reward-pools-directory-rewardsidjson)
6. [Production Reward Pool Templates](#6-production-reward-pool-templates)
   - [6.1 Standard Voting / Daily Crate (`daily_crate.json`)](#61-standard-voting--daily-crate-daily_cratejson)
   - [6.2 Legendary Boss Spoils (`boss_loot.json`)](#62-legendary-boss-spoils-boss_lootjson)
   - [6.3 Mystery Starter Gacha (`starter_gacha.json`)](#63-mystery-starter-gacha-starter_gachajson)
7. [Third-Party Integrations & Automation](#7-third-party-integrations--automation)
   - [7.1 Vote Plugins (NuVotifier / VotingPlugin)](#71-vote-plugins-nuvotifier--votingplugin)
   - [7.2 Quest & Raid Synergies (UltraQuests, CobbleBosses)](#72-quest--raid-synergies-ultraquests-cobblebosses)
8. [Legacy Migration Guide (`/randomreward migrate`)](#8-legacy-migration-guide-randomreward-migrate)
9. [Troubleshooting & Frequently Asked Questions](#9-troubleshooting--frequently-asked-questions)

---

## 1. Architectural Overview & Thread Safety

In large multiplayer networks, executing mass reward distributions (e.g., all-player drop parties, voting reward floods, or dungeon completion payouts) can easily hitch the main server tick loop if rewards require disk I/O, database checks, or complex entity spawning.

CobbleRandomReward isolates the entire RNG roll, reward calculation, and delivery pipeline into an asynchronous worker pool managed by `AsyncContext`:

```
 [ External Trigger: Command / Quest / Vote / NPC ]
                        │
                        ▼
    [/randomreward give <player> <amount> <type>]
                        │
                        ▼
      [ AsyncContext Dedicated Thread Pool ]
       (Core: 1, Max: 4, Bounded Queue: 200)
                        │
          ┌─────────────┴─────────────┐
          ▼                           ▼
  1. Retrieve Pool JSON       2. Execute RNG Rolls
  (rewardsConfig.get(type))     (ItemChance.getRewards)
          │                           │
          └─────────────┬─────────────┘
                        │
                        ▼
              [ Dispatch Rewards ]
      ┌─────────────────┼─────────────────┐
      ▼                 ▼                 ▼
Give Items / NBT    Give Pokémon    Execute Commands
```

### Technical Specifications:
- **ThreadPool Pool**: 1 core thread scaling dynamically to 4 worker threads during high-traffic surges.
- **Queue Buffering**: 200-capacity bounded queue preventing memory bloat during massive voting cascades.
- **Async Execution**: Calculations run via `CompletableFuture.runAsync(..., CobbleRandomReward.EXECUTOR)` with robust exception handling (`.exceptionally(...)`) preventing silent task drops.

---

## 2. Loot Engine & Weighted Distribution Algorithm

### 2.1 Mathematical Probability Model

CobbleRandomReward operates on a relative weight model. Instead of rigid percentages that must sum to exactly 100%, each reward entry is assigned an arbitrary integer weight (`chance`):

$$\text{Probability of Reward } R = \frac{\text{Weight}(R)}{\sum_{k=1}^{n} \text{Weight}(k)} \times 100\%$$

#### Example Distribution:
| Reward Item | Weight (`chance`) | Calculated Probability |
|---|---|---|
| Poké Ball x16 | 70 | $\frac{70}{100} = 70.0\%$ |
| Ultra Ball x8 | 20 | $\frac{20}{100} = 20.0\%$ |
| Master Ball x1 | 9 | $\frac{9}{100} = 9.0\%$ |
| Shiny Charmander | 1 | $\frac{1}{100} = 1.0\%$ |
| **Total Sum** | **100** | **100.0%** |

If you later add an entry with weight `50`, the total sum becomes `150`, and all odds automatically rebalance without requiring manual percentage recalculations.

### 2.2 Multi-Reward Roll Pipeline

When issuing multi-count rewards (`amountRewards > 1`):
1. The engine performs $N$ independent weighted rolls against the specified pool.
2. Multiple rolls can yield identical or distinct items depending on probability.
3. Once all winning items are determined, each `ItemChance.giveReward(player)` executes sequentially on the player's entity context.

---

## 3. Reward Schema & Supported Formats (`ItemChance`)

Reward entries in each `.json` pool utilize CobbleUtils' unified `ItemChance` schema, offering unprecedented flexibility:

### 3.1 Vanilla & Custom NBT Items
Standard Minecraft items are declared with full item IDs, stack counts, display names, lores, and CustomModelData:
```json
{
  "item": "minecraft:diamond_sword",
  "chance": 25,
  "min": 1,
  "max": 1,
  "displayname": "&b&lExcalibur",
  "lore": [
    "&7A blade forged from pure starlight.",
    "&eUnbreakable & Sharpness V"
  ],
  "customModelData": 10042,
  "nbt": "{Unbreakable:1b,Enchantments:[{id:\"minecraft:sharpness\",lvl:5s}]}"
}
```

### 3.2 Cobblemon Pokémon Rewards (`pokemon:`)
Prefixing the `item` string with `pokemon:` routes the reward through Cobblemon's property engine. You can specify any species, shininess, level, nature, ability, gender, IVs, EVs, Poké Ball, or custom properties (such as CobbleSize `size=big` or `scale=1.5`):
```json
{
  "item": "pokemon:rayquaza shiny=true level=70 pokeball=cherish_ball nature=jolly ivs=31/31/31/31/31/31 size=giant",
  "chance": 2
}
```

### 3.3 Multi-Economy Cash Rewards (`money:`)
Direct server economy deposits can be awarded using the `money:` prefix. CobbleUtils handles provider integration (Impactor, Vault, etc.):
```json
{
  "item": "money:vault:2500",
  "chance": 50,
  "displayname": "&a$2,500 Bank Deposit"
}
```

### 3.4 Administrative Commands & Placeholders
Reward entries can trigger arbitrary console commands when won. Use `%player%` to target the receiving player:
```json
{
  "item": "minecraft:paper",
  "chance": 10,
  "displayname": "&6&lVIP Rank Voucher",
  "commands": [
    "lp user %player% parent addtemp vip 30d",
    "broadcast &6%player% &ewon a &630-Day VIP Pass &efrom the Mystery Crate!"
  ]
}
```

---

## 4. Commands & Permissions Reference

### 4.1 Command Matrix

The root command defaults to `/randomreward` (configurable in `config.json`).

| Command | Arguments | Permission Node | Default Level | Description |
|---|---|---|---|---|
| `/randomreward give` | `<player> <amountRewards> <type>` | `cobblerandomreward.admin` | Level 4 (OP) | Rolls and delivers the specified amount of random rewards from pool `<type>` to `<player>`. |
| `/randomreward reload` | *None* | `cobblerandomreward.admin` | Level 4 (OP) | Reloads `config.json`, all language files, and hot-refreshes all reward pool JSON files. |
| `/randomreward migrate` | *None* | `cobblerandomreward.admin` | Level 4 (OP) | Migrates legacy reward files from `/config/cobbleutils/random/` into the new modular format. |

> [!IMPORTANT]
> Note the exact parameter order for `/randomreward give`:
> `/randomreward give <player> <amountRewards: int> <type: string>`
> For example: `/randomreward give AshKetchum 3 daily_crate` rolls 3 rewards from the `daily_crate` pool.

### 4.2 Auto-Completion & Dynamic Suggesters

CobbleRandomReward features full Brigadier tab-completion:
- `<player>`: Suggests all online players.
- `<amountRewards>`: Integer argument prompt.
- `<type>`: Dynamically queries the filesystem in `config/cobblerandomreward/rewards/` and suggests all active pool names (e.g., `default`, `daily_crate`, `boss_loot`) in real time.

---

## 5. Directory Layout & Configuration Reference

All mod data resides under `config/cobblerandomreward/`:

```
config/cobblerandomreward/
├── config.json          # Core settings (debug, lang, aliases)
├── lang/
│   ├── en.json          # English translation
│   └── es.json          # Spanish translation
└── rewards/
    ├── default.json     # Default starter pool
    ├── daily_crate.json # Custom pool 1
    └── boss_loot.json   # Custom pool 2
```

### 5.1 `config.json` Specification

| Field | Type | Default | Description |
|---|---|---|---|
| `debug` | Boolean | `false` | Enables diagnostic logging in server console. |
| `lang` | String | `"en"` | Language code identifying the file in `lang/<lang>.json`. |
| `commands` | Array of Strings | `["randomreward"]` | List of command literals registered with Brigadier. Add aliases like `"rr"` or `"reward"`. |

#### Example `config.json`:
```json
{
  "debug": false,
  "lang": "en",
  "commands": [
    "randomreward",
    "rr",
    "loot"
  ]
}
```

### 5.2 Localization Reference (`lang/<lang>.json`)

| Key | Default | Description |
|---|---|---|
| `prefix` | `"&7[&6CobbleRandomReward&7] "` | Chat message prefix prepended to system notifications. |
| `reload` | `"&aLanguage reloaded"` | Notification sent when `/randomreward reload` succeeds. |
| `invalidType` | `"&cInvalid type"` | Error sent to sender if the requested reward pool does not exist. |

### 5.3 Reward Pools Directory (`rewards/<id>.json`)
Every `.json` file placed inside `config/cobblerandomreward/rewards/` represents an independent pool whose identifier is the file name (without `.json`). Each file must contain a JSON array of `ItemChance` objects.

---

## 6. Production Reward Pool Templates

### 6.1 Standard Voting / Daily Crate (`daily_crate.json`)
Location: `config/cobblerandomreward/rewards/daily_crate.json`

```json
[
  {
    "item": "cobblemon:poke_ball",
    "chance": 50,
    "min": 8,
    "max": 16,
    "displayname": "&fPoké Ball Bundle"
  },
  {
    "item": "cobblemon:ultra_ball",
    "chance": 25,
    "min": 4,
    "max": 8,
    "displayname": "&eUltra Ball Stash"
  },
  {
    "item": "cobblemon:rare_candy",
    "chance": 15,
    "min": 2,
    "max": 4,
    "displayname": "&bRare Candy"
  },
  {
    "item": "money:vault:1000",
    "chance": 10,
    "displayname": "&a$1,000 Cash Prize"
  }
]
```

### 6.2 Legendary Boss Spoils (`boss_loot.json`)
Location: `config/cobblerandomreward/rewards/boss_loot.json`

```json
[
  {
    "item": "cobblemon:master_ball",
    "chance": 40,
    "min": 1,
    "max": 2,
    "displayname": "&d&lMaster Ball"
  },
  {
    "item": "cobblemon:ability_patch",
    "chance": 30,
    "min": 1,
    "max": 1,
    "displayname": "&6&lAbility Patch"
  },
  {
    "item": "pokemon:mewtwo level=70 shiny=false ivs=31/31/31/31/31/31",
    "chance": 20,
    "displayname": "&5&lPristine Mewtwo"
  },
  {
    "item": "pokemon:mewtwo level=70 shiny=true nature=timid ivs=31/31/31/31/31/31",
    "chance": 10,
    "displayname": "&d&l★ SHINY MEWTWO ★",
    "commands": [
      "broadcast &d&lRAID BOSS DEFEATED! &e%player% &7just won a &dShiny Mewtwo&7!"
    ]
  }
]
```

### 6.3 Mystery Starter Gacha (`starter_gacha.json`)
Location: `config/cobblerandomreward/rewards/starter_gacha.json`

```json
[
  {
    "item": "pokemon:bulbasaur level=5 shiny=false nature=modest",
    "chance": 30
  },
  {
    "item": "pokemon:charmander level=5 shiny=false nature=timid",
    "chance": 30
  },
  {
    "item": "pokemon:squirtle level=5 shiny=false nature=bold",
    "chance": 30
  },
  {
    "item": "pokemon:pikachu level=10 shiny=true nature=hasty held_item=cobblemon:light_ball",
    "chance": 10,
    "displayname": "&e&lSecret Starter: Shiny Pikachu!"
  }
]
```

---

## 7. Third-Party Integrations & Automation

### 7.1 Vote Plugins (NuVotifier / VotingPlugin)
Trigger random reward deliveries automatically upon successful server votes:
```yaml
# Inside VotingPlugin or NuVotifier reward configuration:
rewards:
  commands:
    - "randomreward give %player% 1 daily_crate"
```

### 7.2 Quest & Raid Synergies (UltraQuests, CobbleBosses)
- **CobbleBosses Defeat Actions**:
  When a world boss is defeated, configure its completion command to grant boss loot:
  ```json
  "commands": [
    "randomreward give %player% 2 boss_loot"
  ]
  ```
- **UltraQuests Milestone Rewards**:
  Reward players completing long quest chapters with tiered loot rolls:
  ```json
  "commands": [
    "randomreward give %player% 3 starter_gacha"
  ]
  ```

---

## 8. Legacy Migration Guide (`/randomreward migrate`)

Prior versions stored rewards in separate monolithic files under `/config/cobbleutils/random/`:
- `items.json`
- `pokemons.json`
- `money.json`

Executing `/randomreward migrate` performs an automated migration:
1. Reads all legacy items, Pokémon, and money pools.
2. Normalizes Pokémon strings to `pokemon:<species> <properties...>`.
3. Combines multi-category maps and writes independent `<subKey>.json` files directly to `config/cobblerandomreward/rewards/`.
4. Deletes the obsolete `/config/cobbleutils/random/` directory safely.
5. Invokes `CobbleRandomReward.load()` to instantly activate migrated pools.

---

## 9. Troubleshooting & Frequently Asked Questions

### Q: Why do players receive an "Invalid type" error in chat?
**A:** The requested pool name does not match any `.json` file in `config/cobblerandomreward/rewards/`. Remember to omit the `.json` extension in the command (e.g. use `daily_crate`, not `daily_crate.json`).

### Q: Can I give rewards to offline players?
**A:** The command requires an online player entity (`EntityArgumentType.player()`). For offline voting rewards, use a voting plugin that queues commands until the player logs in.

### Q: Why isn't my Pokémon reward receiving the specified IVs or Shiny status?
**A:** Verify that your Pokémon property string matches Cobblemon property syntax:
```json
"item": "pokemon:charizard shiny=true level=50 ivs=31/31/31/31/31/31"
```
Avoid commas or quotes within the `item` string itself.
