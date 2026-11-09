"""Alat uji manual lewat adb: ketuk elemen berdasarkan teksnya (uiautomator dump), screenshot, baca log error.

Pemakaian:
  python tools/uitap.py tap "Beranda"            # ketuk node yang teksnya mengandung "Beranda"
  python tools/uitap.py tap "Berikutnya" --last  # ambil kecocokan terakhir
  python tools/uitap.py shot nama                # simpan screenshot ke scratch/nama.png (diperkecil)
  python tools/uitap.py errors                   # log error proses aplikasi
  python tools/uitap.py dump                     # cetak semua teks yang terlihat
"""
import os
import re
import subprocess
import sys
from pathlib import Path

ADB = os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk", "platform-tools", "adb.exe")
PKG = "com.belajar.cpns"
OUT = Path(os.environ.get("UITAP_OUT", Path(__file__).resolve().parents[1] / "build" / "uitap"))


def adb(*args, binary=False):
    r = subprocess.run([ADB, *args], capture_output=True)
    return r.stdout if binary else r.stdout.decode("utf-8", "replace")


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml")
    out = []
    for m in re.finditer(r'<node[^>]*?text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        text, desc, x1, y1, x2, y2 = m.groups()
        label = text or desc
        if label:
            out.append((label, (int(x1) + int(x2)) // 2, (int(y1) + int(y2)) // 2))
    return out


def tap(query: str, last=False):
    hits = [n for n in nodes() if query.lower() in n[0].lower()]
    if not hits:
        print(f"TIDAK ADA: {query}"); sys.exit(1)
    label, x, y = hits[-1] if last else hits[0]
    adb("shell", "input", "tap", str(x), str(y))
    print(f"tap '{label}' @ {x},{y}")


def shot(name: str):
    OUT.mkdir(parents=True, exist_ok=True)
    png = adb("exec-out", "screencap", "-p", binary=True)
    path = OUT / f"{name}.png"
    path.write_bytes(png)
    try:
        from PIL import Image
        im = Image.open(path); im.thumbnail((450, 1000)); im.save(path)
    except Exception:
        pass
    print(path)


def errors():
    pid = adb("shell", "pidof", PKG).strip()
    if not pid:
        print("APLIKASI TIDAK BERJALAN"); return
    log = adb("logcat", "-d", f"--pid={pid}", "*:E")
    lines = [l for l in log.splitlines() if not l.startswith("-")]
    print("\n".join(lines[-40:]) if lines else "tidak ada error")


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "tap":
        tap(sys.argv[2], last="--last" in sys.argv)
    elif cmd == "shot":
        shot(sys.argv[2])
    elif cmd == "errors":
        errors()
    elif cmd == "dump":
        for label, x, y in nodes():
            # Konsol Windows tidak selalu bisa menampilkan ikon font; ganti dengan '?'.
            print(f"{x:5},{y:5}  {label}".encode("ascii", "replace").decode())
