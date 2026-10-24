package com.arabicfix.extension;

import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.GeyserApi;

public class ArabicGeyserExtension implements Extension {

    @Override
    public void onEnable() {
        logger().info("Arabic Geyser Fix v1.0.0 enabling...");
        GeyserApi.api().eventBus().register(this, new ArabicPacketListener());
        logger().info("Arabic text shaping active for all Bedrock sessions.");
    }

    @Override
    public void onDisable() {
        logger().info("Arabic Geyser Fix disabled.");
    }
}
