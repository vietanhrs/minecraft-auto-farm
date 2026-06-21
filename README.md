# AutoFarm

A client-side Minecraft mod for Forge and NeoForge (Minecraft 26.2) that automates sword combat and fishing.

## Features

### Auto Fishing
- Automatically casts the fishing rod, detects bites, reels in the catch, and re-casts
- **Enabled by default** — just hold a fishing rod in your main hand
- Toggle with `!fish` in chat

### Auto Attack
- Automatically selects a sword from the hotbar and attacks the entity in your crosshair
- Manages food: if hunger drops to a critical level, eats food from the hotbar before resuming attacks
- Toggle with `!attack` in chat

## Chat Commands

| Command | Description |
|---|---|
| `!fish` | Toggle auto-fishing on/off |
| `!fish on` / `!fish off` | Explicitly enable/disable auto-fishing |
| `!attack` | Toggle auto-attack on/off |
| `!attack on` / `!attack off` | Explicitly enable/disable auto-attack |

Commands are intercepted client-side and never sent to the server.

## Requirements

- Minecraft 26.2
- Forge 65.x
- NeoForge 26.2.0.6-beta
- Java 25

## Building

```bash
./gradlew build
```

Build one loader:

```bash
./gradlew :forge:build
./gradlew :neoforge:build
```

Run in development:

```bash
./gradlew :forge:runClient
./gradlew :neoforge:runClient
```

## Configuration

On first launch a config file is generated at `config/autofarm-client.toml`:

| Key | Default | Description |
|---|---|---|
| `attack_interval_seconds` | `10` | Seconds between each auto-attack swing |
| `critical_food_level` | `3` | Food level at or below which the player eats before attacking |
