# Siap SKD

Bahan belajar dan aplikasi Android untuk SKD CPNS (TWK, TIU, TKP). Dibuat untuk belajar pribadi; tidak terafiliasi dengan BKN atau KemenPANRB.

| Folder | Isi |
| --- | --- |
| `android/` | Aplikasi Android (Kotlin, Jetpack Compose, Room): tryout CAT, kuis harian, latihan per materi, materi, progres. Lihat `android/README.md`. |
| `soal/` | Bank soal 1.100 butir dalam JSON (`SCHEMA.md` menjelaskan formatnya), validator, skrip cek jawaban TIU, dan `REVIEW.md` berisi daftar soal yang perlu ditinjau manusia. |
| `materi/` | Materi belajar dalam Markdown: TWK (2 bagian), TIU, TKP. |
| `riset-cpns.html` | Riset aturan SKD: struktur, skor, passing grade, kisi-kisi, sumber. |

## Catatan penting

- Semua soal berstatus `draft`. Kunci TKP dirancang dari prinsip BerAKHLAK dan UU 25/2009, bukan kunci resmi BKN (yang tidak pernah dipublikasikan). Daftar soal yang rawan subjektif ada di `soal/REVIEW.md`.
- Passing grade memakai KepmenPANRB 321/2024 (TWK 65, TIU 80, TKP 166). Aturan 2026 belum terbit saat ini ditulis; angkanya ada di `android/app/src/main/assets/scoring-config.json`.
- Soal dibuat sendiri dengan bantuan AI, lalu dicek mesin (struktur, kunci TIU) dan masih perlu review manusia.

## Build aplikasi

```
cd android
gradlew :app:testDebugUnitTest :app:assembleDebug
```
