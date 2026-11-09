# Catatan review bank soal v2026.11

1.100 soal, semua berstatus `draft`. Validator (`validate.py`) dan empat skrip cek TIU lolos. Yang tercatat di sini adalah hal yang **tidak bisa dicek mesin** dan ditandai sendiri oleh penulis tiap file. Prioritaskan ini saat review manusia; ubah `status` ke `reviewed` per soal setelah dicek.

## Cara kerja review

1. Buka file per subtes, cari id yang disebut di bawah.
2. TKP: cek urutan skor 5 sampai 1 masuk akal; kalau menukar dua skor, pastikan tetap 1-2-3-4-5 lengkap dan perbarui `rationale`.
3. TWK: cek fakta dan nomor pasal di `source` ke teks aturan (peraturan.bpk.go.id, jdih).
4. Jalankan `python validate.py` setelah mengedit.

## TKP: kunci yang rawan subjektif

Kunci resmi BKN tidak pernah dipublikasikan. Skor di bank ini dirancang dari prinsip BerAKHLAK, UU 25/2009, dan pola pembahasan publik (lihat `materi/tkp.md` bagian A). Pola yang paling sering diperdebatkan:

| Pola | Keputusan di bank ini | Soal yang terkena |
| --- | --- | --- |
| Rekan melanggar: ingatkan dulu vs langsung lapor atasan | "Ingatkan dulu, lapor bila berlanjut" = 5; "langsung lapor dengan bukti" = 4. Kecuali ancaman nyata/uang negara besar, lapor jalur resmi = 5 | profesionalisme (mobil dinas, toko daring, daftar hadir fiktif, pungli RS, pengadaan), pelayanan_publik-0022, jejaring_kerja-0004/0015/0031 |
| Gratifikasi: tolak langsung vs terima lalu lapor UPG | Bila bisa ditolak tanpa mempermalukan = tolak (5). Bila sudah terlanjur diterima/keluarga = lapor UPG (5), tolak = 4 | sosial_budaya-0070, profesionalisme (parsel, tiket konser) |
| Lapor atasan tanpa dialog dulu | 3 atau 2 bila masalah masih bisa dibicarakan; 4 bila ancaman nyata | jejaring_kerja-0004/0015, anti_radikalisme-0012/0045/0059 (4 vs 3) |
| Skor 1 vs 2: main hakim sendiri vs diam | Tidak konsisten antar soal, sengaja mengikuti konteks | anti_radikalisme-0016/0075 vs 0031/0046 |
| Koreksi atasan di forum saat itu vs setelahnya | Saat itu, santun = 5; setelah rapat = 4 | jejaring_kerja-0033, sosial_budaya-0031 |
| Adat vs kesetaraan gender / protokol | Menghormati adat tanpa mengorbankan tugas = 5 | sosial_budaya-0043, 0074 |
| Beda skor 4 dan 5 hanya satu unsur tambahan | Jalur resmi yang lebih lengkap = 5 | tik-0017/0041/0042/0060/0063/0067/0074 |
| Komitmen tugas vs hak pribadi (hari libur, lembur tak dibayar) | Menolak = 2, mengikuti kebiasaan kunci TKP | profesionalisme (beberapa soal) |
| Benturan kepentingan: layani sendiri sesuai SOP vs serahkan ke rekan | Layani sendiri sesuai SOP = 5 | pelayanan_publik-0070 |

Pola tebakan yang sudah dinetralkan: kunci bukan lagi opsi terpanjang (rata-rata "asal pilih terpanjang" 2,96 sampai 3,37 per file, di bawah PG 3,69). Yang belum dinetralkan: opsi terpendek jarang skor 5 (tik: 2 dari 75), jadi peserta yang menghindari opsi terpendek sedikit diuntungkan. Perbaiki bertahap saat review.

## TWK: fakta dan nomor pasal yang ditulis dari ingatan

Kunci tidak memakai fakta bertanda "(perlu verifikasi)" di materi, tetapi beberapa nomor pasal di `source`/`explanation` belum dicocokkan ke teks aturan.

| File | Id | Yang perlu dicek |
| --- | --- | --- |
| twk-pancasila | 0013, 0014, 0016 | UU 24/2009 Pasal 48 (materi hanya menyebut 46-49) |
| twk-pancasila | 0013 | "kiri atas" untuk beringin memakai sudut pandang Garuda, bukan pembaca |
| twk-pancasila | 0015 | Makna rantai bulat/persegi: apakah ada di UU 24/2009 |
| twk-pancasila | 0021 | Tanggal pembentukan PPKI 7 Agustus 1945 |
| twk-pancasila | 0027, 0029, 0051 | Kunci sila bisa diperdebatkan (1 vs 2, 2 vs 5) |
| twk-uud1945 | 0002 | UUDS 1950 Pasal 134 (tugas Konstituante) |
| twk-uud1945 | 0004, 0005 | Penpres 3/1960; suara Konstituante tidak capai 2/3 |
| twk-uud1945 | 0052 | Pasal 28I ayat 1 vs Putusan MK 065/PUU-II/2004 |
| twk-nkri | banyak | Nomor pasal: UU 7/2017 Ps 186 & 196, UU 17/2014 Ps 79(3), UU 22/2004 Ps 6, UU 15/2006 Ps 4-5, UU 12/2011 Ps 64(1a)(1b), UU 5/1983 Ps 2, UNCLOS Ps 17/33/57 |
| twk-nkri | 0043 | TAP MPR IV/1973 sebagai asal Wawasan Nusantara di GBHN |
| twk-nkri | 0021 | Pengganti hakim MK diajukan lembaga pengusul: dari praktik, bukan pasal UU MK |
| twk-nasionalisme | wawasan_nusantara-0002, 0003 | "Landasan visional" dan asas solidaritas: dari buku PKn, tidak ada di materi |
| twk-nasionalisme | identitas_nasional-0001 | Klasifikasi fundamental/instrumental/alamiah (skema buku berbeda-beda) |
| twk-nasionalisme | beberapa `source` | Keppres 316/1959; UU 24/2009 Ps 33/36/38; Perpres 16/2018 jo. 12/2021; UU 3/2014; UU 5/2017 |
| twk-integritas-belanegara | kode_etik_asn-0002 | Bunyi persis UU 20/2023 Pasal 24 ("pemerintah" vs "pemerintahan yang sah") |
| twk-integritas-belanegara | antikorupsi-0009, 0010 | Peraturan KPK 1/2026: Rp1,5 juta/tahun per pemberi atau total; tenggat 20 vs 30 hari kerja |
| twk-integritas-belanegara | kode_etik_asn-0005 | Netralitas medsos diatur SKB, bukan UU 20/2023 |
| twk-integritas-belanegara | sishankamrata-0002, 0004 | Rujukan "UU 3/2002 Ps 1 angka 2" untuk ciri sishankamrata; Komcad usia 18-35 dan latihan 3 bulan |
| twk-bhinneka-bahasa | bhinneka-0005, 0007, 0008, 0015 | PP 66/1951; Keppres 6/2000 mencabut Inpres 14/1967; UU 24/2009 Ps 24 dan Ps 36(3) |
| twk-bhinneka-bahasa | ejaan-0006, 0008; kalimat_efektif-0004; kata_baku-0006 | Diftong ei di PUEBI 2015; "kacang bogor" huruf kecil; "dibahas oleh kami" dianggap tidak baku; "mengonsumsi/menyosialisasikan" |
| twk-bhinneka-bahasa | bhinneka-0012 | "Penerimaan terhadap tradisi" vs "toleransi" |

## TIU: catatan

Semua kunci numerik, silogisme, analitis, dan figural sudah dibuktikan dengan skrip (`check-tiu-*.py`). Yang tersisa adalah soal rasa:

| File | Id | Catatan |
| --- | --- | --- |
| tiu-numerik-a | deret_angka-0041 | Pola prima x n, mungkin terlalu sulit untuk 60 detik |
| tiu-numerik-a | deret_angka-0034, 0042 | Deret huruf memutar setelah Z |
| tiu-numerik-a | berhitung-0004, 0006, 0017, 0026, 0036; deret-0009, 0011, 0045 | Pengecoh agak lemah atau tampilan opsi mencolok (pecahan di antara bulat, `27^(2/3)` butuh render superskrip) |
| tiu-numerik-b | perbandingan_kuantitatif-0005, 0013, 0020, 0027, 0034, 0040 | Kunci E (relasi khusus) padahal D juga bisa dibela; ikuti konvensi CAT |
| tiu-numerik-b | soal_cerita-0014, 0015, 0019, 0043, 0049, 0050 | Pengecoh lemah |
| tiu-numerik-b | soal_cerita-0002 | Jarak Jakarta-Cirebon 210 km adalah perkiraan |
| tiu-verbal | analogi-0027, 0035 | Relasi intensitas dan sebab-akibat ekonomi, rawan debat |
| tiu-verbal | analitis-0004, 0020, 0026 | Meja bundar: kanan = berlawanan arah jarum jam (konvensi tiu.md) |
| tiu-verbal | analitis-0013 | Teka-teki pembohong, bukan urutan/posisi |

## Catatan untuk aplikasi

- **Figural** memakai notasi teks, `image` null. Stem diawali `[grid]` atau `[bentuk]`. Aturan render lengkap ada di laporan agen; ringkasnya: grid 3x3 dengan `.` kosong, `#` kotak hitam, `o` lingkaran, `x` silang, `/` dan `\` diagonal, `^` segitiga tegak; bentuk ditulis `bentuk=...; isi=...; rotasi=...; titik=...; panah=...; ukuran=...; garis=...` dengan default putih/0/tanpa/sedang. Lihat `check-tiu-figural.py` untuk parser referensi.
- **Perbandingan kuantitatif** memakai format opsi baku A "P > Q", B "P < Q", C "P = Q", D "tidak dapat ditentukan", E relasi khusus.
- `bank-soal.json` = gabungan 16 file dengan metadata `version`, `generatedAt`, `count`. Bangun ulang dengan skrip gabung bila file sumber diedit.
- Untuk 10 paket tryout tanpa pengulangan dan kuis harian, bank ini cukup untuk sekitar 10 tryout + 2 minggu kuis harian. Target berikutnya 2.200 soal.
