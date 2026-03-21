package ru.imaginaerum.damagecore.api.implementation_skills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "damagecore")
public class SwiftBrewingHandler {

    private static final String NODE_ID = "swift_brewing";

    /**
     * Каждый тик сервера пробегаем игроков с открытыми менюми.
     * Если игрок изучил swift_brewing — уменьшаем brewTicks на 2 (доп.),
     * тем самым давая примерно 3x скорость варки (1 обычный тик + 2 доп.).
     *
     * Также используем набор processedContainers, чтобы не обрабатывать одну и ту же
     * стойку несколько раз в одном тике (когда несколько игроков открыли одно и то же меню).
     */
    @SubscribeEvent
    public static void onBrewingStandTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Set<Integer> processedContainers = new HashSet<>();

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!(player.containerMenu instanceof BrewingStandMenu brewingMenu)) continue;

            // Попытка получить уникальный ключ underlying container чтобы не обрабатывать одну стойку несколько раз
            int containerId = getBrewingStandContainerId(brewingMenu);
            if (containerId != 0 && processedContainers.contains(containerId)) continue;
            if (containerId != 0) processedContainers.add(containerId);

            int treeId = findAlchemyTreeId(player);
            if (treeId == -1) continue;

            float progress = SkillTreeServerHandler.getNodeProgress(player, treeId, NODE_ID);
            if (progress <= 0f) continue; // нода не изучена

            // Получаем текущее значение brewTicks через public метод (если есть)
            int brewTicks;
            try {
                brewTicks = brewingMenu.getBrewingTicks();
            } catch (Throwable t) {
                // Если по какой-то причине метода нет — пропускаем
                continue;
            }

            if (brewTicks > 0) {
                // Уменьшаем таймер на 2 тика (дополнительно к обычному уменьшению)
                int newTicks = brewTicks - 2;
                if (newTicks < 1) newTicks = 1; // минимум 1 тик — чтобы не завершить мгновенно
                setBrewingTicks(brewingMenu, newTicks);
            }
        }
    }

    // Ищем ID дерева, где лежит swift_brewing
    private static int findAlchemyTreeId(ServerPlayer player) {
        try {
            var trees = SkillTreeServerHandler.getTreesMap();
            if (trees == null) return -1;

            for (var entry : trees.entrySet()) {
                int id = entry.getKey();
                var nodes = SkillTreeServerHandler.getNodesMap(entry.getValue());
                if (nodes != null && nodes.containsKey(NODE_ID)) return id;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return -1;
    }

    // Устанавливаем brewTicks через доступ к ContainerData (рефлексия)
    private static void setBrewingTicks(BrewingStandMenu menu, int ticks) {
        try {
            Field dataField = BrewingStandMenu.class.getDeclaredField("brewingStandData");
            dataField.setAccessible(true);
            ContainerData data = (ContainerData) dataField.get(menu);
            data.set(0, ticks); // индекс 0 = brew ticks
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Получаем уникальный id underlying container (brewingStand) чтобы избежать двойной обработки
    private static int getBrewingStandContainerId(BrewingStandMenu menu) {
        try {
            Field field = BrewingStandMenu.class.getDeclaredField("brewingStand");
            field.setAccessible(true);
            Object container = field.get(menu);
            return System.identityHashCode(container);
        } catch (Exception e) {
            // если не удалось — вернём 0 (не будем дедупиться)
            return 0;
        }
    }
}