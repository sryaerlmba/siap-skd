"""Cek ulang kunci jawaban tiu-numerik-a.json dengan perhitungan independen.

Berhitung: tiap soal dihitung ulang dari ekspresinya di skrip ini, lalu
dicocokkan dengan opsi. Deret: tiap deret dibangkitkan ulang dengan kode,
suku yang tampil dicocokkan dengan stem, dan suku yang ditanya (titik-titik)
dibandingkan dengan opsi. Lolos jika tiap soal punya tepat satu opsi yang
benar dan opsi itulah yang berskor 5.

Pemakaian: python check-tiu-numerik-a.py [file.json]
"""
import json
import math
import re
import sys
from collections import Counter
from fractions import Fraction as F
from itertools import islice
from pathlib import Path

PATH = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).with_name("tiu-numerik-a.json")


# ---------- alat bantu berhitung ----------

def isqrt_exact(n):
    r = math.isqrt(n)
    assert r * r == n, f"{n} bukan kuadrat sempurna"
    return r


def iroot_exact(n, k):
    r = round(abs(n) ** (1 / k))
    assert r ** k == abs(n), f"{n} bukan pangkat {k} sempurna"
    return -r if n < 0 else r


def sqrt_frac(x):
    x = F(x)
    return F(isqrt_exact(x.numerator), isqrt_exact(x.denominator))


def ratio(*parts):
    g = 0
    for p in parts:
        g = math.gcd(g, p)
    return tuple(p // g for p in parts)


def ratio_from_fracs(*fr):
    """Ubah nilai pecahan relatif menjadi perbandingan bilangan bulat terkecil."""
    m = math.lcm(*[F(x).denominator for x in fr])
    return ratio(*[int(F(x) * m) for x in fr])


def pct(s):
    return F(s) / 100


# id -> (potongan teks yang wajib ada di stem, nilai benar, mode)
# mode: eq = sama persis, float = bilangan irasional (toleransi),
#       closest = opsi yang paling dekat, ratio = perbandingan
BERHITUNG = {
    1: ("48 − 18 ÷ 3 × 2 + 5", F(48) - F(18) / 3 * 2 + 5, "eq"),
    2: ("3/4 + 5/6 − 1/3", F(3, 4) + F(5, 6) - F(1, 3), "eq"),
    3: ("0,25 × 4,8 + 1,2 ÷ 0,4", F("0.25") * F("4.8") + F("1.2") / F("0.4"), "eq"),
    4: ("15% dari 240 ditambah 40% dari 65", pct(15) * 240 + pct(40) * 65, "eq"),
    5: ("FPB dari 84 dan 126", math.gcd(84, 126), "eq"),
    6: ("(−7) × 4 − (−36) ÷ (−9)", F(-7) * 4 - F(-36) / F(-9), "eq"),
    7: ("2⁵ + 3³ − 4²", 2 ** 5 + 3 ** 3 - 4 ** 2, "eq"),
    8: ("√196 + √225 − √121", isqrt_exact(196) + isqrt_exact(225) - isqrt_exact(121), "eq"),
    9: ("125 × 48", 125 * 48, "eq"),
    10: ("86 × 11", 86 * 11, "eq"),
    11: ("36% dari 25 ditambah 25% dari 44", pct(36) * 25 + pct(25) * 44, "eq"),
    12: ("0,6 + 2/5 × 1,5 − 20%", F("0.6") + F(2, 5) * F("1.5") - pct(20), "eq"),
    13: ("a : b = 3 : 5 dan b : c = 2 : 7", ratio_from_fracs(F(3, 5), 1, F(7, 2)), "ratio"),
    14: ("98 × 97", 98 * 97, "eq"),
    15: ("65² − 35²", 65 ** 2 - 35 ** 2, "eq"),
    16: ("√80 × √45,2", math.sqrt(80) * math.sqrt(45.2), "closest"),
    17: ("27^(2/3) + 16^(3/4)", iroot_exact(27, 3) ** 2 + iroot_exact(16, 4) ** 3, "eq"),
    18: ("9⁴ × 27³ ÷ 3¹⁵", F(9 ** 4 * 27 ** 3, 3 ** 15), "eq"),
    19: ("√3.249 − ∛1.728", isqrt_exact(3249) - iroot_exact(1728, 3), "eq"),
    20: ("KPK dari 12, 18, dan 30", math.lcm(12, 18, 30), "eq"),
    21: ("0,875 × 64 − 12,5% × 96", F("0.875") * 64 - pct("12.5") * 96, "eq"),
    22: ("(−2)⁴ − 2⁴ + (−3)² × (−1)³", (-2) ** 4 - 2 ** 4 + (-3) ** 2 * (-1) ** 3, "eq"),
    23: ("0,98 × 199,6 ÷ 4,02", 0.98 * 199.6 / 4.02, "closest"),
    24: ("2 1/3 × 1 2/7 ÷ 1 1/2", F(7, 3) * F(9, 7) / F(3, 2), "eq"),
    25: ("12,5% dari 96 ditambah 87,5% dari 32", pct("12.5") * 96 + pct("87.5") * 32, "eq"),
    26: ("x : y = 4 : 7 dan x + y = 132", 132 * F(7, 11) - 132 * F(4, 11), "eq"),
    27: ("1,44 ÷ 0,12 + 0,5 × 0,8", F("1.44") / F("0.12") + F("0.5") * F("0.8"), "eq"),
    28: ("√0,0049 + √0,64", sqrt_frac(F("0.0049")) + sqrt_frac(F("0.64")), "eq"),
    29: ("75² + 25²", 75 ** 2 + 25 ** 2, "eq"),
    30: ("999 × 37", 999 * 37, "eq"),
    31: ("−5 × (−3)² − (−4)³ ÷ 8", -5 * (-3) ** 2 - F((-4) ** 3) / 8, "eq"),
    32: ("KPK dan FPB dari 36 dan 60", math.lcm(36, 60) - math.gcd(36, 60), "eq"),
    33: ("1/2 + 1/6 + 1/12 + 1/20 + 1/30 + 1/42", sum(F(1, d) for d in (2, 6, 12, 20, 30, 42)), "eq"),
    34: ("1 − 2 + 3 − 4 + 5 − 6 + ... + 99 − 100", sum(k if k % 2 else -k for k in range(1, 101)), "eq"),
    35: ("Angka satuan dari 7²⁰²⁶", pow(7, 2026, 10), "eq"),
    36: ("(2¹⁰ + 2⁹) ÷ (2⁹ − 2⁸)", F(2 ** 10 + 2 ** 9, 2 ** 9 - 2 ** 8), "eq"),
    37: ("a : b = 2 : 3, b : c = 4 : 5, dan c : d = 6 : 7", ratio_from_fracs(F(2, 3) * F(4, 5) * F(6, 7), 1), "ratio"),
    38: ("(√72 + √50 − √18) ÷ √2", (math.sqrt(72) + math.sqrt(50) - math.sqrt(18)) / math.sqrt(2), "float"),
    39: ("4.012 × 0,049 ÷ 0,0198", 4012 * 0.049 / 0.0198, "closest"),
    40: ("2026² − 2025² + 2024² − 2023²", 2026 ** 2 - 2025 ** 2 + 2024 ** 2 - 2023 ** 2, "eq"),
}


def parse_option(text):
    t = text.strip().replace("−", "-")
    if ":" in t:
        return "ratio", ratio(*[int(p) for p in t.split(":")])
    if "√" in t:
        a, b = t.split("√")
        return "num", (float(a) if a else 1.0) * math.sqrt(float(b))
    t = t.replace(".", "").replace(",", ".")
    m = re.fullmatch(r"(-?)(\d+) (\d+)/(\d+)", t)
    if m:
        v = int(m[2]) + F(int(m[3]), int(m[4]))
        return "num", -v if m[1] else v
    if "/" in t:
        n, d = t.split("/")
        return "num", F(int(n), int(d))
    return "num", F(t)


def correct_berhitung(q, n):
    frag, truth, mode = BERHITUNG[n]
    errs = []
    if frag not in q["stem"]:
        errs.append(f"stem tidak memuat '{frag}'")
    vals = [parse_option(o["text"]) for o in q["options"]]
    if mode == "ratio":
        hits = [i for i, (kind, v) in enumerate(vals) if kind == "ratio" and v == truth]
        shown = ":".join(map(str, truth))
    elif mode == "closest":
        dist = [abs(float(v) - truth) for _, v in vals]
        hits = [i for i, d in enumerate(dist) if d == min(dist)]
        shown = f"~{truth:.4f}"
    elif mode == "float":
        hits = [i for i, (_, v) in enumerate(vals) if abs(float(v) - truth) < 1e-9]
        shown = f"{truth:.6f}"
    else:
        hits = [i for i, (_, v) in enumerate(vals) if v == truth]
        shown = str(truth)
    return hits, shown, errs


# ---------- alat bantu deret ----------

def num(n):
    s = f"{abs(n):,}".replace(",", ".")
    return ("−" if n < 0 else "") + s


def huruf(p):
    """Posisi 1..26, memutar ke A lagi setelah Z."""
    return chr(ord("A") + (p - 1) % 26)


def primes():
    n = 2
    while True:
        if all(n % d for d in range(2, math.isqrt(n) + 1)):
            yield n
        n += 1


def take(gen, k):
    return list(islice(gen, k))


def rec(seed, f, k):
    s = list(seed)
    while len(s) < k:
        s.append(f(s))
    return s


def interleave(a, b):
    out = []
    for x, y in zip(a, b):
        out += [x, y]
    return out


def nums(xs):
    return [num(x) for x in xs]


DERET = {
    1: nums(7 + 6 * i for i in range(8)),
    2: nums(94 - 7 * i for i in range(8)),
    3: nums(3 * 2 ** i for i in range(8)),
    4: nums(1458 // 3 ** i for i in range(6)),
    5: nums((i + 2) ** 2 for i in range(8)),
    6: nums(3 + i * (i + 1) // 2 for i in range(8)),
    7: nums(rec([3, 4], lambda s: s[-1] + s[-2], 8)),
    8: [huruf(2 * i) for i in range(1, 8)],
    9: nums(8 + 9 * i for i in range(6)),
    10: nums(interleave(range(1, 6), range(10, 60, 10))),
    11: nums(take(primes(), 8)),
    12: [huruf(4 + 3 * i) for i in range(6)],
    13: nums(rec([10], lambda s: s[-1] + 3 if len(s) % 2 else s[-1] - 1, 8)),
    14: nums(5 + i * (i + 1) for i in range(8)),
    15: nums(rec([2], lambda s: s[-1] * 2 if len(s) % 2 else s[-1] + 3, 8)),
    16: nums(interleave([3 + 3 * i for i in range(5)], [25 - 3 * i for i in range(5)])),
    17: nums(n * n + 1 for n in range(1, 9)),
    18: nums(n ** 3 + 1 for n in range(1, 8)),
    19: nums(2 ** n - 1 for n in range(1, 9)),
    20: nums(3 * p for p in take(primes(), 8)),
    21: nums(rec([1, 1, 2], lambda s: s[-1] + s[-2] + s[-3], 9)),
    22: nums(3 * (-2) ** i for i in range(7)),
    23: [huruf(2 + i * (i + 1) // 2) for i in range(7)],
    24: interleave([huruf(1 + 2 * i) for i in range(5)], [huruf(26 - 2 * i) for i in range(5)]),
    25: [huruf(2 + i * (i + 3) // 2) + str((i + 2) * (i + 3) // 2) for i in range(6)],
    26: nums(4 + i * (i + 1) for i in range(6)),
    27: nums(rec([2], lambda s: s[-1] * len(s), 7)),
    28: nums(rec([2], lambda s: 3 * s[-1] - 1, 7)),
    29: nums(interleave([5 + 3 * i for i in range(4)], [30 - 4 * i for i in range(4)])),
    30: nums(rec([1, 3], lambda s: s[-1] + s[-2], 9)),
    31: [huruf(26 - i * (i + 1) // 2) for i in range(7)],
    32: nums(p * p for p in take(primes(), 7)),
    33: nums(2 + 2 ** i for i in range(8)),
    34: [huruf(20 + 4 * i) for i in range(7)],
    35: nums(rec([5], lambda s: s[-1] * 2 if len(s) % 2 else s[-1] - 3, 9)),
    36: nums(2 ** n + n for n in range(1, 8)),
    37: nums(n ** 3 + n for n in range(1, 8)),
    38: nums(rec([1, 2], lambda s: s[-1] * s[-2], 8)),
    39: nums(interleave([2 * 2 ** i for i in range(5)], [1 + 3 * i for i in range(5)])),
    40: nums(rec([3, 4], lambda s: s[-1] + s[-2] - 1, 8)),
    41: nums(p * (i + 1) for i, p in enumerate(take(primes(), 8))),
    42: [huruf(n * n) for n in range(1, 8)],
    43: [num(math.factorial(i + 1)) + huruf(26 - i * (i + 3) // 2) for i in range(6)],
    44: nums(n ** n for n in range(1, 7)),
    45: nums(1 + sum(j * j for j in range(i)) for i in range(1, 8)),
}


def norm(s):
    return re.sub(r"\s+", " ", s.strip())


def correct_deret(q, n):
    seq = DERET[n]
    errs = []
    tokens = [t.strip() for t in q["stem"].split("\n")[-1].split(",")]
    if len(tokens) > len(seq):
        return [], "", [f"stem lebih panjang ({len(tokens)}) dari deret yang dibangkitkan ({len(seq)})"]
    asked = []
    for i, t in enumerate(tokens):
        if t == "...":
            asked.append(seq[i])
        elif t != seq[i]:
            errs.append(f"suku ke-{i + 1} di stem '{t}' seharusnya '{seq[i]}'")
    if not asked:
        errs.append("stem tidak punya titik-titik")
    expected = ", ".join(asked)
    hits = [i for i, o in enumerate(q["options"]) if norm(o["text"]) == expected]
    return hits, expected, errs


def main():
    data = json.loads(PATH.read_text(encoding="utf-8"))
    problems = []
    tags = Counter()
    keypos = Counter()
    for q in data:
        qid = q["id"]
        tag = q["tag"]
        n = int(qid.rsplit("-", 1)[1])
        tags[tag] += 1
        if tag == "berhitung":
            hits, shown, errs = correct_berhitung(q, n)
        elif tag == "deret_angka":
            hits, shown, errs = correct_deret(q, n)
        else:
            problems.append(f"{qid}: tag tidak dikenal oleh skrip ini")
            continue
        five = [i for i, o in enumerate(q["options"]) if o["score"] == 5]
        if len(hits) != 1:
            errs.append(f"jumlah opsi benar = {len(hits)} (nilai benar {shown})")
        elif five != hits:
            errs.append(f"kunci skor 5 di {[q['options'][i]['key'] for i in five]}, padahal yang benar {q['options'][hits[0]]['key']} (nilai {shown})")
        for i in five:
            keypos[q["options"][i]["key"]] += 1
        for e in errs:
            problems.append(f"{qid}: {e}")
    missing_b = set(BERHITUNG) - {int(q["id"].rsplit("-", 1)[1]) for q in data if q["tag"] == "berhitung"}
    missing_d = set(DERET) - {int(q["id"].rsplit("-", 1)[1]) for q in data if q["tag"] == "deret_angka"}
    if missing_b or missing_d:
        problems.append(f"soal tidak ada di JSON: berhitung {sorted(missing_b)}, deret {sorted(missing_d)}")
    print(f"{PATH.name}: {len(data)} soal, per tag {dict(tags)}, kunci A-E {dict(sorted(keypos.items()))}")
    if problems:
        print("\n".join(problems))
        print(f"\nGAGAL: {len(problems)} masalah")
        sys.exit(1)
    print("OK: semua kunci dihitung ulang dan cocok, tepat satu opsi benar per soal")


if __name__ == "__main__":
    main()
