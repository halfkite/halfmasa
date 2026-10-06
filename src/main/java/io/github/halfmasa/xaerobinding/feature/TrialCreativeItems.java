package io.github.halfmasa.xaerobinding.feature;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Builds the contents of the custom "trial" creative tab: every vanilla trial chamber
 * spawner configuration (both normal and ominous) plus their cooldown variants, and the
 * two trial vault variants.
 *
 * <p>The items are plain vanilla {@code trial_spawner} / {@code vault} stacks carrying a
 * {@code BLOCK_ENTITY_DATA} component, so they place a preconfigured block. The spawner
 * state itself lives in that component; no custom block is involved.</p>
 */
public final class TrialCreativeItems
{
    /** Vanilla trial chamber spawner configs, one entry per mob the spawner can produce. */
    private static final List<Entry> ENTRIES = List.of(
            new Entry("trial_chamber/breeze", "breeze", "breeze"),
            new Entry("trial_chamber/melee/husk", "husk", "husk"),
            new Entry("trial_chamber/melee/spider", "spider", "spider"),
            new Entry("trial_chamber/melee/zombie", "zombie", "zombie"),
            new Entry("trial_chamber/ranged/poison_skeleton", "poison_skeleton", "poison_skeleton"),
            new Entry("trial_chamber/ranged/skeleton", "skeleton", "skeleton"),
            new Entry("trial_chamber/ranged/stray", "stray", "stray"),
            new Entry("trial_chamber/slow_ranged/poison_skeleton", "slow_poison_skeleton", "poison_skeleton"),
            new Entry("trial_chamber/slow_ranged/skeleton", "slow_skeleton", "skeleton"),
            new Entry("trial_chamber/slow_ranged/stray", "slow_stray", "stray"),
            new Entry("trial_chamber/small_melee/baby_zombie", "baby_zombie", "baby_zombie"),
            new Entry("trial_chamber/small_melee/cave_spider", "cave_spider", "cave_spider"),
            new Entry("trial_chamber/small_melee/silverfish", "silverfish", "silverfish"),
            new Entry("trial_chamber/small_melee/slime", "slime", "slime"));

    private static final String TAG_NORMAL_CONFIG = "normal_config";
    private static final String TAG_OMINOUS_CONFIG = "ominous_config";
    private static final String TAG_TARGET_COOLDOWN = "target_cooldown_length";
    private static final String TAG_REQUIRED_PLAYER_RANGE = "required_player_range";
    private static final String TAG_COOLDOWN_ENDS_AT = "cooldown_ends_at";
    private static final String TAG_SPAWN_DATA = "spawn_data";
    private static final String TAG_ID = "id";

    private static final int DEFAULT_TARGET_COOLDOWN = 36000;
    private static final int DEFAULT_REQUIRED_PLAYER_RANGE = 14;
    /** Longest vanilla cooldown is one hour of in-game time (72000 ticks). */
    private static final long COOLDOWN_OFFSET_TICKS = 72000L;

    private static List<ItemStack> cachedItems;

    private TrialCreativeItems() {}

    /** Returns the tab contents, building them once and reusing the cached copies. */
    public static List<ItemStack> getItems()
    {
        if (cachedItems == null)
        {
            List<ItemStack> items = new ArrayList<>(ENTRIES.size() * 3 + 2);
            for (Entry entry : ENTRIES)
            {
                items.add(spawner(entry, false, false));
                items.add(spawner(entry, true, false));
                items.add(spawner(entry, false, true));
            }
            items.add(vault(false));
            items.add(vault(true));
            cachedItems = List.copyOf(items);
        }
        return cachedItems;
    }

    /** Drops the cached list so the next call rebuilds it. */
    public static void invalidate()
    {
        cachedItems = null;
    }

    private static ItemStack spawner(Entry entry, boolean ominous, boolean cooldown)
    {
        ItemStack stack = new ItemStack(Items.TRIAL_SPAWNER);
        CompoundTag tag = new CompoundTag();
        writeConfig(tag, TAG_NORMAL_CONFIG, entry);
        writeConfig(tag, TAG_OMINOUS_CONFIG, entry);
        tag.putInt(TAG_TARGET_COOLDOWN, DEFAULT_TARGET_COOLDOWN);
        tag.putInt(TAG_REQUIRED_PLAYER_RANGE, DEFAULT_REQUIRED_PLAYER_RANGE);

        if (cooldown)
        {
            // A far-future timestamp puts the placed spawner straight into the cooldown state.
            tag.putLong(TAG_COOLDOWN_ENDS_AT, COOLDOWN_OFFSET_TICKS);
        }
        else
        {
            CompoundTag spawnData = new CompoundTag();
            spawnData.putString(TAG_ID, entityId(entry.entityId));
            tag.put(TAG_SPAWN_DATA, spawnData);
        }

        writeBlockEntityData(stack, halfmasa$trialSpawnerType(), tag);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(nameKey(ominous, cooldown),
                Component.translatable("halfmasa.feature.trial_creative.mob." + entry.translationId)));
        return stack;
    }

    private static ItemStack vault(boolean ominous)
    {
        ItemStack stack = new ItemStack(Items.VAULT);
        writeBlockEntityData(stack, halfmasa$vaultType(), new CompoundTag());
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(ominous
                ? "halfmasa.feature.trial_creative.vault.ominous"
                : "halfmasa.feature.trial_creative.vault.normal"));
        return stack;
    }

    private static String nameKey(boolean ominous, boolean cooldown)
    {
        if (cooldown)
        {
            return "halfmasa.feature.trial_creative.spawner.cooldown";
        }
        return ominous
                ? "halfmasa.feature.trial_creative.spawner.ominous"
                : "halfmasa.feature.trial_creative.spawner.normal";
    }

    /**
     * Writes one of the two spawner configs.
     *
     * <p>From 1.21.3 onward the spawner configs live in the {@code trial_spawner_config}
     * registry and the field is a registry-holder codec, so a plain id string is enough.
     * 1.21.1 predates that registry and expects the full config object inline.</p>
     */
    private static void writeConfig(CompoundTag tag, String key, Entry entry)
    {
        //#if MC >= 1.21.3
        tag.putString(key, "minecraft:" + entry.configId);
        //#else
        //$$ tag.put(key, fallbackConfig(entry.entityId));
        //#endif
    }

    //#if MC < 1.21.3
    /**
     * Builds a complete inline spawner config for 1.21.1, which has no spawner config registry.
     * The values mirror the vanilla trial chamber defaults, with the spawn potential replaced
     * by the requested mob.
     */
    private static CompoundTag fallbackConfig(String entity)
    {
        CompoundTag config = new CompoundTag();
        config.putInt("spawn_range", 4);
        config.putFloat("total_mobs", 6.0F);
        config.putFloat("simultaneous_mobs", 2.0F);
        config.putFloat("total_mobs_added_per_player", 2.0F);
        config.putFloat("simultaneous_mobs_added_per_player", 1.0F);
        config.putInt("ticks_between_spawn", 40);

        CompoundTag potential = new CompoundTag();
        potential.putInt("weight", 1);
        CompoundTag data = new CompoundTag();
        data.putString(TAG_ID, entityId(entity));
        potential.put("data", data);
        ListTag potentials = new ListTag();
        potentials.add(potential);
        config.put("spawn_potentials", potentials);

        ListTag loot = new ListTag();
        CompoundTag key = new CompoundTag();
        key.putString("type", "minecraft:loot_table");
        key.putString("value", "minecraft:spawners/trial_chamber/key");
        loot.add(key);
        CompoundTag consumables = new CompoundTag();
        consumables.putString("type", "minecraft:loot_table");
        consumables.putString("value", "minecraft:spawners/trial_chamber/consumables");
        loot.add(consumables);
        config.put("loot_tables_to_eject", loot);

        CompoundTag ominous = new CompoundTag();
        ominous.putString("type", "minecraft:loot_table");
        ominous.putString("value", "minecraft:spawners/trial_chamber/items_to_drop_when_ominous");
        config.put("items_to_drop_when_ominous", ominous);
        return config;
    }
    //#endif

    private static void writeBlockEntityData(ItemStack stack, BlockEntityType<?> type, CompoundTag tag)
    {
        //#if MC >= 1.21.10
        stack.set(DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.TypedEntityData.of(type, tag));
        //#else
        //$$ stack.set(DataComponents.BLOCK_ENTITY_DATA,
        //$$         net.minecraft.world.item.component.CustomData.of(tag));
        //#endif
    }

    /**
     * The block entity type constants live on {@code BlockEntityType} before 26.2 and on
     * {@code BlockEntityTypes} from 26.2 onward.
     */
    private static BlockEntityType<?> halfmasa$trialSpawnerType()
    {
        //#if MC >= 26.2
        return net.minecraft.world.level.block.entity.BlockEntityTypes.TRIAL_SPAWNER;
        //#else
        //$$ return BlockEntityType.TRIAL_SPAWNER;
        //#endif
    }

    private static BlockEntityType<?> halfmasa$vaultType()
    {
        //#if MC >= 26.2
        return net.minecraft.world.level.block.entity.BlockEntityTypes.VAULT;
        //#else
        //$$ return BlockEntityType.VAULT;
        //#endif
    }

    private static String entityId(String entity)
    {
        return "minecraft:" + entity;
    }

    private record Entry(String configId, String entityId, String translationId) {}
}
