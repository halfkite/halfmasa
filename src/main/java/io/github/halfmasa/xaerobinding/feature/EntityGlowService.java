package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import io.github.halfmasa.xaerobinding.config.Configs;
import io.github.halfmasa.xaerobinding.config.EntityGlowMode;

public final class EntityGlowService
{
    private static volatile Rules rules;

    private EntityGlowService() {}

    public static void reload()
    {
        Map<String, Integer> entities = new HashMap<>();
        Map<String, Integer> items = new HashMap<>();
        for (EntityGlowRule rule : EntityGlowRule.loadAll())
        {
            if (!rule.enabled()) continue;
            switch (rule.type())
            {
                case ENTITY -> entities.put(rule.id().toString(), rule.color());
                case ITEM -> items.put(rule.id().toString(), rule.color());
                case BLOCK -> { }
            }
        }
        rules = new Rules(Map.copyOf(entities), Map.copyOf(items));
    }

    public static Integer resolve(Entity entity)
    {
        if (!Configs.ENTITY_GLOW_HIGHLIGHT.getBooleanValue()) return null;
        if (Configs.ENTITY_GLOW_MODE.getOptionListValue() == EntityGlowMode.ALL)
        {
            return 0xFF000000 | (Configs.ENTITY_GLOW_ALL_COLOR.getIntegerValue() & 0xFFFFFF);
        }

        Rules current = rules;
        if (current == null)
        {
            reload();
            current = rules;
        }
        if (entity instanceof ItemEntity itemEntity)
        {
            String itemId = BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem()).toString();
            Integer itemColor = current.items().get(itemId);
            if (itemColor != null) return itemColor;
        }
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return current.entities().get(entityId);
    }

    private record Rules(Map<String, Integer> entities, Map<String, Integer> items) {}
}
//#endif
