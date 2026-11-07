//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Keep vanilla's cached coordinates in sync after GLFW cursor restoration. */
@Mixin(MouseHandler.class)
public interface AutoOpenChatMouseAccessor
{
    @Accessor("xpos") void halfmasa$setX(double x);
    @Accessor("ypos") void halfmasa$setY(double y);
}
//#endif
