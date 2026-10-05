# Modular Powersuits Addons

Addon modules for [Modular Powersuits](https://github.com/lehjr/MachineMusePowersuits) on Minecraft 1.21.1 (NeoForge), a port of Andrew2448's *Modular Powersuits Addons*.

## Modules

### Ore Scanner
Right-click a block with the power fist. The scanner reads a box that starts at the clicked face and runs into the block, then reports the total value of the ore inside it and the most valuable block found.

* Works with any block in the `c:ores` tag, using per-ore values set in the config (`c:ores/diamond=16` and so on).
* The search size is tweakable per axis in the Tinker Table, up to the configured maximum.
* Each scanned block costs FE from the suit.

### ME Wireless Terminal
Right-click with the power fist to open an Applied Energistics 2 wireless terminal from your inventory. The module tops up the terminal's battery from the suit's FE first. Needs Applied Energistics 2; it does nothing without it.

## Requirements

* Minecraft 1.21.1, NeoForge 21.1+
* Modular Powersuits 3.0.0 and Numina 3.0.0 (1.21.1 builds)
* Applied Energistics 2 19.2+ (optional, for the ME Wireless Terminal)

## What is not here

The original addon also shipped modules that Modular Powersuits has since built in: solar, kinetic, thermal and coal-style generators, the magnet, mob repulsor, leaf blower, flint and steel, lightning, dimensional rift, auto feeder, coolant and water tanks, crafting-table module, clock and compass. Modular Powersuits 1.21.1 already provides them, so they are not duplicated here.

## Configuration

`config/mpsaddons-common.toml` holds the module toggles, energy costs, scan limits and the ore value list.

## Building

Modular Powersuits and Numina are not on a public Maven yet. Put their 1.21.1 jars (and the Applied Energistics 2 jar) in `libs/`, then run:

```
./gradlew build
./gradlew runGameTestServer
```

## Credits

* Original *Modular Powersuits Addons*: Andrew2448, Eximius88 and contributors.
* Modular Powersuits: MachineMuse and lehjr.
* 1.21.1 port: Scwunge.

## Licence

MIT. See `LICENSE`.
