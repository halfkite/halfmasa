package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.compat.MinecraftClientCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.RangeFeedingListMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.EntityHitResult;

/** Client-thread scheduler. Every queued UUID is resolved and revalidated immediately before interaction. */
public final class RangeFeedingService implements IClientTickHandler
{
    private static final RangeFeedingService INSTANCE = new RangeFeedingService();
    private final ArrayDeque<UUID> holdQueue = new ArrayDeque<>();
    private final ArrayDeque<UUID> singleQueue = new ArrayDeque<>();
    private final Map<UUID, Long> attemptedAt = new HashMap<>();
    private final Set<UUID> singleAttempted = new HashSet<>();
    private Set<EntityType<?>> blacklist = Set.of(), whitelist = Set.of();
    private LocalPlayer actor;
    private ClientLevel level;
    private boolean holdActive, singleFeedPending, blockedUntilRelease, hasInteracted;
    private List<Integer> holdKeys = List.of();
    private long ticks, lastInteraction, nextScan;

    private RangeFeedingService() {}
    public static RangeFeedingService getInstance() { return INSTANCE; }

    public void reloadLists()
    {
        Set<String> invalid = new java.util.LinkedHashSet<>();
        this.blacklist = normalize(Configs.RANGE_FEEDING_BLACKLIST.getStrings(), invalid);
        this.whitelist = normalize(Configs.RANGE_FEEDING_WHITELIST.getStrings(), invalid);
        if (!invalid.isEmpty()) XaeroWorldBinding.LOGGER.warn("Ignoring invalid or unregistered range feeding entity IDs: {}", invalid);
    }

    private static Set<EntityType<?>> normalize(List<String> entries, Set<String> invalid)
    {
        Set<EntityType<?>> result = new HashSet<>();
        for (String entry : entries)
        {
            if (entry == null || entry.isBlank()) continue;
            String value = entry.trim().toLowerCase(Locale.ROOT);
            if (!value.contains(":")) value = "minecraft:" + value;
            //#if MC >= 1.21.11
            Identifier id = Identifier.tryParse(value);
            //#else
            //$$ ResourceLocation id = ResourceLocation.tryParse(value);
            //#endif
            var type = id == null ? java.util.Optional.<EntityType<?>>empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id);
            if (type.isPresent()) result.add(type.get()); else invalid.add(entry);
        }
        return Set.copyOf(result);
    }

    public boolean requestSingleFeed()
    {
        Minecraft client = Minecraft.getInstance();
        updateSession(client);
        if (!Configs.RANGE_FEEDING.getBooleanValue() || !canRun(client)) return false;
        // A repeated trigger must not replace a running batch or continuously refill it.
        if (this.singleFeedPending) return true;
        this.singleAttempted.clear();
        this.singleFeedPending = true;
        scan(this.singleQueue);
        if (this.singleQueue.isEmpty()) this.singleFeedPending = false;
        return true;
    }

    public void setHoldActive(boolean active)
    {
        if (!active)
        {
            cancelHoldFeeding();
            return;
        }
        Minecraft client = Minecraft.getInstance();
        updateSession(client);
        if (this.holdActive || this.blockedUntilRelease || !canRun(client) || !isHoldPhysicallyDown()) return;
        this.holdActive = true;
        this.holdKeys = List.copyOf(Configs.RANGE_FEEDING_HOLD.getKeybind().getKeys());
        this.nextScan = this.ticks;
    }

    public void onMasterChanged()
    {
        if (!Configs.RANGE_FEEDING.getBooleanValue()) cancelSingleFeeding();
    }

    public void onProtectionChanged()
    {
        Minecraft client = Minecraft.getInstance();
        updateSession(client);
        if (!canRun(client)) return;
        // Rescanning is essential when protection is removed: those babies were absent from the old snapshot.
        if (this.singleFeedPending)
        {
            this.singleQueue.clear();
            scan(this.singleQueue);
            this.singleQueue.removeIf(this.singleAttempted::contains);
            this.singleFeedPending = !this.singleQueue.isEmpty();
        }
        if (this.holdActive)
        {
            this.holdQueue.clear();
            this.nextScan = this.ticks;
        }
    }

    public void onIntervalChanged()
    {
        this.nextScan = this.ticks;
    }

    private static int intervalTicks()
    {
        return Configs.RANGE_FEEDING_INTERVAL.getIntegerValue();
    }

    private static boolean isHoldPhysicallyDown()
    {
        var keys = Configs.RANGE_FEEDING_HOLD.getKeybind().getKeys();
        return !keys.isEmpty() && keys.stream().allMatch(KeybindMulti::isKeyDown);
    }

    private static boolean canRun(Minecraft client)
    {
        return client.player != null && client.level != null && client.gameMode != null
                && client.player.isAlive() && !client.player.isSpectator()
                && MinecraftClientCompat.getScreen(client) == null && client.isWindowActive();
    }

    private void updateSession(Minecraft client)
    {
        if (this.actor != client.player || this.level != client.level)
        {
            cancelAll();
            this.attemptedAt.clear();
            this.hasInteracted = false;
            this.actor = client.player;
            this.level = client.level;
        }
    }

    @Override public void onClientTick(Minecraft client)
    {
        this.ticks++;
        updateSession(client);
        if (!canRun(client))
        {
            cancelAll();
            return;
        }
        boolean down = isHoldPhysicallyDown();
        if (!down) this.blockedUntilRelease = false;
        if (this.holdActive && (!down || !this.holdKeys.equals(Configs.RANGE_FEEDING_HOLD.getKeybind().getKeys())))
            cancelHoldFeeding();
        if (!Configs.RANGE_FEEDING.getBooleanValue()) cancelSingleFeeding();
        if (!this.holdActive && !this.singleFeedPending) return;

        // Leave manual right-click and another feature's ongoing item use to their owner.
        if (this.actor.isUsingItem() || client.options.keyAttack.isDown() || client.options.keyUse.isDown()) return;
        if (this.holdActive && this.holdQueue.isEmpty() && this.ticks >= this.nextScan)
        {
            scan(this.holdQueue);
            this.nextScan = this.ticks + intervalTicks();
        }
        if (this.hasInteracted && this.ticks - this.lastInteraction < intervalTicks()) return;
        boolean sent = this.singleFeedPending && feedNext(client, this.singleQueue);
        if (this.singleQueue.isEmpty()) this.singleFeedPending = false;
        if (!sent && this.holdActive) sent = feedNext(client, this.holdQueue);
        if (sent)
        {
            this.hasInteracted = true;
            this.lastInteraction = this.ticks;
        }
    }

    private void scan(ArrayDeque<UUID> queue)
    {
        this.attemptedAt.entrySet().removeIf(entry -> this.ticks - entry.getValue() >= intervalTicks());
        double radius = Math.min(Configs.RANGE_FEEDING_RANGE.getIntegerValue(), this.actor.entityInteractionRange());
        this.level.getEntitiesOfClass(Animal.class, this.actor.getBoundingBox().inflate(radius), this::canFeed)
                .stream().sorted(Comparator.comparingDouble(animal -> animal.getBoundingBox().distanceToSqr(this.actor.getEyePosition())))
                .map(Animal::getUUID).forEach(queue::addLast);
    }

    private boolean canFeed(Animal animal)
    {
        if (!animal.isAlive() || animal.isRemoved() || animal.level() != this.level
                || (this.attemptedAt.containsKey(animal.getUUID())
                    && this.ticks - this.attemptedAt.get(animal.getUUID()) < intervalTicks())) return false;
        if (Configs.RANGE_FEEDING_SKIP_BABIES.getBooleanValue() && animal.isBaby()) return false;
        boolean allowed = switch ((RangeFeedingListMode) Configs.RANGE_FEEDING_LIST_MODE.getOptionListValue())
        {
            case NONE -> true;
            case BLACKLIST -> !this.blacklist.contains(animal.getType());
            case WHITELIST -> this.whitelist.contains(animal.getType());
        };
        if (!allowed || this.actor.getMainHandItem().isEmpty() || !animal.isFood(this.actor.getMainHandItem())) return false;
        // Baby feeding remains possible when protection is off, independently of adult breeding readiness.
        if (!animal.isBaby() && !animal.canFallInLove()) return false;
        double radius = Configs.RANGE_FEEDING_RANGE.getIntegerValue();
        return animal.getBoundingBox().distanceToSqr(this.actor.getEyePosition()) <= radius * radius
                && this.actor.isWithinEntityInteractionRange(animal, 0.0) && this.actor.hasLineOfSight(animal);
    }

    private boolean feedNext(Minecraft client, ArrayDeque<UUID> queue)
    {
        while (!queue.isEmpty())
        {
            var entity = findEntity(queue.removeFirst());
            if (!(entity instanceof Animal animal) || !canFeed(animal)) continue;
            this.attemptedAt.put(animal.getUUID(), this.ticks);
            if (queue == this.singleQueue) this.singleAttempted.add(animal.getUUID());
            //#if MC >= 26.0
            client.gameMode.interact(this.actor, animal, new EntityHitResult(animal), InteractionHand.MAIN_HAND);
            //#else
            //$$ var result = client.gameMode.interactAt(this.actor, animal, new EntityHitResult(animal), InteractionHand.MAIN_HAND);
            //$$ if (!result.consumesAction()) client.gameMode.interact(this.actor, animal, InteractionHand.MAIN_HAND);
            //#endif
            return true;
        }
        return false;
    }

    private void cancelHoldFeeding()
    {
        this.holdActive = false;
        this.holdQueue.clear();
        this.holdKeys = List.of();
    }

    private net.minecraft.world.entity.Entity findEntity(UUID id)
    {
        //#if MC >= 1.21.5
        return this.level.getEntity(id);
        //#else
        //$$ for (var entity : this.level.entitiesForRendering())
        //$$     if (entity.getUUID().equals(id)) return entity;
        //$$ return null;
        //#endif
    }

    private void cancelSingleFeeding()
    {
        this.singleFeedPending = false;
        this.singleQueue.clear();
        this.singleAttempted.clear();
    }

    private void cancelAll()
    {
        cancelHoldFeeding();
        cancelSingleFeeding();
        // Regaining focus, closing a GUI or returning to a world must never resume an old hold.
        this.blockedUntilRelease = true;
    }
}
//#endif
