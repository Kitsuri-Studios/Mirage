import shutil
import sys
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8")


HXO_BUILD_DIR = Path(sys.argv[1])
OUTPUT_ASSETS_DIR = Path(sys.argv[2])

DEX_NAME = "classes.dex"
OUT_NAME = "hxo.dex"


dex_files = list(HXO_BUILD_DIR.rglob(DEX_NAME))

print("Searching in:", HXO_BUILD_DIR)
for p in HXO_BUILD_DIR.rglob("*.dex"):
    print("Found dex:", p)


if not dex_files:
    print("No classes.dex found")
    sys.exit(1)

dex_path = dex_files[0]
OUTPUT_ASSETS_DIR.mkdir(parents=True, exist_ok=True)

out_path = OUTPUT_ASSETS_DIR / OUT_NAME
shutil.copyfile(dex_path, out_path)

print(f"Copied {dex_path} → {out_path}")
