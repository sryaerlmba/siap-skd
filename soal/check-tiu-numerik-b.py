"""Cek kunci tiu-numerik-b.json dengan menghitung ulang.

- Perbandingan kuantitatif: P dan Q dihitung (eksak) untuk satu nilai atau
  sekumpulan contoh yang memenuhi informasi soal. Kunci yang diharapkan:
  A/B/C jika urutan P vs Q selalu sama; E jika urutan berubah tetapi relasi
  opsi E selalu berlaku; D jika urutan berubah dan relasi E tidak selalu
  berlaku. Untuk D dan E dicetak dua contoh yang memberi urutan berbeda.
- Soal cerita: jawaban dihitung ulang dan setiap pengecoh dihitung dari
  rumus kesalahannya; himpunan teks opsi harus sama persis.

Pemakaian: python check-tiu-numerik-b.py [tiu-numerik-b.json]
"""
import itertools
import json
import math
import sys
from datetime import date
from decimal import ROUND_HALF_UP, Decimal
from fractions import Fraction as F
from math import comb
from pathlib import Path

import sympy as sp

sys.stdout.reconfigure(encoding="utf-8")
PATH = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).with_name("tiu-numerik-b.json")
DATA = json.loads(PATH.read_text(encoding="utf-8"))
BY_ID = {q["id"]: q for q in DATA}
FAILS = []


def key_of(q):
    return next(o["key"] for o in q["options"] if o["score"] == 5)


def text(q, k):
    return next(o["text"] for o in q["options"] if o["key"] == k)


# ---------- format angka gaya Indonesia ----------
def num(x, nd=None):
    x = F(x)
    if nd is None:
        if x.denominator == 1:
            nd = 0
        else:
            for k in range(1, 7):
                if (x * 10**k).denominator == 1:
                    nd = k
                    break
            else:
                raise ValueError(f"perlu pembulatan: {x}")
    d = (Decimal(x.numerator) / Decimal(x.denominator)).quantize(Decimal(1).scaleb(-nd), rounding=ROUND_HALF_UP)
    return f"{d:,.{nd}f}".replace(",", "_").replace(".", ",").replace("_", ".")


def rp(x):
    assert F(x).denominator == 1, x
    return "Rp" + num(x)


def hhmm(minutes):
    m = F(minutes)
    assert m.denominator == 1, m
    h, mm = divmod(int(m), 60)
    return f"{h:02d}.{mm:02d}"


def jam(hours):
    m = F(hours) * 60
    assert m.denominator == 1, m
    h, mm = divmod(int(m), 60)
    if mm == 0:
        return f"{h} jam"
    return f"{h} jam {mm} menit" if h else f"{mm} menit"


def fr(x):
    x = F(x)
    return f"{x.numerator}/{x.denominator}"


HARI = ["Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"]


def hari(start, plus):
    return HARI[(HARI.index(start) + plus) % 7]


# ---------- perbandingan kuantitatif ----------
def eq(a, b):
    if isinstance(a, (int, F)) and isinstance(b, (int, F)):
        return a == b
    return sp.simplify(sp.expand(sp.sympify(a) - sp.sympify(b))) == 0


def sgn(a, b):
    if isinstance(a, (int, F)) and isinstance(b, (int, F)):
        return (a > b) - (a < b)
    if eq(a, b):
        return 0
    return 1 if (sp.sympify(a) - sp.sympify(b)).evalf(60) > 0 else -1


def roots(poly):
    x = sp.Symbol("x")
    return [F(int(r.p), int(r.q)) for r in sp.solve(poly(x), x)]


def rng(*vals):
    return [F(v) for v in vals]


R = F  # alias singkat untuk pecahan eksak

PK = {
    1: ("P = 2Q", lambda P, Q: eq(P, 2 * Q), lambda: [("tetap", R(18, 100) * 450, R(9, 100) * 900)]),
    2: ("P − Q = 0,25", lambda P, Q: eq(P - Q, R(1, 4)), lambda: [("tetap", R(3, 8) + R(1, 4), R(6, 10))]),
    3: ("P = 1,5Q", lambda P, Q: eq(P, R(3, 2) * Q),
        lambda: [(f"X={X}, Y={Y}", R(12, 10) * X, R(8, 10) * Y)
                 for X, Y in [(100000, 100000), (50000, 100000), (100000, 50000), (60000, 90000)]]),
    4: ("P = Q + 1", lambda P, Q: eq(P, Q + 1),
        lambda: [(f"x={x}, y={y}", x + 2 * y, 2 * x) for x, y in [((10 + 4) // 2, (10 - 4) // 2)]]),
    5: ("P = 2Q", lambda P, Q: eq(P, 2 * Q),
        lambda: [(f"a={a}", 6 * a - 4, 3 * a - 2) for a in rng(-3, -1, 0, R(2, 3), R(1, 2), 1, 2, 5)]),
    6: ("P + Q = 300", lambda P, Q: eq(P + Q, 300), lambda: [("tetap", R(22, 7) * 7 * 7, 12 * 12)]),
    7: ("Q = 2P", lambda P, Q: eq(Q, 2 * P), lambda: [("tetap", 6**20, 3**40)]),
    8: ("P = 2Q", lambda P, Q: eq(P, 2 * Q), lambda: [("tetap", R(1, 2) * 12 * 10, R(1, 2) * (7 + 8) * 8)]),
    9: ("P = Q + 6 cm", lambda P, Q: eq(P, Q + 6),
        lambda: [(f"sisi ketiga={s}", s, 7) for s in rng(R(7, 2), 5, 6, 7, 10, R(129, 10)) if 8 - 5 < s < 8 + 5]),
    10: ("P = 4Q", lambda P, Q: eq(P, 4 * Q),
         lambda: [(f"x={x}", x**2, x) for x in roots(lambda x: x**2 - 16)]),
    11: ("P + Q = 1", lambda P, Q: eq(P + Q, 1), lambda: [("tetap", R(1, 2) ** -3, (-3) ** 2)]),
    12: ("P = Q + 6", lambda P, Q: eq(P, Q + 6),
         lambda: [(f"x={x}", x**2 - x, 5 * x) for x in roots(lambda x: 2 * x + 5 - 17)]),
    13: ("P + Q = 0", lambda P, Q: eq(P + Q, 0),
         lambda: [(f"a={a}, b={10 - a}", a - 5, (10 - a) - 5) for a in rng(-4, 0, 3, 5, 7, R(31, 2))]),
    14: ("P = 2Q", lambda P, Q: eq(P, 2 * Q),
         lambda: [(f"x={x}", x**2, 2 * x) for x in rng(R(1, 10), R(1, 2), 1, 2, 3, 4, 10)]),
    15: ("P = 2Q", lambda P, Q: eq(P, 2 * Q), lambda: [("tetap", 5**20, 2**45)]),
    16: ("Q − P = Rp2.000", lambda P, Q: eq(Q - P, 2000),
         lambda: [("tetap", 200000 * R(70, 100), 200000 * R(80, 100) * R(90, 100))]),
    17: ("P = 2⁹Q", lambda P, Q: eq(P, 2**9 * Q), lambda: [("tetap", 2**10 + 2**10, 2**11)]),
    18: ("Q − P = 11 cm²", lambda P, Q: eq(Q - P, 11),
         lambda: [(f"K={k}x{10 - k}, L={m}x{12 - m}", k * (10 - k), m * (12 - m))
                  for k in rng(*[R(i, 2) for i in range(1, 20)]) for m in rng(*[R(i, 2) for i in range(1, 24)])]),
    19: ("P = Q + 1", lambda P, Q: eq(P, Q + 1), lambda: [("tetap", R(6 * 14 - 24, 5), R(23, 2))]),
    20: ("P = 3Q", lambda P, Q: eq(P, 3 * Q),
         lambda: [(f"k={k}", R(150, 100) * k, k / 2) for k in rng(-4, -1, 0, 1, 2, 10)]),
    21: ("Q − P = 3", lambda P, Q: eq(Q - P, 3), lambda: [("tetap", R(40, 100) * 75, R(75, 100) * 42)]),
    22: ("P = 12Q", lambda P, Q: eq(P, 12 * Q),
         lambda: [(f"x={x}", (x + 3) ** 2 - (x - 3) ** 2, 12 * x) for x in rng(-3, -1, 0, R(1, 2), 2, 7)]),
    23: ("P = Q + 1", lambda P, Q: eq(P, Q + 1),
         lambda: [(f"x={x}, y={y}", x * x, y * y) for x in range(-5, 6) for y in range(-5, 6) if x > y]),
    24: ("P = Q + 0,5", lambda P, Q: eq(P, Q + R(1, 2)),
         lambda: [(f"b={b}, a={R(3, 4) * b}", (R(3, 4) * b + b) / b, R(17, 10)) for b in rng(-8, -2, R(1, 3), 4, 100)]),
    25: ("P + Q = 240°", lambda P, Q: eq(P + Q, 240), lambda: [("tetap", R((5 - 2) * 180, 5), R((6 - 2) * 180, 6))]),
    26: ("P = Q + Rp4.000", lambda P, Q: eq(P, Q + 4000),
         lambda: [("tetap", 80000 * R(125, 100) * R(80, 100), 80000)]),
    27: ("P + Q = 156", lambda P, Q: eq(P + Q, 156),
         lambda: [(f"Dika={d}, Eko={156 - d}", d, 156 - d) for d in rng(56, 70, 78, 80, 100)]),
    28: ("P = Q − 1", lambda P, Q: eq(P, Q - 1), lambda: [("tetap", R(20 * 70 + 30 * 80, 50), 75)]),
    29: ("P = Q − 2", lambda P, Q: eq(P, Q - 2),
         lambda: [(f"data={t}", max(t), 12) for t in itertools.combinations_with_replacement(range(1, 47), 5)
                  if sum(t) == 50]),
    30: ("P = Q + 6", lambda P, Q: eq(P, Q + 6), lambda: [("tetap", 9 * 72 - 8 * 73, 65)]),
    31: ("P = 3Q", lambda P, Q: eq(P, 3 * Q), lambda: [("tetap", sp.root(27, 3) ** 2, sp.sqrt(81))]),
    32: ("P = Q²", lambda P, Q: eq(P, Q**2),
         lambda: [(f"x={x}", sp.sqrt(sp.Rational(x.numerator, x.denominator)), x)
                  for x in rng(R(1, 100), R(1, 10), R(1, 4), R(1, 2), R(9, 10), R(99, 100))]),
    33: ("P = Q + 0,5", lambda P, Q: eq(P, Q + R(1, 2)),
         lambda: [(f"x={x}", x, R(5, 2)) for x in roots(lambda x: x**2 - 5 * x + 6)]),
    34: ("PQ = 1", lambda P, Q: eq(P * Q, 1),
         lambda: [(f"x={x}, y={y}", R(x, y), R(y, x)) for x, y in [(2, 1), (1, 2), (-3, 2), (5, 5), (-1, -4), (3, -6)]]),
    35: ("P = Q − 1", lambda P, Q: eq(P, Q - 1),
         lambda: [(f"x={x}", x, 5) for x in roots(lambda x: x**2 - 7 * x + 12)]),
    36: ("P² + 18 = Q²", lambda P, Q: eq(P**2 + 18, Q**2), lambda: [("tetap", sp.sqrt(50) + sp.sqrt(2), sp.sqrt(70))]),
    37: ("P + Q = 0", lambda P, Q: eq(P + Q, 0),
         lambda: [(f"x,y,z={t}", t[0] + t[1] - 8, 4 - t[2])
                  for t in [(4, 4, 4), (1, 2, 9), (-5, 10, 7), (0, 0, 12), (20, 1, -9), (R(1, 2), R(3, 2), 10)]]),
    38: ("P = 2Q", lambda P, Q: eq(P, 2 * Q), lambda: [(f"n={n}", 2**n, n**2) for n in range(1, 21)]),
    39: ("Q − P = 2 cm", lambda P, Q: eq(Q - P, 2), lambda: [("tetap", 4 * math.isqrt(49), R(22, 7) * 9)]),
    40: ("Q = −2P", lambda P, Q: eq(Q, -2 * P),
         lambda: [(f"n={n}", (-2) ** n, (-2) ** (n + 1)) for n in range(1, 13)]),
}

ORDER_KEY = {1: "A", -1: "B", 0: "C"}
ORDER_TXT = {1: "P > Q", -1: "P < Q", 0: "P = Q"}
STD = ["P > Q", "P < Q", "P = Q", "hubungan P dan Q tidak dapat ditentukan"]


def check_pk():
    print("=== PERBANDINGAN KUANTITATIF ===")
    for n, (e_text, e_rel, sampler) in PK.items():
        qid = f"tiu-numerik-perbandingan_kuantitatif-{n:04d}"
        q = BY_ID[qid]
        samples = sampler()
        assert samples, qid
        res = [(lab, P, Q, sgn(P, Q), bool(e_rel(P, Q))) for lab, P, Q in samples]
        orders = {r[3] for r in res}
        e_all = all(r[4] for r in res)
        if len(orders) == 1:
            o = orders.pop()
            expect = ORDER_KEY[o]
            if e_all:
                FAILS.append(f"{qid}: relasi E '{e_text}' juga selalu benar, soal ambigu")
            lab, P, Q = res[0][:3]
            detail = f"{ORDER_TXT[o]} untuk semua {len(res)} contoh (mis. {lab}: P={P}, Q={Q}); E salah"
        else:
            expect = "E" if e_all else "D"
            by_o = {}
            for r in res:
                by_o.setdefault(r[3], r)
            pick = [1, -1] if {1, -1} <= orders else sorted(orders)[:2]
            ex = [by_o[o] for o in pick]
            detail = " | ".join(f"{r[0]}: P={r[1]}, Q={r[2]} -> {ORDER_TXT[r[3]]}" for r in ex)
            detail += f" | relasi E {'selalu benar' if e_all else 'tidak selalu benar'}"
        texts = [o["text"] for o in q["options"]]
        if texts[:4] != STD:
            FAILS.append(f"{qid}: opsi A-D bukan format baku")
        if texts[4] != e_text:
            FAILS.append(f"{qid}: teks opsi E '{texts[4]}' != '{e_text}' di skrip")
        got = key_of(q)
        status = "OK " if got == expect else "SALAH"
        if got != expect:
            FAILS.append(f"{qid}: kunci file {got}, hitungan {expect}")
        print(f"{status} PK-{n:02d} kunci {got} (hitung {expect}): {detail}")


# ---------- soal cerita: (jawaban, {pengecoh: penyebab}) ----------
def sc01():
    v, t = 80, 2 + F(15, 60)
    return f"{num(v * t)} km", {f"{num(v * 2)} km": "hanya 2 jam", f"{num(v * F(215, 100))} km": "2 jam 15 menit = 2,15",
                                f"{num(v * 2 + 15)} km": "menit ditambahkan langsung", f"{num(v * F(5, 2))} km": "15 menit = 0,5 jam"}


def sc02():
    d, v1, v2, start = 210, 80, 60, 8 * 60
    t = F(d, v1 + v2)
    return hhmm(start + t * 60), {
        hhmm(start + F(d, 2) / v2 * 60): "dianggap bertemu di tengah",
        hhmm(start + 60 + 50): "1,5 jam dibaca 1 jam 50 menit",
        hhmm(start + F(d, F(v1 + v2, 2)) * 60): "pakai rata-rata kecepatan",
        hhmm(start + F(d, v1 - v2) * 60): "pakai selisih kecepatan"}


def sc03():
    v1, v2, jarak = 12, 36, 15
    head = v1 * F(1, 2)
    t = head / (v2 - v1)
    assert v1 * (F(1, 2) + t) < jarak, "Rina sudah sampai pasar"
    start2 = 7 * 60 + 30
    return hhmm(start2 + t * 60), {hhmm(7 * 60 + t * 60): "15 menit ditambahkan ke 07.00",
                                   hhmm(start2 + head / v2 * 60): "Rina dianggap diam",
                                   hhmm(start2 + 25): "0,25 jam dibaca 25 menit",
                                   hhmm(start2 + head / v1 * 60): "dibagi kecepatan Rina"}


def sc04():
    v = F(48 * 60, 2 * 60 - 48)
    assert F(2 * 60 * v, 60 + v) == 48
    return f"{num(v)} km/jam", {"12 km/jam": "60 − 48", "24 km/jam": "48 ÷ 2", f"{num(2 * 48 - 60)} km/jam": "rata-rata biasa",
                                f"{num(F(48 * 60, 60 - 48))} km/jam": "lupa faktor 2 pada rumus"}


def sc05():
    v = F(72 * 1000, 3600)
    return f"{num((400 + 200) / v)} detik", {f"{num(F(600, 72), 1)} detik": "tanpa konversi satuan",
                                             f"{num((400 - 200) / v)} detik": "selisih panjang",
                                             f"{num(400 / v)} detik": "panjang kereta diabaikan",
                                             f"{num((400 + 2 * 200) / v)} detik": "panjang kereta dihitung dua kali"}


def sc06():
    d, v1, v2, s1, s2 = 220, 40, 60, 7 * 60, 7 * 60 + 30
    rem = d - v1 * F(1, 2)
    t = rem / (v1 + v2)
    t_salah = F(d, v1 + v2)
    return hhmm(s2 + t * 60), {hhmm(s1 + t * 60): "2 jam dihitung dari 07.00", hhmm(s1 + t_salah * 60): "berangkat dianggap bersamaan dari 07.00",
                               hhmm(s2 + t_salah * 60): "jarak awal truk diabaikan", hhmm(s2 + 2 * 60 + 20): "2,2 jam dibaca 2 jam 20 menit"}


def sc07():
    ayah, anak = (50 + 26) // 2, (50 - 26) // 2
    return f"{ayah + 5} tahun", {f"{anak} tahun": "usia anak", f"{anak + 5} tahun": "usia anak 5 tahun lagi",
                                 f"{50 - 26} tahun": "50 − 26", f"{ayah} tahun": "lupa ditambah 5"}


def sc08():
    sol = [(r, a) for a in range(1, 60) for r in range(1, 100) if r - 5 == 3 * (a - 5) and r + 3 == 2 * (a + 3)]
    assert len(sol) == 1
    r, a = sol[0]
    return f"{r} tahun", {f"{a} tahun": "usia adik", f"{a + 3} tahun": "adik 3 tahun lagi",
                          f"{r - 5} tahun": "Rani 5 tahun lalu", f"{r + 3} tahun": "Rani 3 tahun lagi"}


def sc09():
    k = F(8, 5 - 3)
    d, e = 5 * k, 3 * k
    n = next(n for n in range(0, 100) if F(d + n, e + n) == F(3, 2))
    return f"{num(d + e + 2 * n)} tahun", {f"{num(d)} tahun": "usia Dimas sekarang", f"{num(d + n)} tahun": "usia Dimas saat itu",
                                           f"{num(d + e)} tahun": "jumlah sekarang", f"{num(d + e + n)} tahun": "hanya satu orang ditambah"}


def sc10():
    c = next(c for c in range(1, 60) if 7 * c + 10 == 4 * (c + 10))
    hadi, cucu = 7 * c + 5, c + 5
    return f"{hadi + cucu} tahun", {f"{7 * c} tahun": "Pak Hadi 5 tahun lalu", f"{hadi} tahun": "Pak Hadi saja",
                                    f"{7 * c + c} tahun": "jumlah 5 tahun lalu", f"{hadi + cucu + 10} tahun": "jumlah 5 tahun lagi"}


def sc11():
    t = 1 / (F(1, 6) + F(1, 12))
    return jam(t), {"6 jam": "selisih 12 − 6", jam(F(6 + 12, 2)): "rata-rata / (1+1)/(6+12)",
                    jam(6 + 12): "jumlah waktu", jam(6 * 12): "lupa dibagi 18"}


def sc12():
    t = 1 / (F(1, 3) + F(1, 6) - F(1, 4))
    return jam(t), {jam(1 / (F(1, 3) + F(1, 6) + F(1, 4))): "pembuangan dijumlahkan", jam(1 / (F(1, 3) + F(1, 6))): "pembuangan diabaikan",
                    jam(3 + 6 - 4): "waktu dijumlah-kurang", jam(3 + 6 + 4): "waktu dijumlahkan"}


def sc13():
    T = next(T for T in [F(i, 10) for i in range(1, 300)] if T / 10 + (T - 5) / 15 == 1)
    bersama = F(10 * 15, 10 + 15)
    return f"{num(T - 5)} hari", {"5 hari": "lama berhenti", f"{num(bersama)} hari": "bekerja bersama sampai selesai",
                                  f"{num(T)} hari": "total waktu", f"{num(bersama + 5)} hari": "6 + 5"}


def sc14():
    rate = F(600, 3 * 4)
    need = F(1500, rate * 3)
    return f"{num(need)} orang", {f"{num(3 * F(4, 3))} orang": "data diabaikan", f"{math.ceil(3 * F(1500, 600) * F(3, 4))} orang": "waktu dianggap senilai",
                                  f"{num(need - 3)} orang": "hanya tambahan", f"{math.ceil(3 * F(1500, 600))} orang": "waktu diabaikan"}


def sc15():
    return f"{num(F(40 * 15, 60))} hari", {"15 hari": "dianggap tidak berubah", f"{num(F(15 * 60, 40))} hari": "dianggap senilai",
                                           f"{num(F(15 * 40, 20))} hari": "dibagi tambahan 20", f"{num(F(15 * 60, 20))} hari": "60 dibagi 20"}


def sc16():
    rem = 18 * (20 - 8)
    sisa = F(rem, 12)
    return f"{num(8 + sisa)} hari", {"12 hari": "sisa jadwal", f"{num(8 + 12 * F(12, 18))} hari": "perbandingan dibalik",
                                     f"{num(sisa)} hari": "hanya sisa hari", "20 hari": "jadwal awal"}


def sc17():
    kmpl = F(96, 8)
    return rp(F(150) / kmpl * 10000), {rp(8 * F(96, 150) * 10000): "perbandingan dibalik", rp(8 * 10000): "biaya 96 km",
                                       rp(F(150, 10) * 10000): "dianggap 10 km/liter", rp(F(150, 8) * 10000): "150 ÷ 8"}


def sc18():
    u = F(840000, 2 + 3 + 7)
    return rp((7 - 3) * u), {rp(3 * u): "bagian Budi", rp((7 - 2) * u): "Cici − Ani", rp(7 * u): "bagian Cici", rp(10 * u): "Budi + Cici"}


def sc19():
    need = F(6 * 12, 8)
    dibalik = F(6 * 8, 12)
    return f"{num(need - 6)} orang", {f"{num(6 - dibalik)} orang": "6 − hasil dibalik", f"{num(dibalik)} orang": "perbandingan dibalik",
                                      "6 orang": "tambahan disamakan dengan jumlah awal", f"{num(need)} orang": "jumlah pekerja, bukan tambahan"}


def sc20():
    return rp(F(15 * 12000 + 10 * 14500, 25)), {rp(F(12000 + 14500, 2)): "rata-rata biasa", rp(F(15 * 14500 + 10 * 12000, 25)): "bobot tertukar",
                                                rp(12000 + 14500): "harga dijumlah", rp(15 * 12000 + 10 * 14500): "harga total"}


def sc21():
    gula = F(15, 100) * 40
    akhir = gula / F(12, 100)
    return f"{num(akhir - 40)} liter", {f"{num(F(3, 100) * 40)} liter": "3% × 40", f"{num(gula)} liter": "banyak gula",
                                        f"{num(40 - 40 * F(12, 15))} liter": "40 − 32", f"{num(akhir)} liter": "volume akhir"}


def sc22():
    x = next(x for x in [F(i, 2) for i in range(1, 400)] if 30 * F(20, 100) + x * F(50, 100) == (30 + x) * F(30, 100))
    return num(x), {num((30 * F(30, 100) - 30 * F(20, 100)) / F(50, 100)): "lupa x di volume total", "20": "selisih kadar 50 − 30",
                    "30": "volume disamakan", num(30 * F(50 - 30, 30 - 20)): "perbandingan dibalik"}


def sc23():
    pokok = F(120000 + 3 * 80000, 4)
    return rp(pokok * F(12, 10)), {rp(pokok): "lupa untung", rp(pokok + 20000): "20% dibaca Rp20.000",
                                   rp(F(120000 + 80000, 2) * F(12, 10)): "rata-rata biasa", rp(F(3 * 120000 + 80000, 4) * F(12, 10)): "perbandingan tertukar"}


def sc24():
    return rp(150000 * F(12, 10)), {rp(150000 * F(2, 10)): "hanya untung", rp(150000 * F(8, 10)): "dikurangi 20%",
                                    rp(150000 / F(12, 10)): "dibagi 1,2", rp(150000 + 20000): "20% dibaca Rp20.000"}


def sc25():
    bayar = 240000 * F(75, 100) * F(80, 100)
    return rp(bayar), {rp(240000 - bayar): "besar potongan", rp(240000 * F(55, 100)): "diskon dijumlah 45%",
                       rp(240000 * F(75, 100)): "hanya diskon pertama", rp(240000 * F(80, 100)): "hanya diskon kedua"}


def sc26():
    untung = 40 * 18000 - 600000
    return f"{num(F(untung * 100, 600000))}%", {f"{num(F(untung * 100, 40 * 18000), 1)}%": "dibagi hasil jual",
                                                f"{num(F(untung * 100, 40 * 12000))}%": "modal 40 kg",
                                                f"{num(F((18000 - 12000) * 100, 12000) - 20)}%": "50% − 20% busuk",
                                                f"{num(F((18000 - 12000) * 100, 12000))}%": "lupa yang busuk"}


def sc27():
    jual = 200000 * F(13, 10)
    return rp(jual / F(8, 10)), {rp(200000 * F(11, 10)): "30% − 20%", rp(jual): "lupa diskon",
                                 rp(200000 * F(15, 10)): "30% + 20%", rp(jual * F(12, 10)): "260.000 × 1,2"}


def sc28():
    b1, b2 = F(1200000) / F(12, 10), F(1200000) / F(8, 10)
    hasil = 2 * 1200000 - (b1 + b2)
    assert hasil < 0
    return f"rugi {rp(-hasil)}", {f"untung {rp(-hasil)}": "tanda tertukar", "tidak untung dan tidak rugi": "20% − 20% = 0",
                                  f"rugi {rp(F(4, 100) * 1200000)}": "4% dari satu sepeda", f"rugi {rp(F(4, 100) * 2400000)}": "4% dari total jual"}


def sc29():
    M, r = 2000000, F(6, 100)
    return rp(M + M * r * F(9, 12)), {rp(M * r * F(9, 12)): "hanya bunga", rp(M + M * r): "bunga setahun",
                                      rp(M + M * r * F(12, 9)): "pecahan waktu terbalik", rp(M + M * r * 9): "9 bulan = 9 tahun"}


def sc30():
    M, r = 6000000, F(12, 100)
    bunga = M * r * F(10, 12)
    return rp((M + bunga) / 10), {rp(F(M, 10)): "tanpa bunga", rp(F(M, 10) * F(101, 100)): "1% dari pokok angsuran",
                                  rp(F(M, 10) + bunga / 12): "bunga dibagi 12", rp(F(M, 10) + M * r / 10): "bunga setahun dibagi 10"}


def sc31():
    b = F(1620000 - 1500000, 1500000)
    return f"{num(b / 10 * 12 * 100, 1)}%", {f"{num(b / 10 * 100, 1)}%": "per bulan", f"{num(b * F(10, 12) * 100, 1)}%": "dikali 10/12",
                                             f"{num(b * 100)}%": "lupa disetahunkan", f"{num(F(120000, 1620000) * F(12, 10) * 100, 1)}%": "dibagi saldo akhir"}


def sc32():
    cm = 36 * 100000
    return f"1 : {num(cm / F(45, 10))}", {f"1 : {num(cm / 10 / F(45, 10))}": "kurang satu nol", f"1 : {num(cm * 10 / F(45, 10))}": "lebih satu nol",
                                          f"1 : {num(cm * F(45, 10))}": "dikali, bukan dibagi", f"1 : {num(cm * 100 / F(45, 10))}": "lebih dua nol"}


def sc33():
    luas = F(6000, 800) * F(4000, 800)
    return f"{num(luas)} cm²", {f"{num(luas / 100)} cm²": "salah konversi", f"{num(luas / 10)} cm²": "salah konversi",
                                f"{num(luas * 10)} cm²": "salah konversi", f"{num(F(6000 * 4000, 800))} cm²": "dibagi 800, bukan 800²"}


def sc34():
    asli = 6 * 250000
    return f"{num(F(asli, 400000))} cm", {"6 cm": "dianggap tetap", f"{num(F(6 * 400000, 250000))} cm": "perbandingan dibalik",
                                          f"{num(F(asli, 100000))} cm": "jarak km ditulis cm", f"{num(F(asli * 10, 400000))} cm": "salah konversi"}


def sc35():
    d = [70, 85, 60, 90, 75, 85, 80]
    s = sorted(d)
    return num(s[3]), {num(F(s[0] + s[-1], 2)): "tengah min-maks", num(F(sum(d), 7), 1): "rata-rata",
                       num(max(set(d), key=d.count)): "modus", num(d[3]): "tanpa diurutkan"}


def sc36():
    sisa = 40 * 75 - 25 * 72
    return num(F(sisa, 15)), {num(F(sisa, 40)): "dibagi 40", num(F(sisa, 25)): "dibagi 25",
                              num(F(40 * 75 - 15 * 72, 25)): "banyak siswa tertukar", num(2 * 75 - 72): "rata-rata biasa"}


def sc37():
    return f"{num(10 * 48 - 8 * F(475, 10))} kg", {f"{num(8 * F(1, 2))} kg": "hanya selisih", f"{num(F(10 * 48 - 8 * F(475, 10), 2))} kg": "rata-rata, bukan jumlah",
                                                    f"{num(2 * F(475, 10))} kg": "2 × 47,5", f"{num(2 * 48)} kg": "2 × 48"}


def median(v):
    s = sorted(v)
    n = len(s)
    return F(s[n // 2]) if n % 2 else F(s[n // 2 - 1] + s[n // 2], 2)


def modus(v):
    m = max(v.count(a) for a in v)
    if m == 1:
        return "tidak ada modus"
    ms = sorted({a for a in v if v.count(a) == m})
    return "modus " + " dan ".join(str(a) for a in ms)


def sc38():
    base = [4, 9, 6, 8, 6]
    x = 6 * 7 - sum(base)
    d = [4, 9, 6, x, 8, 6]
    x5 = 5 * 7 - sum(base)
    d5 = [4, 9, 6, x5, 8, 6]
    unsorted_med = F(d[2] + d[3], 2)
    return f"median {num(median(d))}; {modus(d)}", {
        f"median {num(median(d5))}; {modus(d5)}": "jumlah dihitung dengan 5 data",
        f"median {num(median(d))}; modus 6": "modus kedua terlewat",
        f"median {num(unsorted_med)}; {modus(d)}": "median tanpa diurutkan",
        f"median {num(median(d))}; tidak ada modus": "dua modus dianggap tidak ada"}


def sc39():
    return num(2 * 15 - 3), {num(2 * 15): "hanya dikali", num(2 * 15 + 3): "tanda salah", num(2 * (15 + 3)): "urutan dan tanda salah",
                             num(6 * (2 * 15 - 3)): "jumlah, bukan rata-rata"}


def sc40():
    return f"{45 - (28 + 20 - 8)} orang", {f"{28 + 20 - 45} orang": "irisan lupa dikurangkan", f"{45 - ((28 - 8) + (20 - 8))} orang": "yang bisa keduanya terlupa",
                                           f"{45 - 28} orang": "45 − 28", f"{45 - 20} orang": "45 − 20"}


def sc41():
    iris = 50 + 42 - (80 - 12)
    return f"{42 - iris} orang", {f"{iris} orang": "irisan", f"{50 - iris} orang": "hanya koran", f"{42 - 12} orang": "42 − 12", "42 orang": "semua penonton TV"}


def sc42():
    s = 45 + 40 + 35 - 15 - 12 - 10
    return f"{100 - (s + 5)} orang", {f"{100 - (s + 10)} orang": "irisan tiga ditambah dua kali", f"{100 - s} orang": "lupa + irisan tiga",
                                      f"{100 - (s - 5)} orang": "irisan tiga dikurangkan", f"{100 - (s - 10)} orang": "irisan tiga dikurangkan dua kali"}


def sc43():
    return f"{35 + 30 - 60} siswa", {f"{60 - 35} siswa": "60 − 35", f"{min(35, 30)} siswa": "irisan paling banyak",
                                     "35 siswa": "banyak anggota pramuka", f"{35 + 30} siswa": "dijumlahkan"}


def sc44():
    S = list(itertools.product(range(1, 7), repeat=2))
    p8 = F(sum(a + b == 8 for a, b in S), len(S))
    un = {tuple(sorted(t)) for t in S}
    return fr(p8), {fr(F(sum(a + b == 8 for a, b in un), 36)): "pasangan tak berurutan", fr(F(1, 11)): "jumlah 2-12 dianggap sama peluang",
                    fr(F(sum(a + b == 8 for a, b in un), len(un))): "ruang sampel tak berurutan", fr(F(sum(a + b == 7 for a, b in S), 36)): "peluang jumlah 7"}


def sc45():
    return fr(F(4, 10) * F(6, 9)), {fr(F(4, 10) * F(3, 9)): "merah lalu merah", fr(F(4, 10) * F(6, 10)): "dengan pengembalian",
                                    fr(F(4, 10)): "hanya pengambilan pertama", fr(2 * F(4, 10) * F(6, 9)): "urutan diabaikan"}


def sc46():
    tot = comb(10, 3)
    return fr(F(comb(4, 2) * comb(6, 1), tot)), {fr(F(comb(4, 2), tot)): "pria tidak dihitung", fr(F(4, 10) * F(3, 9) * F(6, 8)): "satu urutan saja",
                                                 fr(3 * F(4, 10) ** 2 * F(6, 10)): "dengan pengembalian",
                                                 fr(F(comb(4, 2) * comb(6, 1) + comb(4, 3), tot)): "paling sedikit 2 wanita"}


def sc47():
    a, b = F(7, 10), F(4, 10)
    return num(a * (1 - b) + (1 - a) * b), {num((1 - a) * b): "hanya Budi lulus", num((1 - a) * (1 - b)): "keduanya gagal",
                                            num(a * b): "keduanya lulus", num(a * (1 - b)): "hanya Ani lulus"}


def sc48():
    l3 = math.lcm(12, 18, 30)
    s = 6 * 60
    return hhmm(s + l3), {hhmm(s + math.gcd(12, 18, 30)): "FPB", hhmm(s + math.lcm(12, 18)): "KPK dua bus",
                          hhmm(s + 12 + 18 + 30): "selang dijumlah", hhmm(s + math.lcm(18, 30)): "KPK dua bus"}


def sc49():
    assert date(2027, 1, 1).weekday() == 4  # Jumat
    selisih = (date(2027, 8, 17) - date(2027, 1, 1)).days
    ans = HARI[date(2027, 8, 17).weekday()]
    assert ans == hari("Jumat", selisih)
    return ans, {hari("Jumat", 29 + 30 * 6 + 17): "semua bulan 30 hari", hari("Jumat", selisih + 1): "dianggap kabisat",
                 hari("Jumat", selisih - 1): "kurang satu hari", hari("Jumat", selisih - 3): "lupa hari ke-31 Maret, Mei, Juli"}


def sc50():
    return hari("Senin", 9 * 9), {hari("Senin", 9): "hari pelayanan ke-2", hari("Senin", 9 * 9 - 1): "kurang satu hari",
                                  hari("Senin", 9 * 9 + 1): "lebih satu hari", hari("Senin", 10 * 9): "10 × 9 hari"}


def check_cerita():
    print("\n=== SOAL CERITA ===")
    for n in range(1, 51):
        qid = f"tiu-numerik-soal_cerita-{n:04d}"
        q = BY_ID[qid]
        ans, dis = globals()[f"sc{n:02d}"]()
        k = key_of(q)
        texts = {o["text"] for o in q["options"]}
        ok = True
        if text(q, k) != ans:
            FAILS.append(f"{qid}: kunci {k} = '{text(q, k)}', hitungan '{ans}'")
            ok = False
        if ans in dis:
            FAILS.append(f"{qid}: pengecoh sama dengan jawaban ({ans})")
            ok = False
        if texts != {ans} | set(dis):
            FAILS.append(f"{qid}: opsi {sorted(texts)} != hitungan {sorted({ans} | set(dis))}")
            ok = False
        print(f"{'OK ' if ok else 'SALAH'} SC-{n:02d} kunci {k} = {ans} | pengecoh: " + "; ".join(f"{t} ({w})" for t, w in dis.items()))


def summary():
    from collections import Counter
    print("\n=== RINGKASAN ===")
    print("per tag:", dict(Counter(q["tag"] for q in DATA)))
    print("kunci:", dict(sorted(Counter(key_of(q) for q in DATA).items())))
    print("difficulty:", dict(sorted(Counter(q["difficulty"] for q in DATA).items())))


if __name__ == "__main__":
    check_pk()
    check_cerita()
    summary()
    if FAILS:
        print("\nGAGAL:")
        print("\n".join(FAILS))
        sys.exit(1)
    print("\nSEMUA CEK LULUS")
