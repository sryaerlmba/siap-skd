# Skema bank soal SKD

Satu file JSON per batch, isinya array objek soal. Satu skema untuk TWK, TIU, TKP.

```json
{
  "id": "tiu-num-deret-0042",
  "subtest": "TIU",
  "group": "numerik",
  "tag": "deret_angka",
  "stem": "2, 3, 5, 8, 12, 17, ...",
  "image": null,
  "options": [
    { "key": "A", "text": "21", "score": 0, "rationale": "" },
    { "key": "B", "text": "23", "score": 5, "rationale": "" },
    { "key": "C", "text": "24", "score": 0, "rationale": "" },
    { "key": "D", "text": "25", "score": 0, "rationale": "" },
    { "key": "E", "text": "27", "score": 0, "rationale": "" }
  ],
  "explanation": "Selisihnya +1, +2, +3, +4, +5, jadi 17 + 6 = 23.",
  "difficulty": 2,
  "source": "ditulis sendiri",
  "status": "draft"
}
```

## Aturan

- `id`: `<subtest>-<group>-<tag>-<nomor 4 digit>`, huruf kecil, unik di seluruh bank.
- `subtest`: `TWK` | `TIU` | `TKP`.
- `group` dan `tag` mengikuti taksonomi di bawah, persis ejaannya.
- `stem`: teks soal. Boleh berisi `\n` untuk baris baru. Untuk TIU figural yang belum ada gambarnya, deskripsikan polanya dengan teks/notasi (lihat tiu.md bagian figural) dan isi `image` dengan `null`.
- `options`: tepat 5, `key` A sampai E berurutan.
  - TWK dan TIU: tepat satu opsi `score` 5, empat lainnya 0. `rationale` boleh kosong.
  - TKP: kelima opsi memakai skor 1, 2, 3, 4, 5 masing-masing tepat satu kali. `rationale` WAJIB diisi untuk setiap opsi (satu kalimat kenapa skornya segitu). Posisi skor 5 harus tersebar merata di A sampai E dalam satu file.
- `explanation`: pembahasan singkat (TWK/TIU: kenapa kunci benar; TKP: prinsip yang dinilai).
- `difficulty`: 1 mudah, 2 sedang, 3 sulit. Target campuran per file kira-kira 30% / 50% / 20%.
- `source`: `ditulis sendiri`, atau rujukan pasal/UU untuk TWK (contoh: `UUD 1945 Pasal 7A`).
- `status`: selalu `draft`; berubah jadi `reviewed` setelah dicek manusia.
- Soal harus orisinal. Jangan menyalin dari buku, media, atau situs bimbel.
- Semua teks bahasa Indonesia baku, tanda kutip lurus, tanpa emoji.

## Taksonomi (group / tag)

| subtest | group | tag |
| --- | --- | --- |
| TWK | nasionalisme | sejarah_pergerakan, proklamasi_kemerdekaan, identitas_nasional, wawasan_nusantara |
| TWK | integritas | antikorupsi, kode_etik_asn, konflik_kepentingan |
| TWK | bela_negara | dasar_hukum, nilai_bela_negara, sishankamrata |
| TWK | pilar_negara | pancasila, uud_1945, nkri, bhinneka_tunggal_ika |
| TWK | bahasa_negara | ejaan, kata_baku, kalimat_efektif, paragraf |
| TIU | verbal | analogi, silogisme, analitis |
| TIU | numerik | berhitung, deret_angka, perbandingan_kuantitatif, soal_cerita |
| TIU | figural | analogi_gambar, ketidaksamaan, serial |
| TKP | pelayanan_publik | pelayanan_publik |
| TKP | jejaring_kerja | jejaring_kerja |
| TKP | sosial_budaya | sosial_budaya |
| TKP | tik | tik |
| TKP | profesionalisme | profesionalisme |
| TKP | anti_radikalisme | anti_radikalisme |

## Validasi

Jalankan sebelum menyerahkan file:

```
python F:\belajar\cpns\soal\validate.py F:\belajar\cpns\soal\<file>.json
```

Skrip menolak file yang melanggar aturan di atas dan mencetak baris yang bermasalah.
