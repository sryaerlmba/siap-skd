"""Cek ulang kunci jawaban tiu-verbal.json secara formal.

Silogisme: premis dan tiap opsi ditulis ulang sebagai rumus di skrip ini,
lalu kesimpulan diuji pada semua model himpunan kecil (diagram Venn dengan
2-4 term, ditambah individu bernama) dan semua baris tabel kebenaran.
Kesimpulan sah = benar di setiap model yang memenuhi premis. Term yang
disebut dianggap tidak kosong (anggapan logika tradisional); itu membuat
pengecoh lebih sulit lolos. Kunci juga dicek tetap sah tanpa anggapan itu.
Opsi "Tidak dapat ditarik kesimpulan" benar jika tidak ada opsi lain yang sah
dan hubungan antar-term yang ditanya tidak dapat dipastikan; opsi "Semua
kesimpulan di atas salah" benar jika tidak ada opsi lain yang sah.

Analitis: semua permutasi/pembagian/pilihan dienumerasi, disaring dengan
syarat soal. Soal "pasti benar" lolos jika tepat satu opsi benar di semua
susunan sah; soal "pasti salah" lolos jika tepat satu opsi salah di semua
susunan sah dan opsi lain benar di paling sedikit satu susunan.

Analogi: hanya dicek jumlah jenis relasi (minimal 15) karena maknanya tidak
bisa diuji mesin.

Lolos jika opsi yang terbukti benar adalah opsi berskor 5 di JSON.
Pemakaian: python check-tiu-verbal.py [file.json]
"""
import json
import sys
from itertools import combinations, permutations, product
from pathlib import Path

PATH = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).with_name("tiu-verbal.json")
DATA = {q["id"]: q for q in json.loads(PATH.read_text(encoding="utf-8"))}


# ===================== silogisme: mesin logika =====================

def semua(a, b): return ("semua", a, b)
def sebagian(a, b): return ("sebagian", a, b)
def tidak_ada(a, b): return ("tidak_ada", a, b)
def sebagian_bukan(a, b): return ("sebagian_bukan", a, b)
def adalah(x, a): return ("is", x, a)
def NOT(s): return ("not", s)
def AND(*s): return ("and",) + s
def OR(*s): return ("or",) + s
def IMP(a, b): return ("imp", a, b)
def IFF(a, b): return ("iff", a, b)


TDK, SKS = "TDK", "SKS"


def ujung(a, b):
    """Semua bentuk kategoris antara term a dan b (negasinya ikut diuji)."""
    return [semua(a, b), sebagian(a, b), semua(b, a)]


def has(region, term):
    return term[1:] not in region if term.startswith("~") else term in region


def ev(s, m):
    inh, ind, atom = m
    if isinstance(s, str):
        return atom[s]
    op = s[0]
    if op == "semua":
        return all(has(r, s[2]) for r in inh if has(r, s[1]))
    if op == "sebagian":
        return any(has(r, s[1]) and has(r, s[2]) for r in inh)
    if op == "tidak_ada":
        return not any(has(r, s[1]) and has(r, s[2]) for r in inh)
    if op == "sebagian_bukan":
        return any(has(r, s[1]) and not has(r, s[2]) for r in inh)
    if op == "is":
        return has(ind[s[1]], s[2])
    if op == "not":
        return not ev(s[1], m)
    if op == "and":
        return all(ev(x, m) for x in s[1:])
    if op == "or":
        return any(ev(x, m) for x in s[1:])
    if op == "imp":
        return (not ev(s[1], m)) or ev(s[2], m)
    if op == "iff":
        return ev(s[1], m) == ev(s[2], m)
    raise ValueError(op)


def kumpulkan(s, terms, inds, atoms):
    if isinstance(s, str):
        atoms.add(s)
        return
    op = s[0]
    if op in ("semua", "sebagian", "tidak_ada", "sebagian_bukan"):
        terms.update(t.lstrip("~") for t in s[1:])
    elif op == "is":
        inds.add(s[1])
        terms.add(s[2].lstrip("~"))
    else:
        for x in s[1:]:
            kumpulkan(x, terms, inds, atoms)


def model_premis(prem, extra, impor):
    terms, inds, atoms = set(), set(), set()
    for s in list(prem) + list(extra):
        kumpulkan(s, terms, inds, atoms)
    terms, inds, atoms = sorted(terms), sorted(inds), sorted(atoms)
    regions = [frozenset(t for t, b in zip(terms, bits) if b) for bits in product((0, 1), repeat=len(terms))]
    hasil = []
    for mask in range(1, 1 << len(regions)):
        inh = [r for i, r in enumerate(regions) if mask >> i & 1]
        if impor and not all(any(t in r for r in inh) for t in terms):
            continue
        for ia in product(inh, repeat=len(inds)):
            for aa in product((False, True), repeat=len(atoms)):
                m = (inh, dict(zip(inds, ia)), dict(zip(atoms, aa)))
                if all(ev(p, m) for p in prem):
                    hasil.append(m)
    return hasil


def sah(models, s):
    return all(ev(s, m) for m in models)


# Tiap opsi: (potongan teks yang wajib ada di opsi JSON, rumus).
# Kunci huruf tidak ditulis di sini; skrip mencari sendiri opsi yang benar.
SIL = {
    1: dict(prem=[semua("L", "S"), semua("S", "U")], buka=ujung("L", "U"), opsi=[
        ("dokumen asli lulus SKD", semua("U", "L")),
        ("lulus SKD wajib", semua("L", "U")),
        ("tidak mengikuti SKB", sebagian_bukan("L", "S")),
        ("tidak lulus SKD", semua("~L", "~U")),
        ("Tidak dapat", TDK)]),
    2: dict(prem=[sebagian("R", "M"), semua("M", "P")], buka=ujung("R", "P"), opsi=[
        ("Semua relawan", semua("R", "P")),
        ("belum mendapat", sebagian_bukan("R", "P")),
        ("posyandu telah mendapat", sebagian("R", "P")),
        ("adalah mahasiswa", semua("P", "M")),
        ("Tidak dapat", TDK)]),
    3: dict(prem=[tidak_ada("K", "U"), semua("M", "K")], buka=ujung("M", "U"), opsi=[
        ("Tidak ada mobil", tidak_ada("M", "U")),
        ("Sebagian mobil", sebagian("M", "U")),
        ("mudik berpelat merah", semua("~U", "M")),
        ("bukan mobil", sebagian_bukan("K", "M")),
        ("Tidak dapat", TDK)]),
    4: dict(prem=[tidak_ada("T", "I"), sebagian("B", "T")], buka=ujung("B", "I"), opsi=[
        ("Semua peserta dari Kota Bogor", semua("B", "~I")),
        ("Bogor tidak diizinkan", sebagian_bukan("B", "I")),
        ("Bogor diizinkan", sebagian("B", "I")),
        ("terlambat berasal", semua("T", "B")),
        ("Tidak dapat", TDK)]),
    5: dict(prem=[semua("C", "B"), semua("P", "B")], buka=ujung("C", "P"), opsi=[
        ("Semua peserta", semua("C", "P")),
        ("Sebagian peserta", sebagian("C", "P")),
        ("Tidak ada peserta", tidak_ada("C", "P")),
        ("bukan peserta", sebagian_bukan("P", "C")),
        ("Tidak dapat", TDK)]),
    6: dict(prem=[semua("S", "K"), NOT(adalah("hendra", "K"))], buka=[adalah("hendra", "S")], opsi=[
        ("tidak menerima", NOT(adalah("hendra", "S"))),
        ("penghargaan lain", "penghargaan_lain"),
        ("Hendra menerima penghargaan Satyalancana", adalah("hendra", "S")),
        ("Sebagian penerima", sebagian_bukan("S", "K")),
        ("Tidak dapat", TDK)]),
    7: dict(prem=[sebagian("G", "S"), sebagian("S", "L")], buka=ujung("G", "L"), opsi=[
        ("Sebagian guru", sebagian("G", "L")),
        ("Semua guru", semua("G", "L")),
        ("Tidak ada guru", tidak_ada("G", "L")),
        ("Semua peserta seleksi", semua("S", "G")),
        ("Tidak dapat", TDK)]),
    8: dict(prem=[semua("O", "C"), sebagian("W", "O")], buka=ujung("W", "C"), opsi=[
        ("Semua warga", semua("W", "C")),
        ("Semua pemilik", semua("C", "O")),
        ("tidak memiliki", sebagian_bukan("W", "C")),
        ("bukan warga", sebagian_bukan("O", "W")),
        ("Semua kesimpulan", SKS)]),
    9: dict(prem=[semua("O", "S"), sebagian_bukan("P", "S")], buka=ujung("P", "O"), opsi=[
        ("adalah operator", sebagian("P", "O")),
        ("Tidak ada staf", tidak_ada("P", "O")),
        ("pemetaan bukan operator", sebagian_bukan("P", "O")),
        ("pemilik sertifikat", sebagian_bukan("S", "O")),
        ("Tidak dapat", TDK)]),
    10: dict(prem=[semua("W", "P"), tidak_ada("D", "P")], buka=ujung("D", "W"), opsi=[
        ("menjadi pemenang", sebagian("D", "W")),
        ("Semua yang presentasi", semua("P", "W")),
        ("Sebagian pemenang", sebagian("W", "D")),
        ("memenangi", tidak_ada("D", "W")),
        ("Tidak dapat", TDK)]),
    11: dict(prem=[semua("L", "T"), adalah("dimas", "T")], buka=[adalah("dimas", "L")], opsi=[
        ("Dimas lolos", adalah("dimas", "L")),
        ("tidak lolos", NOT(adalah("dimas", "L"))),
        ("Semua yang menjalani", semua("T", "L")),
        ("dan lolos", AND(adalah("dimas", "T"), adalah("dimas", "L"))),
        ("Tidak dapat", TDK)]),
    12: dict(prem=[tidak_ada("M", "B"), tidak_ada("S", "M")], buka=ujung("S", "B"), opsi=[
        ("Semua sepeda", semua("S", "B")),
        ("Sebagian sepeda", sebagian("S", "B")),
        ("Tidak ada sepeda", tidak_ada("S", "B")),
        ("adalah sepeda listrik", semua("B", "S")),
        ("Tidak dapat", TDK)]),
    13: dict(prem=[semua("G", "M"), semua("M", "S"), tidak_ada("S", "X")], buka=ujung("G", "X"), opsi=[
        ("Sebagian peserta magang", sebagian("G", "X")),
        ("Semua yang sedang", semua("S", "G")),
        ("Sebagian peserta pertukaran", sebagian("X", "M")),
        ("Tidak ada peserta magang", tidak_ada("G", "X")),
        ("Tidak dapat", TDK)]),
    14: dict(prem=[sebagian("V", "S"), tidak_ada("S", "K")], buka=ujung("V", "K"), opsi=[
        ("Tidak ada pengunjung", tidak_ada("V", "K")),
        ("daerah memiliki", sebagian("V", "K")),
        ("daerah tidak memiliki", sebagian_bukan("V", "K")),
        ("Semua pemilik", tidak_ada("K", "V")),
        ("Tidak dapat", TDK)]),
    15: dict(prem=[semua("L", "P"), tidak_ada("K", "L")], buka=ujung("K", "P"), opsi=[
        ("Tidak ada pegawai", tidak_ada("K", "P")),
        ("Semua pegawai Bagian", semua("K", "P")),
        ("Sebagian pegawai", sebagian("K", "P")),
        ("Semua peserta pelatihan", semua("P", "L")),
        ("Semua kesimpulan", SKS)]),
    16: dict(prem=[tidak_ada("B", "W"), sebagian("B", "K")], buka=ujung("K", "W"), opsi=[
        ("Tidak ada anggota", tidak_ada("K", "W")),
        ("taruna tidak bekerja", sebagian_bukan("K", "W")),
        ("taruna bekerja", sebagian("K", "W")),
        ("adalah penerima", semua("K", "B")),
        ("Tidak dapat", TDK)]),
    17: dict(prem=[semua("J", "G"), sebagian("J", "D")], buka=ujung("D", "G"), opsi=[
        ("Semua dosen", semua("D", "G")),
        ("Sebagian dosen", sebagian("D", "G")),
        ("menjadi juri", semua("G", "J")),
        ("bukan dosen", sebagian_bukan("G", "D")),
        ("Tidak dapat", TDK)]),
    18: dict(prem=[IMP("h", "l"), "h"], buka=["l"], opsi=[
        ("tetap masuk", NOT("l")),
        ("normal", NOT("h")),
        ("banjir", "banjir"),
        ("diliburkan", "l"),
        ("Tidak dapat", TDK)]),
    19: dict(prem=[IMP("p", "q"), NOT("q")], buka=["p"], opsi=[
        ("tidak sedang", NOT("p")),
        ("server sedang", "p"),
        ("setengah hari", "setengah_hari"),
        ("Besok", "besok_perawatan"),
        ("Tidak dapat", TDK)]),
    20: dict(prem=[IMP("p", "q"), IMP("q", "r")], buka=["p", "r"], opsi=[
        ("disetujui, layanan", IMP("p", "r")),
        ("sementara, anggaran", IMP("r", "p")),
        ("tidak disetujui", IMP(NOT("p"), NOT("r"))),
        ("dan renovasi", AND("p", "q")),
        ("Tidak dapat", TDK)]),
    21: dict(prem=[OR("a", "d"), NOT("a")], buka=["d"], opsi=[
        ("dibatalkan", "batal"),
        ("secara daring", "d"),
        ("aula dan daring", AND("a", "d")),
        ("ditunda", "tunda"),
        ("Tidak dapat", TDK)]),
    22: dict(prem=[IMP("p", "q"), IMP("q", "r"), NOT("r")], buka=["p"], opsi=[
        ("mengikuti SKD, tetapi", AND("q", NOT("p"))),
        ("administrasi, tetapi", AND("p", NOT("q"))),
        ("tidak lulus", NOT("p")),
        ("kota selain", AND("q", "kota_lain")),
        ("Tidak dapat", TDK)]),
    23: dict(prem=[IMP("p", "q"), IMP("q", "r")], buka=["p", "r"], opsi=[
        ("dipegang sekretaris, dokumen", IMP("r", "p")),
        ("tidak berstatus rahasia, kuncinya", IMP(NOT("p"), NOT("r"))),
        ("lemari berkunci, dokumen", IMP("q", "p")),
        ("tidak dipegang", IMP(NOT("r"), NOT("p"))),
        ("Tidak dapat", TDK)]),
    24: dict(prem=[IMP("p", "q"), "q"], buka=["p"], opsi=[
        ("listrik PLN padam", "p"),
        ("PLN tidak padam", NOT("p")),
        ("genset tidak menyala", IMP(NOT("p"), NOT("q"))),
        ("uji coba", "uji_coba"),
        ("Tidak dapat", TDK)]),
    25: dict(prem=[IMP("p", "q"), NOT("p")], buka=["q"], opsi=[
        ("tidak memperoleh", NOT("q")),
        ("Darto memperoleh bonus akhir", "q"),
        ("buruk", "buruk"),
        ("kegiatan lain", "bonus_lain"),
        ("Tidak dapat", TDK)]),
    26: dict(prem=[OR("s", "a"), IMP("a", "t"), NOT("t")], buka=["s"], opsi=[
        ("melalui aplikasi SRIKANDI", "a"),
        ("surel dan aplikasi", AND("s", "a")),
        ("Lestari dikirim melalui surel", "s"),
        ("besok", "ttd_besok"),
        ("Tidak dapat", TDK)]),
    27: dict(prem=[NOT(AND("a", "b")), "a"], buka=["b"], opsi=[
        ("Bima tidak", NOT("b")),
        ("Bima bertugas jaga malam", "b"),
        ("sama-sama tidak", AND(NOT("a"), NOT("b"))),
        ("jaga siang", "jaga_siang"),
        ("Tidak dapat", TDK)]),
    28: dict(prem=[OR("g", "b"), IMP("g", "v"), IMP("b", "v")], buka=["v"], opsi=[
        ("Gedung Serbaguna", "g"),
        ("Balai Desa", "b"),
        ("tidak menyediakan", NOT("v")),
        ("Sukajadi menyediakan", "v"),
        ("Tidak dapat", TDK)]),
    29: dict(prem=[IMP("p", "q"), IMP("q", "r")], buka=["p", "r"], opsi=[
        ("Mekarsari mendapat dana hibah atau", OR("p", NOT("r"))),
        ("tidak mendapat dana hibah atau", OR(NOT("p"), "r")),
        ("hibah dan", AND("p", "r")),
        ("Jika laporan", IMP("r", "p")),
        ("Tidak dapat", TDK)]),
    30: dict(prem=[IFF("p", "j"), NOT("p")], buka=["j"], opsi=[
        ("Sabtu ini ada kegiatan", "j"),
        ("Minggu", "buka_minggu"),
        ("tidak ada kegiatan", NOT("j")),
        ("Jumat", "pindah_jumat"),
        ("Tidak dapat", TDK)]),
    31: dict(prem=[semua("T", "P"), IMP(adalah("sari", "P"), "k"), NOT("k")], buka=[adalah("sari", "T")], opsi=[
        ("tidak terlambat", NOT(adalah("sari", "T"))),
        ("terlambat, tetapi", AND(adalah("sari", "T"), NOT(adalah("sari", "P")))),
        ("Tunjangan kinerja Sari dipotong", adalah("sari", "P")),
        ("Semua pegawai", semua("P", "T")),
        ("Tidak dapat", TDK)]),
    32: dict(prem=[IMP(NOT("k"), "t"), NOT("t")], buka=["k"], opsi=[
        ("tidak terpenuhi", NOT("k")),
        ("dibatalkan", "batal"),
        ("tidak hadir", "ada_yang_absen"),
        ("hari ini terpenuhi", "k"),
        ("Tidak dapat", TDK)]),
    33: dict(prem=[NOT(IMP("m", "r")), IMP("m", "s")], buka=["s", "r"], opsi=[
        ("tidak dimutasi", AND(NOT("m"), NOT("r"))),
        ("SK baru, tetapi", AND("s", NOT("r"))),
        ("dimutasi lalu", AND("m", "r")),
        ("tidak menerima", NOT("s")),
        ("Tidak dapat", TDK)]),
    34: dict(prem=[NOT(OR("g", "f")), IMP(NOT("f"), "b")], buka=["b"], opsi=[
        ("Galih ditunjuk", "g"),
        ("Fajar ditunjuk", "f"),
        ("Fajar menjadi", "b"),
        ("Galih menjadi", "galih_bendahara"),
        ("Tidak dapat", TDK)]),
    35: dict(prem=[IMP("p", "q"), NOT("q")], buka=["p"], opsi=[
        ("tidak kurang", NOT("p")),
        ("gudang kurang", "p"),
        ("tepat 100", "tepat_100"),
        ("Dinas provinsi", "kirim_tambahan"),
        ("Tidak dapat", TDK)]),
}


def cek_silogisme(no, spec):
    extra = [s for _, s in spec["opsi"] if s not in (TDK, SKS)] + spec["buka"]
    mods = model_premis(spec["prem"], extra, impor=True)
    mods_bebas = model_premis(spec["prem"], extra, impor=False)
    assert mods, "premis tidak konsisten"
    benar = []
    sah_biasa = []
    for huruf, (_, s) in zip("ABCDE", spec["opsi"]):
        if s in (TDK, SKS):
            continue
        if sah(mods, s):
            sah_biasa.append(huruf)
            if not sah(mods_bebas, s):
                return None, f"opsi {huruf} hanya sah dengan anggapan term tidak kosong"
    for huruf, (_, s) in zip("ABCDE", spec["opsi"]):
        if s == SKS:
            ok = not sah_biasa
        elif s == TDK:
            ok = not sah_biasa and all(not sah(mods, b) and not sah(mods, NOT(b)) for b in spec["buka"])
        else:
            ok = huruf in sah_biasa
        if ok:
            benar.append(huruf)
    return benar, f"{len(mods)} model"


# ===================== analitis: enumerasi =====================

def urutan(names, slots=None):
    slots = list(slots) if slots is not None else list(range(1, len(names) + 1))
    return [dict(zip(names, p)) for p in permutations(slots, len(names))]


def kelompok(names, groups, sizes):
    hasil = []
    for p in product(groups, repeat=len(names)):
        if all(p.count(g) == n for g, n in sizes.items()):
            hasil.append(dict(zip(names, p)))
    return hasil


def pilih(names, k):
    return [{n: (n in c) for n in names} for c in combinations(names, k)]


def hadap(a, b, n=6):
    return (a - b) % n == n // 2


def sebelah(a, b, n=6):
    return (a - b) % n in (1, n - 1)


# Meja bundar: kursi 0..n-1 bernomor searah jarum jam (dilihat dari atas) dan
# semua orang menghadap ke tengah. Tangan kanan seseorang mengarah ke kursi
# bernomor satu lebih kecil (berlawanan arah jarum jam), tangan kiri ke kursi
# bernomor satu lebih besar.
def kanan_dari(a, b, n=6):
    return a == (b - 1) % n


def kiri_dari(a, b, n=6):
    return a == (b + 1) % n


KOTA = ["Medan", "Padang", "Palembang", "Pontianak"]
BID = ["Keuangan", "Umum", "Perencanaan", "Kepegawaian"]
ORG29 = ["Nanda", "Okta", "Puspa", "Rizal"]


def dom29():
    hasil = []
    for k in permutations(KOTA):
        for b in permutations(BID):
            hasil.append({**{n + "_k": x for n, x in zip(ORG29, k)}, **{n + "_b": x for n, x in zip(ORG29, b)}})
    return hasil


def bidang_asal(s, kota):
    return next(s[n + "_b"] for n in ORG29 if s[n + "_k"] == kota)


def posisi(nama, nilai):
    return (nama, lambda s: s[nama] == nilai)


BENAR, SALAH = "pasti benar", "pasti salah"

# Tiap soal: domain, syarat, mode, opsi [(potongan teks opsi, fungsi)].
ANA = {
    1: (urutan("Ayu Bayu Cici Dedi Euis".split()), [
        lambda s: s["Euis"] == 2, lambda s: s["Cici"] == 5,
        lambda s: s["Bayu"] == s["Ayu"] + 1, lambda s: s["Dedi"] < s["Ayu"]],
        BENAR, [posisi(n, 3) for n in "Ayu Bayu Cici Dedi Euis".split()]),
    2: (urutan("Fikri Gilang Hana Indah Joko".split()), [
        lambda s: s["Indah"] == 2, lambda s: s["Joko"] == s["Hana"] + 1, lambda s: s["Gilang"] > s["Joko"]],
        BENAR, [("Hana meraih peringkat 4", lambda s: s["Hana"] == 4),
                ("Gilang", lambda s: s["Gilang"] == 4),
                ("Fikri", lambda s: s["Fikri"] == 1),
                ("Joko", lambda s: s["Joko"] == 5),
                ("Hana meraih peringkat 1", lambda s: s["Hana"] == 1)]),
    3: (urutan("Perencanaan Keuangan Umum Kepegawaian Hukum Humas".split()), [
        lambda s: s["Humas"] == 6, lambda s: s["Perencanaan"] == 2,
        lambda s: s["Keuangan"] < s["Umum"] and s["Keuangan"] < s["Hukum"],
        lambda s: s["Kepegawaian"] == s["Hukum"] + 1],
        BENAR, [("Umum tampil ketiga", lambda s: s["Umum"] == 3),
                ("Hukum tampil keempat", lambda s: s["Hukum"] == 4),
                ("Keuangan", lambda s: s["Keuangan"] == 1),
                ("Kepegawaian", lambda s: s["Kepegawaian"] == 5),
                ("sebelum Hukum", lambda s: s["Umum"] < s["Hukum"])]),
    4: (urutan("Adi Bela Candra Dina Eko Fitri".split(), range(6)), [
        lambda s: hadap(s["Adi"], s["Dina"]), lambda s: kanan_dari(s["Bela"], s["Adi"]),
        lambda s: not sebelah(s["Candra"], s["Dina"]), lambda s: not sebelah(s["Eko"], s["Bela"])],
        BENAR, [(n, lambda s, n=n: hadap(s[n], s["Candra"])) for n in "Adi Bela Dina Eko Fitri".split()]),
    5: (urutan("Dukcapil Perizinan Pajak Pendidikan Kesehatan".split()), [
        lambda s: s["Dukcapil"] == 1, lambda s: s["Pajak"] == s["Perizinan"] + 1,
        lambda s: s["Perizinan"] != 2, lambda s: s["Kesehatan"] != 5,
        lambda s: s["Pendidikan"] > s["Kesehatan"]],
        BENAR, [("Pendidikan", lambda s: s["Pendidikan"] == 5),
                ("Pajak Daerah berada di lantai 4", lambda s: s["Pajak"] == 4),
                ("Perizinan", lambda s: s["Perizinan"] == 3),
                ("Kesehatan", lambda s: s["Kesehatan"] == 2),
                ("Pajak Daerah berada di lantai 3", lambda s: s["Pajak"] == 3)]),
    6: (urutan("Rudi Sinta Tono Umi".split()), [
        lambda s: s["Umi"] == s["Tono"] + 1, lambda s: s["Rudi"] != 1, lambda s: s["Sinta"] > s["Umi"]],
        BENAR, [("Rudi piket pada hari Rabu", lambda s: s["Rudi"] == 3),
                ("Sinta", lambda s: s["Sinta"] == 4),
                ("Umi", lambda s: s["Umi"] == 3),
                ("Tono", lambda s: s["Tono"] == 1),
                ("Rudi piket sebelum Sinta", lambda s: s["Rudi"] < s["Sinta"])]),
    7: (urutan("Ratna Sigit Tari Umar Vina".split()), [
        lambda s: s["Sigit"] == 3, lambda s: s["Vina"] == s["Sigit"] - 1, lambda s: s["Tari"] in (1, 5),
        lambda s: abs(s["Ratna"] - s["Umar"]) != 1],
        BENAR, [("Ratna", lambda s: s["Ratna"] == 1),
                ("Umar duduk di kursi 4", lambda s: s["Umar"] == 4),
                ("Tari duduk di kursi 5", lambda s: s["Tari"] == 5),
                ("Tari duduk di kursi 1", lambda s: s["Tari"] == 1),
                ("Vina", lambda s: abs(s["Umar"] - s["Vina"]) == 1)]),
    8: (urutan("Arif Bintang Citra Danu Erna Fani".split()), [
        lambda s: s["Erna"] < s["Danu"] < s["Arif"], lambda s: s["Arif"] < s["Bintang"],
        lambda s: s["Citra"] == s["Bintang"] + 1, lambda s: s["Fani"] not in (1, 6)],
        SALAH, [("Fani tiba kedua", lambda s: s["Fani"] == 2),
                ("Danu", lambda s: s["Danu"] == 3),
                ("Arif", lambda s: s["Arif"] == 3),
                ("Fani tiba keempat", lambda s: s["Fani"] == 4),
                ("Fani tiba kelima", lambda s: s["Fani"] == 5)]),
    9: (kelompok("Lukman Maya Nina Oscar Putri Qori".split(), "VL", {"V": 3, "L": 3}), [
        lambda s: s["Lukman"] != s["Maya"], lambda s: s["Nina"] == s["Oscar"], lambda s: s["Putri"] != s["Nina"],
        lambda s: s["Lukman"] != "V" or s["Qori"] == "L"],
        BENAR, [("Lukman", lambda s: s["Lukman"] == s["Nina"]),
                ("Maya", lambda s: s["Maya"] == "V"),
                ("Qori masuk", lambda s: s["Qori"] == "L"),
                ("Putri", lambda s: s["Putri"] == s["Qori"]),
                ("Oscar", lambda s: s["Oscar"] == "V")]),
    10: (urutan("Sukamaju Sukasari Cibodas Margahayu Mekarjaya".split()), [
        lambda s: s["Margahayu"] == 3, lambda s: s["Mekarjaya"] == s["Cibodas"] + 1,
        lambda s: s["Sukamaju"] < s["Cibodas"], lambda s: s["Sukasari"] != 1],
        BENAR, [posisi(n, 2) for n in "Sukamaju Sukasari Cibodas Margahayu Mekarjaya".split()]),
    11: (urutan("Gita Heru Irma Joni Kartika Lutfi".split(), range(6)), [
        lambda s: hadap(s["Gita"], s["Joni"]),
        lambda s: sebelah(s["Heru"], s["Gita"]) and sebelah(s["Heru"], s["Irma"]),
        lambda s: not sebelah(s["Kartika"], s["Joni"])],
        BENAR, [(n, lambda s, n=n: hadap(s[n], s["Lutfi"])) for n in "Gita Heru Irma Joni Kartika".split()]),
    12: (kelompok("Fajar Gina Hadi Indra Jaka Kevin".split(), (1, 2, 3), {1: 2, 2: 2, 3: 2}), [
        lambda s: s["Jaka"] == 3, lambda s: s["Fajar"] == s["Gina"], lambda s: s["Indra"] == s["Hadi"] + 1,
        lambda s: s["Kevin"] != 1],
        BENAR, [(n, lambda s, n=n: s[n] == s["Jaka"]) for n in "Fajar Gina Hadi Indra Kevin".split()]),
    13: (pilih("Lina Mega Nisa Oki Panji".split(), 1), [
        # pernyataan: Lina "bukan saya", Mega "Oki pelakunya", Nisa "bukan Panji",
        # Oki "Mega berbohong", Panji "Lina pelakunya"; tepat dua yang benar
        lambda s: sum([not s["Lina"], s["Oki"], not s["Panji"], not s["Oki"], s["Lina"]]) == 2],
        BENAR, [(n, lambda s, n=n: s[n]) for n in "Lina Mega Nisa Oki Panji".split()]),
    14: (urutan("Asih Bowo Cucu Darmo Endang".split()), [
        lambda s: s["Bowo"] == s["Asih"] + 1, lambda s: s["Cucu"] < s["Darmo"],
        lambda s: s["Endang"] not in (1, 5), lambda s: s["Darmo"] != 5],
        SALAH, [("Darmo dilayani kedua", lambda s: s["Darmo"] == 2),
                ("Bowo", lambda s: s["Bowo"] == 3),
                ("Endang dilayani ketiga", lambda s: s["Endang"] == 3),
                ("Darmo dilayani ketiga", lambda s: s["Darmo"] == 3),
                ("Endang dilayani kedua", lambda s: s["Endang"] == 2)]),
    15: (urutan("merah putih hitam abu biru perak".split()), [
        lambda s: s["putih"] in (1, 6), lambda s: s["hitam"] == s["merah"] + 1,
        lambda s: abs(s["biru"] - s["putih"]) != 1, lambda s: s["perak"] == 2],
        SALAH, [("putih di slot 6", lambda s: s["putih"] == 6),
                ("biru di slot 1", lambda s: s["biru"] == 1),
                ("hitam di slot 3", lambda s: s["hitam"] == 3),
                ("abu-abu di slot 4", lambda s: s["abu"] == 4),
                ("merah di slot 5", lambda s: s["merah"] == 5)]),
    16: (urutan("Rani Sapto Tiara Usman".split(), ["Ciawi", "Cisarua", "Megamendung", "Caringin"]), [
        lambda s: s["Rani"] not in ("Ciawi", "Cisarua"), lambda s: s["Sapto"] not in ("Megamendung", "Caringin"),
        lambda s: s["Usman"] != "Cisarua", lambda s: s["Tiara"] != "Ciawi" or s["Usman"] == "Megamendung"],
        SALAH, [("Usman di Caringin", lambda s: s["Usman"] == "Caringin"),
                ("Sapto di Cisarua", lambda s: s["Sapto"] == "Cisarua"),
                ("Tiara di Megamendung", lambda s: s["Tiara"] == "Megamendung"),
                ("Rani di Caringin dan Usman", lambda s: s["Rani"] == "Caringin" and s["Usman"] == "Megamendung"),
                ("Tiara di Ciawi dan Rani", lambda s: s["Tiara"] == "Ciawi" and s["Rani"] == "Megamendung")]),
    17: (urutan("Aceh Bali Jambi Maluku Papua Riau".split()), [
        lambda s: s["Papua"] == 6, lambda s: s["Maluku"] == s["Bali"] - 1, lambda s: s["Bali"] < s["Aceh"],
        lambda s: abs(s["Jambi"] - s["Riau"]) != 1],
        SALAH, [("Jambi tampil pertama", lambda s: s["Jambi"] == 1),
                ("Riau", lambda s: s["Riau"] == 5),
                ("Aceh", lambda s: s["Aceh"] == 3),
                ("Bali", lambda s: s["Bali"] == 2),
                ("Jambi tampil keempat", lambda s: s["Jambi"] == 4)]),
    # kursi 0,1,2 = U1,U2,U3; kursi 3,4,5 = S1,S2,S3; kursi i berhadapan dengan i+3
    18: (urutan("Kurnia Laras Mahmud Nadia Opik Prita".split(), range(6)), [
        lambda s: s["Laras"] == 1,
        lambda s: s["Kurnia"] // 3 == s["Nadia"] // 3 and abs(s["Kurnia"] - s["Nadia"]) == 1,
        lambda s: abs(s["Opik"] - s["Kurnia"]) == 3, lambda s: s["Prita"] < 3],
        BENAR, [(n, lambda s, n=n: abs(s[n] - s["Laras"]) == 3) for n in "Kurnia Mahmud Nadia Opik Prita".split()]),
    19: (pilih("Amir Bela Cahya Dewi Erik Farah".split(), 3), [
        lambda s: not s["Amir"] or not s["Bela"], lambda s: not s["Cahya"] or s["Dewi"],
        lambda s: s["Erik"] != s["Farah"], lambda s: not (s["Bela"] and s["Dewi"])],
        BENAR, [(n, lambda s, n=n: s[n]) for n in "Dewi Amir Cahya Erik Farah".split()]),
    20: (urutan("Asep Budi Cecep Dadang Enjang".split(), range(5)), [
        lambda s: kanan_dari(s["Budi"], s["Asep"], 5), lambda s: kiri_dari(s["Dadang"], s["Cecep"], 5),
        lambda s: not sebelah(s["Cecep"], s["Budi"], 5)],
        BENAR, [("kiri Asep", lambda s: kiri_dari(s["Cecep"], s["Asep"], 5)),
                ("Enjang bersebelahan dengan Budi", lambda s: sebelah(s["Enjang"], s["Budi"], 5)),
                ("Dadang tidak bersebelahan", lambda s: not sebelah(s["Dadang"], s["Asep"], 5)),
                ("kanan Dadang", lambda s: kanan_dari(s["Enjang"], s["Dadang"], 5)),
                ("Cecep bersebelahan dengan Enjang", lambda s: sebelah(s["Cecep"], s["Enjang"], 5))]),
    21: (urutan("Sosial PUPR Perhubungan Kominfo Pariwisata".split()), [
        lambda s: s["PUPR"] < s["Perhubungan"], lambda s: s["Kominfo"] in (3, 4),
        lambda s: s["Pariwisata"] == s["Sosial"] + 1, lambda s: s["Perhubungan"] != 5],
        BENAR, [posisi(n, 4) for n in "Sosial PUPR Perhubungan Kominfo Pariwisata".split()]),
    22: (urutan("Hadi Ira Jono Kiki Lala Mamat".split()), [
        lambda s: abs(s["Hadi"] - s["Ira"]) == 3, lambda s: s["Jono"] < s["Hadi"] and s["Jono"] < s["Ira"],
        lambda s: s["Kiki"] == s["Lala"] + 1, lambda s: s["Mamat"] not in (1, 6)],
        BENAR, [posisi(n, 4) for n in "Hadi Ira Kiki Lala Mamat".split()]),
    # urutan usia: 1 = tertua
    23: (urutan("Rina Sani Tomi Umar Wati".split()), [
        lambda s: s["Tomi"] < s["Rina"] < s["Sani"], lambda s: s["Wati"] < s["Tomi"],
        lambda s: s["Umar"] > s["Rina"], lambda s: s["Umar"] != 5],
        BENAR, [posisi(n, 5) for n in "Rina Sani Tomi Umar Wati".split()]),
    24: (urutan("Hesti Irfan Jihan Kurnia".split(), range(6, 11)), [
        lambda s: s["Jihan"] == 8, lambda s: s["Kurnia"] == s["Irfan"] + 1, lambda s: s["Hesti"] < s["Irfan"]],
        BENAR, [(b, lambda s, m=m: s["Irfan"] == m) for b, m in
                zip(["Juni", "Juli", "Agustus", "September", "Oktober"], range(6, 11))]),
    25: (kelompok("Ani Beni Caca Doni Eli Feri".split(), (1, 2, 3), {1: 2, 2: 2, 3: 2}), [
        lambda s: s["Ani"] != s["Beni"], lambda s: s["Caca"] == 2, lambda s: s["Doni"] == s["Eli"],
        lambda s: s["Feri"] != 1],
        BENAR, [("Ani", lambda s: s["Ani"] == 2), ("Beni", lambda s: s["Beni"] == 3),
                ("Eli", lambda s: s["Eli"] == 3), ("Feri", lambda s: s["Feri"] == 2),
                ("Doni", lambda s: s["Doni"] == 1)]),
    26: (urutan("Yudi Zaki Arum Bunga Citra Dewo".split(), range(6)), [
        lambda s: hadap(s["Arum"], s["Zaki"]), lambda s: kiri_dari(s["Yudi"], s["Arum"]),
        lambda s: kanan_dari(s["Dewo"], s["Zaki"]), lambda s: not sebelah(s["Citra"], s["Arum"])],
        BENAR, [(n, lambda s, n=n: hadap(s[n], s["Citra"])) for n in "Yudi Zaki Arum Bunga Dewo".split()]),
    27: (urutan("Galang Hilda Ikhsan Juwita Kamal Lestari".split()), [
        lambda s: s["Hilda"] == s["Galang"] + 1, lambda s: s["Juwita"] == s["Ikhsan"] + 2,
        lambda s: s["Kamal"] != 1, lambda s: s["Lestari"] == 4],
        SALAH, [("Galang", lambda s: s["Galang"] == 1), ("Kamal berada di urutan kelima", lambda s: s["Kamal"] == 5),
                ("Juwita", lambda s: s["Juwita"] == 3), ("Kamal berada di urutan keenam", lambda s: s["Kamal"] == 6),
                ("Ikhsan", lambda s: s["Ikhsan"] == 1)]),
    28: (urutan("Amin Badu Cahyo Darno Edi".split()), [
        lambda s: s["Cahyo"] == 2, lambda s: s["Edi"] == 3, lambda s: s["Darno"] > s["Edi"],
        lambda s: s["Amin"] != 1, lambda s: abs(s["Amin"] - s["Badu"]) != 1],
        BENAR, [posisi(n, 1) for n in "Amin Badu Cahyo Darno Edi".split()]),
    29: (dom29(), [
        lambda s: bidang_asal(s, "Medan") == "Umum",
        lambda s: s["Nanda_k"] != "Medan" and s["Nanda_b"] != "Keuangan",
        lambda s: s["Okta_k"] == "Padang", lambda s: s["Puspa_b"] == "Perencanaan",
        lambda s: bidang_asal(s, "Pontianak") != "Kepegawaian"],
        BENAR, [(b, lambda s, b=b: bidang_asal(s, "Palembang") == b) for b in BID]
        + [("Tidak dapat", lambda s: False)]),
    30: (pilih("Lia Mamat Nur Oka Pras".split(), 3), [
        lambda s: not s["Lia"] or s["Mamat"], lambda s: not (s["Nur"] and s["Oka"]),
        lambda s: s["Pras"] or s["Nur"], lambda s: not (s["Mamat"] and s["Pras"])],
        BENAR, [(", ".join(t[:-1]) + ", dan " + t[-1], lambda s, t=t: all(s[x] for x in t)) for t in
                [("Lia", "Mamat", "Nur"), ("Lia", "Mamat", "Pras"), ("Mamat", "Nur", "Oka"),
                 ("Nur", "Oka", "Pras"), ("Lia", "Nur", "Pras")]]),
    # ruang 1-3 lantai 1, ruang 4-6 lantai 2 (ruang i+3 tepat di atas ruang i)
    31: (urutan("Arsip Keuangan Umum Rapat Server Pimpinan".split(), range(1, 7)), [
        lambda s: s["Pimpinan"] in (4, 6), lambda s: s["Server"] == s["Pimpinan"] - 3,
        lambda s: abs(s["Arsip"] - s["Server"]) == 1 and (s["Arsip"] - 1) // 3 == (s["Server"] - 1) // 3,
        lambda s: s["Rapat"] >= 4 and abs(s["Rapat"] - s["Pimpinan"]) != 1],
        BENAR, [("Rapat", lambda s: s["Rapat"] == 4), ("Arsip", lambda s: s["Arsip"] == 2),
                ("Keuangan", lambda s: s["Keuangan"] >= 4), ("Umum", lambda s: s["Umum"] == 5),
                ("Server", lambda s: s["Server"] == 1)]),
    32: (urutan("Ambon Biak Kupang Manado Ternate".split()), [
        lambda s: s["Manado"] < s["Kupang"] < s["Ambon"], lambda s: s["Biak"] == s["Ambon"] + 1,
        lambda s: s["Ternate"] != 1],
        BENAR, [posisi(n, 1) for n in "Ambon Biak Kupang Manado Ternate".split()]),
    33: (urutan("Rosa Seno Tia Ucok Vivi Wawan".split()), [
        lambda s: s["Seno"] % 2 == 0, lambda s: s["Rosa"] % 2 == 1 and s["Tia"] == s["Rosa"] + 1,
        lambda s: s["Ucok"] in (1, 6), lambda s: s["Vivi"] < s["Wawan"]],
        SALAH, [("Ucok", lambda s: s["Ucok"] == 6), ("Rosa", lambda s: s["Rosa"] == 5),
                ("Seno", lambda s: s["Seno"] == 6), ("Vivi", lambda s: s["Vivi"] == 3),
                ("Wawan", lambda s: s["Wawan"] == 2)]),
    34: (urutan("Kadis Sekretaris Data Layanan Umum Bendahara".split()), [
        lambda s: s["Kadis"] in (3, 4), lambda s: abs(s["Sekretaris"] - s["Kadis"]) == 1,
        lambda s: s["Bendahara"] in (1, 6), lambda s: abs(s["Umum"] - s["Bendahara"]) == 1,
        lambda s: abs(s["Data"] - s["Layanan"]) != 1],
        BENAR, [("diapit", lambda s: abs(s["Sekretaris"] - s["Kadis"]) == 1
                 and any(abs(s[k] - s["Kadis"]) == 1 for k in ("Data", "Layanan"))),
                ("Sekretaris berdiri bersebelahan", lambda s: abs(s["Sekretaris"] - s["Umum"]) == 1),
                ("Kabid Data", lambda s: s["Data"] in (1, 6)),
                ("posisi 4", lambda s: s["Kadis"] == 4),
                ("Bendahara", lambda s: s["Bendahara"] == 6)]),
    35: (urutan("Makmur Subur Lestari Sejahtera Mandiri".split()), [
        lambda s: s["Subur"] < s["Makmur"], lambda s: s["Lestari"] in (2, 4),
        lambda s: s["Mandiri"] == s["Sejahtera"] + 1, lambda s: s["Makmur"] != 5],
        BENAR, [posisi(n, 3) for n in "Makmur Subur Lestari Sejahtera Mandiri".split()]),
}


def cek_analitis(no, spec):
    dom, syarat, mode, opsi = spec
    sols = [s for s in dom if all(c(s) for c in syarat)]
    if not sols:
        return None, "tidak ada susunan yang memenuhi syarat"
    benar = []
    for huruf, (_, f) in zip("ABCDE", opsi):
        nilai = [bool(f(s)) for s in sols]
        if mode == BENAR and all(nilai):
            benar.append(huruf)
        if mode == SALAH and not any(nilai):
            benar.append(huruf)
        if mode == SALAH and all(nilai):
            return None, f"opsi {huruf} selalu benar, padahal soal menanyakan yang pasti salah"
    return benar, f"{len(sols)} susunan sah"


# ===================== analogi: jenis relasi =====================

RELASI = {
    1: "sinonim", 2: "antonim", 3: "bagian-keseluruhan", 4: "anggota-kelompok", 5: "jenis-kategori",
    6: "sebab-akibat", 7: "alat-fungsi", 8: "profesi-alat", 9: "profesi-tempat", 10: "profesi-hasil",
    11: "bahan-produk", 12: "tingkatan", 13: "urutan/proses", 14: "alat ukur-besaran", 15: "habitat",
    16: "benda-sifat", 17: "simbol-makna", 18: "kekurangan-akibat", 19: "pelaku-tindakan", 20: "sinonim",
    21: "sebab-akibat", 22: "alat-fungsi", 23: "bagian-keseluruhan", 24: "bahan-produk", 25: "profesi-tempat",
    26: "antonim", 27: "tingkatan", 28: "profesi-hasil", 29: "jenis-kategori", 30: "alat ukur-besaran",
    31: "sinonim", 32: "antonim", 33: "kekurangan-akibat", 34: "urutan/proses", 35: "sebab-akibat",
}


def kunci_json(q):
    return next(o["key"] for o in q["options"] if o["score"] == 5)


def cocok_teks(q, opsi):
    salah = []
    for o, (potongan, _) in zip(q["options"], opsi):
        if potongan.lower() not in o["text"].lower():
            salah.append(f"opsi {o['key']} tidak memuat '{potongan}'")
    return salah


def main():
    gagal = 0
    for tag, specs, fungsi in (("silogisme", SIL, cek_silogisme), ("analitis", ANA, cek_analitis)):
        print(f"== {tag} ==")
        for no in range(1, 36):
            qid = f"tiu-verbal-{tag}-{no:04d}"
            q = DATA.get(qid)
            if q is None or no not in specs:
                print(f"  {qid}: GAGAL, soal atau spesifikasi tidak ada")
                gagal += 1
                continue
            spec = specs[no]
            opsi = spec["opsi"] if tag == "silogisme" else spec[3]
            masalah = cocok_teks(q, opsi)
            benar, info = fungsi(no, spec)
            kunci = kunci_json(q)
            if benar is None:
                masalah.append(info)
            elif benar != [kunci]:
                masalah.append(f"opsi benar menurut cek = {benar}, kunci JSON = {kunci}")
            if masalah:
                gagal += 1
                print(f"  {qid}: GAGAL ({info}); " + "; ".join(masalah))
            else:
                print(f"  {qid}: ok, kunci {kunci} ({info})")
    jenis = sorted(set(RELASI.values()))
    analogi = [q for q in DATA.values() if q["tag"] == "analogi"]
    print(f"== analogi ==\n  {len(analogi)} soal, {len(jenis)} jenis relasi: {', '.join(jenis)}")
    if len(jenis) < 15 or len(analogi) != len(RELASI):
        print("  GAGAL: jenis relasi kurang dari 15 atau jumlah soal tidak cocok")
        gagal += 1
    print(f"\n{'SEMUA LOLOS' if not gagal else f'GAGAL: {gagal} soal'}")
    sys.exit(1 if gagal else 0)


if __name__ == "__main__":
    main()
