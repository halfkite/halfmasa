package io.github.halfmasa.xaerobinding.mixin;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.halfmasa.xaerobinding.config.Configs;

@Mixin(ServerSelectionList.class)
public abstract class ServerPingerFixMixin
{
    @Mutable @Final @Shadow private static ThreadPoolExecutor THREAD_POOL;
    @Final @Shadow private List<ServerSelectionList.OnlineServerEntry> onlineServers;

    @Unique private static boolean halfmasa_ownsPool;
    @Unique private static final int VANILLA_THREADS = 5;

    @Inject(method = "refreshEntries", at = @At("HEAD"))
    private void halfmasa_refreshPingerPool(CallbackInfo ci)
    {
        if (!Configs.SERVER_PINGER_FIX.getBooleanValue())
        {
            if (halfmasa_ownsPool)
            {
                halfmasa_replacePool(VANILLA_THREADS);
                halfmasa_ownsPool = false;
            }
            return;
        }

        int desiredThreads = Math.max(VANILLA_THREADS, this.onlineServers.size() + VANILLA_THREADS);
        if (!halfmasa_ownsPool || THREAD_POOL.getActiveCount() >= VANILLA_THREADS ||
            THREAD_POOL.getCorePoolSize() != desiredThreads)
        {
            halfmasa_replacePool(desiredThreads);
            halfmasa_ownsPool = true;
        }
    }

    @Unique
    private static void halfmasa_replacePool(int threads)
    {
        THREAD_POOL.shutdownNow();
        THREAD_POOL = new ScheduledThreadPoolExecutor(
                threads,
                new ThreadFactoryBuilder().setNameFormat("Server Pinger #%d").setDaemon(true).build());
    }
}
