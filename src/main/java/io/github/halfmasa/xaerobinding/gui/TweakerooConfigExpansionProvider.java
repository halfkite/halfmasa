package io.github.halfmasa.xaerobinding.gui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.StringUtils;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.feature.CustomConfigGroupStore;
import io.github.halfmasa.xaerobinding.feature.CustomConfigSources;
import io.github.halfmasa.xaerobinding.feature.TweakerooFoldStateStore;

/** Builds explicit, optional Tweakeroo groups without a compile-time Tweakeroo dependency. */
public final class TweakerooConfigExpansionProvider implements ConfigExpansionProvider
{
    private static final String TWEAKEROO_CONFIG_KEY = "halfmasa.config.tweakeroo";
    private static final List<DuplicateHotkey> DUPLICATE_HOTKEYS = List.of(
            new DuplicateHotkey("FREE_CAMERA_PLAYER_INPUTS", "freeCameraPlayerInputs"),
            new DuplicateHotkey("FREE_CAMERA_PLAYER_MOVEMENT", "freeCameraPlayerMovement"));
    private static final Map<String, String> PRIMARY_CONFIGS = Map.ofEntries(
            Map.entry("accurate_placement_protocol", "accuratePlacementProtocol"),
            Map.entry("accurate_block_placement", "tweakAccurateBlockPlacement"),
            Map.entry("placement", "tweakPlacementRestriction"),
            Map.entry("area_selection", "tweakAreaSelector"),
            Map.entry("breaking_restriction", "tweakBlockTypeBreakRestriction"),
            Map.entry("fast_placement", "tweakFastBlockPlacement"),
            Map.entry("fast_left_click", "tweakFastLeftClick"),
            Map.entry("fast_right_click", "tweakFastRightClick"),
            Map.entry("flexible_placement", "tweakFlexibleBlockPlacement"),
            Map.entry("fly", "tweakFlySpeed"),
            Map.entry("free_camera", "tweakFreeCamera"),
            Map.entry("elytra_camera", "tweakElytraCamera"),
            Map.entry("gamma_darkness", "tweakGammaOverride"),
            Map.entry("hotbar", "tweakHotbarScroll"),
            Map.entry("inventory_preview", "tweakInventoryPreview"),
            Map.entry("periodic_actions", "tweakPeriodicAttack"),
            Map.entry("potion_warning", "tweakPotionWarning"),
            Map.entry("rendering", "tweakRenderLimitEntities"),
            Map.entry("selective_blocks", "tweakSelectiveBlocksRendering"),
            Map.entry("snap_aim", "tweakSnapAim"),
            Map.entry("zoom", "tweakZoom"),
            Map.entry("tool_swap", "tweakToolSwitch"),
            Map.entry("item_management", "tweakHandRestock"),
            Map.entry("chat", "tweakChatBackgroundColor"),
            Map.entry("server_sync", "tweakServerDataSync"),
            Map.entry("entity_interaction", "tweakBlockReachOverride"),
            Map.entry("structure", "tweakStructureBlockLimit"),
            Map.entry("creative_world", "tweakCreativeExtraItems"),
            Map.entry("sculk", "tweakSculkPulseLength"),
            Map.entry("after_clicker", "tweakAfterClicker"),
            Map.entry("movement_states", "tweakPermanentSneak"));

    private static final List<GroupDefinition> GROUPS = List.of(
            group("accurate_placement_protocol", "accuratePlacementProtocolMode"),
            group("accurate_block_placement", "accurateBlockPlacementInto", "accurateBlockPlacementReverse",
                    "toggleAccuratePlacementProtocol", "clientPlacementRotation", "clientPlacementValidation"),
            group("placement", "angelBlockPlacementDistance", "placementGridSize", "placementLimit",
                    "placementRestrictionMode", "placementRestrictionTiedToFast", "placementRestrictionModeColumn",
                    "placementRestrictionModeDiagonal", "placementRestrictionModeFace", "placementRestrictionModeLayer",
                    "placementRestrictionModeLine", "placementRestrictionModePlane", "placementYMirror",
                    "scaffoldPlaceDistance", "scaffoldPlaceVanilla", "tweakAngelBlock", "tweakBreakReplace",
                    "tweakPlacementGrid", "tweakPlacementLimit", "tweakPlacementRestriction",
                    "tweakPlacementRestrictionFirst", "tweakPlacementRestrictionHand", "tweakScaffoldPlace",
                    "tweakYMirror"),
            group("area_selection", "areaSelectionUseAll", "areaSelectionOffset", "areaSelectionAddToList",
                    "areaSelectionRemoveFromList", "tweakAreaSelector"),
            group("breaking_restriction", "blockTypeBreakRestrictionWarn", "blockTypeBreakRestrictionListType",
                    "blockTypeBreakRestrictionBlackList", "blockTypeBreakRestrictionWhiteList", "breakingGridSize",
                    "restrictionLayerHeight", "breakingRestrictionMode", "breakingRestrictionModeColumn",
                    "breakingRestrictionModeDiagonal", "breakingRestrictionModeFace", "breakingRestrictionModeLayer",
                    "breakingRestrictionModeLine", "breakingRestrictionModePlane", "tweakBlockTypeBreakRestriction",
                    "tweakBreakingGrid", "tweakBreakingRestriction"),
            group("fast_placement", "fastBlockPlacementCount", "fastLeftClickAllowTools",
                    "fastPlacementRememberOrientation", "fastPlacementItemListType", "fastPlacementItemBlackList",
                    "fastPlacementItemWhiteList", "tweakFastBlockPlacement"),
            group("fast_left_click", "fastLeftClickCount", "fastLeftClickAllowTools"),
            group("fast_right_click", "fastRightClickCount", "fastRightClickBlockListType",
                    "fastRightClickBlockBlackList", "fastRightClickBlockWhiteList", "fastRightClickListType",
                    "fastRightClickBlackList", "fastRightClickWhiteList"),
            group("flexible_placement", "flexibleBlockPlacementOverlayColor", "rememberFlexibleFromClick",
                    "flexibleBlockPlacementAdjacent", "flexibleBlockPlacementOffset", "flexibleBlockPlacementRotation",
                    "tweakFlexibleBlockPlacement"),
            group("fly", "flyDecelerationFactor", "flySpeedPreset1", "flySpeedPreset2", "flySpeedPreset3",
                    "flySpeedPreset4", "flySpeedIncrement1", "flySpeedIncrement2", "flyPreset1", "flyPreset2",
                    "flyPreset3", "flyPreset4", "flyIncrement1", "flyIncrement2", "tweakCustomFlyDeceleration",
                    "tweakFlySpeed"),
            group("free_camera", "freeCameraPlayerInputs", "freeCameraPlayerMovement", "freeCameraShowHands",
                    "freeCameraShowHotBar", "freeCameraShowStatusBars", "freeCameraPresetAdd", "freeCameraPresetCycle",
                    "freeCameraPresetDelete", "freeCameraPresetDeleteAll", "openCameraPresetEditorGui"),
            group("elytra_camera", "elytraCameraIndicator", "elytraCamera", "swapElytraChestplate",
                    "rocketSwapAllowExplosions", "tweakAutoSwitchElytra", "tweakAutoSwitchRockets",
                    "tweakElytraCamera"),
            group("gamma_darkness", "gammaOverrideValue", "darknessScaleOverrideValue", "tweakDarknessVisibility",
                    "tweakGammaOverride", "tweakLavaVisibility", "tweakMatchingSkyFog", "tweakWaterVisibility"),
            group("hotbar", "hotbarSlotCycleMax", "hotbarSlotRandomizerMax", "hotbarSwapOverlayAlignment",
                    "hotbarSwapOverlayOffsetX", "hotbarSwapOverlayOffsetY", "hotbarScroll", "hotbarSwapBase",
                    "hotbarSwap1", "hotbarSwap2", "hotbarSwap3", "tweakHotbarScroll", "tweakHotbarSlotCycle",
                    "tweakHotbarSlotRandomizer", "tweakHotbarSwap"),
            group("inventory_preview", "customInventoryGuiScale", "inventoryPreviewVillagerBGColor",
                    "bundleDisplayBgColor", "bundleDisplayRequireShift", "bundleDisplayRowWidth", "mapPreviewRequireShift",
                    "mapPreviewSize", "shulkerDisplayBgColor", "shulkerDisplayEnderChest", "shulkerDisplayRequireShift",
                    "inventoryPreview", "inventoryPreviewToggleScreen", "playerInventoryPeek", "tweakBundleDisplay",
                    "tweakInventoryPreview", "tweakMapPreview", "tweakPlayerInventoryPeek", "tweakShulkerBoxDisplay",
                    "tweakCustomInventoryScreenScale"),
            group("periodic_actions", "periodicAttackInterval", "periodicAttackResetIntervalOnActivate",
                    "periodicUseInterval", "periodicUseResetIntervalOnActivate", "periodicHoldAttackDuration",
                    "periodicHoldAttackInterval", "periodicHoldAttackResetIntervalOnActivate", "periodicHoldUseDuration",
                    "periodicHoldUseInterval", "periodicHoldUseResetIntervalOnActivate", "tweakHoldAttack",
                    "tweakHoldUse", "tweakPeriodicAttack", "tweakPeriodicHoldAttack", "tweakPeriodicHoldUse",
                    "tweakPeriodicUse"),
            group("potion_warning", "potionWarningBeneficialOnly", "potionWarningThreshold", "potionWarningListType",
                    "potionWarningBlackList", "potionWarningWhiteList", "tweakPotionWarning"),
            group("rendering", "renderLimitItem", "renderLimitXPOrb", "skipAllRendering", "skipWorldRendering",
                    "tweakExplosionReducedParticles", "tweakF3Cursor", "tweakPlayerListAlwaysVisible",
                    "tweakRenderEdgeChunks", "tweakRenderInvisibleEntities", "tweakRenderLimitEntities", "writeMapsAsImages"),
            group("selective_blocks", "selectiveBlocksTrackPistons", "selectiveBlocksHideParticles",
                    "selectiveBlocksHideEntities", "selectiveBlocksNoHit", "selectiveBlocksListType",
                    "selectiveBlocksWhitelist", "selectiveBlocksBlacklist", "tweakSelectiveBlocksRendering",
                    "tweakSelectiveBlocksRenderOutline"),
            group("snap_aim", "snapAimIndicator", "snapAimIndicatorColor", "snapAimMode", "snapAimOnlyCloseToAngle",
                    "snapAimPitchOvershoot", "snapAimPitchStep", "snapAimThresholdPitch", "snapAimThresholdYaw",
                    "snapAimYawStep", "tweakAimLock", "tweakSnapAim", "tweakSnapAimLock"),
            group("zoom", "zoomAdjustMouseSensitivity", "zoomFov", "zoomFovDifference", "zoomFovDifferenceCtrl",
                    "zoomResetFovOnActivate", "zoomActivate", "tweakSpyglassUsesTweakZoom", "tweakZoom"),
            group("tool_swap", "toolSwitchableSlots", "toolSwitchIgnoredSlots", "toolPick", "itemSwapDurabilityThreshold",
                    "swapSpyglassAndActivate", "toolSwapAllowUnenchantedToBreak", "toolSwapBetterEnchants",
                    "toolSwapPreferFortuneOverride", "toolSwapPreferSilkTouch", "toolSwapBambooUsesSwordFirst",
                    "toolSwapLeavesUsesHoeFirst", "toolSwapNeedsShearsFirst", "toolSwapNeedsPickaxeFirst",
                    "toolSwapSilkTouchFirst", "toolSwapSilkTouchOres", "toolSwapSilkTouchOverride",
                    "toolSwapPickaxeOverride", "weaponSwapBetterEnchants", "tweakPickBeforePlace",
                    "tweakSwapAlmostBrokenTools", "tweakToolSwitch", "tweakWeaponSwitch", "pickaxeOverride",
                    "silkTouchOverride"),
            group("item_management", "handRestockPre", "handRestockPreThreshold", "handRestockListType",
                    "handRestockBlackList", "handRestockWhiteList", "repairModeSlots", "unstackingItems",
                    "tweakEmptyShulkerBoxesStack", "tweakHandRestock", "tweakItemUnstackingProtection", "tweakRepairMode"),
            group("chat", "chatBackgroundColor", "chatTimeFormat", "copySignText", "tweakChatBackgroundColor",
                    "tweakChatPersistentText", "tweakChatTimestamp", "tweakCommandBlockExtraFields", "tweakPrintDeathCoordinates",
                    "tweakSignCopy", "tweakTabCompleteCoordinate"),
            group("server_sync", "entityDataSync", "entityDataSyncBackup", "entityDataSyncCacheRefresh",
                    "entityDataSyncCacheTimeout", "serverDataSyncCacheRefresh", "serverDataSyncCacheTimeout",
                    "serverNbtRequestRate", "slotSyncWorkaround", "slotSyncWorkaroundAlways", "tweakServerDataSync",
                    "tweakServerDataSyncBackup"),
            group("entity_interaction", "blockReachDistance", "entityReachDistance", "entityTypeAttackRestrictionWarn",
                    "entityTypeAttackRestrictionListType", "entityTypeAttackRestrictionBlackList",
                    "entityTypeAttackRestrictionWhiteList", "entityWeaponMapping", "hangableEntityBypassInverse",
                    "tweakBlockReachOverride", "tweakEntityReachOverride", "tweakEntityTypeAttackRestriction",
                    "tweakHangableEntityBypass"),
            group("structure", "fillCloneLimit", "structureBlockMaxSize", "tweakFillCloneLimit",
                    "tweakStructureBlockLimit"),
            group("creative_world", "creativeExtraItems", "flatWorldPresets", "tweakCreativeExtraItems",
                    "tweakCustomFlatPresets"),
            group("sculk", "sculkSensorPulseLength", "tweakSculkPulseLength"),
            group("after_clicker", "afterClickerClickCount", "tweakAfterClicker"),
            group("movement_states", "permanentSneakAllowInGUIs", "tweakFakeSneaking", "tweakFakeSneakPlacement",
                    "tweakMovementKeysLast", "tweakPermanentSneak", "tweakPermanentSprint", "tweakSneak_1.15.2",
                    "tweakSpectatorTeleport"),
            group("pets", "sitDownNearbyPets", "standUpNearbyPets"));

    private final Map<IConfigBase, IConfigBase> parents = new IdentityHashMap<>();
    private final Map<IConfigBase, List<IConfigBase>> children = new IdentityHashMap<>();
    private final Map<IConfigBase, String> groupIds = new IdentityHashMap<>();
    private final Map<IConfigBase, ConfigBoolean> expansionConfigs = new IdentityHashMap<>();
    private final Map<IConfigBase, ConfigOptionWrapper> primaryWrappers = new IdentityHashMap<>();
    private final Map<IConfigBase, List<ConfigOptionWrapper>> groupedChildren = new IdentityHashMap<>();
    private final Map<IConfigBase, IConfigBase> primaryByConfig = new IdentityHashMap<>();
    private final Map<String, IConfigBase> inlineHotkeys = new HashMap<>();
    private final Set<String> inlineCompanions = new HashSet<>();
    private final HalfMasaConfigExpansionProvider customProvider =
            new HalfMasaConfigExpansionProvider(CustomConfigGroupStore.TWEAKEROO_SOURCE);

    @Override
    public String getConfigSource()
    {
        return CustomConfigGroupStore.TWEAKEROO_SOURCE;
    }

    @Override
    public Collection<ConfigOptionWrapper> prepareEntries(
            Collection<ConfigOptionWrapper> entries,
            boolean searchActive)
    {
        this.inlineHotkeys.clear();
        this.inlineCompanions.clear();
        List<ConfigOptionWrapper> source = new ArrayList<>(entries);
        this.normalizeDuplicateHotkeys(source);
        this.collectInlineFlightPairs(source);
        if (Configs.CUSTOM_CONFIG_GROUPS.getBooleanValue())
        {
            CustomConfigGroupStore.getInstance().ensureDefaults(
                    CustomConfigGroupStore.TWEAKEROO_SOURCE, getBuiltInGroupTemplates());
            return this.customProvider.prepareEntries(source, searchActive);
        }
        this.parents.clear();
        this.children.clear();
        this.groupIds.clear();
        this.expansionConfigs.clear();
        this.primaryWrappers.clear();
        this.groupedChildren.clear();
        this.primaryByConfig.clear();

        this.addMissingPrimaryGroupEntries(source);
        Map<String, GroupDefinition> definitions = GROUPS.stream()
                .flatMap(group -> group.members().stream().map(name -> Map.entry(name, group)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, ignored) -> first));
        Map<GroupDefinition, List<ConfigOptionWrapper>> grouped = new LinkedHashMap<>();
        for (ConfigOptionWrapper wrapper : source)
        {
            IConfigBase config = wrapper.getConfig();
            if (config == null || isDisableOption(config))
            {
                continue;
            }
            GroupDefinition definition = definitions.get(config.getName());
            if (definition != null)
            {
                grouped.computeIfAbsent(definition, ignored -> new ArrayList<>()).add(wrapper);
            }
        }

        for (Map.Entry<GroupDefinition, List<ConfigOptionWrapper>> entry : grouped.entrySet())
        {
            Map<String, ConfigOptionWrapper> byName = new HashMap<>();
            for (ConfigOptionWrapper wrapper : entry.getValue())
            {
                if (wrapper.getConfig() != null)
                {
                    byName.putIfAbsent(wrapper.getConfig().getName(), wrapper);
                }
            }
            List<ConfigOptionWrapper> ordered = new ArrayList<>();
            for (String name : entry.getKey().members())
            {
                ConfigOptionWrapper wrapper = byName.get(name);
                if (wrapper != null)
                {
                    ordered.add(wrapper);
                }
            }
            entry.setValue(ordered);
        }

        Set<GroupDefinition> usable = grouped.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        for (GroupDefinition definition : GROUPS)
        {
            if (!usable.contains(definition))
            {
                continue;
            }
            ConfigBoolean expansion = new ConfigBoolean(
                    "tweakerooGroup." + definition.id(),
                    TweakerooFoldStateStore.isExpanded(definition.id()))
                    .apply(TWEAKEROO_CONFIG_KEY);
            ConfigOptionWrapper primary = grouped.get(definition).stream()
                    .filter(wrapper -> wrapper.getConfig() != null &&
                            wrapper.getConfig().getName().equals(definition.primary()))
                    .findFirst()
                    .orElse(grouped.get(definition).get(0));
            IConfigBase primaryConfig = primary.getConfig();
            this.groupIds.put(primaryConfig, definition.id());
            this.expansionConfigs.put(primaryConfig, expansion);
            this.primaryWrappers.put(primaryConfig, primary);

            List<ConfigOptionWrapper> rest = grouped.get(definition).stream()
                    .filter(wrapper -> wrapper != primary)
                    .toList();
            this.groupedChildren.put(primaryConfig, rest);
            this.children.put(primaryConfig, rest.stream()
                    .map(ConfigOptionWrapper::getConfig)
                    .toList());
            for (ConfigOptionWrapper wrapper : grouped.get(definition))
            {
                IConfigBase config = wrapper.getConfig();
                if (config != null)
                {
                    this.primaryByConfig.put(config, primaryConfig);
                    if (config != primaryConfig)
                    {
                        this.parents.put(config, primaryConfig);
                    }
                }
            }
        }

        List<ConfigOptionWrapper> result = new ArrayList<>(source.size());
        Set<IConfigBase> emitted = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ConfigOptionWrapper wrapper : source)
        {
            IConfigBase config = wrapper.getConfig();
            IConfigBase primaryConfig = config == null ? null : this.primaryByConfig.get(config);
            if (primaryConfig == null)
            {
                result.add(wrapper);
                continue;
            }
            if (emitted.add(primaryConfig))
            {
                ConfigOptionWrapper primary = this.primaryWrappers.get(primaryConfig);
                if (primary != null)
                {
                    result.add(primary);
                }
                ConfigBoolean expansion = this.expansionConfigs.get(primaryConfig);
                if (expansion != null && (expansion.getBooleanValue() || searchActive))
                {
                    result.addAll(this.groupedChildren.getOrDefault(primaryConfig, List.of()));
                }
            }
        }
        return result;
    }

    @Override
    public IConfigBase getInlineCompanion(IConfigBase config)
    {
        return config == null ? null : this.inlineHotkeys.get(config.getName());
    }

    @Override
    public boolean isInlineCompanion(IConfigBase config)
    {
        return config != null && this.inlineCompanions.contains(config.getName());
    }

    @Override
    public IConfigBase getExpansionParent(IConfigBase config)
    {
        if (Configs.CUSTOM_CONFIG_GROUPS.getBooleanValue())
        {
            return this.customProvider.getExpansionParent(config);
        }
        return this.parents.get(config);
    }

    @Override
    public List<IConfigBase> getExpansionChildren(IConfigBase config)
    {
        if (Configs.CUSTOM_CONFIG_GROUPS.getBooleanValue())
        {
            return this.customProvider.getExpansionChildren(config);
        }
        return this.children.getOrDefault(config, List.of());
    }

    @Override
    public ConfigBoolean getExpansionConfig(IConfigBase config)
    {
        if (Configs.CUSTOM_CONFIG_GROUPS.getBooleanValue())
        {
            return this.customProvider.getExpansionConfig(config);
        }
        return this.expansionConfigs.get(config);
    }

    @Override
    public void onExpansionChanged(IConfigBase config, boolean expanded)
    {
        if (Configs.CUSTOM_CONFIG_GROUPS.getBooleanValue())
        {
            this.customProvider.onExpansionChanged(config, expanded);
            return;
        }
        String id = this.groupIds.get(config);
        if (id != null)
        {
            TweakerooFoldStateStore.setExpanded(id, expanded);
        }
    }

    private static boolean isDisableOption(IConfigBase config)
    {
        return config.getName().startsWith("disable");
    }

    private void normalizeDuplicateHotkeys(List<ConfigOptionWrapper> entries)
    {
        boolean changed = false;
        for (DuplicateHotkey duplicate : DUPLICATE_HOTKEYS)
        {
            HotkeyPair pair = resolveDuplicateHotkey(duplicate.fieldName());
            if (pair == null)
            {
                continue;
            }
            String legacyKeys = pair.legacy().getKeybind().getStringValue();
            if (!legacyKeys.isEmpty())
            {
                if (pair.preferred().getKeybind().getKeys().isEmpty())
                {
                    pair.preferred().getKeybind().setValueFromString(legacyKeys);
                }
                pair.legacy().getKeybind().setValueFromString("");
                changed = true;
            }
            entries.removeIf(wrapper -> wrapper.getConfig() == pair.legacy() ||
                    (wrapper.getConfig() != null &&
                            duplicate.configName().equals(wrapper.getConfig().getName()) &&
                            !(wrapper.getConfig() instanceof ConfigBooleanHotkeyed)));
        }
        if (changed)
        {
            saveTweakerooConfig();
        }
    }

    private static HotkeyPair resolveDuplicateHotkey(String fieldName)
    {
        try
        {
            ClassLoader loader = TweakerooConfigExpansionProvider.class.getClassLoader();
            Class<?> genericClass = Class.forName(
                    "fi.dy.masa.tweakeroo.config.Configs$Generic", false, loader);
            Class<?> hotkeysClass = Class.forName(
                    "fi.dy.masa.tweakeroo.config.Hotkeys", false, loader);
            Field preferredField = genericClass.getField(fieldName);
            Field legacyField = hotkeysClass.getField(fieldName);
            Object preferred = preferredField.get(null);
            Object legacy = legacyField.get(null);
            if (preferred instanceof ConfigBooleanHotkeyed preferredHotkey && legacy instanceof IHotkey legacyHotkey)
            {
                return new HotkeyPair(preferredHotkey, legacyHotkey);
            }
        }
        catch (Throwable throwable)
        {
            XaeroWorldBinding.LOGGER.debug("Unable to resolve Tweakeroo duplicate hotkey {}", fieldName, throwable);
        }
        return null;
    }

    private static void saveTweakerooConfig()
    {
        try
        {
            Class<?> configsClass = Class.forName(
                    "fi.dy.masa.tweakeroo.config.Configs", false,
                    TweakerooConfigExpansionProvider.class.getClassLoader());
            Method save = configsClass.getMethod("saveToFile");
            save.invoke(null);
        }
        catch (Throwable throwable)
        {
            XaeroWorldBinding.LOGGER.warn("Failed to save migrated Tweakeroo hotkeys", throwable);
        }
    }

    public static List<CustomConfigGroupStore.GroupTemplate> getBuiltInGroupTemplates()
    {
        List<CustomConfigGroupStore.GroupTemplate> templates = new ArrayList<>();
        for (GroupDefinition definition : GROUPS)
        {
            if (definition.members().size() < 2)
            {
                continue;
            }
            String main = definition.primary();
            if (main == null || main.isBlank())
            {
                main = definition.members().get(0);
            }
            String selectedMain = main;
            List<String> children = definition.members().stream()
                    .filter(name -> !name.equals(selectedMain))
                    .toList();
            templates.add(new CustomConfigGroupStore.GroupTemplate(
                    "builtin_tweakeroo_" + definition.id(),
                    StringUtils.translate(TWEAKEROO_CONFIG_KEY + ".name." + definition.id()),
                    main,
                    children));
        }
        return List.copyOf(templates);
    }

    private static GroupDefinition group(String id, String... members)
    {
        List<String> ordered = new ArrayList<>(members.length + 1);
        String primary = PRIMARY_CONFIGS.get(id);
        if (primary != null && !primary.isBlank())
        {
            ordered.add(primary);
        }
        for (String member : members)
        {
            if (!ordered.contains(member))
            {
                ordered.add(member);
            }
        }
        return new GroupDefinition(id, List.copyOf(ordered), primary);
    }

    private void addMissingPrimaryGroupEntries(List<ConfigOptionWrapper> entries)
    {
        Set<String> names = entries.stream()
                .map(ConfigOptionWrapper::getConfig)
                .filter(config -> config != null)
                .map(IConfigBase::getName)
                .collect(Collectors.toSet());
        Map<String, IConfigBase> available = new HashMap<>();
        for (CustomConfigSources.Candidate candidate :
                CustomConfigSources.getCandidates(CustomConfigSources.TWEAKEROO))
        {
            available.putIfAbsent(candidate.name(), candidate.config());
        }

        for (GroupDefinition definition : GROUPS)
        {
            String primary = definition.primary();
            if (primary == null || !names.contains(primary))
            {
                continue;
            }
            for (String member : definition.members())
            {
                IConfigBase config = available.get(member);
                if (config != null && names.add(member))
                {
                    entries.add(ConfigOptionWrapper.createFor(List.of(config)).get(0));
                }
            }
        }
    }

    private void collectInlineFlightPairs(List<ConfigOptionWrapper> entries)
    {
        Set<String> names = entries.stream()
                .map(ConfigOptionWrapper::getConfig)
                .filter(config -> config != null)
                .map(IConfigBase::getName)
                .collect(Collectors.toSet());
        Map<String, IConfigBase> available = new HashMap<>();
        for (ConfigOptionWrapper entry : entries)
        {
            IConfigBase config = entry.getConfig();
            if (config != null)
            {
                available.putIfAbsent(config.getName(), config);
            }
        }
        for (CustomConfigSources.Candidate candidate :
                CustomConfigSources.getCandidates(CustomConfigSources.TWEAKEROO))
        {
            available.putIfAbsent(candidate.name(), candidate.config());
        }
        for (int index = 1; index <= 4; index++)
        {
            this.collectInlinePair(names, available,
                    "flySpeedPreset" + index, "flyPreset" + index);
        }
        for (int index = 1; index <= 2; index++)
        {
            this.collectInlinePair(names, available,
                    "flySpeedIncrement" + index, "flyIncrement" + index);
        }
    }

    private void collectInlinePair(
            Set<String> names, Map<String, IConfigBase> available, String valueName, String hotkeyName)
    {
        if (!names.contains(valueName))
        {
            return;
        }
        IConfigBase hotkey = available.get(hotkeyName);
        if (hotkey == null)
        {
            return;
        }
        this.inlineHotkeys.put(valueName, hotkey);
        if (names.contains(hotkeyName))
        {
            this.inlineCompanions.add(hotkeyName);
        }
    }

    private record GroupDefinition(String id, List<String> members, String primary)
    {
    }

    private record DuplicateHotkey(String fieldName, String configName)
    {
    }

    private record HotkeyPair(ConfigBooleanHotkeyed preferred, IHotkey legacy)
    {
    }
}
