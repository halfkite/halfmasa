package io.github.halfmasa.xaerobinding.better_multiplayer.privacy;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Process presence is a risk hint, never proof of recording or of safety. */
public final class WindowsCaptureDetector {
    public record Result(boolean obs, boolean other, boolean failed) {}
    private static final Set<String> OBS = Set.of("obs64.exe", "obs32.exe", "obs", "obs-studio");
    private static final Set<String> OTHER = Set.of("bandicam.exe", "bdcam.exe", "streamlabs obs.exe",
            "streamlabs desktop.exe", "xsplit.core.exe", "gamebar.exe", "gamebarftserver.exe",
            "nvidia share.exe", "nvsphelper64.exe", "action.exe", "screenrec.exe", "ffmpeg.exe");
    public Result detect() {
        try (var processes = ProcessHandle.allProcesses()) {
            Set<String> names = new HashSet<>();
            processes.forEach(p -> p.info().command().ifPresent(c -> names.add(Path.of(c).getFileName().toString().toLowerCase(java.util.Locale.ROOT))));
            return new Result(names.stream().anyMatch(OBS::contains), names.stream().anyMatch(OTHER::contains), false);
        } catch (SecurityException error) { return new Result(false, false, true); }
    }
}
