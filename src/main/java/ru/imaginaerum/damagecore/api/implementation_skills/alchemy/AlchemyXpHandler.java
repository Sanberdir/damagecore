package ru.imaginaerum.damagecore.api.implementation_skills.alchemy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.brewing.PlayerBrewedPotionEvent;
import net.minecraftforge.event.brewing.PotionBrewEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "damagecore")
public class AlchemyXpHandler {

    private static final String NODE_ID = "alchemy_experience";
    private static final String ROOT_KEY = "damagecore_skilltree";
    private static final String NODE_LEVEL_PREFIX = "node_level_";
    private static final int XP_REWARD = 5;
    public static void tryGiveXp(ServerPlayer player) {
        if (!brewed) return;

        if (!hasNode(player)) return;

        player.giveExperiencePoints(XP_REWARD);

        brewed = false;
    }
    // запоминаем предыдущие зелья
    private static List<ItemStack> lastPotions = new ArrayList<>();

    // был ли реальный крафт
    private static boolean brewed = false;

    // 1. ДО варки
    @SubscribeEvent
    public static void onBrewPre(PotionBrewEvent.Pre event) {
        lastPotions.clear();

        for (int i = 0; i < 3; i++) {
            lastPotions.add(event.getItem(i).copy());
        }

        brewed = false;
    }

    // 2. ПОСЛЕ варки
    @SubscribeEvent
    public static void onBrewPost(PotionBrewEvent.Post event) {
        for (int i = 0; i < 3; i++) {
            ItemStack before = lastPotions.get(i);
            ItemStack after = event.getItem(i);

            if (!ItemStack.isSameItemSameTags(before, after)) {
                brewed = true;
                return;
            }
        }
    }

    // 3. Игрок забрал
    @SubscribeEvent
    public static void onTake(PlayerBrewedPotionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (!brewed) return; // не было реального крафта

        ItemStack stack = event.getStack();
        if (stack.isEmpty()) return;

        if (PotionUtils.getMobEffects(stack).isEmpty()) return;

        if (!hasNode(player)) return;

        player.giveExperiencePoints(XP_REWARD);

        brewed = false; // сбрасываем
    }

    private static boolean hasNode(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag modTag = persisted.getCompound(ROOT_KEY);

        for (String key : modTag.getAllKeys()) {
            if (!key.startsWith("tree_")) continue;

            CompoundTag tree = modTag.getCompound(key);
            if (tree.contains(NODE_LEVEL_PREFIX + NODE_ID)) {
                if (tree.getInt(NODE_LEVEL_PREFIX + NODE_ID) > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}