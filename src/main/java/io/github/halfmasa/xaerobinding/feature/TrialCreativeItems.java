package io.github.halfmasa.xaerobinding.feature;

import java.util.ArrayList;
import java.util.List;
//#if MC >= 1.21.1
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.component.BlockItemStateProperties;
//#endif

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
        //#if MC >= 1.21.1
        // Templates are cached, but absolute cooldown timestamps belong to the current world.
        Minecraft client = Minecraft.getInstance();
        long gameTime = client != null && client.level != null ? client.level.getGameTime() : 0;
        List<ItemStack> items = new ArrayList<>(cachedItems);
        for (int i = 2; i < ENTRIES.size() * 3; i += 3)
        {
            ItemStack copy = cachedItems.get(i).copy();
            var data = copy.get(DataComponents.BLOCK_ENTITY_DATA);
            //#if MC >= 1.21.10
            CompoundTag tag = data.copyTagWithoutId();
            //#else
            //$$ CompoundTag tag = data.copyTag();
            //#endif
            tag.putLong(TAG_COOLDOWN_ENDS_AT, gameTime + COOLDOWN_OFFSET_TICKS);
            writeBlockEntityData(copy, halfmasa$trialSpawnerType(), tag);
            items.set(i, copy);
        }
        return List.copyOf(items);
        //#else
        //$$ return cachedItems;
        //#endif
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
        //#if MC >= 1.21.1
        // The registered config supplies valid SpawnData, including baby zombie NBT,
        // bogged instead of the nonexistent poison_skeleton type, and slime sizes.
        stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of(
                "ominous", Boolean.toString(ominous),
                "trial_spawner_state", cooldown ? "cooldown" : "waiting_for_players")));
        //#else
        //$$ else
        //$$ {
        //$$     CompoundTag spawnData = new CompoundTag();
        //$$     spawnData.putString(TAG_ID, entityId(entry.entityId));
        //$$     tag.put(TAG_SPAWN_DATA, spawnData);
        //$$ }
        //#endif

        writeBlockEntityData(stack, halfmasa$trialSpawnerType(), tag);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(nameKey(ominous, cooldown),
                Component.translatable("halfmasa.feature.trial_creative.mob." + entry.translationId)));
        return stack;
    }

    private static ItemStack vault(boolean ominous)
    {
        ItemStack stack = new ItemStack(Items.VAULT);
        CompoundTag tag = new CompoundTag();
        //#if MC >= 1.21.1
        stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of(
                "ominous", Boolean.toString(ominous))));
        if (ominous)
        {
            CompoundTag config = new CompoundTag();
            config.putString("loot_table", "minecraft:chests/trial_chambers/reward_ominous");
            CompoundTag key = new CompoundTag();
            key.putString("id", "minecraft:ominous_trial_key");
            key.putInt("count", 1);
            config.put("key_item", key);
            tag.put("config", config);
        }
        //#endif
        writeBlockEntityData(stack, halfmasa$vaultType(), tag);
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
        tag.putString(key, "minecraft:" + entry.configId +
                (TAG_OMINOUS_CONFIG.equals(key) ? "/ominous" : "/normal"));
        //#else
        //$$ tag.put(key, fallbackConfig(entry, key));
        //#endif
    }

    //#if MC < 1.21.3
    //$$ /** Read the exact inline config from this version's naturally generated chamber. */
    //$$ private static CompoundTag fallbackConfig(Entry entry, String key)
    //$$ {
        //$$ String path = entry.configId.substring("trial_chamber/".length());
        //$$ if (path.equals("breeze")) path = "breeze/breeze";
        //$$ String resource = "/data/minecraft/structure/trial_chambers/spawner/" + path + ".nbt";
        //$$ try (var stream = TrialCreativeItems.class.getResourceAsStream(resource))
        //$$ {
            //$$ if (stream == null) throw new IllegalStateException("Missing vanilla trial spawner " + resource);
            //$$ CompoundTag structure = net.minecraft.nbt.NbtIo.readCompressed(stream, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            //$$ ListTag blocks = structure.getList("blocks", net.minecraft.nbt.Tag.TAG_COMPOUND);
            //$$ for (int i = 0; i < blocks.size(); i++)
            //$$ {
                //$$ CompoundTag data = blocks.getCompound(i).getCompound("nbt");
                //$$ if (data.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND))
                //$$ {
                    //$$ // TrialSpawnerBlockEntity.loadAdditional merges normal values into ominous config.
                    //$$ CompoundTag base = TAG_OMINOUS_CONFIG.equals(key) ? data.getCompound(TAG_NORMAL_CONFIG).copy() : new CompoundTag();
                    //$$ return base.merge(data.getCompound(key));
                //$$ }
            //$$ }
            //$$ throw new IllegalStateException("Missing " + key + " in vanilla trial spawner " + resource);
        //$$ }
        //$$ catch (java.io.IOException exception)
        //$$ {
            //$$ throw new IllegalStateException("Cannot read vanilla trial spawner " + resource, exception);
        //$$ }
    //$$ }
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
