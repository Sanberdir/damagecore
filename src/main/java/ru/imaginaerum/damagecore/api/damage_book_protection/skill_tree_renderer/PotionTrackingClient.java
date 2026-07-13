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
import net.minecraftforge.event.GameShuttingDownEvent;
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
import java.util.*;

@Mod.EventBusSubscriber(modid = DamageCore.MODID, value = Dist.CLIENT)
public final class PotionTrackingClient {

    private static final boolean DEBUG = true;

    private static final int CANDIDATE_LIFETIME_TICKS = 6 * 20;
    private static final int WATCH_TIMEOUT_TICKS      = 3 * 20;
    private static final int ATTACKER_LIFETIME_TICKS  = 3 * 20;
    private static final int LOGIN_GRACE_PERIOD        = 60;

    private static final Map<MobEffect, PotionEffectEntry> ACTIVE           = new LinkedHashMap<>();
    private static final Map<MobEffect, Integer>           INITIAL_DURATION = new LinkedHashMap<>();
    private static final List<Candidate>                   CANDIDATES       = new ArrayList<>();
    private static final List<WatchedThrown>               WATCHED_THROWN   = new ArrayList<>();
    private static final List<WatchedCloud>                WATCHED_CLOUDS   = new ArrayList<>();
    private static final List<RecentAttacker>              RECENT_ATTACKERS = new ArrayList<>();

    private static List<MobEffectInstance> previousSnapshot = List.of();
    private static String currentWorldId = "default";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** >0 — не чистить ACTIVE (эффекты ещё не синхронизировались после входа) */
    private static int loginGraceTicks = 0;

    /** true — не чистить ACTIVE (идёт выход из мира/игры) */
    private static boolean isDisconnecting = false;

    /** true — первый тик в мире ещё не случился (loadFromDisk ещё не вызван) */
    private static boolean pendingLoad = false;

    private PotionTrackingClient() {}

    // =========================================================================
    // Публичный API
    // =========================================================================

    public static PotionEffectEntry get(MobEffect effect) {
        return ACTIVE.get(effect);
    }

    public static float getRemainingFraction(MobEffect effect, int currentDurationTicks) {
        Integer initial = INITIAL_DURATION.get(effect);
        if (initial == null || initial <= 0) return 1f;
        return net.minecraft.util.Mth.clamp(currentDurationTicks / (float) initial, 0f, 1f);
    }

    public static void registerMobEffectFromServer(MobEffect effect, EntityType<?> sourceType) {
        PotionEffectEntry existing = ACTIVE.get(effect);
        if (existing != null && existing.getApplicationType() != PotionApplicationType.MOB_ATTACK) {
            if (DEBUG) System.out.println("[PotionDebug] SERVER ignored — non-MOB_ATTACK already present");
            saveToDisk();
            return;

        }

        Player player = Minecraft.getInstance().player;
        MobEffectInstance inst = player != null ? player.getEffect(effect) : null;
        int duration = inst != null ? inst.getDuration() : 200;

        ACTIVE.put(effect, new PotionEffectEntry(ItemStack.EMPTY, PotionApplicationType.MOB_ATTACK, List.of(effect), sourceType));
        INITIAL_DURATION.put(effect, duration);

        if (DEBUG) System.out.println("[PotionDebug] SERVER MOB_ATTACK effect=" + effect + " source=" + BuiltInRegistries.ENTITY_TYPE.getKey(sourceType));
    }

    // =========================================================================
    // События
    // =========================================================================

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player != Minecraft.getInstance().player) return;

        ItemStack stack = event.getItem();
        if (!stack.is(Items.POTION)) return;

        List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
        if (effects.isEmpty()) return;

        List<MobEffect> granted = effects.stream().map(MobEffectInstance::getEffect).toList();
        PotionEffectEntry entry = new PotionEffectEntry(stack.copy(), PotionApplicationType.DRINK, granted);

        for (MobEffectInstance inst : effects) {
            ACTIVE.put(inst.getEffect(), entry);
            saveToDisk();
            INITIAL_DURATION.put(inst.getEffect(), inst.getDuration());
        }
        if (DEBUG) System.out.println("[PotionDebug] DRINK effects=" + granted);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        if (entity instanceof ThrownPotion thrown) {
            WATCHED_THROWN.add(new WatchedThrown(thrown, WATCH_TIMEOUT_TICKS));
        } else if (entity instanceof AreaEffectCloud cloud) {
            WATCHED_CLOUDS.add(new WatchedCloud(cloud, WATCH_TIMEOUT_TICKS));
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!event.getEntity().level().isClientSide()) return;
        if (event.getEntity() != Minecraft.getInstance().player) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker == null || attacker.getType() == EntityType.PLAYER) return;
        RECENT_ATTACKERS.add(new RecentAttacker(attacker.getType(), ATTACKER_LIFETIME_TICKS));
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (DEBUG) System.out.println("[PotionDebug] LoggingIn");
        isDisconnecting = false;
        pendingLoad = true;

        // Определяем ID мира/сервера один раз, пока контекст точно доступен
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getCurrentServer() != null) {
                currentWorldId = mc.getCurrentServer().ip;
            } else if (mc.getSingleplayerServer() != null) {
                currentWorldId = mc.getSingleplayerServer().getWorldData().getLevelName();
            } else {
                currentWorldId = "default";
            }
        } catch (Throwable t) {
            currentWorldId = "default";
        }
        currentWorldId = currentWorldId.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (DEBUG) System.out.println("[PotionDebug] worldId=" + currentWorldId);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        isDisconnecting = true;
        pendingLoad = false;
        // Не вызываем saveToDisk если ACTIVE пуст — файл уже актуален от последнего prune
        if (!ACTIVE.isEmpty()) {
            saveToDisk();
        }
        clearTransientState();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side != LogicalSide.CLIENT) return;
        Player player = event.player;
        if (player != Minecraft.getInstance().player) return;

        if (pendingLoad) {
            pendingLoad = false;
            loadFromDisk();
            // После loadFromDisk сразу обновляем previousSnapshot текущими эффектами,
            // чтобы matchNewEffectsToCandidates не считал их новыми
            previousSnapshot = List.copyOf(player.getActiveEffects());
        }

        tickWatchedThrown();
        tickWatchedClouds();
        matchNewEffectsToCandidates(player);
        pruneExpiredCandidates();
        pruneRecentAttackers();
        pruneExpiredActiveEntries(player);
    }

    // =========================================================================
    // Тик — внутренние методы
    // =========================================================================

    private static void tickWatchedThrown() {
        WATCHED_THROWN.removeIf(watched -> {
            ThrownPotion thrown = watched.entity;
            if (thrown.isRemoved()) return true;

            ItemStack potionStack = thrown.getItem();
            Potion potion = PotionUtils.getPotion(potionStack);
            if (potion != Potions.EMPTY) {
                List<MobEffectInstance> effects = PotionUtils.getMobEffects(potionStack);
                if (!effects.isEmpty() && isSplashOrLingering(potionStack)) {
                    PotionApplicationType type = potionStack.is(Items.LINGERING_POTION)
                            ? PotionApplicationType.LINGERING : PotionApplicationType.SPLASH;
                    Entity owner = thrown.getOwner();
                    registerCandidate(potionStack.copy(), type,
                            effects.stream().map(MobEffectInstance::getEffect).toList(),
                            owner != null ? owner.getType() : null);
                }
                return true;
            }
            if (--watched.ticksLeft <= 0) return true;
            return false;
        });
    }

    private static void tickWatchedClouds() {
        WATCHED_CLOUDS.removeIf(watched -> {
            AreaEffectCloud cloud = watched.entity;
            if (cloud.isRemoved()) return true;

            Potion potion = cloud.getPotion();
            if (potion != Potions.EMPTY) {
                List<MobEffectInstance> effects = PotionUtils.getAllEffects(potion, List.of());
                if (!effects.isEmpty()) {
                    ItemStack representative = new ItemStack(Items.LINGERING_POTION);
                    PotionUtils.setPotion(representative, potion);
                    LivingEntity owner = cloud.getOwner();
                    registerCandidate(representative, PotionApplicationType.LINGERING,
                            effects.stream().map(MobEffectInstance::getEffect).toList(),
                            owner != null ? owner.getType() : null);
                }
                return true;
            }
            if (--watched.ticksLeft <= 0) return true;
            return false;
        });
    }

    private static void matchNewEffectsToCandidates(Player player) {
        List<MobEffectInstance> current = List.copyOf(player.getActiveEffects());

        for (MobEffectInstance inst : current) {
            MobEffect effect = inst.getEffect();

            boolean isNew = previousSnapshot.stream().noneMatch(prev -> prev.getEffect() == effect);
            PotionEffectEntry existing = ACTIVE.get(effect);
            boolean isFallback = existing != null
                    && existing.getApplicationType() == PotionApplicationType.MOB_ATTACK
                    && existing.getPotionStack().isEmpty();

            if (existing != null && !isFallback) continue;
            if (!isNew) continue;
            boolean matched = false;
            for (Candidate candidate : CANDIDATES) {
                if (candidate.effects.contains(effect)) {
                    ACTIVE.put(effect, new PotionEffectEntry(candidate.stack, candidate.type, candidate.effects, candidate.sourceEntityType));
                    saveToDisk();
                    INITIAL_DURATION.put(effect, inst.getDuration());
                    matched = true;
                    if (DEBUG) System.out.println("[PotionDebug] MATCHED effect=" + effect + " potion=" + candidate.stack.getHoverName().getString());
                    break;
                }
            }

            if (!matched && isNew) {
                EntityType<?> attackerType = findRecentAttacker();
                if (attackerType != null) {
                    ACTIVE.put(effect, new PotionEffectEntry(ItemStack.EMPTY, PotionApplicationType.MOB_ATTACK, List.of(effect), attackerType));
                    INITIAL_DURATION.put(effect, inst.getDuration());
                    if (DEBUG) System.out.println("[PotionDebug] FALLBACK effect=" + effect + " attacker=" + BuiltInRegistries.ENTITY_TYPE.getKey(attackerType));
                }
            }
        }

        previousSnapshot = current;
    }

    private static void pruneExpiredCandidates() {
        CANDIDATES.removeIf(c -> --c.ticksToLive <= 0);
    }

    private static void pruneRecentAttackers() {
        RECENT_ATTACKERS.removeIf(a -> --a.ticksLeft <= 0);
    }

    private static void pruneExpiredActiveEntries(Player player) {
        if (loginGraceTicks > 0) { loginGraceTicks--; return; }
        if (isDisconnecting) return;

        // Сохраняем ДО удаления
        boolean willChange = ACTIVE.keySet().stream()
                .anyMatch(effect -> player.getEffect(effect) == null);

        int before = ACTIVE.size();
        ACTIVE.keySet().removeIf(effect -> player.getEffect(effect) == null);
        INITIAL_DURATION.keySet().retainAll(ACTIVE.keySet());

        if (ACTIVE.size() != before) {
            saveToDisk(); // сохраняем актуальное состояние после prune
            System.out.println("[PotionDebug] prune: " + before + " -> " + ACTIVE.size());
        }
    }

    private static boolean isSplashOrLingering(ItemStack stack) {
        return stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    private static void registerCandidate(ItemStack stack, PotionApplicationType type, List<MobEffect> effects, EntityType<?> sourceEntityType) {
        CANDIDATES.add(new Candidate(stack, type, effects, sourceEntityType, CANDIDATE_LIFETIME_TICKS));
    }

    private static EntityType<?> findRecentAttacker() {
        return RECENT_ATTACKERS.isEmpty() ? null : RECENT_ATTACKERS.get(RECENT_ATTACKERS.size() - 1).type;
    }

    // =========================================================================
    // Персистентность
    // =========================================================================

    private static void clearTransientState() {
        CANDIDATES.clear();
        WATCHED_THROWN.clear();
        WATCHED_CLOUDS.clear();
        RECENT_ATTACKERS.clear();
        previousSnapshot = List.of();
        // ACTIVE и INITIAL_DURATION не трогаем — нужны для следующего saveToDisk
    }

    private static Path getStorageFile() {
        return FMLPaths.GAMEDIR.get()
                .resolve("config")
                .resolve(DamageCore.MODID)
                .resolve("potion_tracking_" + currentWorldId + ".json");
    }

    private static void saveToDisk() {
        if (DEBUG) System.out.println("[PotionDebug] saveToDisk ACTIVE.size=" + ACTIVE.size());

        List<PersistedEntry> toSave = new ArrayList<>();

        for (var mapEntry : ACTIVE.entrySet()) {
            MobEffect effect = mapEntry.getKey();
            PotionEffectEntry entry = mapEntry.getValue();

            ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            if (effectId == null) continue;

            PersistedEntry p = new PersistedEntry();
            p.effectId       = effectId.toString();
            p.applicationType = entry.getApplicationType().name();
            p.initialDuration = INITIAL_DURATION.getOrDefault(effect, 0);

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
            if (DEBUG) System.out.println("[PotionDebug]   saving effect=" + effectId + " type=" + p.applicationType + " potionId=" + p.potionId + " initialDuration=" + p.initialDuration);
        }

        try {
            Path file = getStorageFile();
            Files.createDirectories(file.getParent());
            if (toSave.isEmpty()) {
                Files.deleteIfExists(file);
            } else {
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    GSON.toJson(toSave, writer);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void loadFromDisk() {
        Path file = getStorageFile();
        if (DEBUG) System.out.println("[PotionDebug] loadFromDisk file exists=" + Files.exists(file));
        if (!Files.exists(file)) return;

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            PersistedEntry[] loaded = GSON.fromJson(reader, PersistedEntry[].class);
            if (loaded == null) return;

            ACTIVE.clear();
            INITIAL_DURATION.clear();

            for (PersistedEntry p : loaded) {
                if (p.effectId == null || p.applicationType == null) continue;

                MobEffect effect = BuiltInRegistries.MOB_EFFECT.getOptional(ResourceLocation.tryParse(p.effectId)).orElse(null);
                if (effect == null) continue;

                PotionApplicationType type;
                try { type = PotionApplicationType.valueOf(p.applicationType); }
                catch (IllegalArgumentException ex) { continue; }

                ItemStack stack = ItemStack.EMPTY;
                if (p.potionId != null) {
                    Potion potion = BuiltInRegistries.POTION.getOptional(ResourceLocation.tryParse(p.potionId)).orElse(null);
                    if (potion != null) {
                        stack = new ItemStack(itemForApplicationType(type));
                        PotionUtils.setPotion(stack, potion);
                    }
                }

                EntityType<?> sourceType = null;
                if (p.sourceEntityTypeId != null) {
                    sourceType = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(p.sourceEntityTypeId)).orElse(null);
                }

                ACTIVE.put(effect, new PotionEffectEntry(stack, type, List.of(effect), sourceType));
                if (p.initialDuration > 0) INITIAL_DURATION.put(effect, p.initialDuration);

                if (DEBUG) System.out.println("[PotionDebug]   loaded effect=" + p.effectId + " type=" + p.applicationType + " potionId=" + p.potionId);
            }

            // Grace period стартует здесь — когда ACTIVE уже заполнен
            loginGraceTicks = LOGIN_GRACE_PERIOD;
            if (DEBUG) System.out.println("[PotionDebug] loadFromDisk done ACTIVE.size=" + ACTIVE.size() + " graceTicks=" + loginGraceTicks);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static Item itemForApplicationType(PotionApplicationType type) {
        return switch (type) {
            case DRINK     -> Items.POTION;
            case SPLASH    -> Items.SPLASH_POTION;
            case LINGERING -> Items.LINGERING_POTION;
            case MOB_ATTACK -> Items.POTION;
        };
    }

    // =========================================================================
    // Внутренние структуры
    // =========================================================================

    private static final class PersistedEntry {
        String effectId;
        String applicationType;
        String potionId;
        String sourceEntityTypeId;
        int initialDuration;
    }

    private static final class Candidate {
        final ItemStack stack;
        final PotionApplicationType type;
        final List<MobEffect> effects;
        final EntityType<?> sourceEntityType;
        int ticksToLive;
        Candidate(ItemStack stack, PotionApplicationType type, List<MobEffect> effects, EntityType<?> sourceEntityType, int ticksToLive) {
            this.stack = stack; this.type = type; this.effects = effects; this.sourceEntityType = sourceEntityType; this.ticksToLive = ticksToLive;
        }
    }

    private static final class WatchedThrown {
        final ThrownPotion entity; int ticksLeft;
        WatchedThrown(ThrownPotion entity, int ticksLeft) { this.entity = entity; this.ticksLeft = ticksLeft; }
    }

    private static final class WatchedCloud {
        final AreaEffectCloud entity; int ticksLeft;
        WatchedCloud(AreaEffectCloud entity, int ticksLeft) { this.entity = entity; this.ticksLeft = ticksLeft; }
    }

    private static final class RecentAttacker {
        final EntityType<?> type; int ticksLeft;
        RecentAttacker(EntityType<?> type, int ticksLeft) { this.type = type; this.ticksLeft = ticksLeft; }
    }
}