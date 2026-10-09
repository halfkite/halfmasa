package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 26.3
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;

/** Reads actual components, including vanilla pick-block items, without changing placement data. */
public final class SpawnerItemAppearance
{
    public enum Kind { SPAWNER, TRIAL_SPAWNER, VAULT }
    public record Info(Kind kind, boolean ominous, String state, boolean cooling,
                       long cooldownEndsAt, List<CompoundTag> mobs, Identifier model) {}
    private static final Map<String, List<CompoundTag>> VANILLA_CONFIGS = new HashMap<>();
    private SpawnerItemAppearance() {}

    public static boolean supports(ItemStack stack)
    {
        return stack.is(Items.SPAWNER) || stack.is(Items.TRIAL_SPAWNER) || stack.is(Items.VAULT);
    }

    public static Info read(ItemStack stack, HolderLookup.Provider registries, long gameTime)
    {
        if (!supports(stack)) return null;
        Kind kind = stack.is(Items.TRIAL_SPAWNER) ? Kind.TRIAL_SPAWNER :
                stack.is(Items.VAULT) ? Kind.VAULT : Kind.SPAWNER;
        var component = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag data = component != null ? component.copyTagWithoutId() : new CompoundTag();
        var blockState = stack.get(DataComponents.BLOCK_STATE);
        Map<String, String> properties = blockState != null ? blockState.properties() : Map.of();
        String lootTable = data.getCompoundOrEmpty("config").getStringOr("loot_table", "");
        boolean ominous = kind != Kind.SPAWNER && Boolean.parseBoolean(properties.getOrDefault("ominous",
                Boolean.toString(kind == Kind.VAULT && lootTable.endsWith("reward_ominous"))));
        long cooldownEndsAt = data.getLongOr("cooldown_ends_at", 0);
        String state = properties.getOrDefault(kind == Kind.VAULT ? "vault_state" : "trial_spawner_state",
                kind == Kind.TRIAL_SPAWNER && cooldownEndsAt > gameTime ? "cooldown" : "inactive");
        if (kind == Kind.TRIAL_SPAWNER && !List.of("inactive", "waiting_for_players", "active",
                "waiting_for_reward_ejection", "ejecting_reward", "cooldown").contains(state)) state = "inactive";
        if (kind == Kind.VAULT && !List.of("inactive", "active", "unlocking", "ejecting").contains(state)) state = "inactive";
        boolean cooling = kind == Kind.TRIAL_SPAWNER && state.equals("cooldown");
        List<CompoundTag> mobs = kind == Kind.VAULT ? List.of() : readMobs(kind, data, ominous, registries);
        String model = switch (kind)
        {
            case SPAWNER -> "spawner";
            case TRIAL_SPAWNER -> switch (state)
            {
                case "active", "waiting_for_players", "waiting_for_reward_ejection" -> "trial_spawner_active" + (ominous ? "_ominous" : "");
                case "ejecting_reward" -> "trial_spawner_ejecting_reward" + (ominous ? "_ominous" : "");
                default -> ominous ? "trial_spawner_inactive_ominous" : "trial_spawner";
            };
            case VAULT -> switch (state)
            {
                case "active" -> "vault_active" + (ominous ? "_ominous" : "");
                case "unlocking" -> "vault_unlocking" + (ominous ? "_ominous" : "");
                case "ejecting" -> "vault_ejecting_reward" + (ominous ? "_ominous" : "");
                default -> "vault" + (ominous ? "_ominous" : "");
            };
        };
        return new Info(kind, ominous, state, cooling, cooldownEndsAt, mobs,
                Identifier.fromNamespaceAndPath("halfmasa", "cages/" + model));
    }

    private static List<CompoundTag> readMobs(Kind kind, CompoundTag data, boolean ominous,
                                              HolderLookup.Provider registries)
    {
        String spawnKey = kind == Kind.SPAWNER ? "SpawnData" : "spawn_data";
        CompoundTag selected = data.getCompoundOrEmpty(spawnKey).getCompoundOrEmpty("entity");
        if (validMob(selected)) return List.of(selected.copy());
        if (kind == Kind.SPAWNER) return potentials(data, "SpawnPotentials");
        String configKey = ominous ? "ominous_config" : "normal_config";
        CompoundTag inline = data.getCompoundOrEmpty(configKey);
        if (!inline.isEmpty()) return potentials(inline, "spawn_potentials");
        String id = data.getStringOr(configKey, "");
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) return List.of();
        if (registries != null)
        {
            var lookup = registries.lookup(Registries.TRIAL_SPAWNER_CONFIG);
            if (lookup.isPresent())
            {
                var config = lookup.get().get(ResourceKey.create(Registries.TRIAL_SPAWNER_CONFIG, identifier));
                if (config.isPresent())
                    return distinct(config.get().value().spawnPotentialsDefinition().unwrap().stream()
                            .map(weighted -> weighted.value().getEntityToSpawn().copy()).toList());
            }
        }
        // These server-side configs are not necessarily present in a client's synced registries.
        // Vanilla's packaged definitions supply the same preview as the server defaults.
        if (!identifier.getNamespace().equals("minecraft") || !identifier.getPath().startsWith("trial_chamber/")) return List.of();
        return VANILLA_CONFIGS.computeIfAbsent(id, ignored -> loadVanillaConfig(identifier));
    }

    private static List<CompoundTag> loadVanillaConfig(Identifier id)
    {
        String resource = "/data/minecraft/trial_spawner/" + id.getPath() + ".json";
        var stream = SpawnerItemAppearance.class.getResourceAsStream(resource);
        if (stream == null) return List.of();
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
        {
            var tag = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, JsonParser.parseReader(reader));
            return tag instanceof CompoundTag compound ? potentials(compound, "spawn_potentials") : List.of();
        }
        catch (IOException exception)
        {
            XaeroWorldBinding.LOGGER.warn("Unable to read vanilla trial spawner preview {}", id, exception);
            return List.of();
        }
    }

    private static List<CompoundTag> potentials(CompoundTag config, String key)
    {
        var result = new ArrayList<CompoundTag>();
        var potentials = config.getListOrEmpty(key);
        for (int i = 0; i < potentials.size(); i++)
        {
            CompoundTag mob = potentials.getCompoundOrEmpty(i).getCompoundOrEmpty("data").getCompoundOrEmpty("entity");
            if (validMob(mob)) result.add(mob.copy());
        }
        return distinct(result);
    }

    private static boolean validMob(CompoundTag data)
    {
        Identifier id = Identifier.tryParse(data.getStringOr("id", ""));
        return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id);
    }

    private static List<CompoundTag> distinct(List<CompoundTag> mobs)
    {
        return mobs.stream().filter(SpawnerItemAppearance::validMob).distinct().toList();
    }

    public static Component mobName(CompoundTag data)
    {
        Identifier id = Identifier.tryParse(data.getStringOr("id", ""));
        var type = id != null ? BuiltInRegistries.ENTITY_TYPE.getValue(id) : null;
        Component name = type != null ? type.getDescription() : Component.translatable("halfmasa.spawner_item.unknown_mob");
        if (data.getBooleanOr("IsBaby", false)) name = Component.translatable("halfmasa.spawner_item.baby", name);
        if (data.contains("Size")) name = Component.translatable("halfmasa.spawner_item.size", name,
                1 << Math.clamp(data.getIntOr("Size", 0), 0, 6));
        return name;
    }

    public static void addTooltip(ItemStack stack, HolderLookup.Provider registries, long gameTime, List<Component> lines)
    {
        Info info = read(stack, registries, gameTime);
        if (info == null) return;
        String kind = info.kind().name().toLowerCase(java.util.Locale.ROOT) + (info.ominous() ? "_ominous" : "");
        lines.add(Component.translatable("halfmasa.spawner_item.type",
                Component.translatable("halfmasa.spawner_item.kind." + kind)).withStyle(ChatFormatting.GRAY));
        if (info.kind() != Kind.VAULT)
        {
            Component mobs = Component.empty();
            for (int i = 0; i < info.mobs().size(); i++)
            {
                if (i > 0) mobs = mobs.copy().append(Component.literal(", "));
                mobs = mobs.copy().append(mobName(info.mobs().get(i)));
            }
            if (info.mobs().isEmpty()) mobs = Component.translatable("halfmasa.spawner_item.unknown_mob");
            lines.add(Component.translatable("halfmasa.spawner_item.mobs", mobs).withStyle(ChatFormatting.GRAY));
        }
        if (info.kind() != Kind.SPAWNER)
            lines.add(Component.translatable("halfmasa.spawner_item.state",
                    Component.translatable("halfmasa.spawner_item.state." + info.state())).withStyle(ChatFormatting.GRAY));
        if (info.kind() == Kind.TRIAL_SPAWNER)
            lines.add(Component.translatable("halfmasa.spawner_item.cooling", Component.translatable(
                    info.cooling() ? "halfmasa.spawner_item.yes" : "halfmasa.spawner_item.no")).withStyle(ChatFormatting.GRAY));
    }
}
//#endif
