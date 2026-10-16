"""Use cached Loom mappings to run test-only mods against distributable 1.21 jars."""
import os
from pathlib import Path
import subprocess


def mappings(version, classpath):
    cache = Path.home() / f".gradle/caches/fabric-loom/{version}"
    candidates = list(cache.glob("*/mappings.tiny"))
    matches = [path for path in candidates if path.parent.name in classpath]
    for path in matches or candidates:
        if "net/minecraft/client/Minecraft" in path.read_text(encoding="utf-8"):
            return path
    raise RuntimeError(f"Cached Mojang mappings not found for {version}")


def field_name(mapping, owner, name):
    current = None
    for line in mapping.read_text(encoding="utf-8").splitlines():
        columns = line.split("\t")
        if columns[0] == "c":
            current = columns[-1]
        elif current == owner and len(columns) > 5 and columns[1] == "f" and columns[-1] == name:
            return columns[-2]
    raise RuntimeError(f"Missing mapped field {owner}.{name}")


def remap(java_home, source, target, mapping, classpath, log_path, source_namespace="named", target_namespace="intermediary"):
    cache = Path.home() / ".gradle/caches/modules-2/files-2.1"
    tiny = sorted((cache / "net.fabricmc/tiny-remapper").glob("*/*/tiny-remapper-*.jar"))[-1]
    mapping_io = sorted((cache / "net.fabricmc/mapping-io").glob("*/*/mapping-io-*.jar"))[-1]
    # An empty CLI path becomes the working directory and recursively scans build archives.
    entries = list(dict.fromkeys(path for path in classpath.split(os.pathsep) if path))
    tool_cp = os.pathsep.join([str(tiny), str(mapping_io), *entries])
    command = [str(java_home / "bin/java.exe"), "-Xmx2G", "-XX:ActiveProcessorCount=2", "-cp", tool_cp,
               "net.fabricmc.tinyremapper.Main", str(source), str(target), str(mapping),
               source_namespace, target_namespace, *entries, "--mixin"]
    argument_file = log_path.with_suffix(".args")
    argument_file.write_text("\n".join('"' + arg.replace("\\", "\\\\").replace('"', '\\"') + '"' for arg in command[1:]), encoding="utf-8")
    with log_path.open("w", encoding="utf-8") as output:
        subprocess.run([command[0], "@" + str(argument_file)], stdout=output, stderr=subprocess.STDOUT, check=True)
    return target
