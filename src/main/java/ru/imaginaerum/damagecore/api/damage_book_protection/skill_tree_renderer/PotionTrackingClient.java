package ru.imaginaerum.damagecore.api.damage_book_protection.skill_tree_renderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import ru.imaginaerum.damagecore.DamageCore;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Клиентский, чисто визуальный трекер: "каким зельем (или атакой моба) был наложен этот mob-эффект".
 * GUI-данные (вкладка эффектов), чтобы показывать иконку зелья/иконку моба вместо иконки эффекта.
 *
 * Привязка для DRINK точная (берётся прямо из предмета, который допил игрок,
 * ItemStack там приходит на клиент сразу полностью с NBT).
 *
 * Привязка для SPLASH/LINGERING устроена иначе и НЕ читает зелье один раз
 * в момент появления сущности: synced data (ItemStack у ThrownPotion,
 * Potion-id у AreaEffectCloud) на клиенте в момент EntityJoinLevelEvent
 * зачастую ещё пустая (Potions.EMPTY) — реальное содержимое прилетает только
 * следующим update-пакетом, на один-два тика позже. Поэтому вместо
 * одноразового чтения мы держим списки "наблюдаемых" ThrownPotion/AreaEffectCloud
 * по id сущности и перечитываем их getItem()/getPotion() КАЖДЫЙ ТИК, пока
 * либо не получим непустой результат (тогда регистрируем кандидата и
 * прекращаем наблюдение), либо не истечёт время наблюдения / сущность не исчезнет.
 *
 * Источник зелья (кто бросил) берётся из synced owner у ThrownPotion/AreaEffectCloud
 * (UUID владельца синхронизируется ванильно, отдельная сеть не нужна).
 *
 * Для эффектов, наложенных НЕ зельем-предметом (тиснутая стрела скелета,
 * удар визер-скелета, яд пещерного паука и т.п.) используется отдельный
 * механизм: ловим LivingHurtEvent по локальному игроку и запоминаем тип
 * атакующего на короткое окно — если следом появляется новый эффект без
 * зельного кандидата, приписываем его этому недавнему атакующему.
 *
 * ПЕРСИСТЕНТНОСТЬ: сами эффекты переживают релог (сервер хранит их в NBT игрока),
 * но "кто их наложил" — чисто клиентская live-информация, которую неоткуда
 * восстановить заново после релога (брошенная сущность уже не существует,
 * событие удара уже произошло в прошлой сессии). Поэтому при выходе из мира
 * текущее содержимое ACTIVE сохраняется в небольшой JSON-файл, а при следующем
 * входе — восстанавливается; устаревшие записи (для эффектов, которых у игрока
 * уже нет) автоматически вычищаются обычным pruneExpiredActiveEntries().
 */
@Mod.EventBusSubscriber(modid = DamageCore.MODID, value = Dist.CLIENT)
public final class PotionTrackingClient {

    private static final boolean DEBUG = true; // временно для диагностики, потом убрать

    private static final int CANDIDATE_LIFETIME_TICKS = 6 * 20;  // сколько кандидат живёт после успешного распознавания зелья
    private static final int WATCH_TIMEOUT_TICKS       = 3 * 20; // сколько тиков ждём, пока synced data зелья не перестанет быть EMPTY
    private static final int ATTACKER_LIFETIME_TICKS   = 2 * 20; // окно, в течение которого новый эффект приписывается недавнему атакующему

    /** Текущая привязка: какой mob-эффект каким зельем/атакой был дан (последний раз). */
    private static final Map<MobEffect, PotionEffectEntry> ACTIVE = new LinkedHashMap<>();

    /** Недавно распознанные зелья (брошенные/поставленные), ожидающие применения эффекта на игроке. */
    private static final List<Candidate> CANDIDATES = new ArrayList<>();

    /** ThrownPotion, чьё содержимое ещё не распознано (synced data была EMPTY) — перечитываем каждый тик. */
    private static final List<WatchedThrown> WATCHED_THROWN = new ArrayList<>();

    /** AreaEffectCloud, чьё содержимое ещё не распознано — перечитываем каждый тик. */
    private static final List<WatchedCloud> WATCHED_CLOUDS = new ArrayList<>();

    /** Недавние атакующие игрока — для приписывания эффектов от стрел/ударов мобов
     *  (визер-скелет: эффект Wither на ударе; обычный скелет: тиснутая стрела; и т.д.). */
    private static final List<RecentAttacker> RECENT_ATTACKERS = new ArrayList<>();

    /** Эффекты игрока на предыдущем тике — чтобы ловить момент появления новых. */
    private static List<MobEffectInstance> previousSnapshot = List.of();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PotionTrackingClient() {}

    // =========================================================================
    // Публичный API
    // =========================================================================

    /** Зелье/атака, наложившие данный эффект, либо null, если неизвестно (например ванильный /effect give). */
    public static PotionEffectEntry get(MobEffect effect) {
        return ACTIVE.get(effect);
    }

    // =========================================================================
    // DRINK — точная привязка, ItemStack приходит сразу полностью
    // =========================================================================

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player != Minecraft.getInstance().player) return;

        ItemStack stack = event.getItem();
        if (!isDrinkablePotion(stack)) return;

        List<MobEffectInstance> potionEffects = PotionUtils.getMobEffects(stack);
        if (potionEffects.isEmpty()) return;

        List<MobEffect> granted = potionEffects.stream().map(MobEffectInstance::getEffect).toList();
        PotionEffectEntry entry = new PotionEffectEntry(stack.copy(), PotionApplicationType.DRINK, granted);

        for (MobEffect effect : granted) {
            ACTIVE.put(effect, entry);
        }

        if (DEBUG) {
            System.out.println("[PotionDebug] DRINK registered ACTIVE for effects=" + granted);
        }
    }

    private static boolean isDrinkablePotion(ItemStack stack) {
        return stack.is(Items.POTION);
    }

    // =========================================================================
    // SPLASH / LINGERING — начинаем наблюдение при появлении сущности,
    // фактическое распознавание зелья происходит в onPlayerTick
    // =========================================================================

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) return;

        Entity entity = event.getEntity();

        if (entity instanceof ThrownPotion thrown) {
            WATCHED_THROWN.add(new WatchedThrown(thrown, WATCH_TIMEOUT_TICKS));
            if (DEBUG) {
                System.out.println("[PotionDebug] Start watching ThrownPotion id=" + thrown.getId());
            }
        } else if (entity instanceof AreaEffectCloud cloud) {
            WATCHED_CLOUDS.add(new WatchedCloud(cloud, WATCH_TIMEOUT_TICKS));
            if (DEBUG) {
                System.out.println("[PotionDebug] Start watching AreaEffectCloud id=" + cloud.getId());
            }
        }
    }

    // =========================================================================
    // MOB_ATTACK — атрибуция эффектов, наложенных без зелья-предмета
    // (тиснутая стрела, удар визер-скелета и т.п.)
    // =========================================================================

    /**
     * Ловим входящий урон по локальному игроку, чтобы знать "кто это сделал".
     * DamageSource.getEntity() — это "истинный" источник: для стрелы это стрелок,
     * для прямого удара — сам моб. Берём именно его, а не getDirectEntity()
     * (которая для стрелы вернула бы саму стрелу-снаряд).
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!event.getEntity().level().isClientSide()) return;
        if (event.getEntity() != Minecraft.getInstance().player) return;

        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;

        RECENT_ATTACKERS.add(new RecentAttacker(attacker.getType(), ATTACKER_LIFETIME_TICKS));

        if (DEBUG) {
            System.out.println("[PotionDebug] Recorded recent attacker type="
                    + BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()));
        }
    }

    private static void pruneRecentAttackers() {
        RECENT_ATTACKERS.removeIf(a -> --a.ticksLeft <= 0);
    }

    /** Самый свежий из недавних атакующих (последний добавленный). */
    private static EntityType<?> findRecentAttacker() {
        return RECENT_ATTACKERS.isEmpty()
                ? null
                : RECENT_ATTACKERS.get(RECENT_ATTACKERS.size() - 1).type;
    }

    // =========================================================================
    // ПЕРСИСТЕНТНОСТЬ: сохранение/восстановление ACTIVE при выходе/входе в мир
    // =========================================================================

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        saveToDisk();

        // Сущности из старого мира больше не валидны — очищаем все ссылки на них,
        // чтобы не держать "мёртвые" объекты и не словить исключения на новом тике.
        CANDIDATES.clear();
        WATCHED_THROWN.clear();
        WATCHED_CLOUDS.clear();
        RECENT_ATTACKERS.clear();
        previousSnapshot = List.of();
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        loadFromDisk();
    }

    private static Path getStorageFile() {
        Path dir = FMLPaths.GAMEDIR.get().resolve("config").resolve(DamageCore.MODID);
        return dir.resolve("potion_tracking_client.json");
    }

    private static void saveToDisk() {
        if (ACTIVE.isEmpty()) {
            // Нечего сохранять — но если файл с прошлого раза остался, его лучше затереть,
            // чтобы при следующем входе не подтянуть устаревшие записи.
            try {
                Files.deleteIfExists(getStorageFile());
            } catch (IOException ignored) {}
            return;
        }

        List<PersistedEntry> toSave = new ArrayList<>();

        for (var mapEntry : ACTIVE.entrySet()) {
            MobEffect effect = mapEntry.getKey();
            PotionEffectEntry entry = mapEntry.getValue();

            ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            if (effectId == null) continue;

            PersistedEntry p = new PersistedEntry();
            p.effectId = effectId.toString();
            p.applicationType = entry.getApplicationType().name();

            ItemStack stack = entry.getPotionStack();
            if (!stack.isEmpty()) {
                Potion potion = PotionUtils.getPotion(stack);
                if (potion != Potions.EMPTY) {
                    ResourceLocation potionId = BuiltInRegistries.POTION.getKey(potion);
                    if (potionId != null) p.potionId = potionId.toString();
                }
            }

            EntityType<?> sourceType = entry.getSourceEntityType();
            if (sourceType != null) {
                ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(sourceType);
                if (typeId != null) p.sourceEntityTypeId = typeId.toString();
            }

            toSave.add(p);
        }

        try {
            Path file = getStorageFile();
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(toSave, writer);
            }
            if (DEBUG) {
                System.out.println("[PotionDebug] Saved " + toSave.size() + " ACTIVE entries to " + file);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void loadFromDisk() {
        Path file = getStorageFile();
        if (!Files.exists(file)) return;

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            PersistedEntry[] loaded = GSON.fromJson(reader, PersistedEntry[].class);
            if (loaded == null) return;

            ACTIVE.clear();

            for (PersistedEntry p : loaded) {
                if (p.effectId == null || p.applicationType == null) continue;

                MobEffect effect = BuiltInRegistries.MOB_EFFECT
                        .getOptional(ResourceLocation.tryParse(p.effectId))
                        .orElse(null);
                if (effect == null) continue;

                PotionApplicationType type;
                try {
                    type = PotionApplicationType.valueOf(p.applicationType);
                } catch (IllegalArgumentException ex) {
                    continue;
                }

                ItemStack stack = ItemStack.EMPTY;
                if (p.potionId != null) {
                    Potion potion = BuiltInRegistries.POTION
                            .getOptional(ResourceLocation.tryParse(p.potionId))
                            .orElse(null);
                    if (potion != null) {
                        Item item = itemForApplicationType(type);
                        stack = new ItemStack(item);
                        PotionUtils.setPotion(stack, potion);
                    }
                }

                EntityType<?> sourceType = null;
                if (p.sourceEntityTypeId != null) {
                    sourceType = BuiltInRegistries.ENTITY_TYPE
                            .getOptional(ResourceLocation.tryParse(p.sourceEntityTypeId))
                            .orElse(null);
                }

                ACTIVE.put(effect, new PotionEffectEntry(stack, type, List.of(effect), sourceType));
            }

            if (DEBUG) {
                System.out.println("[PotionDebug] Loaded " + ACTIVE.size() + " ACTIVE entries from " + file);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static Item itemForApplicationType(PotionApplicationType type) {
        return switch (type) {
            case DRINK -> Items.POTION;
            case SPLASH -> Items.SPLASH_POTION;
            case LINGERING -> Items.LINGERING_POTION;
            case MOB_ATTACK -> Items.POTION; // не используется (potionId всегда null для MOB_ATTACK)
        };
    }

    /** Плоская сериализуемая форма ACTIVE-записи для сохранения на диск между сессиями. */
    private static final class PersistedEntry {
        String effectId;
        String applicationType;
        String potionId;          // nullable — для MOB_ATTACK без зелья
        String sourceEntityTypeId; // nullable — для DRINK без источника
    }

    // =========================================================================
    // Общий тик
    // =========================================================================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side != LogicalSide.CLIENT) return;

        Player player = event.player;
        if (player != Minecraft.getInstance().player) return;

        tickWatchedThrown();
        tickWatchedClouds();

        matchNewEffectsToCandidates(player);
        pruneExpiredCandidates();
        pruneRecentAttackers();
        pruneExpiredActiveEntries(player);
    }

    /** Перечитывает getItem() у наблюдаемых ThrownPotion, пока он не перестанет быть EMPTY. */
    private static void tickWatchedThrown() {
        WATCHED_THROWN.removeIf(watched -> {
            ThrownPotion thrown = watched.entity;

            if (thrown.isRemoved()) {
                return true; // сущность уже разбилась/исчезла без распознанного зелья
            }

            ItemStack potionStack = thrown.getItem();
            Potion potion = PotionUtils.getPotion(potionStack);

            if (potion != Potions.EMPTY) {
                List<MobEffectInstance> potionEffects = PotionUtils.getMobEffects(potionStack);

                if (DEBUG) {
                    System.out.println("[PotionDebug] ThrownPotion id=" + thrown.getId()
                            + " resolved potionId=" + BuiltInRegistries.POTION.getKey(potion)
                            + " effects=" + potionEffects);
                }

                if (!potionEffects.isEmpty() && isSplashOrLingering(potionStack)) {
                    PotionApplicationType type = potionStack.is(Items.LINGERING_POTION)
                            ? PotionApplicationType.LINGERING
                            : PotionApplicationType.SPLASH;

                    // synced owner: доступен на клиенте сразу, отдельная сеть не нужна
                    Entity owner = thrown.getOwner();
                    EntityType<?> sourceType = owner != null ? owner.getType() : null;

                    registerCandidate(potionStack.copy(), type,
                            potionEffects.stream().map(MobEffectInstance::getEffect).toList(),
                            sourceType);

                    if (DEBUG) {
                        System.out.println("[PotionDebug] Registered candidate from ThrownPotion id="
                                + thrown.getId() + " type=" + type + " owner=" + owner);
                    }
                }
                return true; // распознано (или не интересно) — прекращаем наблюдение
            }

            if (--watched.ticksLeft <= 0) {
                if (DEBUG) {
                    System.out.println("[PotionDebug] ThrownPotion id=" + thrown.getId()
                            + " watch timed out, still EMPTY");
                }
                return true;
            }
            return false;
        });
    }

    /** Перечитывает getPotion() у наблюдаемых AreaEffectCloud, пока он не перестанет быть EMPTY. */
    private static void tickWatchedClouds() {
        WATCHED_CLOUDS.removeIf(watched -> {
            AreaEffectCloud cloud = watched.entity;

            if (cloud.isRemoved()) {
                return true;
            }

            Potion potion = cloud.getPotion();

            if (potion != Potions.EMPTY) {
                List<MobEffectInstance> cloudEffects = PotionUtils.getAllEffects(potion, List.of());

                if (DEBUG) {
                    System.out.println("[PotionDebug] AreaEffectCloud id=" + cloud.getId()
                            + " resolved potionId=" + BuiltInRegistries.POTION.getKey(potion)
                            + " effects=" + cloudEffects);
                }

                if (!cloudEffects.isEmpty()) {
                    ItemStack representative = new ItemStack(Items.LINGERING_POTION);
                    PotionUtils.setPotion(representative, potion);

                    LivingEntity owner = cloud.getOwner();
                    EntityType<?> sourceType = owner != null ? owner.getType() : null;

                    registerCandidate(representative, PotionApplicationType.LINGERING,
                            cloudEffects.stream().map(MobEffectInstance::getEffect).toList(),
                            sourceType);

                    if (DEBUG) {
                        System.out.println("[PotionDebug] Registered candidate from AreaEffectCloud id="
                                + cloud.getId() + " owner=" + owner);
                    }
                }
                return true;
            }

            if (--watched.ticksLeft <= 0) {
                if (DEBUG) {
                    System.out.println("[PotionDebug] AreaEffectCloud id=" + cloud.getId()
                            + " watch timed out, still EMPTY");
                }
                return true;
            }
            return false;
        });
    }

    private static boolean isSplashOrLingering(ItemStack stack) {
        return stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    private static void registerCandidate(ItemStack stack, PotionApplicationType type,
                                          List<MobEffect> effects, EntityType<?> sourceEntityType) {
        CANDIDATES.add(new Candidate(stack, type, effects, sourceEntityType, CANDIDATE_LIFETIME_TICKS));
    }

    private static void matchNewEffectsToCandidates(Player player) {
        List<MobEffectInstance> current = List.copyOf(player.getActiveEffects());

        for (MobEffectInstance inst : current) {
            boolean isNew = previousSnapshot.stream()
                    .noneMatch(prev -> prev.getEffect() == inst.getEffect());
            if (!isNew) continue;

            MobEffect effect = inst.getEffect();
            boolean matched = false;

            // сначала пробуем привязать к зельному кандидату (точнее)
            for (Candidate candidate : CANDIDATES) {
                if (candidate.effects.contains(effect)) {
                    ACTIVE.put(effect, new PotionEffectEntry(
                            candidate.stack, candidate.type, candidate.effects, candidate.sourceEntityType));
                    matched = true;
                    if (DEBUG) {
                        System.out.println("[PotionDebug] MATCHED effect=" + effect
                                + " to candidate stack=" + candidate.stack);
                    }
                    break;
                }
            }

            // если зелья нет — пробуем привязать к недавней атаке моба
            if (!matched) {
                EntityType<?> attackerType = findRecentAttacker();
                if (attackerType != null) {
                    ACTIVE.put(effect, new PotionEffectEntry(
                            ItemStack.EMPTY,
                            PotionApplicationType.MOB_ATTACK,
                            List.of(effect),
                            attackerType));

                    if (DEBUG) {
                        System.out.println("[PotionDebug] MATCHED effect=" + effect
                                + " to recent attacker type="
                                + BuiltInRegistries.ENTITY_TYPE.getKey(attackerType));
                    }
                }
            }
        }

        previousSnapshot = current;
    }

    private static void pruneExpiredCandidates() {
        CANDIDATES.removeIf(c -> --c.ticksToLive <= 0);
    }

    private static void pruneExpiredActiveEntries(Player player) {
        ACTIVE.keySet().removeIf(effect -> player.getEffect(effect) == null);
    }

    private static final class Candidate {
        final ItemStack stack;
        final PotionApplicationType type;
        final List<MobEffect> effects;
        final EntityType<?> sourceEntityType;
        int ticksToLive;

        Candidate(ItemStack stack, PotionApplicationType type, List<MobEffect> effects,
                  EntityType<?> sourceEntityType, int ticksToLive) {
            this.stack = stack;
            this.type = type;
            this.effects = effects;
            this.sourceEntityType = sourceEntityType;
            this.ticksToLive = ticksToLive;
        }
    }

    private static final class WatchedThrown {
        final ThrownPotion entity;
        int ticksLeft;

        WatchedThrown(ThrownPotion entity, int ticksLeft) {
            this.entity = entity;
            this.ticksLeft = ticksLeft;
        }
    }

    private static final class WatchedCloud {
        final AreaEffectCloud entity;
        int ticksLeft;

        WatchedCloud(AreaEffectCloud entity, int ticksLeft) {
            this.entity = entity;
            this.ticksLeft = ticksLeft;
        }
    }

    private static final class RecentAttacker {
        final EntityType<?> type;
        int ticksLeft;

        RecentAttacker(EntityType<?> type, int ticksLeft) {
            this.type = type;
            this.ticksLeft = ticksLeft;
        }
    }
}