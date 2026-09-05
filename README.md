# Absensi Pribadi Pro 2.3 — GitHub Ready

Project Android Kotlin + Jetpack Compose untuk absensi pribadi.

## Yang diperbaiki pada 2.3

- Project Android berada langsung di root repository.
- Tidak membutuhkan file ZIP sumber lain.
- Tidak memakai langkah `unzip` di GitHub Actions.
- Workflow sudah memakai branch `main`.
- GitHub Actions membangun APK dengan Java 17 + Gradle 8.9.
- APK otomatis disimpan sebagai Artifact bernama `AbsensiPribadi-2.3-APK`.
- `workflow_dispatch` tersedia untuk menjalankan build manual dari menu Actions.

## Fitur aplikasi

- PIN login (default: `1234`)
- PIN disimpan sebagai SHA-256
- Check-in / check-out
- Selfie
- GPS
- Waktu dan catatan
- Riwayat absensi
- Laporan bulanan
- Export PDF
- Backup JSON
- Database Room / SQLite
- ProGuard untuk release

## Cara upload ke GitHub dari HP

1. Buat repository baru, misalnya `AbsensiPribadi`.
2. Upload **isi folder project ini**, bukan ZIP-nya.
3. Pastikan file berikut ada di root:
   - `settings.gradle.kts`
   - `build.gradle.kts`
   - `gradle.properties`
   - folder `app`
   - folder `.github/workflows`
4. Pastikan workflow berada di:
   `.github/workflows/build-apk.yml`
5. Commit ke branch `main`.
6. Buka tab **Actions**.
7. Pilih **Build APK Absensi Pribadi**.
8. Tekan **Run workflow** bila ingin menjalankan manual.
9. Setelah selesai dan status hijau, buka hasil workflow.
10. Di bagian **Artifacts**, download `AbsensiPribadi-2.3-APK`.

## Penting

Untuk upload lewat GitHub web, GitHub kadang menyulitkan upload folder tersembunyi `.github` dari HP. Jika `.github/workflows/build-apk.yml` tidak ikut ter-upload, buat file tersebut secara manual di GitHub dengan path yang sama lalu tempel isi workflow dari project ini.

Default PIN aplikasi: **1234**. Segera ganti melalui menu Pengaturan setelah aplikasi berhasil dibangun.

Catatan: build debug dibuat untuk instalasi/pengujian pribadi. Release production masih memerlukan signing key dan pengujian perangkat.
