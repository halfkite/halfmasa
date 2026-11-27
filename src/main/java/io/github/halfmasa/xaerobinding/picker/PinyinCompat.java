//#if MC >= 1.21.1
package io.github.halfmasa.xaerobinding.picker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.fabricmc.loader.api.FabricLoader;

/** Optional JustEnoughCharacters bridge. No hard dependency, no private reflection. */
final class PinyinCompat {
    private static MethodHandle match=resolve();
    private static MethodHandle resolve(){
        if(!FabricLoader.getInstance().isModLoaded("jecharacters"))return null;
        try {
            var type = Class.forName("me.towdium.jecharacters.utils.Match");
            return MethodHandles.publicLookup().findStatic(type, "contains",
                    MethodType.methodType(boolean.class, String.class, CharSequence.class))
                    .asType(MethodType.methodType(boolean.class, String.class, String.class));
        } catch(ReflectiveOperationException|LinkageError ex){return null;}
    }
    static boolean contains(String name,String query){
        if(match==null||query.isEmpty())return false;
        try {return (boolean)match.invokeExact(name,query);}
        catch(RuntimeException|LinkageError ex){match=null;return false;}
        catch(Error ex){throw ex;}
        catch(Throwable ex){match=null;return false;}
    }
}
//#endif
