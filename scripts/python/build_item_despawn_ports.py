"""Build, archive and validate requested ports sequentially; stop on the first failure."""
import argparse
from datetime import datetime, timedelta, timezone
import json
import os
from pathlib import Path
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("versions", nargs="+")
    parser.add_argument("--java-home", type=Path, default=Path("C:/Program Files/Java/jdk-25.0.3"))
    parser.add_argument("--legacy-java-home", type=Path, help="Java 21 runtime for 1.21.x client launches")
    args = parser.parse_args()
    supported = json.loads((ROOT / "settings.json").read_text(encoding="utf-8"))["versions"]
    if any(version not in supported for version in args.versions):
        parser.error("Unsupported project version")
    directory = ROOT / "build/item-despawn-port-results" / datetime.now(timezone(timedelta(hours=8))).strftime("%Y%m%d-%H%M%S")
    directory.mkdir(parents=True)
    environment = os.environ.copy()
    environment["JAVA_HOME"] = str(args.java_home)
    reports = []
    for version in args.versions:
        print(f"PORT {version}: BUILD", flush=True)
        command = [str(ROOT / "gradlew.bat"), f":{version}:build", "--offline", "--console=plain", "--gradle-user-home", str(Path.home() / ".gradle")]
        log_path = directory / f"{version}-build.log"
        with log_path.open("w", encoding="utf-8") as output:
            result = subprocess.run(command, cwd=ROOT, env=environment, stdout=output, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        if result.returncode:
            print(log_path, flush=True)
            print("\n".join(log_path.read_text(encoding="utf-8", errors="replace").splitlines()[-65:]), flush=True)
            return result.returncode
        jars = [path for path in (ROOT / f"versions/{version}/build/libs").glob("halfmasa-fabric-*.jar") if not path.name.endswith(("-sources.jar", "-dev.jar"))]
        artifact = max(jars, key=lambda path: path.stat().st_mtime)
        archiver = Path.home() / ".codex/skills/build-game-mods/scripts/archive_mod_build.py"
        archived = subprocess.run([sys.executable, str(archiver), "--artifact", str(artifact), "--output-root", str(ROOT / "mod-builds"), "--mod-name", "halfmasa", "--game-version", version, "--build-command", subprocess.list2cmdline(command)], capture_output=True, text=True)
        if archived.returncode:
            print(archived.stdout + archived.stderr, flush=True)
            return archived.returncode
        archive = Path(archived.stdout.strip())
        artifact = archive / artifact.name
        print(f"PORT {version}: ARCHIVED {archive}", flush=True)
        with zipfile.ZipFile(artifact) as jar:
            mixins = json.loads(jar.read("halfmasa.mixins.json"))
            if "ItemDespawnLabelMixin" not in mixins["client"]:
                raise AssertionError(f"Timer renderer missing from {version}")
        result = subprocess.run([sys.executable, str(ROOT / "scripts/python/test_item_despawn_protocol.py"), version], cwd=ROOT, env=environment)
        if result.returncode:
            return result.returncode
        print(f"PORT {version}: STARTUP", flush=True)
        runtime = args.legacy_java_home if version.startswith("1.21.") and args.legacy_java_home else args.java_home
        result = subprocess.run([sys.executable, str(ROOT / "scripts/python/run_startup_smoke.py"), "--version", version, "--java-home", str(runtime), "--artifact", str(artifact), "--timeout", "120"], cwd=ROOT, env=environment)
        reports.append({"version": version, "artifact": str(artifact), "archive": str(archive), "build_log": str(log_path), "build": "PASS", "protocol": "PASS", "startup": "PASS" if result.returncode == 0 else "FAIL"})
        report_path = directory / "report.json"
        report_path.write_text(json.dumps(reports, ensure_ascii=False, indent=2), encoding="utf-8")
        if result.returncode:
            print(report_path, flush=True)
            return result.returncode
        print(f"PORT {version}: PASS", flush=True)
    print(directory / "report.json", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
