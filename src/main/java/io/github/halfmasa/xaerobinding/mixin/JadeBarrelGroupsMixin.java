//#if MC >= 26.1
package io.github.halfmasa.xaerobinding.mixin;

import java.util.List;
import io.github.halfmasa.xaerobinding.feature.jade.JadeBarrelPreview;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.*;

@Pseudo
@Mixin(targets = "snownee.jade.addon.universal.ItemStorageProvider$Extension", remap = false)
public abstract class JadeBarrelGroupsMixin
{
    @Inject(method = "getClientGroups", at = @At("HEAD"), cancellable = true)
    private void halfmasa$combineHalves(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups,
            CallbackInfoReturnable<List<ClientViewGroup<ItemView>>> cir)
    {
        var combined = JadeBarrelPreview.groups(accessor, groups);
        if (combined != groups) cir.setReturnValue(ClientViewGroup.map(combined, ItemView::new, null));
    }
}
//#endif
