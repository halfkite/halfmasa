package io.github.halfmasa.xaerobinding.gui;

//#if MC >= 1.21.1
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
//#if MC >= 1.21.11
import fi.dy.masa.malilib.render.GuiContext;
//#endif
//#if MC >= 26.1
import net.minecraft.client.gui.GuiGraphicsExtractor;
//#else
//$$ import net.minecraft.client.gui.GuiGraphics;
//#endif
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import io.github.halfmasa.xaerobinding.feature.EntityGlowRule;

/** MaLiLib-style searchable registry picker with direct, explicit result actions. */
public final class EntityGlowSearchScreen extends GuiBase
{
    private final EntityGlowRule.TargetType type;
    //#if MC >= 1.21.11
    private final Consumer<Identifier> selected;
    //#else
    //$$ private final Consumer<ResourceLocation> selected;
    //#endif
    private final List<SearchEntry> entries = new ArrayList<>();
    private String query = "";
    private int page;
    private boolean buttonsDirty;

    //#if MC >= 1.21.11
    public EntityGlowSearchScreen(Screen parent, EntityGlowRule.TargetType type, Consumer<Identifier> selected)
    //#else
    //$$ public EntityGlowSearchScreen(Screen parent, EntityGlowRule.TargetType type, Consumer<ResourceLocation> selected)
    //#endif
    {
        this.setParent(parent);
        this.setTitle(Component.translatable(type == EntityGlowRule.TargetType.ENTITY
                ? "halfmasa.entity_glow.search_entities" : type == EntityGlowRule.TargetType.ITEM
                ? "halfmasa.entity_glow.search_items" : "halfmasa.entity_glow.search_blocks").getString());
        this.type = type;
        this.selected = selected;
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.loadEntries();
        int contentWidth = Math.min(550, this.width - 24);
        GuiTextFieldGeneric search = new GuiTextFieldGeneric(
                (this.width - contentWidth) / 2, 36, contentWidth, 20, this.font);
        this.addTextField(search, field -> {
            //#if MC >= 1.21.11
            this.query = field.getValueWrapper();
            //#else
            //$$ this.query = field.getTextWrapper();
            //#endif
            this.page = 0;
            this.buttonsDirty = true;
            return true;
        });
        search.setMaxLengthWrapper(256);
        //#if MC >= 1.21.11
        search.setValueWrapper(this.query);
        //#else
        //$$ search.setTextWrapper(this.query);
        //#endif
        search.setCursorPosition(0);
        search.setHighlightPos(0);
        this.rebuildButtons();
    }

    @Override
    //#if MC >= 26.1
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    //#elseif MC >= 1.21.11
    //$$ protected void drawContents(GuiContext graphics, int mouseX, int mouseY, float partialTick)
    //#else
    //$$ protected void drawContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    //#endif
    {
        if (this.buttonsDirty)
        {
            this.rebuildButtons();
            this.buttonsDirty = false;
        }
        //#if MC >= 26.1
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        //#else
        //$$ super.drawContents(graphics, mouseX, mouseY, partialTick);
        //#endif
    }

    private void loadEntries()
    {
        this.entries.clear();
        switch (this.type)
        {
            case ENTITY -> {
                for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE)
                {
                    this.entries.add(new SearchEntry(BuiltInRegistries.ENTITY_TYPE.getKey(entityType),
                            entityType.getDescription().getString()));
                }
            }
            case ITEM -> {
                for (Item item : BuiltInRegistries.ITEM)
                {
                    if (item != Items.AIR)
                    {
                        this.entries.add(new SearchEntry(BuiltInRegistries.ITEM.getKey(item),
                                item.getDefaultInstance().getHoverName().getString()));
                    }
                }
            }
            case BLOCK -> {
                for (Block block : BuiltInRegistries.BLOCK)
                {
                    if (block != Blocks.AIR)
                    {
                        this.entries.add(new SearchEntry(BuiltInRegistries.BLOCK.getKey(block),
                                block.getName().getString()));
                    }
                }
            }
        }
        this.entries.sort(Comparator.comparing(SearchEntry::name).thenComparing(entry -> entry.id().toString()));
    }

    private void rebuildButtons()
    {
        this.clearButtons();
        String needle = this.query.strip().toLowerCase(Locale.ROOT);
        List<SearchEntry> filtered = this.entries.stream()
                .filter(entry -> entry.name().toLowerCase(Locale.ROOT).contains(needle)
                        || entry.id().toString().contains(needle))
                .toList();
        int rows = Math.max(2, (this.height - 110) / 22);
        int maxPage = Math.max(0, (filtered.size() - 1) / rows);
        this.page = Math.min(this.page, maxPage);
        int x = (this.width - Math.min(550, this.width - 24)) / 2;
        int width = Math.min(550, this.width - 24);
        for (int index = this.page * rows; index < Math.min(filtered.size(), (this.page + 1) * rows); index++)
        {
            SearchEntry entry = filtered.get(index);
            String label = entry.name() + "  [" + entry.id() + "]";
            int available = Math.max(20, width - 12);
            String clipped = this.font.plainSubstrByWidth(label, available);
            this.addButton(new ButtonGeneric(x, 62 + (index % rows) * 22, width, 20, clipped),
                    (button, mouseButton) -> this.choose(entry.id()));
        }
        int footerY = this.height - 31;
        this.addButton(new ButtonGeneric(x, footerY, 66, 20, "<"), (button, mouseButton) -> {
            if (this.page > 0) { this.page--; this.buttonsDirty = true; }
        });
        this.addButton(new ButtonGeneric(x + 69, footerY, 90, 20,
                (this.page + 1) + " / " + (maxPage + 1)), (button, mouseButton) -> {});
        this.addButton(new ButtonGeneric(x + 162, footerY, 66, 20, ">"), (button, mouseButton) -> {
            if (this.page < maxPage) { this.page++; this.buttonsDirty = true; }
        });
        this.addButton(new ButtonGeneric(x + width - 100, footerY, 100, 20,
                Component.translatable("halfmasa.entity_glow.back").getString()),
                (button, mouseButton) -> this.closeGui(true));
    }

    //#if MC >= 1.21.11
    private void choose(Identifier id)
    //#else
    //$$ private void choose(ResourceLocation id)
    //#endif
    {
        this.selected.accept(id);
        this.closeGui(true);
    }

    //#if MC >= 1.21.11
    private record SearchEntry(Identifier id, String name) {}
    //#else
    //$$ private record SearchEntry(ResourceLocation id, String name) {}
    //#endif
}
//#endif
