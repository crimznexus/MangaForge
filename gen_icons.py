"""
Generate all MangaForge icon assets from the provided 1024x1024 source logo.

Outputs:
  drawable/ic_mihon.png                (1024x1024 — tightly cropped source)
  drawable/ic_launcher_foreground.png  (432x432, transparent bg, logo in 288px safe zone)
  drawable/ic_launcher_monochrome.png  (432x432, white-on-transparent silhouette)
  mipmap-mdpi/ic_launcher.webp         (48x48)
  mipmap-hdpi/ic_launcher.webp         (72x72)
  mipmap-xhdpi/ic_launcher.webp        (96x96)
  mipmap-xxhdpi/ic_launcher.webp       (144x144)
  mipmap-xxxhdpi/ic_launcher.webp      (192x192)
"""

from PIL import Image
import numpy as np
import os

SRC  = r"C:\Users\ICrim\Documents\MangaForge\Gradient Icon logo 1024x1024.png"
BASE = r"C:\Users\ICrim\Documents\MangaForge\app\src\main\res"

# ── 1. Load source ────────────────────────────────────────────────────────────
src = Image.open(SRC).convert("RGBA")
print(f"Source: {src.size}")

# ── 2. Auto-crop transparent padding ─────────────────────────────────────────
arr = np.array(src)
alpha = arr[:, :, 3]
rows = np.any(alpha > 10, axis=1)
cols = np.any(alpha > 10, axis=0)
rmin, rmax = np.where(rows)[0][[0, -1]]
cmin, cmax = np.where(cols)[0][[0, -1]]
margin = 2
rmin = max(0, rmin - margin)
rmax = min(src.height - 1, rmax + margin)
cmin = max(0, cmin - margin)
cmax = min(src.width - 1, cmax + margin)
cropped = src.crop((cmin, rmin, cmax + 1, rmax + 1))
# Make square
side = max(cropped.width, cropped.height)
square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
x_off = (side - cropped.width) // 2
y_off = (side - cropped.height) // 2
square.paste(cropped, (x_off, y_off))
print(f"Cropped+squared: {square.size}")

# ── 3. Save ic_mihon.png (1024x1024) ─────────────────────────────────────────
logo = square.resize((1024, 1024), Image.LANCZOS)
logo.save(f"{BASE}/drawable/ic_mihon.png")
print(f"Saved ic_mihon.png")

# ── 4. ic_launcher_foreground.png (432x432, content in 288px safe zone) ──────
# Adaptive icon: 108dp canvas, 72dp safe zone = 288px at 4x density
CANVAS_FG = 432
SAFE      = 288
pad       = (CANVAS_FG - SAFE) // 2   # 72px each side

fg_logo = square.resize((SAFE, SAFE), Image.LANCZOS)
fg = Image.new("RGBA", (CANVAS_FG, CANVAS_FG), (0, 0, 0, 0))
fg.paste(fg_logo, (pad, pad), fg_logo)
fg.save(f"{BASE}/drawable/ic_launcher_foreground.png")
print(f"Saved ic_launcher_foreground.png  (content in {SAFE}px safe zone, {pad}px padding each side)")

# ── 5. ic_launcher_monochrome.png (white silhouette) ─────────────────────────
fg_arr = np.array(fg).copy()
fg_arr[:, :, 0] = 255
fg_arr[:, :, 1] = 255
fg_arr[:, :, 2] = 255
mono = Image.fromarray(fg_arr, "RGBA")
mono.save(f"{BASE}/drawable/ic_launcher_monochrome.png")
print("Saved ic_launcher_monochrome.png")

# ── 6. Static mipmap WebPs ───────────────────────────────────────────────────
densities = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}
for folder, px in densities.items():
    out_path = f"{BASE}/{folder}/ic_launcher.webp"
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    icon = square.resize((px, px), Image.LANCZOS)
    icon.save(out_path, format="WEBP", lossless=True, quality=100)
    print(f"Saved {folder}/ic_launcher.webp  ({px}x{px})")

print("\nAll done!")
