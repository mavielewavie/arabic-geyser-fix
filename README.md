# ArabicGeyserFix (Paper plugin)

Fixes Arabic text for **Bedrock players who join a Java server through Geyser**.

Bedrock draws Arabic letters one by one: they are not joined and they run left to right. This plugin joins the
letters (Arabic Presentation Forms) and reverses the order for Bedrock players only. **Java players never see any
change**: the check happens before a packet is even decoded.

## Requirements

- Paper 1.21+ (Java 21)
- [PacketEvents](https://modrinth.com/plugin/packetevents) 2.x installed as a plugin
- [Geyser-Spigot](https://geysermc.org/) (and optionally Floodgate), which you already run

Drop `ArabicGeyserFix-<version>.jar` and PacketEvents into `plugins/` and restart.

## What gets fixed (Bedrock players only)

| Kind of text | Notes |
|---|---|
| Player chat, system messages, `/msg`, join/quit messages | all chat packet types |
| Titles, subtitles, action bar | |
| Tab list | header, footer and player names |
| Boss bars | |
| Scoreboards | sidebar title, team prefix/suffix, score display names |
| GUI / chest titles | |
| Item names and lore | Minecraft 1.20.5 and newer |
| Name tags and text displays | entity custom names |
| Kick screen messages | |

Every group can be switched off in `config.yml`.

Colours, bold/italic and click/hover events are kept: text is shaped per styled piece, so a red word stays red
after the order is reversed.

## Mixed text

With `auto-direction: true` (default):

- `مرحبا بكم` (starts with Arabic) is reversed as a whole line.
- `[VIP] Mavi: مرحبا` (starts with Latin) keeps `[VIP] Mavi:` in place and only reverses the Arabic part.
- Numbers, English words, `10:30` and `3.5` stay readable inside Arabic lines.

Set it to `false` to always reverse the whole line, like the Arabic Text Fixer addon.

## Bedrock detection

Tried in this order: Floodgate API, Geyser API, Floodgate-style UUID (first half zero), optional name prefix.
Nothing else is required.

## Not covered

- Signs, books and other text stored in the world or in chunks (Bedrock gets these straight from the chunk data)
- Item names and lore on servers older than 1.20.5
- Long lines: Bedrock wraps a reversed line at the wrong end, so very long Arabic messages can wrap in an odd order

## Building

```
mvn package
```

The jar is `target/ArabicGeyserFix-<version>.jar`. GitHub Actions builds it on every push (see the Actions tab).

## Credits

The joining rules and the "reverse, keep Latin runs, mirror brackets" approach follow the **Arabic Text Fixer**
behaviour pack by *Minecraft Bedrock Arabic* and *Noor Bazooka*
(<https://modbay.org/user/mbarabic/>). That pack is licensed "Custom (Permission granted by creators)", so ask them
before publishing this plugin.
