"""Profile preparation separately from drawing, using identical full-slot world previews."""
import run_minihud_world_preview_smoke as world
import run_minihud_independent_smoke as independent

SOURCE = world.SOURCE.replace('checks,parses;', 'checks,parses,comparisons,builds;').replace(
    'int frame,warmup,startParses,startCalls;', 'int frame,warmup,startParses,startCalls,startComparisons,startBuilds;'
).replace('startParses=parses;startCalls=worldCalls;',
          'startParses=parses;startCalls=worldCalls;startComparisons=comparisons;startBuilds=builds;').replace(
    '" samples="+nanos.size()',
    '" context_builds="+(builds-startBuilds)+" structure_visits="+(comparisons-startComparisons)+" samples="+nanos.size()'
)

WORLD_PROBE = independent.WORLD_PROBE.replace(
    '\n}',
    '''
 @Inject(method="getTargetInventoryFromBlock",at=@At("HEAD")) private void build(CallbackInfoReturnable<InventoryOverlayContext> ci){PreviewSmoke.builds++;}
}
'''
)
COMPARE_PROBE = r'''package io.github.halfmasa.previewsmoke.mixin;
import io.github.halfmasa.previewsmoke.PreviewSmoke;
import io.github.halfmasa.xaerobinding.feature.minihud.PreviewCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=PreviewCache.class,remap=false) public class MapProbeMixin {
 @Inject(method="sameData",at=@At("HEAD")) private static void compare(CallbackInfoReturnable<Boolean> ci){PreviewSmoke.comparisons++;}
}
'''


def main():
    world.SOURCE = SOURCE
    independent.WORLD_PROBE = WORLD_PROBE
    independent.MAP_PROBE = COMPARE_PROBE
    return world.main()


if __name__ == '__main__':
    raise SystemExit(main())
