"""Cek ulang kunci tiu-figural.json dengan solver yang berdiri sendiri.

Setiap gambar dibaca dari notasi teks di stem dan opsi, lalu diubah menjadi
struktur data:
  [grid]   -> tuple 3 string berisi karakter . # o x ^ / \\
  [bentuk] -> dict dengan kunci bentuk, isi, rotasi, titik, panah, ukuran, garis

Solver tidak membaca kunci atau pembahasan. Ia menyelesaikan soal sendiri:
- Analogi: semua aturan di pustaka yang memetakan Gambar 1 ke Gambar 2
  (8 transformasi geometri x operasi isi untuk grid; putar/cermin x operasi
  per atribut untuk bentuk) diterapkan ke Gambar 3. Lolos jika hasilnya
  hanya mengenai opsi kunci, jadi tidak ada pengecoh yang bisa dibenarkan
  oleh tafsiran lain.
- Ketidaksamaan: pustaka ciri (jumlah per simbol, simetri, kelas rotasi,
  segaris, jumlah sisi, paritas, selisih atribut, ketergantungan antar-
  atribut). Setiap ciri yang membagi lima opsi menjadi 4 lawan 1 harus
  menunjuk opsi kunci, dan minimal ada satu ciri seperti itu.
- Serial: pustaka aturan barisan (aturan grid berulang atau selang-seling,
  putaran yang sudutnya bertambah, gerak per simbol di tepi grid dengan
  langkah tetap/bertambah, busur yang memanjang/memendek, periode 2/3, deret
  per atribut dan hubungan antaratribut). Semua ramalan Gambar 5 yang cocok
  dengan Gambar 1-4 hanya boleh mengenai opsi kunci.
Ditambah cek format: penanda notasi di awal stem, opsi sah dan berbeda
secara visual, pembahasan menyebut kunci yang sama.

Pemakaian: python check-tiu-figural.py [file.json]
"""
import json
import re
import sys
from collections import Counter
from fractions import Fraction
from itertools import permutations, product
from pathlib import Path

# =====================================================================
# Notasi 1: grid 3x3
# =====================================================================
GRID_CHARS = set(".#ox^/\\")
SYMS = "#ox^/\\"
FLIP = str.maketrans("/\\", "\\/")

# sel tujuan (r, c) diambil dari sel sumber f(r, c); True = garis / dan \ bertukar
GEOM = {
    "id": (lambda r, c: (r, c), False),
    "r90": (lambda r, c: (2 - c, r), True),        # putar 90 derajat searah jarum jam
    "r180": (lambda r, c: (2 - r, 2 - c), False),
    "r270": (lambda r, c: (c, 2 - r), True),       # putar 90 derajat berlawanan arah
    "mv": (lambda r, c: (r, 2 - c), True),         # cermin sumbu vertikal (kiri-kanan)
    "mh": (lambda r, c: (2 - r, c), True),         # cermin sumbu horizontal (atas-bawah)
    "tr": (lambda r, c: (c, r), False),            # cermin diagonal kiri atas-kanan bawah
    "atr": (lambda r, c: (2 - c, 2 - r), False),   # cermin diagonal kanan atas-kiri bawah
}
GEOM_NAMES = list(GEOM)
# '^' = segitiga kecil yang selalu tegak; hanya identitas dan cermin kiri-kanan
# yang tetap menghasilkan segitiga tegak, transformasi lain tidak bisa ditulis.
UPRIGHT_SAFE = {"id", "mv"}

PER = [(0, 0), (0, 1), (0, 2), (1, 2), (2, 2), (2, 1), (2, 0), (1, 0)]  # tepi 1..8 searah jarum jam
PIDX = {p: i for i, p in enumerate(PER)}
CENTER = (1, 1)
CORNERS = [(0, 0), (0, 2), (2, 0), (2, 2)]
EDGES = [(0, 1), (1, 0), (1, 2), (2, 1)]
LINES = [frozenset((r, c) for c in range(3)) for r in range(3)] + \
        [frozenset((r, c) for r in range(3)) for c in range(3)] + \
        [frozenset((i, i) for i in range(3)), frozenset((i, 2 - i) for i in range(3))]


def parse_grid(text):
    rows = tuple(text.split("\n"))
    if len(rows) != 3 or any(len(r) != 3 or not set(r) <= GRID_CHARS for r in rows):
        raise ValueError(f"grid tidak sah: {text!r}")
    return rows


def fmt_grid(g):
    return "\n".join(g)


def geom(g, name):
    if name not in UPRIGHT_SAFE and any("^" in r for r in g):
        return None
    f, flip = GEOM[name]
    out = []
    for r in range(3):
        row = "".join(g[f(r, c)[0]][f(r, c)[1]] for c in range(3))
        out.append(row.translate(FLIP) if flip else row)
    return tuple(out)


# posisi sumber -> posisi tujuan, untuk memindahkan himpunan sel
FWD = {n: {f(r, c): (r, c) for r in range(3) for c in range(3)} for n, (f, _) in GEOM.items()}


def op_replace(a, b):
    return lambda g: tuple(r.replace(a, b) for r in g)


def op_swap(a, b):
    t = str.maketrans(a + b, b + a)
    return lambda g: tuple(r.translate(t) for r in g)


def op_center(s):
    return lambda g: (g[0], g[1][0] + s + g[1][2], g[2])


def op_corners(s):
    def f(g):
        rows = [list(r) for r in g]
        for r, c in CORNERS:
            if rows[r][c] == ".":
                rows[r][c] = s
        return tuple("".join(r) for r in rows)
    return f


OPS = [("tetap", lambda g: g)]
for _a in SYMS:
    for _b in SYMS + ".":
        if _a != _b:
            OPS.append((f"ganti {_a} jadi {_b}", op_replace(_a, _b)))
_all = SYMS + "."
for _i, _a in enumerate(_all):
    for _b in _all[_i + 1:]:
        OPS.append((f"tukar {_a} dan {_b}", op_swap(_a, _b)))
for _s in "#ox^.":
    OPS.append((f"pusat jadi {_s}", op_center(_s)))
for _s in "#ox^":
    OPS.append((f"pojok kosong jadi {_s}", op_corners(_s)))

GRID_RULES = [(f"{gn} + {on}", gn, op) for gn in GEOM_NAMES for on, op in OPS]


def apply_rule(rule, g):
    t = geom(g, rule[1])
    return None if t is None else rule[2](t)


def cells(g, s):
    return frozenset((r, c) for r in range(3) for c in range(3) if g[r][c] == s)


# =====================================================================
# Notasi 2: deskripsi bentuk satu baris
# =====================================================================
KEYS = ["bentuk", "isi", "rotasi", "titik", "panah", "ukuran", "garis"]
SIDES = {"lingkaran": 0, "segitiga": 3, "persegi": 4, "belah ketupat": 4, "persegi panjang": 4,
         "trapesium": 4, "layang-layang": 4, "segilima": 5, "segienam": 6, "segitujuh": 7,
         "segidelapan": 8}
NAME_BY_SIDES = {3: "segitiga", 4: "persegi", 5: "segilima", 6: "segienam", 7: "segitujuh", 8: "segidelapan"}
# sudut simetri putar; rotasi dibandingkan modulo nilai ini (0 = tidak bisa diputar)
SYM = {"lingkaran": 0, "segitiga": 120, "persegi": 90, "belah ketupat": 180, "persegi panjang": 180,
       "trapesium": 360, "layang-layang": 360, "segilima": 72, "segienam": 60, "segitujuh": 360,
       "segidelapan": 45}
ISI = ["putih", "abu", "hitam"]
UKURAN = ["kecil", "sedang", "besar"]
DIRS = {"atas": 0, "kanan-atas": 45, "kanan": 90, "kanan-bawah": 135, "bawah": 180,
        "kiri-bawah": 225, "kiri": 270, "kiri-atas": 315}
DIR_NAME = {v: k for k, v in DIRS.items()}


def parse_bentuk(text):
    d = {}
    for part in text.split("; "):
        k, _, v = part.partition("=")
        if k in d or k not in KEYS:
            raise ValueError(f"kunci tidak sah/ganda {k!r} di {text!r}")
        if k == "bentuk" and v in SIDES:
            d[k] = v
        elif k == "isi" and v in ISI:
            d[k] = v
        elif k == "ukuran" and v in UKURAN:
            d[k] = v
        elif k == "panah" and v in DIRS:
            d[k] = DIRS[v]
        elif k == "rotasi" and v.isdigit() and int(v) % 45 == 0 and int(v) < 360:
            d[k] = int(v)
        elif k in ("titik", "garis") and v.isdigit():
            d[k] = int(v)
        else:
            raise ValueError(f"nilai tidak sah {part!r} di {text!r}")
    if list(d) != sorted(d, key=KEYS.index) or "bentuk" not in d:
        raise ValueError(f"urutan kunci harus {KEYS}: {text!r}")
    if "rotasi" in d and SYM[d["bentuk"]] == 0:
        raise ValueError(f"rotasi tidak bermakna untuk {d['bentuk']}")
    return d


def fmt_bentuk(d):
    return "; ".join(f"{k}={DIR_NAME[v] if k == 'panah' else v}" for k, v in d.items())


def canon_rot(bentuk, r):
    s = SYM[bentuk]
    return r % s if s else 0


def canon_b(d):
    out = dict(d)
    if "rotasi" in out:
        out["rotasi"] = canon_rot(out["bentuk"], out["rotasi"])
    return tuple((k, out[k]) for k in KEYS if k in out)


GEOM_B = [(f"putar {a}", a, None) for a in range(0, 360, 45)] + [("cermin kiri-kanan", 0, "v"), ("cermin atas-bawah", 0, "h")]


def geom_b(d, a, m):
    if m == "v":
        f = lambda x: (-x) % 360  # noqa: E731
    elif m == "h":
        f = lambda x: (180 - x) % 360  # noqa: E731
    else:
        f = lambda x: (x + a) % 360  # noqa: E731
    out = dict(d)
    for k in ("rotasi", "panah"):
        if k in out:
            out[k] = f(out[k])
    return out


# =====================================================================
# Solver analogi
# =====================================================================
def solve_analogi_grid(g1, g2, g3):
    preds = {}
    for rule in GRID_RULES:
        if apply_rule(rule, g1) == g2:
            p = apply_rule(rule, g3)
            if p is not None:
                preds.setdefault(p, []).append(rule[0])
    return preds


def cat_cands(order, a, b, c):
    out = {p[c] for p in (dict(zip(order, q)) for q in permutations(order)) if p[a] == b}
    x = order.index(c) + order.index(b) - order.index(a)
    if 0 <= x < len(order):
        out.add(order[x])
    return out


def num_cands(a, b, c):
    out = {c + (b - a)}
    if a != 0:
        x = Fraction(c) * Fraction(b, a)
        if x.denominator == 1:
            out.add(int(x))
    return {v for v in out if v >= 0}


def shape_cands(a, b, c):
    if a == b:
        return {c}
    sa, sb, sc = SIDES[a], SIDES[b], SIDES[c]
    if not (sa and sb and sc):
        return set()
    return {NAME_BY_SIDES[v] for v in num_cands(sa, sb, sc) if v in NAME_BY_SIDES}


def solve_analogi_bentuk(d1, d2, d3):
    keys = list(d1)
    geo_keys = [k for k in ("rotasi", "panah") if k in keys]
    geos = []
    for _, a, m in GEOM_B:
        t = geom_b(d1, a, m)
        ok = True
        for k in geo_keys:
            if k == "rotasi":
                ok &= canon_rot(d2["bentuk"], t[k]) == canon_rot(d2["bentuk"], d2[k])
            else:
                ok &= t[k] == d2[k]
        if ok:
            geos.append((a, m))
    preds = {}
    for a, m in geos:
        g3 = geom_b(d3, a, m)
        for b in shape_cands(d1["bentuk"], d2["bentuk"], d3["bentuk"]):
            per = {"bentuk": {b}}
            for k in keys:
                if k == "bentuk":
                    continue
                if k in geo_keys:
                    per[k] = {g3[k]}
                elif k == "isi":
                    per[k] = cat_cands(ISI, d1[k], d2[k], d3[k])
                elif k == "ukuran":
                    per[k] = cat_cands(UKURAN, d1[k], d2[k], d3[k])
                else:  # titik, garis: +k, xk, atau disamakan dengan jumlah sisi
                    per[k] = num_cands(d1[k], d2[k], d3[k])
                    if d2[k] == SIDES[d2["bentuk"]]:
                        per[k] = per[k] | {SIDES[b]}
            label = f"geometri {a}{m or ''}"
            for combo in product(*(sorted(per[k], key=str) for k in keys)):
                preds.setdefault(canon_b(dict(zip(keys, combo))), []).append(label)
    return preds


# =====================================================================
# Solver ketidaksamaan
# =====================================================================
def grid_features(g):
    s = "".join(g)
    f = {f"jumlah {ch}": s.count(ch) for ch in SYMS}
    f["jumlah elemen"] = 9 - s.count(".")
    f["macam simbol"] = len(set(s) - {"."})
    f["selisih # dan o"] = s.count("#") - s.count("o")
    for gn in GEOM_NAMES[1:]:
        t = geom(g, gn)
        f[f"simetri {gn}"] = None if t is None else t == g
    rots = [geom(g, n) for n in ("id", "r90", "r180", "r270")]
    f["kelas rotasi"] = None if None in rots else min(rots)
    allg = [geom(g, n) for n in GEOM_NAMES]
    f["kelas rotasi+cermin"] = None if None in allg else min(allg)
    f["isi pusat"] = g[1][1]
    f["pojok terisi"] = sum(g[r][c] != "." for r, c in CORNERS)
    f["tepi tengah terisi"] = sum(g[r][c] != "." for r, c in EDGES)
    occ = {(r, c) for r in range(3) for c in range(3) if g[r][c] != "."}
    f["segaris"] = any(occ <= ln for ln in LINES) if len(occ) >= 2 else None
    f["garis miring campur"] = "/" in s and "\\" in s
    return f


def bentuk_features(d):
    f = {k: v for k, v in canon_b(d)}
    s = SIDES[d["bentuk"]]
    f["sisi"] = s
    f["sisi genap"] = (s % 2 == 0) if s else None
    if "titik" in d:
        f["titik genap"] = d["titik"] % 2 == 0
        f["titik - sisi"] = d["titik"] - s
    if "garis" in d:
        f["garis - sisi"] = d["garis"] - s
    if "titik" in d and "garis" in d:
        f["titik + garis"] = d["titik"] + d["garis"]
        f["titik - garis"] = d["titik"] - d["garis"]
        f["titik + garis - sisi"] = d["titik"] + d["garis"] - s
    if "panah" in d and "rotasi" in d:
        f["panah relatif rotasi"] = canon_rot(d["bentuk"], d["panah"] - d["rotasi"])
    return f


DEP_KEYS = ["bentuk", "sisi genap", "isi", "ukuran", "panah", "rotasi", "titik genap"]


def odd_one_flags(feats, deps=False):
    flags = {}
    for name in feats[0]:
        vals = [repr(f.get(name)) for f in feats]
        cnt = Counter(vals)
        if sorted(cnt.values()) == [1, 4]:
            i = next(j for j, v in enumerate(vals) if cnt[v] == 1)
            flags.setdefault(i, []).append(name)
    if deps:
        # "P menentukan Q": di empat opsi lain P->Q konsisten (minimal dua nilai P),
        # nilai P milik opsi i muncul minimal dua kali di sana (aturannya terbukti),
        # tetapi Q milik opsi i berbeda.
        keys = [k for k in DEP_KEYS if k in feats[0]]
        for P in keys:
            for Q in keys:
                if P == Q:
                    continue
                for i in range(5):
                    m, seen, ok = {}, Counter(), True
                    for j, f in enumerate(feats):
                        if j != i:
                            seen[repr(f[P])] += 1
                            if m.setdefault(repr(f[P]), repr(f[Q])) != repr(f[Q]):
                                ok = False
                    pi = repr(feats[i][P])
                    if ok and len(m) >= 2 and seen[pi] >= 2 and m[pi] != repr(feats[i][Q]):
                        flags.setdefault(i, []).append(f"{P} menentukan {Q}")
    return flags


# =====================================================================
# Solver serial
# =====================================================================
def shift(P, k):
    return frozenset(PER[(PIDX[p] + k) % 8] for p in P)


def arc_of(P):
    if not P or len(P) >= 8 or CENTER in P:
        return None
    for s in range(8):
        if P == frozenset(PER[(s + i) % 8] for i in range(len(P))):
            return s, len(P)
    return None


def symbol_seq_preds(Ps):
    C = {}
    if Ps[0] == Ps[2] and Ps[1] == Ps[3]:
        C[Ps[0]] = "periode 2/tetap"
    if Ps[0] == Ps[3]:
        C.setdefault(Ps[1], "periode 3")
    for gn in GEOM_NAMES[1:]:
        T = lambda P, m=FWD[gn]: frozenset(m[p] for p in P)  # noqa: E731
        if all(T(Ps[i]) == Ps[i + 1] for i in range(3)):
            C.setdefault(T(Ps[3]), f"posisi {gn} tiap langkah")
    if all(Ps) and all(CENTER not in P for P in Ps):
        ks = [[k for k in range(8) if shift(Ps[i], k) == Ps[i + 1]] for i in range(3)]
        for k1, k2, k3 in product(*ks):
            if (k2 - k1) % 8 == (k3 - k2) % 8:
                C.setdefault(shift(Ps[3], (2 * k3 - k2) % 8), f"geser {k1},{k2},{k3}")
    arcs = [arc_of(P) for P in Ps]
    if all(arcs):
        s = [a[0] for a in arcs]
        ln = [a[1] for a in arcs]
        ds, dl = (s[1] - s[0]) % 8, ln[1] - ln[0]
        if all((s[i + 1] - s[i]) % 8 == ds and ln[i + 1] - ln[i] == dl for i in range(3)):
            L, S = ln[3] + dl, (s[3] + ds) % 8
            if 0 <= L <= 8:
                C.setdefault(frozenset(PER[(S + i) % 8] for i in range(L)), "busur")
    return C


def solve_serial_grid(fr):
    preds = {}

    def add(p, why):
        if p is not None:
            preds.setdefault(p, []).append(why)

    for rule in GRID_RULES:  # satu aturan diulang
        if all(apply_rule(rule, fr[i]) == fr[i + 1] for i in range(3)):
            add(apply_rule(rule, fr[3]), f"ulang {rule[0]}")
    A = [r for r in GRID_RULES if apply_rule(r, fr[0]) == fr[1] and apply_rule(r, fr[2]) == fr[3]]
    if A:  # dua aturan bergantian
        for r in GRID_RULES:
            if apply_rule(r, fr[1]) == fr[2]:
                add(apply_rule(r, fr[3]), f"selang-seling {A[0][0]} / {r[0]}")
    if fr[0] == fr[2] and fr[1] == fr[3]:
        add(fr[0], "periode 2")
    if fr[0] == fr[3]:
        add(fr[1], "periode 3")
    rots = ["id", "r90", "r180", "r270"]  # sudut putar bertambah tetap
    ks = [[k for k in range(4) if geom(fr[i], rots[k]) == fr[i + 1]] for i in range(3)]
    for k1, k2, k3 in product(*ks):
        if (k2 - k1) % 4 == (k3 - k2) % 4:
            add(geom(fr[3], rots[(2 * k3 - k2) % 4]), "putaran bertambah")
    syms = sorted(set("".join("".join(f) for f in fr)) - {"."})
    per = [[(s, P, why) for P, why in symbol_seq_preds([cells(f, s) for f in fr]).items()] for s in syms]
    for combo in product(*per):
        grid = [["."] * 3 for _ in range(3)]
        ok = True
        for s, P, _ in combo:
            for r, c in P:
                ok &= grid[r][c] == "."
                grid[r][c] = s
        if ok:
            add(tuple("".join(r) for r in grid), "per simbol: " + ", ".join(f"{s} {w}" for s, _, w in combo))
    return preds


def seq_num_preds(v, mod=None):
    norm = (lambda x: x % mod) if mod else (lambda x: x)  # noqa: E731
    P = set()
    d = [norm(v[i + 1] - v[i]) for i in range(3)]
    if d[0] == d[1] == d[2]:
        P.add(norm(v[3] + d[2]))
    if norm(d[1] - d[0]) == norm(d[2] - d[1]):
        P.add(norm(v[3] + d[2] + d[2] - d[1]))
    if v[0] == v[2] and v[1] == v[3]:
        P.add(v[0])
    if v[0] == v[3]:
        P.add(v[1])
    if not mod:
        if 0 not in v[:3]:
            r = Fraction(v[1], v[0])
            if Fraction(v[2], v[1]) == r and Fraction(v[3], v[2]) == r and (v[3] * r).denominator == 1:
                P.add(int(v[3] * r))
        if v[2] == v[0] + v[1] and v[3] == v[1] + v[2]:
            P.add(v[2] + v[3])
    return P


def seq_cat_preds(v):
    P = set()
    if v[0] == v[2] and v[1] == v[3]:
        P.add(v[0])
    if v[0] == v[3]:
        P.add(v[1])
    return P


def solve_serial_bentuk(fr):
    keys = list(fr[0])
    cand = {}
    for k in keys:
        vals = [f[k] for f in fr]
        if k == "bentuk":
            C = seq_cat_preds(vals)
            sides = [SIDES[v] for v in vals]
            if all(sides):
                C |= {NAME_BY_SIDES[x] for x in seq_num_preds(sides) if x in NAME_BY_SIDES}
        elif k in ("isi", "ukuran"):
            order = ISI if k == "isi" else UKURAN
            C = seq_cat_preds(vals) | {order[x] for x in seq_num_preds([order.index(x) for x in vals]) if 0 <= x < 3}
        elif k in ("rotasi", "panah"):
            C = seq_num_preds(vals, 360)
        else:
            C = {x for x in seq_num_preds(vals) if x >= 0}
        cand[k] = C

    def num(f, k):
        return SIDES[f["bentuk"]] if k == "sisi" else f[k]

    for X, Y in (("titik", "sisi"), ("garis", "sisi"), ("titik", "garis"), ("garis", "titik")):
        if X in keys and (Y == "sisi" or Y in keys):
            diffs = {num(f, X) - num(f, Y) for f in fr}
            if len(diffs) == 1:
                c = diffs.pop()
                ys = [SIDES[b] for b in cand["bentuk"]] if Y == "sisi" else list(cand[Y])
                cand[X] = cand[X] | {y + c for y in ys if y + c >= 0}
    if "panah" in keys and "rotasi" in keys:
        diffs = {(f["panah"] - f["rotasi"]) % 360 for f in fr}
        if len(diffs) == 1:
            c = diffs.pop()
            cand["panah"] = cand["panah"] | {(r + c) % 360 for r in cand["rotasi"]}
            cand["rotasi"] = cand["rotasi"] | {(p - c) % 360 for p in cand["panah"]}
    preds = {}
    for combo in product(*(sorted(cand[k], key=str) for k in keys)):
        d = dict(zip(keys, combo))
        if "rotasi" in d and SYM[d["bentuk"]] == 0:
            continue
        preds.setdefault(canon_b(d), []).append("deret per atribut")
    return preds


# =====================================================================
# Uji mandiri transformasi (memastikan arah putar dan cermin benar)
# =====================================================================
def self_test():
    L = ("#..", "#..", "##.")
    assert geom(L, "r90") == ("###", "#..", "..."), "r90 salah"
    assert geom(L, "r270") == ("...", "..#", "###"), "r270 salah"
    assert geom(("###", "#..", "..."), "mh") == ("...", "#..", "###"), "cermin atas-bawah salah"
    assert geom(("#o.", "...", "..."), "mv") == (".o#", "...", "..."), "cermin kiri-kanan salah"
    assert geom(("/..", "...", "..."), "r90") == ("..\\", "...", "..."), "garis tidak ikut berputar"
    assert geom(("/..", "...", "..."), "mv") == ("..\\", "...", "..."), "garis tidak ikut dicermin"
    assert geom(("/..", "...", "..."), "r180") == ("...", "...", "../"), "r180 garis salah"
    g = ("#o/", "x.\\", "..#")
    assert geom(geom(g, "r90"), "r90") == geom(g, "r180")
    assert geom(geom(geom(g, "r90"), "r90"), "r180") == g
    assert geom(geom(g, "mv"), "mh") == geom(g, "r180")
    assert geom(geom(g, "r90"), "mv") == geom(g, "tr")
    assert geom(geom(g, "r270"), "r90") == g
    assert geom(("^..", "...", "..."), "r90") is None and geom(("^..", "...", "..."), "mv") == ("..^", "...", "...")
    assert geom_b({"bentuk": "segitiga", "panah": DIRS["kanan-atas"]}, 90, None)["panah"] == DIRS["kanan-bawah"]
    assert geom_b({"bentuk": "segitiga", "panah": DIRS["kanan-atas"]}, 0, "v")["panah"] == DIRS["kiri-atas"]
    assert geom_b({"bentuk": "segitiga", "panah": DIRS["kanan-atas"]}, 0, "h")["panah"] == DIRS["kanan-bawah"]
    assert canon_rot("segitiga", 120) == canon_rot("segitiga", 0) and canon_rot("persegi", 90) == 0
    assert shift(frozenset([PER[7]]), 1) == frozenset([PER[0]])


# =====================================================================
# Pemeriksaan per soal
# =====================================================================
NOTE_RE = re.compile(r"^\[(grid|bentuk)\] ")


def figures(stem, notation):
    if notation == "grid":
        found = re.findall(r"Gambar (\d+):\n([^\n]{3}\n[^\n]{3}\n[^\n]{3})", stem)
        return {int(n): parse_grid(t) for n, t in found}
    found = re.findall(r"Gambar (\d+): ([^\n]+)", stem)
    return {int(n): parse_bentuk(t) for n, t in found}


def check_question(q):
    m = NOTE_RE.match(q["stem"])
    assert m, "stem tidak diawali [grid] atau [bentuk]"
    notation = m.group(1)
    parse = parse_grid if notation == "grid" else parse_bentuk
    canon = (lambda g: g) if notation == "grid" else canon_b  # noqa: E731
    opts = [parse(o["text"]) for o in q["options"]]
    key = [o["score"] for o in q["options"]].index(5)
    copts = [canon(o) for o in opts]
    assert len(set(copts)) == 5, "ada dua opsi yang secara visual sama"
    figs = figures(q["stem"], notation)
    if notation == "bentuk":
        shapes = [list(o) for o in opts] + [list(f) for f in figs.values()]
        assert all(s == shapes[0] for s in shapes), "kunci atribut tidak seragam dalam satu soal"
    tag = q["tag"]
    if tag in ("analogi", "analogi_gambar"):
        assert sorted(figs) == [1, 2, 3], f"stem harus memuat Gambar 1-3, dapat {sorted(figs)}"
        assert canon(figs[1]) != canon(figs[2]), "Gambar 1 sama dengan Gambar 2"
        solve = solve_analogi_grid if notation == "grid" else solve_analogi_bentuk
        preds = solve(figs[1], figs[2], figs[3])
        hits = [i for i, c in enumerate(copts) if c in preds]
        why = preds.get(copts[key], ["-"])[0]
    elif tag == "serial":
        assert sorted(figs) == [1, 2, 3, 4], f"stem harus memuat Gambar 1-4, dapat {sorted(figs)}"
        fr = [figs[i] for i in (1, 2, 3, 4)]
        preds = solve_serial_grid(fr) if notation == "grid" else solve_serial_bentuk(fr)
        hits = [i for i, c in enumerate(copts) if c in preds]
        why = preds.get(copts[key], ["-"])[0]
    elif tag == "ketidaksamaan":
        assert not figs, "soal ketidaksamaan tidak memuat Gambar bernomor di stem"
        feats = [grid_features(o) if notation == "grid" else bentuk_features(o) for o in opts]
        flags = odd_one_flags(feats, deps=notation == "bentuk")
        hits = sorted(flags)
        why = ", ".join(flags.get(key, ["-"]))
    else:
        raise AssertionError(f"tag tidak dikenal {tag}")
    letters = "ABCDE"
    assert hits == [key], (f"opsi yang lolos aturan = {[letters[i] for i in hits]}, kunci = {letters[key]}"
                           + (f"; ciri: { {letters[i]: v for i, v in flags.items()} }" if tag == "ketidaksamaan" else ""))
    mk = re.search(r"Jawabannya ([A-E])\.", q["explanation"])
    assert mk and mk.group(1) == letters[key], "pembahasan tidak menyebut 'Jawabannya <kunci>.' yang sama"
    return notation, why


def main():
    self_test()
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).with_name("tiu-figural.json")
    data = json.loads(path.read_text(encoding="utf-8"))
    errors, per_tag, per_note = [], Counter(), Counter()
    spread = []
    for q in data:
        try:
            notation, why = check_question(q)
        except (AssertionError, ValueError) as e:
            errors.append(f"{q.get('id')}: {e}")
            continue
        per_tag[q["tag"]] += 1
        per_note[notation] += 1
        lens = [len(o["text"]) for o in q["options"]]
        spread.append(max(lens) - min(lens))
        print(f"{q['id']}: lolos [{notation}] {why[:90]}")
    print(f"\nlolos per tag: {dict(per_tag)}; per notasi: {dict(per_note)}")
    if spread:
        print(f"selisih panjang opsi terpanjang-terpendek: maks {max(spread)}, soal dengan panjang sama persis: {spread.count(0)}")
    if errors:
        print("\n".join(errors))
        print(f"\nGAGAL: {len(errors)} soal")
        sys.exit(1)
    print(f"\nOK: {len(data)} soal, setiap soal tepat satu opsi yang memenuhi aturan dan itulah kuncinya")


if __name__ == "__main__":
    main()
