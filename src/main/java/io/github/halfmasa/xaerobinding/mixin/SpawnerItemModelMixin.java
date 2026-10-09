package io.github.halfmasa.xaerobinding.mixin;

//#if MC >= 26.3
import io.github.halfmasa.xaerobinding.feature.SpawnerItemAppearance;
import io.github.halfmasa.xaerobinding.feature.SpawnerItemModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
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
    @Shadow protected abstract ItemModel getItemModel(Identifier identifier);

    @Redirect(method = "appendItemLayers", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;getItemModel(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/item/ItemModel;"))
    private ItemModel halfmasa_statefulSpawnerModel(ItemModelResolver resolver, Identifier original,
            ItemStackRenderState renderState, ItemStack stack, ItemDisplayContext context,
            Level level, ItemOwner owner, int seed)
    {
        // Respect an explicit custom item-model component supplied by a pack or command.
        if (!SpawnerItemAppearance.supports(stack) ||
                !original.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return this.getItemModel(original);
        var info = SpawnerItemAppearance.read(stack, level != null ? level.registryAccess() : null,
                level != null ? level.getGameTime() : 0);
        return new SpawnerItemModel(this.getItemModel(info.model()), info);
    }
}
//#endif
