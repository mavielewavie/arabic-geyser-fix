package com.arabicfix.paper.packets;

import com.arabicfix.paper.Settings;
import com.arabicfix.paper.bedrock.BedrockDetector;
import com.arabicfix.paper.shaping.ComponentShaper;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_19_3;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemLore;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerActionBar;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBossBar;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisguisedChat;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerListHeaderAndFooter;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleSubtitle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleText;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shapes Arabic text in outgoing packets, for Bedrock players only.
 *
 * <p>Java players never reach the shaping code: the check is done before a packet is even decoded.</p>
 */
public final class BedrockTextListener extends PacketListenerAbstract {

    private final Settings settings;
    private final BedrockDetector detector;
    private final ComponentShaper shaper;
    private final Logger logger;
    private final Set<PacketType.Play.Server> reportedFailures = ConcurrentHashMap.newKeySet();

    public BedrockTextListener(Settings settings, BedrockDetector detector, ComponentShaper shaper, Logger logger) {
        super(PacketListenerPriority.HIGH); // after normal-priority plugins have made their edits
        this.settings = settings;
        this.detector = detector;
        this.shaper = shaper;
        this.logger = logger;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPacketType() instanceof PacketType.Play.Server type)) return;
        if (!enabled(type)) return;

        User user = event.getUser();
        if (user == null || !detector.isBedrock(user.getUUID(), user.getName())) return;

        try {
            if (handle(event, type)) {
                event.markForReEncode(true);
                if (settings.debug()) {
                    logger.info("[debug] shaped " + type.name() + " for " + user.getName());
                }
            }
        } catch (Throwable t) {
            // never break a player's connection because of a text fix
            if (reportedFailures.add(type)) {
                logger.log(Level.WARNING, "Could not shape " + type.name() + " (further errors of this type are hidden)", t);
            }
        }
    }

    private boolean enabled(PacketType.Play.Server type) {
        return switch (type) {
            case CHAT_MESSAGE, DISGUISED_CHAT, SYSTEM_CHAT_MESSAGE -> settings.chat();
            case SET_TITLE_TEXT, SET_TITLE_SUBTITLE -> settings.titles();
            case ACTION_BAR -> settings.actionBar();
            case PLAYER_LIST_HEADER_AND_FOOTER, PLAYER_INFO_UPDATE -> settings.tabList();
            case BOSS_BAR -> settings.bossBars();
            case SCOREBOARD_OBJECTIVE, TEAMS, UPDATE_SCORE -> settings.scoreboard();
            case OPEN_WINDOW -> settings.windowTitles();
            case SET_SLOT, WINDOW_ITEMS -> settings.items();
            case ENTITY_METADATA -> settings.entityNames();
            case DISCONNECT -> settings.kickMessages();
            default -> false;
        };
    }

    private boolean handle(PacketSendEvent event, PacketType.Play.Server type) {
        return switch (type) {
            case SYSTEM_CHAT_MESSAGE -> systemChat(event);
            case DISGUISED_CHAT -> disguisedChat(event);
            case CHAT_MESSAGE -> playerChat(event);
            case ACTION_BAR -> actionBar(event);
            case SET_TITLE_TEXT -> titleText(event);
            case SET_TITLE_SUBTITLE -> subtitleText(event);
            case PLAYER_LIST_HEADER_AND_FOOTER -> headerFooter(event);
            case PLAYER_INFO_UPDATE -> playerInfo(event);
            case BOSS_BAR -> bossBar(event);
            case SCOREBOARD_OBJECTIVE -> objective(event);
            case TEAMS -> teams(event);
            case UPDATE_SCORE -> score(event);
            case OPEN_WINDOW -> openWindow(event);
            case SET_SLOT -> setSlot(event);
            case WINDOW_ITEMS -> windowItems(event);
            case ENTITY_METADATA -> entityMetadata(event);
            case DISCONNECT -> disconnect(event);
            default -> false;
        };
    }

    // ------------------------------------------------------------------ chat

    private boolean systemChat(PacketSendEvent event) {
        WrapperPlayServerSystemChatMessage w = new WrapperPlayServerSystemChatMessage(event);
        Component message = w.getMessage();
        Component shaped = shaper.shape(message);
        if (shaped == message) return false;
        w.setMessage(shaped);
        return true;
    }

    private boolean disguisedChat(PacketSendEvent event) {
        WrapperPlayServerDisguisedChat w = new WrapperPlayServerDisguisedChat(event);
        Component message = w.getMessage();
        Component shaped = shaper.shape(message);
        if (shaped == message) return false;
        w.setMessage(shaped);
        return true;
    }

    private boolean playerChat(PacketSendEvent event) {
        WrapperPlayServerChatMessage w = new WrapperPlayServerChatMessage(event);
        ChatMessage message = w.getMessage();
        boolean changed = false;

        if (message instanceof ChatMessage_v1_19_3 signed) {
            // 1.19.3+: the plain text is what Geyser shows unless unsigned content is attached
            String plain = signed.getPlainContent();
            String shapedPlain = plain == null ? null : shaper.shape(plain);
            if (shapedPlain != null && !shapedPlain.equals(plain)) {
                signed.setPlainContent(shapedPlain);
                changed = true;
            }
            Optional<Component> unsigned = signed.getUnsignedChatContent();
            if (unsigned.isPresent()) {
                Component shaped = shaper.shape(unsigned.get());
                if (shaped != unsigned.get()) {
                    signed.setUnsignedChatContent(shaped);
                    changed = true;
                }
            }
        } else {
            Component content = message.getChatContent();
            Component shaped = shaper.shape(content);
            if (shaped != content) {
                message.setChatContent(shaped);
                changed = true;
            }
        }
        if (changed) w.setMessage(message);
        return changed;
    }

    // ------------------------------------------------------------------ titles, action bar

    private boolean actionBar(PacketSendEvent event) {
        WrapperPlayServerActionBar w = new WrapperPlayServerActionBar(event);
        Component text = w.getActionBarText();
        Component shaped = shaper.shape(text);
        if (shaped == text) return false;
        w.setActionBarText(shaped);
        return true;
    }

    private boolean titleText(PacketSendEvent event) {
        WrapperPlayServerSetTitleText w = new WrapperPlayServerSetTitleText(event);
        Component title = w.getTitle();
        Component shaped = shaper.shape(title);
        if (shaped == title) return false;
        w.setTitle(shaped);
        return true;
    }

    private boolean subtitleText(PacketSendEvent event) {
        WrapperPlayServerSetTitleSubtitle w = new WrapperPlayServerSetTitleSubtitle(event);
        Component subtitle = w.getSubtitle();
        Component shaped = shaper.shape(subtitle);
        if (shaped == subtitle) return false;
        w.setSubtitle(shaped);
        return true;
    }

    // ------------------------------------------------------------------ tab list

    private boolean headerFooter(PacketSendEvent event) {
        WrapperPlayServerPlayerListHeaderAndFooter w = new WrapperPlayServerPlayerListHeaderAndFooter(event);
        boolean changed = false;
        Component header = w.getHeader();
        Component shapedHeader = shaper.shape(header);
        if (shapedHeader != header) {
            w.setHeader(shapedHeader);
            changed = true;
        }
        Component footer = w.getFooter();
        Component shapedFooter = shaper.shape(footer);
        if (shapedFooter != footer) {
            w.setFooter(shapedFooter);
            changed = true;
        }
        return changed;
    }

    private boolean playerInfo(PacketSendEvent event) {
        WrapperPlayServerPlayerInfoUpdate w = new WrapperPlayServerPlayerInfoUpdate(event);
        boolean changed = false;
        for (WrapperPlayServerPlayerInfoUpdate.PlayerInfo info : w.getEntries()) {
            Component name = info.getDisplayName();
            if (name == null) continue;
            Component shaped = shaper.shape(name);
            if (shaped != name) {
                info.setDisplayName(shaped);
                changed = true;
            }
        }
        return changed;
    }

    // ------------------------------------------------------------------ boss bars, scoreboards

    private boolean bossBar(PacketSendEvent event) {
        WrapperPlayServerBossBar w = new WrapperPlayServerBossBar(event);
        Component title = w.getTitle();
        if (title == null) return false;
        Component shaped = shaper.shape(title);
        if (shaped == title) return false;
        w.setTitle(shaped);
        return true;
    }

    private boolean objective(PacketSendEvent event) {
        WrapperPlayServerScoreboardObjective w = new WrapperPlayServerScoreboardObjective(event);
        Component name = w.getDisplayName();
        if (name == null) return false;
        Component shaped = shaper.shape(name);
        if (shaped == name) return false;
        w.setDisplayName(shaped);
        return true;
    }

    private boolean teams(PacketSendEvent event) {
        WrapperPlayServerTeams w = new WrapperPlayServerTeams(event);
        Optional<WrapperPlayServerTeams.ScoreBoardTeamInfo> optional = w.getTeamInfo();
        if (optional.isEmpty()) return false;
        WrapperPlayServerTeams.ScoreBoardTeamInfo info = optional.get();
        boolean changed = false;

        Component displayName = info.getDisplayName();
        Component shapedName = shaper.shape(displayName);
        if (shapedName != displayName) {
            info.setDisplayName(shapedName);
            changed = true;
        }
        Component prefix = info.getPrefix();
        Component shapedPrefix = shaper.shape(prefix);
        if (shapedPrefix != prefix) {
            info.setPrefix(shapedPrefix);
            changed = true;
        }
        Component suffix = info.getSuffix();
        Component shapedSuffix = shaper.shape(suffix);
        if (shapedSuffix != suffix) {
            info.setSuffix(shapedSuffix);
            changed = true;
        }
        if (changed) w.setTeamInfo(info);
        return changed;
    }

    private boolean score(PacketSendEvent event) {
        WrapperPlayServerUpdateScore w = new WrapperPlayServerUpdateScore(event);
        Component name = w.getEntityDisplayName();
        if (name == null) return false;
        Component shaped = shaper.shape(name);
        if (shaped == name) return false;
        w.setEntityDisplayName(shaped);
        return true;
    }

    // ------------------------------------------------------------------ inventories

    private boolean openWindow(PacketSendEvent event) {
        WrapperPlayServerOpenWindow w = new WrapperPlayServerOpenWindow(event);
        Component title = w.getTitle();
        Component shaped = shaper.shape(title);
        if (shaped == title) return false;
        w.setTitle(shaped);
        return true;
    }

    private boolean setSlot(PacketSendEvent event) {
        WrapperPlayServerSetSlot w = new WrapperPlayServerSetSlot(event);
        ItemStack item = w.getItem();
        if (!shapeItem(item)) return false;
        w.setItem(item);
        return true;
    }

    private boolean windowItems(PacketSendEvent event) {
        WrapperPlayServerWindowItems w = new WrapperPlayServerWindowItems(event);
        List<ItemStack> items = w.getItems();
        boolean changed = false;
        for (ItemStack item : items) {
            changed |= shapeItem(item);
        }
        if (changed) w.setItems(items);
        return changed;
    }

    /** Custom name, item name and lore. Item components exist on 1.20.5+; older versions are left alone. */
    private boolean shapeItem(ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasComponentPatches()) return false;
        boolean changed = false;

        Optional<Component> customName = item.getComponent(ComponentTypes.CUSTOM_NAME);
        if (customName.isPresent()) {
            Component shaped = shaper.shape(customName.get());
            if (shaped != customName.get()) {
                item.setComponent(ComponentTypes.CUSTOM_NAME, shaped);
                changed = true;
            }
        }

        Optional<Component> itemName = item.getComponent(ComponentTypes.ITEM_NAME);
        if (itemName.isPresent()) {
            Component shaped = shaper.shape(itemName.get());
            if (shaped != itemName.get()) {
                item.setComponent(ComponentTypes.ITEM_NAME, shaped);
                changed = true;
            }
        }

        Optional<ItemLore> lore = item.getComponent(ComponentTypes.LORE);
        if (lore.isPresent()) {
            List<Component> lines = lore.get().getLines();
            List<Component> shapedLines = new ArrayList<>(lines.size());
            boolean loreChanged = false;
            for (Component line : lines) {
                Component shaped = shaper.shape(line);
                loreChanged |= shaped != line;
                shapedLines.add(shaped);
            }
            if (loreChanged) {
                item.setComponent(ComponentTypes.LORE, new ItemLore(shapedLines));
                changed = true;
            }
        }
        return changed;
    }

    // ------------------------------------------------------------------ entities, kick screen

    @SuppressWarnings("unchecked")
    private boolean entityMetadata(PacketSendEvent event) {
        WrapperPlayServerEntityMetadata w = new WrapperPlayServerEntityMetadata(event);
        boolean changed = false;
        for (EntityData<?> data : w.getEntityMetadata()) {
            Object value = data.getValue();
            if (value instanceof Component component) {
                // plain component fields, e.g. the text of a text display
                Component shaped = shaper.shape(component);
                if (shaped != component) {
                    ((EntityData<Object>) data).setValue(shaped);
                    changed = true;
                }
            } else if (value instanceof Optional<?> optional
                    && optional.isPresent() && optional.get() instanceof Component component) {
                // optional component fields, e.g. the custom name shown above entities
                Component shaped = shaper.shape(component);
                if (shaped != component) {
                    ((EntityData<Object>) data).setValue(Optional.of(shaped));
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean disconnect(PacketSendEvent event) {
        WrapperPlayServerDisconnect w = new WrapperPlayServerDisconnect(event);
        Component reason = w.getReason();
        Component shaped = shaper.shape(reason);
        if (shaped == reason) return false;
        w.setReason(shaped);
        return true;
    }
}
