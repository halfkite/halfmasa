package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 1.21.4
import io.github.halfmasa.xaerobinding.feature.SpawnerItemAppearance;
//#if MC >= 1.21.10
import io.github.halfmasa.xaerobinding.feature.SpawnerItemModel;
//#else
//$$ import io.github.halfmasa.xaerobinding.feature.LegacySpawnerItemModel;
//#endif
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
//#if MC >= 1.21.10
import net.minecraft.world.entity.ItemOwner;
//#else
//$$ import net.minecraft.world.entity.LivingEntity;
//#endif
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemModelResolver.class)
public abstract class SpawnerItemModelMixin
{
    //#if MC >= 26.0
    @Shadow protected abstract ItemModel getItemModel(Identifier identifier);
    //#else
    //$$ @Shadow @org.spongepowered.asm.mixin.Final private java.util.function.Function<Identifier, ItemModel> modelGetter;
    //$$ private ItemModel getItemModel(Identifier identifier) { return this.modelGetter.apply(identifier); }
    //#endif

    //#if MC >= 26.0
    @Redirect(method = "appendItemLayers", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;getItemModel(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/item/ItemModel;"))
    private ItemModel halfmasa_statefulSpawnerModel(ItemModelResolver resolver, Identifier original,
    //#else
    //$$ @Redirect(method = "appendItemLayers", at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"))
    //$$ private Object halfmasa_statefulSpawnerModel(java.util.function.Function<Identifier, ItemModel> getter, Object modelKey,
    //#endif
            ItemStackRenderState renderState, ItemStack stack, ItemDisplayContext context,
            //#if MC >= 1.21.10
            Level level, ItemOwner owner, int seed)
            //#else
            //$$ Level level, LivingEntity owner, int seed)
            //#endif
    {
        //#if MC < 26.0
        //$$ Identifier original = (Identifier) modelKey;
        //#endif
        // Respect an explicit custom item-model component supplied by a pack or command.
        if (!SpawnerItemAppearance.supports(stack) ||
                !original.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return this.getItemModel(original);
        var info = SpawnerItemAppearance.read(stack, level != null ? level.registryAccess() : null,
                level != null ? level.getGameTime() : 0);
        //#if MC >= 1.21.10
        return new SpawnerItemModel(this.getItemModel(info.model()), info);
        //#else
        //$$ return new LegacySpawnerItemModel(this.getItemModel(info.model()), info);
        //#endif
    }
}
//#endif
