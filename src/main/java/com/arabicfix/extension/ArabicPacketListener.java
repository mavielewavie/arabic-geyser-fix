package com.arabicfix.extension;

import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.bedrock.SessionSendPacketEvent;

public class ArabicPacketListener {

    @Subscribe
    public void onText(SessionSendPacketEvent event) {
        if (!(event.packet() instanceof TextPacket pkt)) return;
        String msg = pkt.getMessage();
        if (ArabicTextShaper.containsArabic(msg))
            pkt.setMessage(ArabicTextShaper.shape(msg));
    }

    @Subscribe
    public void onTitle(SessionSendPacketEvent event) {
        if (!(event.packet() instanceof SetTitlePacket pkt)) return;
        String t = pkt.getText();
        if (ArabicTextShaper.containsArabic(t))
            pkt.setText(ArabicTextShaper.shape(t));
    }

    @Subscribe
    public void onToast(SessionSendPacketEvent event) {
        if (!(event.packet() instanceof ToastRequestPacket pkt)) return;
        if (ArabicTextShaper.containsArabic(pkt.getTitle()))
            pkt.setTitle(ArabicTextShaper.shape(pkt.getTitle()));
        if (ArabicTextShaper.containsArabic(pkt.getContent()))
            pkt.setContent(ArabicTextShaper.shape(pkt.getContent()));
    }

    @Subscribe
    public void onInventorySlot(SessionSendPacketEvent event) {
        if (!(event.packet() instanceof InventorySlotPacket pkt)) return;
        ItemData shaped = shapeItem(pkt.getItem());
        if (shaped != pkt.getItem()) pkt.setItem(shaped);
    }

    @Subscribe
    public void onContainerContent(SessionSendPacketEvent event) {
        if (!(event.packet() instanceof ContainerSetContentPacket pkt)) return;
        boolean changed = false;
        ItemData[] items = pkt.getContents().toArray(new ItemData[0]);
        for (int i = 0; i < items.length; i++) {
            ItemData s = shapeItem(items[i]);
            if (s != items[i]) { items[i] = s; changed = true; }
        }
        if (changed) {
            pkt.getContents().clear();
            for (ItemData it : items) pkt.getContents().add(it);
        }
    }

    @Subscribe
    public void onBlockEntity(SessionSendPacketEvent event) {
        if (!(event.packet() instanceof BlockEntityDataPacket pkt)) return;
        NbtMap tag = pkt.getData();
        if (tag == null) return;
        NbtMapBuilder b = tag.toBuilder();
        boolean changed = false;
        for (String key : new String[]{"Text1","Text2","Text3","Text4"}) {
            if (tag.containsKey(key)) {
                String v = tag.getString(key);
                if (ArabicTextShaper.containsArabic(v)) {
                    b.putString(key, ArabicTextShaper.shape(v));
                    changed = true;
                }
            }
        }
        changed |= shapePanel(tag, b, "FrontText");
        changed |= shapePanel(tag, b, "BackText");
        if (changed) pkt.setData(b.build());
    }

    private ItemData shapeItem(ItemData item) {
        if (item == null || item.getTag() == null) return item;
        NbtMap tag = item.getTag();
        NbtMap display = tag.getCompound("display");
        if (display == null) return item;
        NbtMapBuilder db = display.toBuilder();
        boolean changed = false;
        if (display.containsKey("Name")) {
            String name = display.getString("Name");
            if (ArabicTextShaper.containsArabic(name)) {
                db.putString("Name", ArabicTextShaper.shape(name));
                changed = true;
            }
        }
        if (!changed) return item;
        NbtMapBuilder tb = tag.toBuilder();
        tb.putCompound("display", db.build());
        return item.toBuilder().tag(tb.build()).build();
    }

    private boolean shapePanel(NbtMap tag, NbtMapBuilder b, String key) {
        if (!tag.containsKey(key)) return false;
        NbtMap panel = tag.getCompound(key);
        if (panel == null) return false;
        NbtMapBuilder pb = panel.toBuilder();
        boolean changed = false;
        if (panel.containsKey("Text")) {
            String t = panel.getString("Text");
            if (ArabicTextShaper.containsArabic(t)) {
                pb.putString("Text", ArabicTextShaper.shape(t));
                changed = true;
            }
        }
        for (String mk : new String[]{"Message1","Message2","Message3","Message4"}) {
            if (panel.containsKey(mk)) {
                String t = panel.getString(mk);
                if (ArabicTextShaper.containsArabic(t)) {
                    pb.putString(mk, ArabicTextShaper.shape(t));
                    changed = true;
                }
            }
        }
        if (changed) b.putCompound(key, pb.build());
        return changed;
    }
}
