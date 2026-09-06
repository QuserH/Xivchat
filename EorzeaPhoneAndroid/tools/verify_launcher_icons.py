"""Check adaptive resources and the legacy icons without installing the app."""

from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

from PIL import Image


RES = Path(__file__).resolve().parents[1] / "app/src/main/res"
ANDROID = "{http://schemas.android.com/apk/res/android}"
BACKGROUND = (23, 27, 25, 255)


class LauncherIconTest(unittest.TestCase):
    def test_adaptive_variants_share_full_bleed_layers(self):
        for name in ("ic_launcher", "ic_launcher_round"):
            icon = ET.parse(RES / f"mipmap-anydpi-v26/{name}.xml").getroot()
            self.assertEqual("adaptive-icon", icon.tag)
            self.assertEqual("@color/launcher_background", icon.find("background").get(ANDROID + "drawable"))
            self.assertEqual("@drawable/ic_launcher_foreground", icon.find("foreground").get(ANDROID + "drawable"))
        foreground = ET.parse(RES / "drawable/ic_launcher_foreground.xml").getroot()
        self.assertAlmostEqual(1 / 6, float(foreground.get(ANDROID + "inset").strip("%")) / 100)
        self.assertEqual("fill", foreground.find("bitmap").get(ANDROID + "gravity"))
        self.assertEqual("@drawable/launcher_art", foreground.find("bitmap").get(ANDROID + "src"))
        colors = ET.parse(RES / "values/launcher_colors.xml").getroot()
        self.assertEqual("#171B19", colors.find("color[@name='launcher_background']").text)

    def test_artwork_has_no_transparent_or_white_outer_border(self):
        with Image.open(RES / "drawable-nodpi/launcher_art.png") as icon:
            self.assertEqual((432, 432), icon.size)
            self.assertEqual((255, 255), icon.getchannel("A").getextrema())
            for i in range(432):
                for point in ((i, 0), (i, 431), (0, i), (431, i)):
                    self.assertEqual(BACKGROUND, icon.getpixel(point))

    def test_legacy_density_and_round_masks(self):
        for density, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
            for suffix in ("", "_round"):
                with Image.open(RES / f"mipmap-{density}/ic_launcher{suffix}.png") as icon:
                    self.assertEqual((size, size), icon.size)
                    self.assertEqual(0 if suffix else 255, icon.getpixel((0, 0))[3])
                    top = icon.getpixel((size // 2, 0))
                    self.assertEqual(BACKGROUND[:3], top[:3])
                    self.assertGreaterEqual(top[3], 250)  # antialiased circle edge
                    self.assertEqual(255, icon.getpixel((size // 2, size // 2))[3])


if __name__ == "__main__":
    unittest.main(verbosity=2)
