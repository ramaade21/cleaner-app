# File Cleaner — Android App

Aplikasi manajemen file untuk Android (minimal Android 11 / API 30) dengan fitur:

1. **Dashboard penyimpanan** — total & sisa storage, progress bar pemakaian.
2. **File tersembunyi & cache** — lihat ukurannya, masuk ke daftar detail.
3. **File besar** — semua file ≥ 50 MB (tersembunyi maupun tidak), beserta ukurannya.
4. **Pilih & hapus file** — checkbox per file, "Pilih Semua", konfirmasi sebelum hapus, info total ukuran yang akan dibebaskan.
5. **Kategori berdasarkan jenis** — Gambar, Video, Dokumen, Audio, APK, Lainnya — masing-masing dengan total ukuran & jumlah file.

> ⚠️ **Bukan antivirus.** Sesuai permintaan terakhir, fitur deteksi virus **tidak** disertakan — aplikasi ini murni file manager/cleaner.

---

## Cara mendapatkan file .apk (tanpa install Android Studio)

Karena Anda tidak punya Android Studio, cara **paling mudah dan gratis** adalah memanfaatkan **GitHub Actions** — Anda hanya perlu akun GitHub (gratis), tanpa install apa pun di komputer.

### Langkah 1 — Buat akun & repository GitHub
1. Buka [github.com](https://github.com) → daftar jika belum punya akun.
2. Klik tombol **New repository** (ikon `+` di kanan atas → "New repository").
3. Beri nama misalnya `file-cleaner-app`, pilih **Public** atau **Private** (keduanya gratis untuk Actions), lalu klik **Create repository**.

### Langkah 2 — Upload project ini ke GitHub
Cara paling mudah tanpa command line:
1. Di halaman repository yang baru dibuat, klik **"uploading an existing file"**.
2. **Extract** file ZIP yang saya berikan ke komputer Anda.
3. **Seret (drag) semua isi folder** `FileManagerApp` (bukan folder itu sendiri, tapi isinya: `app`, `gradle`, `.github`, `build.gradle`, dst) ke halaman upload GitHub.
   - GitHub web upload kadang tidak menerima folder tersembunyi seperti `.github`. Jika itu terjadi, gunakan **GitHub Desktop** (app gratis, lebih mudah dari command line) — lihat Langkah 2B di bawah.
4. Klik **Commit changes**.

### Langkah 2B — Alternatif: pakai GitHub Desktop (lebih disarankan)
1. Download & install [GitHub Desktop](https://desktop.github.com/) (gratis, tinggal klik next-next).
2. Login dengan akun GitHub Anda.
3. Klik **File → Add Local Repository**, pilih folder `FileManagerApp` hasil extract ZIP.
4. Jika diminta "create a repository", klik itu.
5. Klik **Publish repository** → pilih nama → Publish.
6. Selesai — semua file termasuk folder `.github` otomatis terupload dengan benar.

### Langkah 3 — Tunggu APK otomatis dibuat
1. Setelah upload selesai, buka tab **Actions** di repository GitHub Anda.
2. Anda akan melihat proses **"Build APK"** berjalan otomatis (otomatis terpicu begitu file di-upload).
3. Tunggu sekitar 2–5 menit sampai muncul tanda centang ✅ hijau (artinya build berhasil).
4. Klik proses build yang sudah selesai tersebut.
5. Scroll ke bawah ke bagian **Artifacts**, klik **app-debug-apk** untuk download.
6. File yang terdownload adalah `.zip` — extract, di dalamnya ada `app-debug.apk`.

### Langkah 4 — Install APK ke HP Android
1. Pindahkan file `app-debug.apk` ke HP Android (lewat kabel USB, Google Drive, WhatsApp ke diri sendiri, dll).
2. Di HP, buka file tersebut. Jika muncul peringatan "Install dari sumber tidak dikenal", izinkan (ini normal untuk APK di luar Play Store).
3. Install seperti biasa.

### Langkah 5 — Berikan izin akses file di HP
1. Buka aplikasi **File Cleaner**.
2. Klik tombol **"Berikan Izin Akses File"**.
3. Anda akan diarahkan ke halaman Settings Android → aktifkan **"Allow access to manage all files"**.
4. Kembali ke aplikasi, klik **"Pindai Sekarang"**.

---

## Jika build APK gagal di GitHub Actions

Klik tab Actions → klik build yang gagal (tanda ❌ merah) → lihat log error berwarna merah. Penyebab paling umum:
- Folder `.github` tidak ikut terupload → pastikan pakai GitHub Desktop (Langkah 2B), bukan upload manual web.
- Jika ada error spesifik, salin pesan error tersebut dan tanyakan kembali ke saya — saya bisa bantu perbaiki.

---

## Struktur Project

```
FileManagerApp/
├── app/
│   ├── build.gradle                 # Dependency aplikasi
│   └── src/main/
│       ├── AndroidManifest.xml      # Permission & deklarasi activity
│       ├── java/com/cleaner/filemanager/
│       │   ├── MainActivity.kt      # Dashboard utama
│       │   ├── FileListActivity.kt  # Daftar file per kategori + hapus
│       │   ├── adapter/             # RecyclerView adapters
│       │   ├── model/               # Data class
│       │   └── util/                # Scanner, formatter ukuran file
│       └── res/                     # Layout, drawable, string, warna
├── .github/workflows/build-apk.yml  # Auto-build APK via GitHub Actions
├── build.gradle / settings.gradle   # Konfigurasi project level
└── gradlew                          # Script build Gradle
```

## Catatan teknis
- Minimum Android 11 (API 30), menggunakan permission `MANAGE_EXTERNAL_STORAGE` (All Files Access) — wajib disetujui manual oleh user di Settings karena ini kebijakan keamanan Android, bukan sesuatu yang bisa di-bypass aplikasi.
- "File besar" didefinisikan sebagai ≥ 50 MB (bisa diubah di `FileScanner.kt`, konstanta `LARGE_FILE_THRESHOLD`).
- Folder bernama `cache` dihitung ukurannya secara keseluruhan (bukan dirinci per file di dalamnya) supaya scan lebih cepat dan hasilnya mirip pembersih cache pada umumnya.
- APK yang dihasilkan workflow ini adalah **debug build** (untuk testing). Jika nanti ingin publish ke Play Store, perlu dibuat **release build** bertanda tangan (signed) — beri tahu saya jika butuh bantuan untuk itu.
