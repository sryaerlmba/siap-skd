"""Memecah file materi Markdown di ../../materi menjadi assets/materi.json, satu halaman per sub-materi.

Pemakaian: python tools/build_materi.py  (dari folder android/)
"""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MATERI = ROOT.parent / "materi"
OUT = ROOT / "app" / "src" / "main" / "assets" / "materi.json"

SKIP_TITLES = ("latihan soal", "sumber", "kunci dan pembahasan", "contoh soal dan skor", "rekap kunci")


def split_sections(text: str, level: int):
    """Potong teks pada heading dengan level tertentu. Mengembalikan (judul, isi) berurutan; teks sebelum heading pertama dibuang."""
    pattern = re.compile(rf"^{'#' * level} (?!#)(.+)$", re.M)
    matches = list(pattern.finditer(text))
    out = []
    for i, m in enumerate(matches):
        end = matches[i + 1].start() if i + 1 < len(matches) else len(text)
        out.append((m.group(1).strip(), text[m.end():end].strip()))
    return out


OPTION_LINE = re.compile(r"^A\. .* · B\. ")
KUNCI_LINE = re.compile(r"^\*\*Kunci [A-E]\.\*\*")


def clean(md: str) -> str:
    md = re.sub(r"\n---\n", "\n", md)
    # Contoh soal di tiu.md ditulis tiga baris rapat: "**N.** soal", "A. x · B. y ...", "**Kunci X.** ...".
    # Markdown menggabungkan baris rapat jadi satu paragraf, jadi pisahkan: opsi jadi blok sendiri
    # (dirender satu opsi per baris oleh aplikasi) dan kunci jadi kutipan berwarna.
    out = []
    for line in md.split("\n"):
        if OPTION_LINE.match(line):
            out += ["", line, ""]
        elif KUNCI_LINE.match(line):
            out += ["", "> " + line, ""]
        else:
            out.append(line)
    md = "\n".join(out)
    md = re.sub(r"\n{3,}", "\n\n", md)
    return md.strip()


def minutes(md: str) -> int:
    words = len(re.findall(r"\w+", md))
    return max(3, round(words / 180))


def strip_number(title: str) -> str:
    return re.sub(r"^(\d+(\.\d+)?|[A-E](\.\d+)?)\.?\s+", "", title).strip()


pages = []


def add(pid, subtest, group, title, tags, md, order):
    md = clean(md)
    if not md:
        return
    pages.append({
        "id": pid, "subtest": subtest, "group": group, "title": title,
        "tags": tags, "readMinutes": minutes(md), "order": order, "markdown": md
    })


# ---------- TWK bagian 1 ----------
twk1 = (MATERI / "twk-1-pilar-negara.md").read_text(encoding="utf-8")
map1 = {
    "Pancasila": ("pilar_negara", ["pancasila"]),
    "UUD NRI 1945": ("pilar_negara", ["uud_1945"]),
    "NKRI dan Sistem Ketatanegaraan": ("pilar_negara", ["nkri"]),
    "Hierarki Peraturan Perundang-undangan": ("pilar_negara", ["nkri"]),
}
for i, (title, body) in enumerate(split_sections(twk1, 2)):
    t = strip_number(title)
    if t.lower().startswith(SKIP_TITLES):
        continue
    group, tags = map1[t]
    add(f"twk-{tags[0]}-{i}", "TWK", group, t, tags, body, 100 + i)

# ---------- TWK bagian 2 ----------
twk2 = (MATERI / "twk-2-kebangsaan.md").read_text(encoding="utf-8")
map2 = {
    "Nasionalisme": ("nasionalisme", ["sejarah_pergerakan", "proklamasi_kemerdekaan", "identitas_nasional", "wawasan_nusantara"]),
    "Integritas": ("integritas", ["antikorupsi", "kode_etik_asn", "konflik_kepentingan"]),
    "Bela Negara": ("bela_negara", ["dasar_hukum", "nilai_bela_negara", "sishankamrata"]),
    "Bhinneka Tunggal Ika": ("pilar_negara", ["bhinneka_tunggal_ika"]),
    "Bahasa Negara": ("bahasa_negara", ["ejaan", "kata_baku", "kalimat_efektif", "paragraf"]),
}
for i, (title, body) in enumerate(split_sections(twk2, 2)):
    t = strip_number(title)
    if t.lower().startswith(SKIP_TITLES):
        continue
    group, tags = map2[t]
    add(f"twk-{group}-{i}", "TWK", group, t, tags, body, 120 + i)

# ---------- TIU ----------
tiu = (MATERI / "tiu.md").read_text(encoding="utf-8")
tiu_tags = {
    "Analogi Verbal": ("verbal", ["analogi"]),
    "Silogisme (Penarikan Kesimpulan)": ("verbal", ["silogisme"]),
    "Penalaran Analitis": ("verbal", ["analitis"]),
    "Berhitung": ("numerik", ["berhitung"]),
    "Deret Angka dan Huruf": ("numerik", ["deret_angka"]),
    "Perbandingan Kuantitatif": ("numerik", ["perbandingan_kuantitatif"]),
    "Soal Cerita": ("numerik", ["soal_cerita"]),
    "Analogi Figural": ("figural", ["analogi_gambar"]),
    "Ketidaksamaan Figural": ("figural", ["ketidaksamaan"]),
    "Serial Figural": ("figural", ["serial"]),
}
order = 200
for h1_title, h1_body in split_sections(tiu, 1):
    t1 = h1_title.strip()
    if t1.startswith("BAGIAN"):
        for title, body in split_sections(h1_body, 2):
            t = strip_number(title)
            group, tags = tiu_tags[t]
            # Buang bagian "Pola siap gambar" (untuk pembuat aplikasi), bukan untuk pembaca.
            body = re.split(r"^### Pola siap gambar.*$", body, flags=re.M)[0]
            add(f"tiu-{group}-{tags[0]}", "TIU", group, t, tags, body, order)
            order += 1
    elif t1.startswith("Strategi Waktu"):
        add("tiu-strategi", "TIU", "strategi", "Strategi waktu TIU di CAT", [], h1_body, 290)
    elif t1.startswith("Cheat Sheet"):
        add("tiu-cheatsheet", "TIU", "strategi", "Cheat sheet rumus TIU", [], h1_body, 291)
    elif t1.startswith("Materi TIU"):
        add("tiu-pengantar", "TIU", "strategi", "Peta TIU: apa yang diuji", [], h1_body, 199)

# ---------- TKP ----------
tkp = (MATERI / "tkp.md").read_text(encoding="utf-8")
tkp_tags = {
    "Pelayanan Publik": "pelayanan_publik",
    "Jejaring Kerja": "jejaring_kerja",
    "Sosial Budaya": "sosial_budaya",
    "Teknologi Informasi dan Komunikasi": "tik",
    "Profesionalisme": "profesionalisme",
    "Anti Radikalisme": "anti_radikalisme",
}
for title, body in split_sections(tkp, 2):
    t = strip_number(title)
    if t.startswith("Cara Kerja Penilaian"):
        add("tkp-penilaian", "TKP", "penilaian", "Cara kerja skor TKP 1 sampai 5", [], body, 300)
    elif t.startswith("Enam Aspek"):
        for i, (sub, sbody) in enumerate(split_sections(body, 3)):
            st = strip_number(sub)
            if st.startswith("Pegangan nilai"):
                add("tkp-berakhlak", "TKP", "penilaian", "Pegangan nilai: BerAKHLAK dan UU 25/2009", [], sbody, 301)
            else:
                tag = tkp_tags[st]
                add(f"tkp-{tag}", "TKP", tag, st, [tag], sbody, 310 + i)
    elif t.startswith("Kesalahan Umum"):
        add("tkp-strategi", "TKP", "penilaian", "Kesalahan umum dan strategi TKP", [], body, 330)

pages.sort(key=lambda p: p["order"])
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps({"version": "2026.11", "pages": pages}, ensure_ascii=False, indent=1), encoding="utf-8")
total_words = sum(len(re.findall(r"\w+", p["markdown"])) for p in pages)
print(f"{len(pages)} halaman, {total_words} kata -> {OUT.relative_to(ROOT)}")
for p in pages:
    print(f"  {p['subtest']:3} {p['group']:16} {p['title'][:48]:48} {p['readMinutes']:2} mnt  tags={p['tags']}")
