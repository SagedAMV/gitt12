"""Full-frame RGB SSIM; no crop, alignment, rescale, mask, or background subtraction.
Requires: pillow numpy scikit-image. Run from any working directory.
"""
from pathlib import Path
from PIL import Image
import numpy as np
from skimage.metrics import structural_similarity
import json
root = Path(__file__).resolve().parents[1]
rows = []
for name in ['repos-light','repos-dark','files-light','files-dark','error-light']:
    a = np.array(Image.open(root/'html'/f'{name}.png').convert('RGB'))
    b = np.array(Image.open(root/'native'/f'{name}.png').convert('RGB'))
    assert a.shape == b.shape == (640,360,3)
    score = float(structural_similarity(a,b,channel_axis=2,data_range=255))
    rows.append(dict(screen=name,ssim=score,passes=score>=.90))
report = dict(metric='skimage RGB SSIM, default 7px window; full frame, data_range=255',width=360,height=640,native='Compose + Robolectric SDK35, native Skia + Roborazzi; mdpi; not a physical device',reference='Chromium HTML; status removed; SVG symbols retained; original design content unchanged',results=rows,minimum=min(r['ssim'] for r in rows),mean=sum(r['ssim'] for r in rows)/len(rows),all_pass=all(r['passes'] for r in rows))
(root/'metrics.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
raise SystemExit(0 if report['all_pass'] else 1)
