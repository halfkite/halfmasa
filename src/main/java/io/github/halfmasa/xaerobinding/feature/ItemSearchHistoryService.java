package io.github.halfmasa.xaerobinding.feature;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;

//#if MC >= 1.21.11
import net.minecraft.util.Util;
//#else
//$$ import net.minecraft.Util;
//#endif
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
//#if MC >= 1.21.8
import net.minecraft.nbt.NbtOps;
//#endif
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

import io.github.halfmasa.xaerobinding.XaeroWorldBinding;
import io.github.halfmasa.xaerobinding.config.Configs;

/** Stores independent recent-item lists for creative, REI and JEI */
public final class ItemSearchHistoryService implements IClientTickHandler
{
    private static final ItemSearchHistoryService INSTANCE = new ItemSearchHistoryService();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_ENTRIES = 512;
    private static final long SAVE_DELAY_MS = 500L;

    private final EnumMap<Channel, History> histories = new EnumMap<>(Channel.class);

    private ItemSearchHistoryService()
    {
        for (Channel channel : Channel.values())
        {
            this.histories.put(channel, new History(channel));
        }
    }

    public static ItemSearchHistoryService getInstance()
    {
        return INSTANCE;
    }

    public List<ItemStack> get(Channel channel)
    {
        History history = this.histories.get(channel);
        history.loadIfPossible();
        return history.copyEntries();
    }

    public void record(Channel channel, ItemStack stack)
    {
        if (!isEnabled(channel) || stack == null || stack.isEmpty())
        {
            return;
        }

        History history = this.histories.get(channel);
        if (!history.loadIfPossible())
        {
            return;
        }

        ItemStack normalized = stack.copyWithCount(1);
        history.entries.removeIf(entry -> ItemStack.isSameItemSameComponents(entry, normalized));
        history.entries.addFirst(normalized);
        while (history.entries.size() > MAX_ENTRIES)
        {
            history.entries.removeLast();
        }
        history.dirty = true;
        history.saveAfter = Util.getMillis() + SAVE_DELAY_MS;
    }

    public void flush()
    {
        for (History history : this.histories.values())
        {
            history.saveNow();
        }
    }

    @Override
    public void onClientTick(Minecraft client)
    {
        long now = Util.getMillis();
        for (History history : this.histories.values())
        {
            if (history.dirty && now >= history.saveAfter)
            {
                history.saveNow();
            }
        }
    }

    private static boolean isEnabled(Channel channel)
    {
        return channel == Channel.CREATIVE
                ? Configs.ITEM_SEARCH_HISTORY.getBooleanValue()
                : Configs.ITEM_MANAGER_RECIPE_HISTORY.getBooleanValue();
    }

    public enum Channel
    {
        CREATIVE("creative"),
        REI("rei"),
        JEI("jei");

        private final String fileName;

        Channel(String fileName)
        {
            this.fileName = fileName;
        }
    }

    private static final class History
    {
        private final Channel channel;
        private final List<ItemStack> entries = new ArrayList<>();
        private HolderLookup.Provider provider;
        private boolean loaded;
        private boolean dirty;
        private long saveAfter;

        private History(Channel channel)
        {
            this.channel = channel;
        }

        private boolean loadIfPossible()
        {
            if (this.loaded)
            {
                return true;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.level == null)
            {
                return false;
            }

            this.provider = client.level.registryAccess();
            this.loaded = true;
            Path file = file();
            if (!Files.isReadable(file))
            {
                return true;
            }

            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
            {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject())
                {
                    throw new IllegalArgumentException("History root is not an object");
                }
                JsonArray array = parsed.getAsJsonObject().getAsJsonArray("entries");
                if (array == null)
                {
                    return true;
                }
                for (JsonElement element : array)
                {
                    if (!element.isJsonPrimitive())
                    {
                        continue;
                    }
                    try
                    {
                        //#if MC >= 1.21.8
                        CompoundTag tag = TagParser.parseCompoundFully(element.getAsString());
                        ItemStack stack = ItemStack.CODEC.parse(
                                this.provider.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
                        //#else
                        //$$ CompoundTag tag = TagParser.parseTag(element.getAsString());
                        //$$ ItemStack stack = ItemStack.parseOptional(this.provider, tag);
                        //#endif
                        if (!stack.isEmpty())
                        {
                            stack.setCount(1);
                            if (this.entries.stream().noneMatch(existing -> ItemStack.isSameItemSameComponents(existing, stack)))
                            {
                                this.entries.add(stack);
                            }
                        }
                    }
                    catch (Exception exception)
                    {
                        XaeroWorldBinding.LOGGER.warn("Skipped an invalid {} search history entry", this.channel.fileName);
                    }
                    if (this.entries.size() >= MAX_ENTRIES)
                    {
                        break;
                    }
                }
            }
            catch (Exception exception)
            {
                XaeroWorldBinding.LOGGER.warn("Failed to load {} search history from {}", this.channel.fileName, file, exception);
            }
            return true;
        }

        private List<ItemStack> copyEntries()
        {
            return this.entries.stream().map(ItemStack::copy).toList();
        }

        private void saveNow()
        {
            if (!this.dirty || this.provider == null)
            {
                return;
            }

            Path file = file();
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try
            {
                Files.createDirectories(file.getParent());
                JsonObject root = new JsonObject();
                root.addProperty("schemaVersion", SCHEMA_VERSION);
                JsonArray array = new JsonArray();
                for (ItemStack entry : this.entries)
                {
                    //#if MC >= 1.21.8
                    array.add(ItemStack.CODEC.encodeStart(
                            this.provider.createSerializationContext(NbtOps.INSTANCE),
                            entry.copyWithCount(1)).getOrThrow().toString());
                    //#else
                    //$$ array.add(entry.copyWithCount(1).save(this.provider).toString());
                    //#endif
                }
                root.add("entries", array);
                try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8))
                {
                    GSON.toJson(root, writer);
                }
                try
                {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                }
                catch (AtomicMoveNotSupportedException exception)
                {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
                this.dirty = false;
            }
            catch (Exception exception)
            {
                XaeroWorldBinding.LOGGER.warn("Failed to save {} search history to {}", this.channel.fileName, file, exception);
            }
        }

        private Path file()
        {
            return Configs.getHalfMasaDirectory()
                    .resolve("search-history")
                    .resolve(this.channel.fileName + ".json");
        }
    }
}
