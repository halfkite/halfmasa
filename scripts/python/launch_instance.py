"""Launch a Minecraft version instance directly from its version JSON (offline auth)."""
import hashlib
import json
import os
import sys

DOT = r"D:\我的世界\.minecraft"


def rule_allows(rule_list, os_name="windows", arch="x64"):
    if not rule_list:
        return True
    allowed = False
    for rule in rule_list:
        if "features" in rule or "feature" in rule:
            continue  # feature-gated args are never enabled in offline launches
        r_os = rule.get("os", {})
        if r_os.get("name") not in (None, os_name):
            continue
        if r_os.get("arch") == "x86" and arch != "x86":
            continue
        if r_os.get("arch") == "x64" and arch != "x64":
            continue
        allowed = rule["action"] == "allow"
    return allowed


def sub_all(s, subs):
    for k, v in subs.items():
        s = s.replace(k, v)
    return s


def resolve_lib(lib):
    dl = lib.get("downloads") or {}
    art = dl.get("artifact") or {}
    if art.get("path"):
        p = os.path.join(DOT, "libraries", art["path"].replace("/", os.sep))
        if os.path.exists(p):
            return p
    name = lib.get("name", "")
    parts = name.split(":")
    if len(parts) < 3:
        return None
    coords = parts[0].replace(".", os.sep) + os.sep + parts[1] + os.sep + parts[2]
    base = os.path.join(DOT, "libraries", coords)
    cand = []
    if os.path.isdir(base):
        for root, _, files in os.walk(base):
            for f in files:
                if f.endswith(".jar"):
                    cand.append(os.path.join(root, f))
    if cand:
        plain = [c for c in cand if "natives" not in c and "sources" not in c]
        return (plain or cand)[0]
    return None


def main(version_dir):
    json_path = os.path.join(version_dir, os.path.basename(version_dir) + ".json")
    data = json.load(open(json_path, encoding="utf-8"))

    classpath = [os.path.join(version_dir, os.path.basename(version_dir) + ".jar")]
    missing = []
    for lib in data["libraries"]:
        if not rule_allows(lib.get("rules")):
            continue
        p = resolve_lib(lib)
        if p:
            classpath.append(p)
        else:
            missing.append(lib.get("name"))

    natives_dir = os.path.join(version_dir, os.path.basename(version_dir) + "-natives")
    subs = {
        "${natives_directory}": natives_dir,
        "${library_directory}": os.path.join(DOT, "libraries"),
        "${classpath_separator}": ";",
        "${classpath}": ";".join(classpath),
        "${launcher_name}": "halfmasa-test",
        "${launcher_version}": "1.0",
        "${version_name}": os.path.basename(version_dir),
    }

    jvm = ["-Xmx4G", "-Xms1G"]
    skip_next = False
    for a in data["arguments"]["jvm"]:
        if skip_next:
            skip_next = False
            continue
        if isinstance(a, dict):
            if rule_allows(a.get("rules")):
                v = a["value"]
                jvm.extend(v if isinstance(v, list) else [v])
        else:
            if a == "-cp" or "FabricMcEmu" in a:
                # JSON supplies its own classpath/main-class emulation; we provide
                # -cp and the real main class ourselves after filtering.
                if a == "-cp":
                    skip_next = True
                continue
            jvm.append(a)
    jvm = [subs.get(x, x) for x in jvm]
    jvm = [sub_all(x, subs) for x in jvm]

    offline_name = "halfmasa_dev"
    b = bytearray(bytes.fromhex(hashlib.md5(("OfflinePlayer:" + offline_name).encode()).hexdigest()))
    b[6] = (b[6] & 0x0F) | 0x30
    b[8] = (b[8] & 0x3F) | 0x80
    uuid = "".join(f"{x:02x}" for x in b)

    vals = {
        "${auth_player_name}": offline_name,
        "${version_name}": os.path.basename(version_dir),
        "${game_directory}": version_dir,
        "${assets_root}": os.path.join(DOT, "assets"),
        "${assets_index_name}": data["assetIndex"]["id"],
        "${asset_index_name}": data["assetIndex"]["id"],
        "${auth_uuid}": uuid,
        "${auth_access_token}": "0",
        "${auth_session}": "0",
        "${user_type}": "msa",
        "${user_properties}": "{}",
        "${version_type}": data.get("type", "release"),
        "${auth_xuid}": "0",
        "${clientid}": "0",
        "${resolution_width}": "1280",
        "${resolution_height}": "720",
    }
    game = []
    for a in data["arguments"]["game"]:
        if isinstance(a, dict):
            if rule_allows(a.get("rules")):
                v = a["value"]
                game.extend(v if isinstance(v, list) else [v])
        else:
            game.append(a)
    game = [vals.get(x, x) for x in game]
    game = [sub_all(x, vals) for x in game]

    cmd = ["java"] + jvm + ["-cp", ";".join(classpath), data["mainClass"]] + game
    with open(os.path.join(version_dir, "launch_cmd.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(cmd))
    print("classpath entries:", len(classpath))
    print("missing libs:", missing)
    print("cmd written:", os.path.join(version_dir, "launch_cmd.txt"))


if __name__ == "__main__":
    main(sys.argv[1])
