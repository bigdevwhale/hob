"""Walks the demo build on a phone over adb and saves the README and Google Play screenshots.

./gradlew installDebug -Pdemo -Plang=ru && python tools/shots.py ru
./gradlew installDebug -Pdemo -Plang=en && python tools/shots.py en

Clears the demo's data, turns the tips off, visits eight screens and writes docs/screenshots/<lang>/*.jpg
at the phone's full width, with the status bar cut off. Real screenshots, not the Robolectric renders,
so the newer emoji are drawn. Keep the phone unlocked.
"""
import io, os, re, shutil, subprocess, sys, time
from pathlib import Path

from PIL import Image

ADB = shutil.which("adb") or os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk", "platform-tools", "adb.exe")
PKG = "app.hob.demo"
lang = sys.argv[1]
out = Path(__file__).resolve().parent.parent / "docs/screenshots" / lang
out.mkdir(parents=True, exist_ok=True)
L = lambda en, ru: en if lang == "en" else ru


def adb(*a, binary=False):
    r = subprocess.run([ADB, *a], capture_output=True)
    return r.stdout if binary else r.stdout.decode("utf-8", "replace")


def sh(cmd):
    return adb("shell", cmd)


def nodes():
    sh("uiautomator dump /sdcard/ui.xml >/dev/null")
    xml = adb("exec-out", "cat", "/sdcard/ui.xml")
    for m in re.finditer(r'<node [^>]*?text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        t, d, x1, y1, x2, y2 = m.groups()
        yield (t or d), (int(x1) + int(x2)) // 2, (int(y1) + int(y2)) // 2


def tap(text, tries=4):
    for _ in range(tries):
        for t, x, y in nodes():
            if t == text:
                sh(f"input tap {x} {y}")
                time.sleep(1.6)
                return
        sh("input swipe 540 1700 540 1000 300")
        time.sleep(1)
    raise SystemExit(f"not found: {text}")


def shot(name):
    time.sleep(1.2)
    im = Image.open(io.BytesIO(adb("exec-out", "screencap", "-p", binary=True))).convert("RGB")
    bar = int(re.search(r"ITYPE_STATUS_BAR frame=\[0,0\]\[\d+,(\d+)\]", sh("dumpsys window")).group(1))
    im.crop((0, bar, im.width, im.height)).save(out / f"{name}.jpg", quality=90, optimize=True)
    print("saved", name)


def hide_keyboard():
    if "mInputShown=true" in sh("dumpsys input_method"):
        sh("input keyevent 4")
        time.sleep(1)


def back():
    sh("input keyevent 4")
    time.sleep(1.2)


def top():
    for _ in range(3):
        sh("input swipe 540 700 540 1900 200")
    time.sleep(0.8)


sys.stdout.reconfigure(encoding="utf-8")
sh("svc power stayon usb")
sh(f"pm clear {PKG}")
sh(f"monkey -p {PKG} -c android.intent.category.LAUNCHER 1")
time.sleep(5)
tap(L("Don’t show tips", "Не показывать подсказки"))
top()
shot("1-home")
tap(L("Hallway", "Прихожая"))
shot("2-room")
tap(L("Winter 110–116", "Зима 110–116"))
tap(L("Blue down jacket", "Пуховик синий"))
shot("3-thing")
back(); back(); back()
tap(L("Find", "Найти"))
hide_keyboard()
shot("4-find")
tap(L("People", "Люди"))
shot("5-people")
tap(L("Vanya", "Ваня"))
shot("6-kid")
back()
tap(L("Hob", "Домовой"))
shot("7-hob")
tap(L("Home", "Дом"))
tap(L("Sort it out in five minutes", "Разобрать за пять минут"))
shot("8-clear")
back()
