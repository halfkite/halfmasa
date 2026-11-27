package io.github.halfmasa.xaerobinding.feature;

//#if MC >= 1.21.1
import io.github.halfmasa.xaerobinding.compat.TweakerooReachCompat;
import io.github.halfmasa.xaerobinding.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
//#if MC >= 1.21.11
import net.minecraft.resources.Identifier;
//#else
//$$ import net.minecraft.resources.ResourceLocation;
//#endif
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Read-only, local-player reach policy. No attribute mutation or tick listener. */
public final class CreativeSurvivalReach
{
    //#if MC >= 1.21.11
    private static final Identifier BLOCK_BONUS = Identifier.withDefaultNamespace("creative_mode_block_range");
    private static final Identifier ENTITY_BONUS = Identifier.withDefaultNamespace("creative_mode_entity_range");
    //#else
    //$$ private static final ResourceLocation BLOCK_BONUS = ResourceLocation.withDefaultNamespace("creative_mode_block_range");
    //$$ private static final ResourceLocation ENTITY_BONUS = ResourceLocation.withDefaultNamespace("creative_mode_entity_range");
    //#endif

    private CreativeSurvivalReach() {}

    private static boolean applies(Entity entity)
    {
        return Configs.CREATIVE_SURVIVAL_REACH.getBooleanValue()
                && entity instanceof LocalPlayer player
                && player == Minecraft.getInstance().player
                && player.isCreative();
    }

    public static boolean appliesToBlock(Entity entity)
    {
        return applies(entity) && !TweakerooReachCompat.isBlockOverrideEnabled();
    }

    public static boolean appliesToEntity(Entity entity)
    {
        return applies(entity) && !TweakerooReachCompat.isEntityOverrideEnabled();
    }

    public static double blockRange(Player player, double original)
    {
        return appliesToBlock(player)
                ? withoutModifier(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_BONUS, original)
                : original;
    }

    public static double entityRange(Player player, double original)
    {
        return appliesToEntity(player)
                ? withoutModifier(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_BONUS, original)
                : original;
    }

    //#if MC >= 1.21.11
    static double withoutModifier(AttributeInstance attribute, Identifier excluded, double original)
    //#else
    //$$ static double withoutModifier(AttributeInstance attribute, ResourceLocation excluded, double original)
    //#endif
    {
        if (attribute == null || !attribute.hasModifier(excluded)) return original;

        double base = attribute.getBaseValue();
        double multipliedBase = 0.0;
        double multipliedTotal = 1.0;

        for (AttributeModifier modifier : attribute.getModifiers())
        {
            if (excluded.equals(modifier.id())) continue;
            switch (modifier.operation())
            {
                case ADD_VALUE -> base += modifier.amount();
                case ADD_MULTIPLIED_BASE -> multipliedBase += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> multipliedTotal *= 1.0 + modifier.amount();
            }
        }

        return attribute.getAttribute().value().sanitizeValue(
                (base + base * multipliedBase) * multipliedTotal);
    }
}
//#endif
