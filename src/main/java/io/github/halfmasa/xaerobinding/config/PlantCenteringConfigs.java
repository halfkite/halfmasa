package io.github.halfmasa.xaerobinding.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.hotkeys.IHotkey;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Configurations for removing the vanilla random offset from decorative plants. */
public final class PlantCenteringConfigs
{
    private static final String TRANSLATION_KEY = "halfmasa.config.ported";

    public static final ConfigBooleanHotkeyed CENTER_PLANT_MODELS = new ConfigBooleanHotkeyed(
            "centerPlantModels", false, "").apply(TRANSLATION_KEY);
    public static final ConfigBoolean CENTER_PLANT_MODELS_EXPANDED = new ConfigBoolean(
            "centerPlantModelsExpanded", false).apply(TRANSLATION_KEY);

    private static final Map<Block, ConfigBooleanHotkeyed> BY_BLOCK = new LinkedHashMap<>();

    public static final ConfigBoolean DANDELION = add("dandelion", Blocks.DANDELION);
    public static final ConfigBoolean POPPY = add("poppy", Blocks.POPPY);
    public static final ConfigBoolean BLUE_ORCHID = add("blue_orchid", Blocks.BLUE_ORCHID);
    public static final ConfigBoolean ALLIUM = add("allium", Blocks.ALLIUM);
    public static final ConfigBoolean AZURE_BLUET = add("azure_bluet", Blocks.AZURE_BLUET);
    public static final ConfigBoolean RED_TULIP = add("red_tulip", Blocks.RED_TULIP);
    public static final ConfigBoolean ORANGE_TULIP = add("orange_tulip", Blocks.ORANGE_TULIP);
    public static final ConfigBoolean WHITE_TULIP = add("white_tulip", Blocks.WHITE_TULIP);
    public static final ConfigBoolean PINK_TULIP = add("pink_tulip", Blocks.PINK_TULIP);
    public static final ConfigBoolean OXEYE_DAISY = add("oxeye_daisy", Blocks.OXEYE_DAISY);
    public static final ConfigBoolean CORNFLOWER = add("cornflower", Blocks.CORNFLOWER);
    public static final ConfigBoolean LILY_OF_THE_VALLEY = add("lily_of_the_valley", Blocks.LILY_OF_THE_VALLEY);
    public static final ConfigBoolean WITHER_ROSE = add("wither_rose", Blocks.WITHER_ROSE);
    public static final ConfigBoolean TORCHFLOWER = add("torchflower", Blocks.TORCHFLOWER);
    public static final ConfigBoolean PITCHER_PLANT = add("pitcher_plant", Blocks.PITCHER_PLANT);
    public static final ConfigBoolean SUNFLOWER = add("sunflower", Blocks.SUNFLOWER);
    public static final ConfigBoolean LILAC = add("lilac", Blocks.LILAC);
    public static final ConfigBoolean ROSE_BUSH = add("rose_bush", Blocks.ROSE_BUSH);
    public static final ConfigBoolean PEONY = add("peony", Blocks.PEONY);

    public static final ConfigBoolean SHORT_GRASS = add("short_grass", Blocks.SHORT_GRASS);
    public static final ConfigBoolean FERN = add("fern", Blocks.FERN);
    public static final ConfigBoolean TALL_GRASS = add("tall_grass", Blocks.TALL_GRASS);
    public static final ConfigBoolean LARGE_FERN = add("large_fern", Blocks.LARGE_FERN);
    public static final ConfigBoolean DEAD_BUSH = add("dead_bush", Blocks.DEAD_BUSH);
    public static final ConfigBoolean SEAGRASS = add("seagrass", Blocks.SEAGRASS);
    public static final ConfigBoolean TALL_SEAGRASS = add("tall_seagrass", Blocks.TALL_SEAGRASS);

    public static final ConfigBoolean BROWN_MUSHROOM = add("brown_mushroom", Blocks.BROWN_MUSHROOM);
    public static final ConfigBoolean RED_MUSHROOM = add("red_mushroom", Blocks.RED_MUSHROOM);
    public static final ConfigBoolean CRIMSON_FUNGUS = add("crimson_fungus", Blocks.CRIMSON_FUNGUS);
    public static final ConfigBoolean WARPED_FUNGUS = add("warped_fungus", Blocks.WARPED_FUNGUS);
    public static final ConfigBoolean CRIMSON_ROOTS = add("crimson_roots", Blocks.CRIMSON_ROOTS);
    public static final ConfigBoolean WARPED_ROOTS = add("warped_roots", Blocks.WARPED_ROOTS);
    public static final ConfigBoolean NETHER_SPROUTS = add("nether_sprouts", Blocks.NETHER_SPROUTS);

    public static final ConfigBoolean SWEET_BERRY_BUSH = add("sweet_berry_bush", Blocks.SWEET_BERRY_BUSH);
    public static final ConfigBoolean AZALEA = add("azalea", Blocks.AZALEA);
    public static final ConfigBoolean FLOWERING_AZALEA = add("flowering_azalea", Blocks.FLOWERING_AZALEA);
    public static final ConfigBoolean MANGROVE_PROPAGULE = add("mangrove_propagule", Blocks.MANGROVE_PROPAGULE);
    public static final ConfigBoolean BAMBOO_SAPLING = add("bamboo_sapling", Blocks.BAMBOO_SAPLING);
    public static final ConfigBoolean BAMBOO = add("bamboo", Blocks.BAMBOO);
    public static final ConfigBoolean HANGING_ROOTS = add("hanging_roots", Blocks.HANGING_ROOTS);
    public static final ConfigBoolean BIG_DRIPLEAF = add("big_dripleaf", Blocks.BIG_DRIPLEAF);
    public static final ConfigBoolean BIG_DRIPLEAF_STEM = add("big_dripleaf_stem", Blocks.BIG_DRIPLEAF_STEM);
    public static final ConfigBoolean SMALL_DRIPLEAF = add("small_dripleaf", Blocks.SMALL_DRIPLEAF);
    public static final ConfigBoolean SPORE_BLOSSOM = add("spore_blossom", Blocks.SPORE_BLOSSOM);
    public static final ConfigBoolean GLOW_LICHEN = add("glow_lichen", Blocks.GLOW_LICHEN);
    public static final ConfigBoolean VINE = add("vine", Blocks.VINE);
    public static final ConfigBoolean CAVE_VINES = add("cave_vines", Blocks.CAVE_VINES);
    public static final ConfigBoolean CAVE_VINES_PLANT = add("cave_vines_plant", Blocks.CAVE_VINES_PLANT);
    public static final ConfigBoolean WEEPING_VINES = add("weeping_vines", Blocks.WEEPING_VINES);
    public static final ConfigBoolean WEEPING_VINES_PLANT = add("weeping_vines_plant", Blocks.WEEPING_VINES_PLANT);
    public static final ConfigBoolean TWISTING_VINES = add("twisting_vines", Blocks.TWISTING_VINES);
    public static final ConfigBoolean TWISTING_VINES_PLANT = add("twisting_vines_plant", Blocks.TWISTING_VINES_PLANT);
    public static final ConfigBoolean KELP = add("kelp", Blocks.KELP);
    public static final ConfigBoolean KELP_PLANT = add("kelp_plant", Blocks.KELP_PLANT);
    public static final ConfigBoolean LILY_PAD = add("lily_pad", Blocks.LILY_PAD);

    public static final List<IConfigBase> CHILDREN = List.copyOf(BY_BLOCK.values());
    public static final List<IHotkey> HOTKEYS = Stream.concat(
            Stream.of((IHotkey) CENTER_PLANT_MODELS),
            CHILDREN.stream().map(config -> (IHotkey) config)).toList();
    public static final List<IConfigBase> ALL;

    static
    {
        java.util.ArrayList<IConfigBase> configs = new java.util.ArrayList<>(List.of(
                CENTER_PLANT_MODELS,
                CENTER_PLANT_MODELS_EXPANDED));
        configs.addAll(CHILDREN);
        ALL = List.copyOf(configs);
    }

    private PlantCenteringConfigs()
    {
    }

    private static ConfigBoolean add(String blockName, Block block)
    {
        ConfigBooleanHotkeyed config = new ConfigBooleanHotkeyed("centerPlant_" + blockName, true, "")
                .apply(TRANSLATION_KEY);
        config.setTranslatedName("block.minecraft." + blockName);
        config.setPrettyName("block.minecraft." + blockName);
        config.setComment("halfmasa.config.ported.comment.centerPlant");
        BY_BLOCK.put(block, config);
        return config;
    }

    public static boolean isEnabled(Block block)
    {
        ConfigBoolean config = BY_BLOCK.get(block);
        return CENTER_PLANT_MODELS.getBooleanValue() && config != null && config.getBooleanValue();
    }
}
