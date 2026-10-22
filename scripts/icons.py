import sys
from playwright.sync_api import sync_playwright
svg=open('public/logo.svg').read()
with sync_playwright() as p:
    b=p.chromium.launch(executable_path='/opt/pw-browsers/chromium') if False else p.chromium.launch()
    for size,name,pad in [(192,'icon-192.png',0),(512,'icon-512.png',0),(512,'icon-maskable-512.png',1),(180,'apple-touch-icon.png',0)]:
        pg=b.new_page(viewport={'width':size,'height':size})
        inner = size if not pad else int(size*0.8)
        bg = '#5E1A3A' if pad else 'transparent'
        pg.set_content(f'<html><body style="margin:0;background:{bg};display:grid;place-items:center;height:{size}px">{svg.replace("<svg ", f"<svg width={inner} height={inner} ",1)}</body></html>')
        pg.screenshot(path='public/'+name, omit_background=not pad)
    b.close()
