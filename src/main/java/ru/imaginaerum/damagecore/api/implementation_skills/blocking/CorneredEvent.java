package ru.imaginaerum.damagecore.api.implementation_skills.blocking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.imaginaerum.damagecore.api.damage_book_protection.SkillTreeServerHandler;

@Mod.EventBusSubscriber
public class CorneredEvent {

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!SkillTreeServerHandler.isNodeLearned(player, "cornered")) return;

        ItemStack shield = player.getUseItem();
        if (shield.isEmpty() || !(shield.getItem() instanceof ShieldItem)) return;

        // Запоминаем состояние щита ДО того, как Forge нанесёт урон
        int damageBeforeBlock = shield.getDamageValue();
        int maxDamage = shield.getMaxDamage();

        // Запускаем проверку на следующем тике — после того, как Forge реально сломает щит
        player.getServer().tell(new net.minecraft.server.TickTask(
                player.getServer().getTickCount() + 1,
                () -> {
                    // Игрок уже онлайн?
                    if (!player.isAlive()) return;

                    ItemStack currentItem = player.getUseItem();

                    boolean shieldBroke =
                            // Щит пропал из руки (сломался и исчез)
                            currentItem.isEmpty()
                                    // Или durability достиг максимума (сломан, но ещё в инвентаре)
                                    || (currentItem.getItem() instanceof ShieldItem
                                    && currentItem.getDamageValue() >= maxDamage)
                                    // Или до блока оставалось мало прочности и теперь щит другой
                                    || (damageBeforeBlock < maxDamage
                                    && !(currentItem.getItem() instanceof ShieldItem));

                    if (shieldBroke) {
                        player.addEffect(new MobEffectInstance(
                                MobEffects.DAMAGE_BOOST,
                                20 * 20,
                                1
                        ));
                    }
                }
        ));
    }
}