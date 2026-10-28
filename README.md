<img src="https://avatars.githubusercontent.com/u/30272840?s=96&v=4" alt="Sr_Edition" title="Sr_Edition" align="right" height="96" width="96"/>

# 🎮 Solary-Economy 💸

## The Ultimate Economy System for Your Minecraft Server

[![GitHub All Releases](https://img.shields.io/github/downloads/sredition/Solary-Economy/total.svg?logoColor=fff)](https://github.com/sredition/Solary-Economy/releases/latest)

---

**Solary-Economy** is a robust, lightweight, and highly customizable economy plugin for Minecraft servers. Version **1.6** is built against the **Spigot/CraftBukkit 1.5.2** API and runs on **Java 8**. It offers a complete economy management experience — from basic operations like balance checking and transfers to advanced features such as leaderboards, plugin integrations, and support for multiple databases.

> **Compatibility.** Tested on Spigot `git-Spigot-934` (1.5.2) with Vault 1.2.25, Legendchat 1.1.2, ChestShop 3.5, Essentials 2.11.1 and PlugMan 1.8.4. Only the 1.5.2 API is used (nothing that changed between versions, such as `Bukkit.getOnlinePlayers()`), so newer servers are expected to work, but they are **not** tested. The MySQL backend has unit tests for its SQL but has not been run against a real MySQL server.

### 🌟 Main Features

**1. Full Economy Management**

* Create, delete, and manage player accounts via console or in-game commands.
* Easily set, add, or remove player balances with intuitive commands.
* Transfer money atomically between players: either both balances change or neither does.

**2. Richest Player Leaderboard**

* Display the top richest players using `/money top`.
* Customize how many players are shown and how often the leaderboard updates.
* Highlight the server’s **wealthiest player** with a custom tag in chat (Legendchat).

**3. Vault Integration**

* Compatible with the **Vault** API for seamless interaction with other plugins (shops, jobs, factions...).
* Supports PlaceholderAPI placeholders on servers that have it.

**4. Value Abbreviations**

* Shorten large amounts (thousands, millions, billions) with customizable suffixes (e.g., 1k, 1m, 1b).
* Players can type them too: `/money pay Steve 1,5k`.

**5. Multi-Database Support**

* Choose between **SQLite** (default) or **MySQL** to store economy data.
* Your old data is imported automatically, and the old table is never touched.

**6. Full Customization**

* Edit all plugin messages in `mensagens.yml` to match your server’s style.
* Define singular and plural currency names to create a unique economy.

**7. Commands & Permissions**

* Easy-to-use commands like `/money`, `/money pay`, `/money top`, `/money magnata`, and more, with tab completion.
* Detailed permission system to control access to each feature.

**8. Safe by design**

* Balances can never go negative; every amount from a command or from another plugin is validated.
* Every SQL statement is parameterized, so a player named `a' OR '1'='1` is just a (harmless) name.
* Writes happen in the background in a single transaction, and again on shutdown. If the database is down, nothing is lost: changes are retried, and as a last resort dumped to a CSV file.
* Every balance change is recorded in `transactions.0.log` (rotated, 5 × 1 MB).

---

## ⚙️ Commands

```text
/money                        - View your current balance.
/money <player>               - View another player's balance.
/money create <name> <amount> - Create a new account.            (alias: criar, new)
/money delete <name>          - Delete an account.               (alias: deletar, del)
/money pay <player> <amount>  - Send money to another player.    (alias: pagar, enviar)
/money set <player> <amount>  - Set a player's balance.          (alias: definir, setar)
/money add <player> <amount>  - Add money to a player's account. (alias: give)
/money remove <player> <amount> - Remove money from an account.  (alias: take)
/money toggle                 - Enable/disable receiving money.
/money top                    - View the richest players.        (alias: rank)
/money magnata                - View the current server magnate.
/money reload                 - Reload config and message files. (alias: recarregar)
/money help                   - View the list of available commands. (alias: ajuda, ?)
```

Amounts accept `1500`, `1,5` or `1.5` (both are decimal separators) and, when enabled, abbreviations such as `32k` or `2,5m`. Anything else (`5q`, `-5`, `1e9`, `abc`) is refused with the "invalid value" message.

---

## 🔒 Permissions

```text
solaryeconomy.commands.money.other   - Use /money <player>
solaryeconomy.commands.top           - Use /money top
solaryeconomy.commands.criar         - Use /money create
solaryeconomy.commands.deletar       - Use /money delete
solaryeconomy.commands.add           - Use /money add
solaryeconomy.commands.remove        - Use /money remove
solaryeconomy.commands.set           - Use /money set
solaryeconomy.commands.pay           - Use /money pay
solaryeconomy.commands.toggle        - Use /money toggle
solaryeconomy.commands.reload        - Use /money reload
solaryeconomy.commands.magnata       - Use /money magnata
```

`/money`, `/money help` and `/money <your own name>` need no permission. The other nodes are not declared in `plugin.yml`, so without a permissions plugin only operators have them.

---

## 📜 Configuration

Both files are created in `plugins/Solary-Economy/` on the first run, fully commented (in Portuguese): see [`config.yml`](src/main/resources/config.yml) and [`mensagens.yml`](src/main/resources/mensagens.yml). A key missing from your file simply uses the built-in default, and the console tells you which ones. An invalid value (a typo, a number out of range) is reported and replaced by the default instead of breaking the plugin.

| Key | Default | Meaning |
|---|---|---|
| `mysql.enable` | `false` | `true` for MySQL, otherwise SQLite in `storage.db`. Needs a restart. |
| `mysql.hostname` / `database` / `username` / `password` | | Connection data (`hostname` may include a port: `localhost:3306`). |
| `mysql.table` | `solaryeconomy` | Base table name. Accounts live in `<table>_accounts`; the old `<table>` is only read, once. |
| `world` | `world` | Main world, used to look up Vault prefixes of offline players. |
| `use_vault` | `true` | Register this economy in Vault. Needs a restart. |
| `start_value` | `0` | Balance of a new account. **See the offline-mode notes below.** |
| `decimal_places` | `2` | Decimals kept in balances (0–8). Extra decimals are dropped, never rounded in the player's favor. |
| `max_balance` | `1000000000000000000` | Largest balance; an operation that would exceed it is refused. |
| `pay_minimum` | `1` | Smallest amount accepted by `/money pay`. |
| `save_interval` | `30` | Seconds between background writes to the database. |
| `audit_log` | `true` | Record every balance change in `transactions.0.log`. |
| `check_updates` | `true` | Ask GitHub once at startup whether a newer release exists. |
| `debug` | `false` | Extra diagnostic lines in the console. |
| `economy_top.size` / `refresh_time` / `prefix` / `name_size` | `10` / `300` / `true` / `16` | Leaderboard size, refresh seconds, Vault prefixes, longest name listed (hides factions). |
| `currency_name.singular` / `plural` | `coin` / `coins` | Currency names. |
| `magnata_tag` / `magnata_broadcast` | `true` / `true` | Legendchat tag `{solary_economy_magnata}` and the "new magnate" announcement. |
| `abbreviations.*` | | Enable per messages/commands, `decimal_places`, and the `dictionary` (`display` + `divider`). |

`/money reload` re-reads both files; if one of them is broken nothing changes and the reason is shown. Database settings and `use_vault` only change after a restart (the console says so).

### Placeholders (PlaceholderAPI, where installed)

```text
%solaryeconomy_balance%            - Player's balance as in messages, e.g. "1.5k" (or "1.500 coins" without abbreviations)
%solaryeconomy_balance_formatted%  - Short form, e.g. "1.5k" (or "1.5K" without abbreviations)
```

---

## 🏴‍☠️ Offline-mode (cracked) servers and UUIDs

Minecraft 1.5.2 has no player UUIDs, and a server in offline mode could not trust them anyway. Accounts are therefore identified by **name**, compared **case-insensitively**: `Steve`, `steve` and `STEVE` are one account (the server itself treats them as one player when they log in). The name is shown with the casing used when the account was created.

What this plugin cannot do is stop someone from logging in *as* somebody else in offline mode. Use an authentication plugin (AuthMe or similar) and keep `online-mode` consistent. Two more things to consider:

* **`start_value`:** if every new name receives money, one person with many names (or a bot) can collect it and `/money pay` it to a main account. Keep it at `0` unless you limit accounts per IP.
* A name that cannot be an account (empty, over 40 characters, control characters or `§`) never gets an account; the console logs a warning, since in offline mode that usually means someone is probing the server.

---

## ⬆️ Upgrading from 1.5.x

Drop the new jar in and start the server. On the first run the old table (`solaryeconomy`: `name`, `valor`, `toggle`) is imported into the new one (`solaryeconomy_accounts`), and the console reports everything it had to adjust:

* Accounts that differed only by casing (`Steve` / `steve`) become one account; the **larger balance is kept** and the console says which two were merged.
* Rows with an unreadable balance or an invalid name are skipped, each with a warning.
* Balances above `max_balance` are reduced to it; fractions beyond `decimal_places` are dropped; negative balances (the old plugin allowed them) are kept and counted.

The old table is not modified or deleted, so going back to 1.5.x is possible (without anything that happened after the upgrade). The import runs only while the new table is empty, and is retried at the next start if it fails.

Other behavior changes: `/money pay`, `remove` and Vault withdrawals can no longer overdraw an account; an unknown abbreviation (`5q`) is an error instead of "5"; `abbreviations.decimal_places` is finally honored (it used to be hard-coded to 2); the leaderboard is computed from memory, so it is exact for any size (the old query sorted with `CAST(valor AS DECIMAL)`, which in MySQL defaults to 10 digits); balances are written every `save_interval` seconds instead of every 5 minutes.

---

## 💻 For Developers

Other plugins should talk to **Vault**: it works with every economy plugin. The classes below are for those that want more, such as the richest player or the leaderboard. They live in the Solary-Economy jar (declare it as `provided`/`depend`).

```java
import com.redeskyller.bukkit.solaryeconomy.SolaryEconomy;
import com.redeskyller.bukkit.solaryeconomy.economy.EconomyService;
import com.redeskyller.bukkit.solaryeconomy.economy.RankAccount;
import com.redeskyller.bukkit.solaryeconomy.economy.TransactionResult;

import java.math.BigDecimal;
import java.util.List;

public class YourPlugin {

    public void yourMethod() {
        // The current magnate (null if nobody is ranked) and the leaderboard, richest first
        RankAccount magnate = SolaryEconomy.getMagnata();
        List<RankAccount> leaderboard = SolaryEconomy.getMoneyTop();

        // The economy: thread-safe, names are case-insensitive, nothing throws for "business" failures
        EconomyService economy = SolaryEconomy.getEconomy(); // null while the plugin is disabled
        BigDecimal balance = economy.getBalance("PlayerName");

        TransactionResult result = economy.transfer("Alice", "Bob", new BigDecimal("10"));
        if (!result.isSuccess())
            System.out.println("Transfer refused: " + result); // INSUFFICIENT_FUNDS, RECEIVING_DISABLED, ...
    }
}
```

> The Java API of 1.5.x (`SolaryEconomy.economia`, `Economia`, the static `config`/`database` fields) was removed; the example that used to be here did not match the code either. Vault is unchanged.

### Building

```text
mvn clean package        # -> build/Solary-Economy-<version>.jar
```

Use a JDK 8 or newer (it was verified with JDK 8 and 25). With JDK 9+ the build uses `--release 8`, so the compiler rejects APIs newer than Java 8. The source only uses Java 7 features; to target Java 7, set `jdkVersion` to `1.7` in the `pom.xml` and build with a JDK up to 19. The Bukkit 1.5.2 API comes from `maven.elmakers.com`, because `repo.bukkit.org` no longer exists.

The test suite (`mvn test`, 154 tests) covers the economy rules and their concurrency (parallel transfers in opposite directions must neither deadlock nor change the total), amount parsing, SQLite persistence and rollback, the import of old data, outage handling, the commands, and more. It runs on the same SQLite driver version that 1.5.2 bundles.

---

## 💙 About the Project

**Solary-Economy** is an **open source** project built with passion and dedication for the Minecraft community. With over **8 years of development**, it has improved over time thanks to server feedback and contributions.

### 🤝 Contributions Welcome!

This is a community-driven project. You can:

* Send **pull requests** with improvements or bug fixes.
* Report **issues** and suggest new features.
* Share your feedback to make **Solary-Economy** even better.

Every contribution counts and helps keep the project alive and evolving!

### 🙏 Special Thanks

Thanks to all users and supporters of **Solary-Economy** over the years. You are the reason this project keeps growing stronger. Let’s continue building an amazing economy system for Minecraft servers together! 💰
