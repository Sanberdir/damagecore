package ru.imaginaerum.damagecore.item.custom;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import ru.imaginaerum.damagecore.DamageCore;
import ru.imaginaerum.damagecore.effect.DCEffects;
import ru.imaginaerum.damagecore.item.DCItems;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Arrays;
import java.util.List;

public class ScarletStaplerRing extends Item implements ICurioItem {

    private static final String TAG_HUNGRY = "hungry";
    private static final String TAG_CHARGES = "charges";
    private static final String TAG_COOLDOWN = "cooldown"; // Для задержки между снятиями
    private static final int DAMAGE_AMOUNT = 10;
    public static final int MAX_CHARGES = 10; // Максимальное количество зарядов
    private static final int COOLDOWN_TICKS = 100; // 2 секунды задержки между снятиями (20 тиков = 1 секунда)

    // Список эффектов кровотечения, которые нужно снимать
    private static final List<MobEffect> BLEEDING_EFFECTS = Arrays.asList(
            DCEffects.BLEEDING_1.get(),
            DCEffects.BLEEDING_2.get(),
            DCEffects.BLEEDING_3.get()
    );

    public ScarletStaplerRing(Properties properties) {
        super(properties);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        // Устанавливаем начальные значения при крафте
        setHungry(stack, true);
        setCharges(stack, 0);
        setCooldown(stack, 0);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        setHungry(stack, true);
        setCharges(stack, 0);
        setCooldown(stack, 0);
        return stack;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals("ring") &&
                ICurioItem.super.canEquip(slotContext, stack);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canSync(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        LivingEntity entity = slotContext.entity();

        if (!entity.level().isClientSide()) {
            // Инициализируем NBT при экипировке, если его нет
            if (!stack.hasTag() || !stack.getTag().contains(TAG_HUNGRY)) {
                setHungry(stack, true);
            }
            if (!stack.hasTag() || !stack.getTag().contains(TAG_CHARGES)) {
                setCharges(stack, 0);
            }
            if (!stack.hasTag() || !stack.getTag().contains(TAG_COOLDOWN)) {
                setCooldown(stack, 0);
            }

            // Если кольцо голодное (true) - наносим урон, насыщаем и даем 10 зарядов
            if (isHungry(stack)) {
                applyRingEffect(entity, stack);
            }
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        ICurioItem.super.onUnequip(slotContext, newStack, stack);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        Level level = entity.level();

        if (!level.isClientSide()) {
            // Проверяем наличие NBT и инициализируем если нужно
            if (!stack.hasTag() || !stack.getTag().contains(TAG_HUNGRY)) {
                setHungry(stack, true);
            }
            if (!stack.hasTag() || !stack.getTag().contains(TAG_CHARGES)) {
                setCharges(stack, 0);
            }
            if (!stack.hasTag() || !stack.getTag().contains(TAG_COOLDOWN)) {
                setCooldown(stack, 0);
            }

            // Если кольцо сытое (false) и есть заряды - снимаем кровотечения и тратим заряды
            if (!isHungry(stack) && getCharges(stack) > 0) {
                // Уменьшаем кулдаун каждый тик
                int currentCooldown = getCooldown(stack);
                if (currentCooldown > 0) {
                    setCooldown(stack, currentCooldown - 1);
                }

                // Снимаем эффекты кровотечения только если кулдаун прошел
                if (currentCooldown <= 0) {
                    boolean removedBleeding = removeBleedingEffects(entity, stack);

                    // Если сняли кровотечение - тратим заряд и устанавливаем кулдаун
                    if (removedBleeding) {
                        consumeCharge(stack, entity);
                        setCooldown(stack, COOLDOWN_TICKS); // Устанавливаем задержку
                    }
                }

                // Также проверяем активное кровотечение и тратим заряды со временем
                // (но только если нет кулдауна)
                if (getCooldown(stack) <= 0) {
                    checkActiveBleeding(entity, stack);
                }
            }
        }
    }

    /**
     * Снимает эффекты кровотечения с игрока
     * Возвращает true, если был снят хотя бы один эффект
     */
    private boolean removeBleedingEffects(LivingEntity entity, ItemStack stack) {
        boolean removed = false;
        for (MobEffect effect : BLEEDING_EFFECTS) {
            if (entity.hasEffect(effect)) {
                entity.removeEffect(effect);
                removed = true;

                // Визуальный эффект при снятии кровотечения
                spawnCleanseParticles(entity);

                // Сообщение при снятии сильного кровотечения
                if (effect == DCEffects.BLEEDING_3.get() && entity instanceof Player player) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§aКольцо остановило сильное кровотечение!"),
                            true
                    );
                }
                break; // Снимаем только один эффект за раз
            }
        }
        return removed;
    }

    /**
     * Проверяет активное кровотечение и тратит заряды
     */
    private void checkActiveBleeding(LivingEntity entity, ItemStack stack) {
        // Если у игрока есть эффект кровотечения, тратим заряд каждые 4 секунды
        if (hasAnyBleedingEffect(entity) && entity.tickCount % 80 == 0) {
            // Только если есть заряды и прошла задержка
            if (getCooldown(stack) <= 0) {
                consumeCharge(stack, entity);
                setCooldown(stack, COOLDOWN_TICKS);
            }
        }
    }

    /**
     * Проверяет, есть ли у игрока любой эффект кровотечения
     */
    private boolean hasAnyBleedingEffect(LivingEntity entity) {
        for (MobEffect effect : BLEEDING_EFFECTS) {
            if (entity.hasEffect(effect)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Применяет эффект кольца: наносит урон, насыщает и дает 10 зарядов
     */
    private void applyRingEffect(LivingEntity entity, ItemStack stack) {
        if (entity.isAlive()) {
            // Наносим урон
            entity.hurt(entity.damageSources().magic(), DAMAGE_AMOUNT);
            setHungry(stack, false);
            setCharges(stack, MAX_CHARGES);
            setCooldown(stack, 0); // Сбрасываем кулдаун при насыщении
            spawnParticles(entity);

            if (entity instanceof Player player) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§cКольцо забрало 10 жизней и насытилось!"),
                        true
                );
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§aПолучено §e10 §aзарядов для подавления кровотечения"),
                        true
                );
            }
        }
    }

    /**
     * Тратит один заряд кольца
     */
    private void consumeCharge(ItemStack stack, LivingEntity entity) {
        int currentCharges = getCharges(stack);

        if (currentCharges > 0) {
            int newCharges = currentCharges - 1;
            setCharges(stack, newCharges);

            // Если заряды на исходе, предупреждаем игрока
            if (newCharges <= 3 && newCharges > 0 && entity instanceof Player player) {
                if (entity.tickCount % 80 == 0) { // Каждые 4 секунды
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§6Заряды кольца на исходе: §e" + newCharges),
                            true
                    );
                }
            }

            // Если заряды закончились - кольцо снова голодное
            if (newCharges <= 0) {
                setHungry(stack, true);
                setCooldown(stack, 0); // Сбрасываем кулдаун

                // Визуальный эффект опустошения
                spawnEmptyParticles(entity);

                // Сообщение игроку
                if (entity instanceof Player player) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§cКольцо проголодалось! Наденьте снова, чтобы насытить."),
                            true
                    );
                }
            } else {
                // Сообщение о расходе заряда (только иногда, чтобы не спамить)
                if (entity instanceof Player player && entity.tickCount % 40 == 0) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§7Кольцо израсходовало заряд. Осталось: §e" + newCharges),
                            true
                    );
                }
            }
        }
    }

    /**
     * Создает частицы для визуального эффекта насыщения
     */
    private void spawnParticles(LivingEntity entity) {
        Level level = entity.level();
        if (!level.isClientSide()) return;

        for (int i = 0; i < 30; i++) {
            double x = entity.getX() + (level.random.nextDouble() - 0.5) * 2.0;
            double y = entity.getY() + level.random.nextDouble() * 2.0;
            double z = entity.getZ() + (level.random.nextDouble() - 0.5) * 2.0;

            level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR,
                    x, y, z,
                    0, 0.1, 0
            );

            // Красные частицы крови
            level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                    x, y, z,
                    0, 0.05, 0
            );
        }
    }

    /**
     * Создает частицы для эффекта очистки кровотечения
     */
    private void spawnCleanseParticles(LivingEntity entity) {
        Level level = entity.level();
        if (!level.isClientSide()) return;

        for (int i = 0; i < 15; i++) {
            double x = entity.getX() + (level.random.nextDouble() - 0.5) * 1.5;
            double y = entity.getY() + level.random.nextDouble() * 1.5;
            double z = entity.getZ() + (level.random.nextDouble() - 0.5) * 1.5;

            // Золотые/исцеляющие частицы
            level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    x, y, z,
                    0, 0.05, 0
            );

            // Белые частицы очищения
            if (i % 3 == 0) {
                level.addParticle(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        x, y, z,
                        (level.random.nextDouble() - 0.5) * 0.05,
                        0.05,
                        (level.random.nextDouble() - 0.5) * 0.05
                );
            }
        }
    }

    /**
     * Создает частицы для эффекта опустошения
     */
    private void spawnEmptyParticles(LivingEntity entity) {
        Level level = entity.level();
        if (!level.isClientSide()) return;

        for (int i = 0; i < 40; i++) {
            double x = entity.getX() + (level.random.nextDouble() - 0.5) * 2.5;
            double y = entity.getY() + level.random.nextDouble() * 2.5;
            double z = entity.getZ() + (level.random.nextDouble() - 0.5) * 2.5;

            level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.SMOKE,
                    x, y, z,
                    (level.random.nextDouble() - 0.5) * 0.05,
                    0.05,
                    (level.random.nextDouble() - 0.5) * 0.05
            );

            level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.ASH,
                    x, y, z,
                    0, 0.02, 0
            );
        }
    }

    /* =========================
       NBT методы для hungry
       ========================= */

    public static boolean isHungry(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_HUNGRY)) {
            setHungry(stack, true);
            return true;
        }
        return stack.getTag().getBoolean(TAG_HUNGRY);
    }

    public static void setHungry(ItemStack stack, boolean value) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(TAG_HUNGRY, value);
        stack.setTag(tag);
    }

    /* =========================
       NBT методы для charges
       ========================= */

    public static int getCharges(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_CHARGES)) {
            setCharges(stack, 0);
            return 0;
        }
        return stack.getTag().getInt(TAG_CHARGES);
    }

    public static void setCharges(ItemStack stack, int charges) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_CHARGES, Math.max(0, Math.min(charges, MAX_CHARGES)));
        stack.setTag(tag);
    }

    /* =========================
       NBT методы для cooldown
       ========================= */

    public static int getCooldown(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG_COOLDOWN)) {
            setCooldown(stack, 0);
            return 0;
        }
        return stack.getTag().getInt(TAG_COOLDOWN);
    }

    public static void setCooldown(ItemStack stack, int cooldown) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_COOLDOWN, Math.max(0, cooldown));
        stack.setTag(tag);
    }

    /* =========================
       Методы для отображения информации
       ========================= */

    @Override
    public void appendHoverText(ItemStack stack, Level level, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        if (isHungry(stack)) {
            tooltip.add(net.minecraft.network.chat.Component.literal("§cГолодное"));
            tooltip.add(net.minecraft.network.chat.Component.literal("§7Заберет 10 HP при надевании"));
            tooltip.add(net.minecraft.network.chat.Component.literal("§7Даст 10 зарядов для подавления кровотечения"));
        } else {
            tooltip.add(net.minecraft.network.chat.Component.literal("§aСытое"));
            int charges = getCharges(stack);
            int cooldown = getCooldown(stack);

            tooltip.add(net.minecraft.network.chat.Component.literal(
                    String.format("§7Заряды: §e%d§7/§a%d", charges, MAX_CHARGES)
            ));

            // Показываем время до следующего снятия
            if (cooldown > 0) {
                int seconds = (cooldown + 19) / 20; // Округление вверх
                tooltip.add(net.minecraft.network.chat.Component.literal(
                        String.format("§7До следующего снятия: §e%d§7 сек", seconds)
                ));
            } else {
                tooltip.add(net.minecraft.network.chat.Component.literal("§7Готово к снятию кровотечения"));
            }

            tooltip.add(net.minecraft.network.chat.Component.literal("§7Снимает эффекты кровотечения"));
            tooltip.add(net.minecraft.network.chat.Component.literal("§7Тратит заряды при снятии кровотечения"));
            tooltip.add(net.minecraft.network.chat.Component.literal("§7Задержка между снятиями: §e5 секунд"));

            // Прогресс-бар в тексте
            StringBuilder progressBar = new StringBuilder("§7[");
            int filled = (charges * 10) / MAX_CHARGES;
            for (int i = 0; i < 10; i++) {
                if (i < filled) {
                    progressBar.append("§a█");
                } else {
                    progressBar.append("§8░");
                }
            }
            progressBar.append("§7]");
            tooltip.add(net.minecraft.network.chat.Component.literal(progressBar.toString()));
        }

        tooltip.add(net.minecraft.network.chat.Component.literal("§8Кольцо жажды крови"));
        tooltip.add(net.minecraft.network.chat.Component.literal("§7Насыщается кровью владельца"));
        tooltip.add(net.minecraft.network.chat.Component.literal("§7Использует энергию для подавления кровотечения"));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        // Показываем полосу зарядов, если кольцо сытое
        return !isHungry(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (isHungry(stack)) {
            return 0;
        }
        float chargePercent = (float) getCharges(stack) / MAX_CHARGES;
        return Math.round(13.0F * chargePercent);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // Цвет полосы: от зеленого (полный) к желтому (пустой)
        if (isHungry(stack)) {
            return 0xFF0000; // Красный для голодного
        }
        float chargePercent = (float) getCharges(stack) / MAX_CHARGES;

        if (chargePercent > 0.5f) {
            // Зеленый для высокого уровня зарядов
            int g = 255;
            int r = (int) (255 * (1 - (chargePercent - 0.5f) * 2));
            return (r << 16) | (g << 8);
        } else {
            // Желтый/оранжевый для низкого уровня зарядов
            int r = 255;
            int g = (int) (255 * chargePercent * 2);
            return (r << 16) | (g << 8);
        }
    }

    /* =========================
       Client-only model property
       ========================= */

    @Mod.EventBusSubscriber(modid = DamageCore.MODID,value = Dist.CLIENT,bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Client {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                ItemProperties.register(
                        DCItems.SCARLET_STAPLER_RING.get(),
                        new ResourceLocation(DamageCore.MODID, "hungry"),
                        (stack, level, entity, seed) -> {
                            return isHungry(stack) ? 1.0F : 0.0F;
                        }
                );

                // Дополнительное свойство для отображения уровня зарядов
                ItemProperties.register(
                        DCItems.SCARLET_STAPLER_RING.get(),
                        new ResourceLocation(DamageCore.MODID, "charge_level"),
                        (stack, level, entity, seed) -> {
                            if (isHungry(stack)) {
                                return 0.0F;
                            }
                            return (float) getCharges(stack) / MAX_CHARGES;
                        }
                );
            });
        }
    }
}