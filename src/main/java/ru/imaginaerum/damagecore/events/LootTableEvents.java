package ru.imaginaerum.damagecore.events;

import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.item.DCItems;

@Mod.EventBusSubscriber(modid = "damagecore", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LootTableEvents {
    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation id = event.getName();
        if (id == null) return;

        // ТОЛЬКО birch_leaves
        if (!id.equals(new ResourceLocation("minecraft", "blocks/birch_leaves"))) return;

        LootPool.Builder pool = LootPool.lootPool()
                .name("damagecore_extra_birch_leaf")
                .setRolls(ConstantValue.exactly(1))
                // не ножницы и не silk touch
                .when(InvertedLootItemCondition.invert(
                        MatchTool.toolMatches(
                                ItemPredicate.Builder.item()
                                        .of(Items.SHEARS)
                                        .hasEnchantment(
                                                new EnchantmentPredicate(
                                                        Enchantments.SILK_TOUCH,
                                                        MinMaxBounds.Ints.atLeast(1)
                                                )
                                        )
                        )
                ))
                // шанс с Fortune
                .add(LootItem.lootTableItem(DCItems.BIRCH_LEAF.get())
                        .when(BonusLevelTableCondition.bonusLevelFlatChance(
                                Enchantments.BLOCK_FORTUNE,
                                0.20f, // Fortune 0
                                0.25f, // I
                                0.30f, // II
                                0.40f, // III
                                0.50f  // IV+
                        ))
                );

        event.getTable().addPool(pool.build());
    }


}
