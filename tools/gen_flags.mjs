// Builds the language flag atlas (textures/gui/flags.png + flags.json) from flag-icons (MIT, lipis/flag-icons).
// Usage, from the repository root:
//   npm install --prefix <dir> flag-icons@7.6.0 @resvg/resvg-js@2.6.2
//   node tools/gen_flags.mjs <dir>/node_modules <path to Minecraft's pack.mcmeta language list (json)>
// The second argument is the "language" object of the vanilla pack.mcmeta, saved as a file.
import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';

const [modules, languagesFile] = process.argv.slice(2);
const require = createRequire(path.join(modules, 'x.js'));
const { Resvg } = require('@resvg/resvg-js');
const FLAGS = path.join(modules, 'flag-icons', 'flags', '4x3');
const OUT = 'src/main/resources/assets/swiftclient/textures/gui/';
const CELL_W = 48;
const CELL_H = 36;
const COLS = 16;

// Languages whose code does not name their country, or that have a flag of their own in flag-icons.
const OVERRIDES = {
  bar: 'de', brb: 'nl', esan: 'es', ksh: 'de', lmo: 'it', lzh: 'cn', nah: 'mx', ovd: 'se', pls: 'mx',
  rpr: 'ru', sxu: 'de', szl: 'pl', vro: 'ee', zlm_arab: 'my', sah_sah: 'ru', cv_cu: 'ru', be_latn: 'by',
  la_la: 'va', li_li: 'nl', sr_cs: 'rs', sr_sp: 'rs', enp: 'gb', enws: 'gb',
  ca_es: 'es-ct', gl_es: 'es-ga', eu_es: 'es-pv', cy_gb: 'gb-wls', gd_gb: 'gb-sct',
};
// Constructed or joke languages: they keep their code tag.
const NONE = new Set(['en_pt', 'en_ud', 'lol_us', 'tlh_aa', 'qya_aa', 'jbo_en', 'io_en', 'eo_uy', 'isv', 'tok', 'vp_vl', 'qid']);

const langs = Object.keys(JSON.parse(fs.readFileSync(languagesFile, 'utf8'))).sort();
const countries = [];
const map = {};
for (const lang of langs) {
  if (NONE.has(lang)) continue;
  let country = OVERRIDES[lang];
  if (!country) {
    const parts = lang.split('_');
    country = parts.length === 2 && parts[1].length === 2 ? parts[1] : null;
  }
  if (!country || !fs.existsSync(path.join(FLAGS, country + '.svg'))) {
    console.log('no flag for', lang, country);
    continue;
  }
  if (!countries.includes(country)) countries.push(country);
  map[lang] = countries.indexOf(country);
}

const rows = Math.ceil(countries.length / COLS);
const W = COLS * CELL_W;
const H = rows * CELL_H;
const atlas = Buffer.alloc(W * H * 4);
countries.forEach((country, i) => {
  const svg = fs.readFileSync(path.join(FLAGS, country + '.svg'));
  const png = new Resvg(svg, { fitTo: { mode: 'width', value: CELL_W } }).render();
  const px = png.pixels;
  const ox = (i % COLS) * CELL_W;
  const oy = Math.floor(i / COLS) * CELL_H;
  for (let y = 0; y < Math.min(CELL_H, png.height); y++) {
    for (let x = 0; x < Math.min(CELL_W, png.width); x++) {
      px.copy(atlas, ((oy + y) * W + ox + x) * 4, (y * png.width + x) * 4, (y * png.width + x) * 4 + 4);
    }
  }
});

// Write the atlas as a plain RGBA PNG (no filter, deflate from node:zlib).
const zlib = await import('node:zlib');
const raw = Buffer.alloc((W * 4 + 1) * H);
for (let y = 0; y < H; y++) {
  raw[y * (W * 4 + 1)] = 0;
  atlas.copy(raw, y * (W * 4 + 1) + 1, y * W * 4, (y + 1) * W * 4);
}
const crcTable = new Int32Array(256).map((_, n) => {
  let c = n;
  for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
  return c;
});
const crc = (buf) => {
  let c = -1;
  for (const b of buf) c = crcTable[(c ^ b) & 0xff] ^ (c >>> 8);
  return (c ^ -1) >>> 0;
};
const chunk = (type, data) => {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const body = Buffer.concat([Buffer.from(type), data]);
  const sum = Buffer.alloc(4);
  sum.writeUInt32BE(crc(body));
  return Buffer.concat([len, body, sum]);
};
const ihdr = Buffer.alloc(13);
ihdr.writeUInt32BE(W, 0);
ihdr.writeUInt32BE(H, 4);
ihdr[8] = 8;
ihdr[9] = 6;
const png = Buffer.concat([
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
  chunk('IHDR', ihdr),
  chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
  chunk('IEND', Buffer.alloc(0)),
]);
fs.writeFileSync(OUT + 'flags.png', png);
fs.writeFileSync(OUT + 'flags.json', JSON.stringify({ cellW: CELL_W, cellH: CELL_H, cols: COLS, width: W, height: H, langs: map }));
console.log(countries.length, 'flags,', Object.keys(map).length, 'languages,', W + 'x' + H);
