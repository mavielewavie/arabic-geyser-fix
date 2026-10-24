package com.arabicfix.extension;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.bedrock.SessionSendPacketEvent;
import java.lang.reflect.Method;

public class ArabicPacketListener {

    @Subscribe
    public void onPacket(SessionSendPacketEvent event) {
        Object packet = event.packet();
        if (packet == null) return;
        try {
            switch (packet.getClass().getSimpleName()) {
                case "TextPacket"        -> fix(packet, "getMessage", "setMessage");
                case "SetTitlePacket"    -> fix(packet, "getText",    "setText");
                case "ToastRequestPacket" -> {
                    fix(packet, "getTitle",   "setTitle");
                    fix(packet, "getContent", "setContent");
                }
            }
        } catch (Exception ignored) {}
    }

    private void fix(Object packet, String getter, String setter) throws Exception {
        Method get = packet.getClass().getMethod(getter);
        String val = (String) get.invoke(packet);
        if (val != null && ArabicTextShaper.containsArabic(val)) {
            packet.getClass().getMethod(setter, String.class)
                  .invoke(packet, ArabicTextShaper.shape(val));
        }
    }
}
