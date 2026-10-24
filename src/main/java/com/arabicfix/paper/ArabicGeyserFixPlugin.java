package com.arabicfix.paper;

import com.arabicfix.paper.bedrock.BedrockDetector;
import com.arabicfix.paper.packets.BedrockTextListener;
import com.arabicfix.paper.shaping.ComponentShaper;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Fixes Arabic text for Bedrock players that join a Java server through Geyser.
 * Java players are never touched.
 */
public final class ArabicGeyserFixPlugin extends JavaPlugin implements Listener {

    private BedrockDetector detector;
    private BedrockTextListener packetListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Settings settings = Settings.from(getConfig());

        detector = new BedrockDetector(
                getLogger(),
                settings.useFloodgate(),
                settings.useGeyser(),
                settings.uuidFallback(),
                settings.namePrefix(),
                getClassLoader());

        ComponentShaper shaper = new ComponentShaper(settings.autoDirection());
        packetListener = new BedrockTextListener(settings, detector, shaper, getLogger());
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Arabic text shaping is active for Bedrock players. Java players are not touched.");
    }

    @Override
    public void onDisable() {
        if (packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            packetListener = null;
        }
        if (detector != null) detector.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (detector != null) detector.forget(event.getPlayer().getUniqueId());
    }
}
