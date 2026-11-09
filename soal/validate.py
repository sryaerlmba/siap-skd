"""Validasi file bank soal SKD terhadap SCHEMA.md.

Pemakaian: python validate.py file1.json [file2.json ...]
Keluar dengan kode 1 jika ada pelanggaran.
"""
import json
import re
import sys
from collections import Counter
from pathlib import Path

TAXONOMY = {
    "TWK": {
        "nasionalisme": {"sejarah_pergerakan", "proklamasi_kemerdekaan", "identitas_nasional", "wawasan_nusantara"},
        "integritas": {"antikorupsi", "kode_etik_asn", "konflik_kepentingan"},
        "bela_negara": {"dasar_hukum", "nilai_bela_negara", "sishankamrata"},
        "pilar_negara": {"pancasila", "uud_1945", "nkri", "bhinneka_tunggal_ika"},
        "bahasa_negara": {"ejaan", "kata_baku", "kalimat_efektif", "paragraf"},
    },
    "TIU": {
        "verbal": {"analogi", "silogisme", "analitis"},
        "numerik": {"berhitung", "deret_angka", "perbandingan_kuantitatif", "soal_cerita"},
        "figural": {"analogi_gambar", "ketidaksamaan", "serial"},
    },
    "TKP": {
        "pelayanan_publik": {"pelayanan_publik"},
        "jejaring_kerja": {"jejaring_kerja"},
        "sosial_budaya": {"sosial_budaya"},
        "tik": {"tik"},
        "profesionalisme": {"profesionalisme"},
        "anti_radikalisme": {"anti_radikalisme"},
    },
}
ID_RE = re.compile(r"^[a-z0-9_]+(-[a-z0-9_]+)*-\d{4}$")
CURLY = "“”‘’"


def check(path: Path, seen_ids: set) -> list[str]:
    errors = []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception as e:  # noqa: BLE001
        return [f"{path.name}: JSON tidak valid: {e}"]
    if not isinstance(data, list) or not data:
        return [f"{path.name}: harus array soal yang tidak kosong"]

    five_pos = Counter()
    diff = Counter()
    longest_is_key = 0
    longest_scores = []  # skor opsi terpanjang per soal TKP: simulasi peserta yang asal pilih yang panjang
    stems = Counter()
    for i, q in enumerate(data):
        where = f"{path.name}[{i}] {q.get('id', '?')}"
        for k in ("id", "subtest", "group", "tag", "stem", "options", "explanation", "difficulty", "source", "status"):
            if k not in q:
                errors.append(f"{where}: field '{k}' hilang")
        if "image" not in q:
            errors.append(f"{where}: field 'image' hilang (isi null jika tidak ada)")
        qid = q.get("id", "")
        if not ID_RE.match(qid):
            errors.append(f"{where}: id tidak sesuai pola")
        if qid in seen_ids:
            errors.append(f"{where}: id duplikat")
        seen_ids.add(qid)
        st, gr, tg = q.get("subtest"), q.get("group"), q.get("tag")
        if st not in TAXONOMY:
            errors.append(f"{where}: subtest '{st}' tidak dikenal")
        elif gr not in TAXONOMY[st]:
            errors.append(f"{where}: group '{gr}' tidak ada di {st}")
        elif tg not in TAXONOMY[st][gr]:
            errors.append(f"{where}: tag '{tg}' tidak ada di {st}/{gr}")
        if st and qid and not qid.startswith(st.lower() + "-"):
            errors.append(f"{where}: id harus diawali '{st.lower()}-'")
        if not isinstance(q.get("stem"), str) or len(q.get("stem", "")) < 15:
            errors.append(f"{where}: stem terlalu pendek")
        if q.get("difficulty") not in (1, 2, 3):
            errors.append(f"{where}: difficulty harus 1, 2, atau 3")
        diff[q.get("difficulty")] += 1
        if q.get("status") != "draft":
            errors.append(f"{where}: status harus 'draft'")
        opts = q.get("options")
        if not isinstance(opts, list) or len(opts) != 5:
            errors.append(f"{where}: harus tepat 5 opsi")
            continue
        keys = [o.get("key") for o in opts]
        if keys != ["A", "B", "C", "D", "E"]:
            errors.append(f"{where}: key opsi harus A..E berurutan")
        texts = [str(o.get("text", "")).strip() for o in opts]
        if any(not t for t in texts):
            errors.append(f"{where}: ada opsi kosong")
        if len(set(texts)) != 5:
            errors.append(f"{where}: ada opsi yang sama persis")
        scores = [o.get("score") for o in opts]
        if st == "TKP":
            if sorted(scores) != [1, 2, 3, 4, 5]:
                errors.append(f"{where}: skor TKP harus 1,2,3,4,5 masing-masing sekali, dapat {scores}")
            for o in opts:
                if not str(o.get("rationale", "")).strip():
                    errors.append(f"{where}: rationale opsi {o.get('key')} kosong (wajib untuk TKP)")
        else:
            if sorted(scores) != [0, 0, 0, 0, 5]:
                errors.append(f"{where}: skor TWK/TIU harus satu 5 dan empat 0, dapat {scores}")
        if 5 in scores:
            five_pos[keys[scores.index(5)]] += 1
            key_len = len(texts[scores.index(5)])
            others = [len(t) for j, t in enumerate(texts) if j != scores.index(5)]
            if key_len > max(others):  # hanya jika kunci paling panjang sendirian, seri tidak dihitung
                longest_is_key += 1
        if st == "TKP" and sorted(scores) == [1, 2, 3, 4, 5]:
            longest_scores.append(scores[max(range(5), key=lambda j: len(texts[j]))])
        stems[str(q.get("stem", ""))[:80].lower()] += 1
        if not str(q.get("explanation", "")).strip():
            errors.append(f"{where}: explanation kosong")
        blob = json.dumps(q, ensure_ascii=False)
        if any(c in blob for c in CURLY):
            errors.append(f"{where}: pakai tanda kutip lurus, bukan “ ” ‘ ’")

    n = len(data)
    dup_stems = [s for s, c in stems.items() if c > 1]
    if dup_stems:
        errors.append(f"{path.name}: {len(dup_stems)} stem yang awalnya sama persis (soal duplikat?)")
    if n >= 20 and longest_is_key / n > 0.45:
        # Peserta bisa menebak kunci dari opsi terpanjang. Bukan error fatal, tapi harus diperbaiki.
        print(f"{path.name}: PERINGATAN kunci/skor-5 adalah opsi terpanjang di {longest_is_key} dari {n} soal (batas wajar 45%)")
    if len(longest_scores) >= 20:
        avg = sum(longest_scores) / len(longest_scores)
        tag = "PERINGATAN" if avg > 3.4 else "ok"
        print(f"{path.name}: {tag} rata-rata skor jika selalu pilih opsi terpanjang = {avg:.2f} (passing grade butuh 3,69; batas wajar 3,40)")
    if n >= 20:
        lo, hi = n * 0.12, n * 0.30
        for k in "ABCDE":
            if not (lo <= five_pos[k] <= hi):
                errors.append(f"{path.name}: posisi kunci/skor-5 tidak merata, {k} = {five_pos[k]} dari {n} (target 12-30%)")
    print(f"{path.name}: {n} soal, kunci A-E = {dict(sorted(five_pos.items()))}, difficulty = {dict(sorted(diff.items()))}")
    return errors


def main() -> None:
    # Tanpa argumen: semua file sumber, kecuali bank-soal.json (hasil gabungan, formatnya objek).
    files = [Path(p) for p in sys.argv[1:]] or sorted(p for p in Path(__file__).parent.glob("*.json") if p.name != "bank-soal.json")
    seen: set = set()
    all_errors: list[str] = []
    for f in files:
        all_errors += check(f, seen)
    if all_errors:
        print("\n".join(all_errors[:200]))
        if len(all_errors) > 200:
            print(f"... dan {len(all_errors) - 200} pelanggaran lain")
        print(f"\nGAGAL: {len(all_errors)} pelanggaran")
        sys.exit(1)
    print(f"\nOK: {len(seen)} soal valid, id unik semua")


if __name__ == "__main__":
    main()
