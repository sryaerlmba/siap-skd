# Siap SKD (Android)

Aplikasi belajar SKD CPNS: tryout CAT, kuis harian, materi, latihan per materi, progres. Offline-first.

## Build

```
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
gradlew :app:testDebugUnitTest :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Pasang dengan `adb install -r <apk>`.

## Struktur

- `data/model` — model domain (Question, ScoringConfig, kategori pelamar).
- `data/content/BankParser` — membaca `assets/bank-soal.json` dan `assets/scoring-config.json` (format: `../soal/SCHEMA.md`).
- `data/db` — Room: soal, sesi, jawaban, hasil, statistik tag, antrian pengulangan.
- `data/QuestionRepository` — impor bank soal sekali per versi ke Room.
- `data/SessionRepository` — sesi tryout/kuis/latihan, skor, statistik, antrian ulang (1/3/7 hari).
- `domain/Scoring` — aturan CAT: TWK/TIU 5-0, TKP 1-5, lolos per subtes atau kumulatif. Diuji di `app/src/test`.
- `domain/TryoutComposer` — susun paket 30/35/45 proporsional per tag, hindari soal yang sudah muncul.
- `data/MaterialRepository` — halaman materi dari `assets/materi.json` (dibuat `tools/build_materi.py` dari `../materi/*.md`).
- `domain/Streak`, `domain/Strength` — streak harian dan label lemah/sedang/kuat per tag.
- `ui/*` — Compose: onboarding, beranda, tryout (+ grid), hasil, pembahasan, kuis (kuis harian + latihan dengan umpan balik langsung), latihan per tag + pengulangan, materi (+ renderer Markdown), progres (grafik tryout + peta kekuatan), pengaturan.

## Memperbarui materi

```
python tools/build_materi.py
```
Membaca `../materi/*.md`, memecah per sub-materi, menulis `app/src/main/assets/materi.json`.

## Memperbarui bank soal

1. Edit/tambah file di `../soal/`, jalankan `python ../soal/validate.py`.
2. Gabungkan ulang jadi `bank-soal.json` (naikkan `version`), salin ke `app/src/main/assets/`.
3. Aplikasi mengimpor ulang otomatis saat versi berubah.
