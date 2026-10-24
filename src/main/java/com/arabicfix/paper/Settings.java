package com.arabicfix.paper;

import org.bukkit.configuration.file.FileConfiguration;

/** Values from config.yml. */
public record Settings(
        boolean autoDirection,
        boolean debug,
        boolean useFloodgate,
        boolean useGeyser,
        boolean uuidFallback,
        String namePrefix,
        boolean chat,
        boolean titles,
        boolean actionBar,
        boolean tabList,
        boolean bossBars,
        boolean scoreboard,
        boolean windowTitles,
        boolean items,
        boolean entityNames,
        boolean kickMessages
) {
    public static Settings from(FileConfiguration c) {
        return new Settings(
                c.getBoolean("auto-direction", true),
                c.getBoolean("debug", false),
                c.getBoolean("detection.floodgate-api", true),
                c.getBoolean("detection.geyser-api", true),
                c.getBoolean("detection.uuid-fallback", true),
                c.getString("detection.name-prefix", ""),
                c.getBoolean("shape.chat", true),
                c.getBoolean("shape.titles", true),
                c.getBoolean("shape.action-bar", true),
                c.getBoolean("shape.tab-list", true),
                c.getBoolean("shape.boss-bars", true),
                c.getBoolean("shape.scoreboard", true),
                c.getBoolean("shape.window-titles", true),
                c.getBoolean("shape.items", true),
                c.getBoolean("shape.entity-names", true),
                c.getBoolean("shape.kick-messages", true)
        );
    }
}
