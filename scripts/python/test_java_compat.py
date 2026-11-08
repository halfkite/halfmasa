import os
import shutil
import unittest
from pathlib import Path


def java_tools():
    configured = os.environ.get("JAVA_HOME")
    if configured:
        home = Path(configured)
        java = home / "bin" / ("java.exe" if os.name == "nt" else "java")
        javac = home / "bin" / ("javac.exe" if os.name == "nt" else "javac")
        if java.exists() and javac.exists():
            return str(java), str(javac)
    java = shutil.which("java")
    javac = shutil.which("javac")
    if not java or not javac:
        raise unittest.SkipTest("Java compiler/runtime is unavailable")
    return java, javac
