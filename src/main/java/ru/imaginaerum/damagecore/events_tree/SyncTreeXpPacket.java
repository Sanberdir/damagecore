package ru.imaginaerum.damagecore.events_tree;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import ru.imaginaerum.damagecore.api.damage_book_protection.DamageBookRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncTreeXpPacket {
    public final Map<Integer, Integer> treeXp;
    public final Map<Integer, Integer> treeLevel;

    public SyncTreeXpPacket(Map<Integer, Integer> treeXp, Map<Integer, Integer> treeLevel) {
        this.treeXp = treeXp != null ? new HashMap<>(treeXp) : new HashMap<>();
        this.treeLevel = treeLevel != null ? new HashMap<>(treeLevel) : new HashMap<>();
    }

    // Удобный конструктор для одного дерева (если нужно)
    public SyncTreeXpPacket(int singleTreeId, int singleXp) {
        Map<Integer,Integer> xp = new HashMap<>();
        xp.put(singleTreeId, singleXp);
        this.treeXp = xp;
        this.treeLevel = new HashMap<>();
    }

    public static SyncTreeXpPacket single(int treeId, int xp) {
        return new SyncTreeXpPacket(treeId, xp);
    }

    public static void encode(SyncTreeXpPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.treeXp.size());
        for (var entry : pkt.treeXp.entrySet()) {
            buf.writeInt(entry.getKey());
            buf.writeInt(entry.getValue());
        }

        buf.writeInt(pkt.treeLevel.size());
        for (var entry : pkt.treeLevel.entrySet()) {
            buf.writeInt(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    public static SyncTreeXpPacket decode(FriendlyByteBuf buf) {
        Map<Integer, Integer> xp = new HashMap<>();
        Map<Integer, Integer> level = new HashMap<>();

        int xpSize = buf.readInt();
        for (int i = 0; i < xpSize; i++) {
            xp.put(buf.readInt(), buf.readInt());
        }

        int levelSize = buf.readInt();
        for (int i = 0; i < levelSize; i++) {
            level.put(buf.readInt(), buf.readInt());
        }

        return new SyncTreeXpPacket(xp, level);
    }

    public static void handle(SyncTreeXpPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            try {
                // применяем карты в рендер
                DamageBookRenderer.clearXpData();
                for (var entry : pkt.treeXp.entrySet()) DamageBookRenderer.setXp(entry.getKey(), entry.getValue());
                for (var entry : pkt.treeLevel.entrySet()) DamageBookRenderer.setLevel(entry.getKey(), entry.getValue());

                System.out.println("[SyncTreeXpPacket] client applied: xp=" + pkt.treeXp + " level=" + pkt.treeLevel);

                // попробуем вызвать рефлексивно метод обновления экрана (если он есть)
                Object screen = DamageBookRenderer.currentScreen;
                boolean updated = false;
                if (screen != null) {
                    try {
                        var m = screen.getClass().getMethod("recalculateProgress");
                        m.invoke(screen);
                        System.out.println("[SyncTreeXpPacket] invoked recalculateProgress() on currentScreen");
                        updated = true;
                    } catch (NoSuchMethodException ns) {
                        // может не быть — нормально
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    // как запасной вариант — метод может называться updateNodes(), попытка:
                    try {
                        var m2 = screen.getClass().getMethod("updateNodes");
                        m2.invoke(screen);
                        System.out.println("[SyncTreeXpPacket] invoked updateNodes() on currentScreen");
                        updated = true;
                    } catch (NoSuchMethodException ignored) {}
                    catch (Throwable t) { t.printStackTrace(); }
                }
                DamageBookRenderer.forceRefresh();
                // Если не удалось обновить через методы экрана — принудительно перерисуем/реинициализируем экран:
                if (!updated) {
                    try {
                        Screen s = (Screen) DamageBookRenderer.currentScreen;
                        if (s != null) {
                            Minecraft mc = Minecraft.getInstance();
                            System.out.println("[SyncTreeXpPacket] forcing screen refresh (close+reopen) to apply XP visuals");
                            // Закроем и снова поставим тот же объект: это вызовет init()/рендер заново.
                            mc.setScreen(null);
                            mc.setScreen(s);
                        }
                    } catch (Throwable t) {
                        // любое исключение — логируем, но не ломаем клиент
                        t.printStackTrace();
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
        ctx.setPacketHandled(true);
    }
}