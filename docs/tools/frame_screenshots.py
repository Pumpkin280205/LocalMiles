"""
Puts raw phone screenshots into Play Store images using the LocalMiles colours.

How to run:
    python frame_screenshots.py <folder with raw screenshots> <output folder>

Name the raw files like in docs/SHOT_LIST.md (01-login.png, 02-biometric.png, ...)
You need Python 3 and Pillow:  pip install pillow
Output is 1080 x 1920 PNGs, ready for the Play Console
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

TEAL = (0x00, 0x47, 0x59)
DARK_TEAL = (0x00, 0x2F, 0x3C)
LIME = (0x8A, 0xC2, 0x37)
ORANGE = (0xF2, 0x88, 0x00)
WHITE = (255, 255, 255)

W, H = 1080, 1920

# text shown above each screenshot (picked by the number at the start of the file name)
CAPTIONS = {
    "01": "Sign in with Google",
    "02": "Unlock with your fingerprint or face",
    "03": "Make it yours: theme, language and security",
    "04": "Work through every stop in order",
    "05": "No signal? Keep working. We sync later.",
    "06": "Proof of delivery with photo, signature and GPS",
    "07": "See every driver on a live map",
    "08": "Manage your vehicles and drivers",
    "09": "Follow your shipment stop by stop",
    "10": "Real-time push notifications",
    "11": "English, Afrikaans and isiZulu",
    "12": "Dispatch jobs in seconds",
}


def load_font(names, size):
    for name in names:
        for folder in ("C:/Windows/Fonts", "/Library/Fonts", "/System/Library/Fonts/Supplemental",
                       "/usr/share/fonts/truetype/dejavu"):
            path = os.path.join(folder, name)
            if os.path.exists(path):
                return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def wrap(draw, text, font, max_width):
    lines, line = [], ""
    for word in text.split():
        trial = (line + " " + word).strip()
        if draw.textlength(trial, font=font) <= max_width:
            line = trial
        else:
            lines.append(line)
            line = word
    lines.append(line)
    return lines


def frame(raw_path, caption):
    # teal background that fades darker going down
    bg = Image.new("RGB", (W, H), TEAL)
    d = ImageDraw.Draw(bg)
    for y in range(H):
        t = y / H
        d.line([(0, y), (W, y)], fill=tuple(int(TEAL[i] + (DARK_TEAL[i] - TEAL[i]) * t) for i in range(3)))

    # text at the top
    font = load_font(["segoeuib.ttf", "arialbd.ttf", "Arial Bold.ttf", "DejaVuSans-Bold.ttf"], 72)
    y = 110
    for line in wrap(d, caption, font, W - 160):
        d.text((80, y), line, font=font, fill=WHITE)
        y += 92
    d.rounded_rectangle([80, y + 14, 240, y + 24], radius=5, fill=ORANGE)  # small accent bar
    top = y + 90

    # the screenshot, made to fit under the text, with round corners and a lime border
    shot = Image.open(raw_path).convert("RGB")
    box_w, box_h = W - 220, H - top - 90
    scale = min(box_w / shot.width, box_h / shot.height)
    shot = shot.resize((int(shot.width * scale), int(shot.height * scale)), Image.LANCZOS)
    x = (W - shot.width) // 2

    mask = Image.new("L", shot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, shot.width, shot.height], radius=56, fill=255)
    edge = 8
    d.rounded_rectangle([x - edge, top - edge, x + shot.width + edge, top + shot.height + edge],
                        radius=64, fill=LIME)
    bg.paste(shot, (x, top), mask)
    return bg


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(1)
    src, out = sys.argv[1], sys.argv[2]
    os.makedirs(out, exist_ok=True)
    files = sorted(f for f in os.listdir(src) if f.lower().endswith((".png", ".jpg", ".jpeg")))
    if not files:
        print("No screenshots found in", src)
        sys.exit(1)
    for name in files:
        stem = os.path.splitext(name)[0]
        caption = CAPTIONS.get(stem[:2], stem[3:].replace("-", " ").capitalize() or stem)
        frame(os.path.join(src, name), caption).save(os.path.join(out, stem + "-play.png"))
        print("framed", name)


if __name__ == "__main__":
    main()
