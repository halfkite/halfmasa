"""Verify archived release jars with matching official Conflux builds in isolated worlds."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run_check(script, arguments, report_root, version):
    result = subprocess.run([sys.executable, str(ROOT / "scripts/python" / script), *arguments],
                            cwd=ROOT, env={**os.environ, "PYTHONIOENCODING": "utf-8"},
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8",
                            creationflags=subprocess.CREATE_NO_WINDOW)
    reports = list(report_root.glob(f"{version}/*/report.json")) if script == "run_startup_smoke.py" else list(report_root.glob("*/report.json"))
    report = max(reports, key=lambda path: path.stat().st_mtime) if reports else None
    if result.returncode or report is None:
        print(result.stdout[-7000:], flush=True)
        raise RuntimeError(f"{version}: {script} failed; report={report}")
    data = json.loads(report.read_text(encoding="utf-8"))
    if data["result"] != "PASS":
        raise RuntimeError(f"Failed report: {report}")
    print(f"{version}: {script} PASS ({data['duration_seconds']}s)", flush=True)
    return str(report)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--versions", nargs="*")
    args = parser.parse_args()
    artifacts = json.loads((ROOT / "build/release-1.7.0-artifacts.json").read_text(encoding="utf-8"))
    path = ROOT / "build/release-1.7.0-verification.json"
    previous = json.loads(path.read_text(encoding="utf-8")) if path.is_file() else {"versions": {}}
    harnesses = [ROOT / "scripts/python" / name for name in ["run_startup_smoke.py", "run_range_feeding_smoke.py", "smoke_remap.py"]]
    harness_hash = hashlib.sha256("".join(digest(harness) for harness in harnesses).encode()).hexdigest()
    result = {"release": "1.7.0", "harness_sha256": harness_hash, "versions": previous["versions"]}
    api_versions = dict(re.findall(r"'([\d.]+)': '([^']+)'", (ROOT / "common.gradle").read_text(encoding="utf-8")))
    required = ["RangeFeedingService", "AutoEatService", "AutoEatFoodRules", "SilentFoodTransaction", "BossBarGrouper", "BossBarHealth", "EntityRenderDisableService", "ConfluxWaypointScreenBridge", "HoldToShow", "SwingVisualTracker"]
    try:
        for record in artifacts:
            version = record["version"]
            if args.versions and version not in args.versions:
                continue
            artifact = Path(record["artifact"])
            artifact_hash = digest(artifact)
            old = result["versions"].get(version, {})
            if old.get("result") == "PASS" and old.get("sha256") == artifact_hash and old.get("harness_sha256") == harness_hash:
                print(f"{version}: reusing exact-artifact, exact-harness PASS", flush=True)
                continue
            with zipfile.ZipFile(artifact) as jar:
                manifest = json.loads(jar.read("fabric.mod.json"))
                if manifest["version"] != "1.7.0":
                    raise AssertionError(f"Incorrect embedded version: {artifact}")
                for feature in required:
                    if f"io/github/halfmasa/xaerobinding/feature/{feature}.class" not in jar.namelist():
                        raise AssertionError(f"Missing feature {feature}: {artifact}")
            java = Path(r"C:\Program Files\Java\jdk-25.0.3") if version.startswith("26.") else Path(r"C:\Users\28656\AppData\Local\Temp\fga-26-3-build-jdk21\jdk-21.0.12.1+1")
            conflux_version = "1.21.9" if version == "1.21.10" else version
            conflux = ROOT / f"build/conflux-port-dependencies/confluxmap-{conflux_version}-0.1.11-beta.1.jar"
            api = next(candidate for candidate in (Path.home() / f".gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/{api_versions[version]}").glob("*/*.jar") if not candidate.name.endswith(("-sources.jar", "-javadoc.jar")))
            common = ["--version", version, "--artifact", str(artifact), "--java-home", str(java), "--disable-clipboard-init", "--jvm-arg=-Djava.awt.headless=true", "--jvm-arg=-Dsun.java2d.d3d=false"]
            if version.startswith("26."):
                common.extend(["--loader-version", "0.19.5"])
            startup = run_check("run_startup_smoke.py", [*common, "--extra-mod", str(conflux)], ROOT / "build/startup-tests", version)
            gameplay = run_check("run_range_feeding_smoke.py", [*common, "--animal", "chicken", "--extra-mod", str(conflux), "--extra-mod", str(api)], ROOT / "build/range-feeding-tests", version)
            result["versions"][version] = {**record, "sha256": artifact_hash, "result": "PASS", "harness_sha256": harness_hash, "embedded_version": manifest["version"], "conflux": str(conflux), "integration_loader": "0.19.5" if version.startswith("26.") else "0.17.3", "startup_report": startup, "gameplay_report": gameplay}
            path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
        result["result"] = "PASS" if len(result["versions"]) == len(artifacts) and all(entry.get("result") == "PASS" for entry in result["versions"].values()) else "INCOMPLETE"
    finally:
        path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(path, flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
