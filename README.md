# PureClip - Android Video Downloader App

<p align="center">
  <b>Ứng dụng tải video không logo chất lượng cao từ TikTok, Facebook, Instagram dành cho Android.</b>
</p>

---

## Tính năng

- **Tải video không watermark**: Hỗ trợ bóc tách link và tải video HD từ TikTok, Facebook, Instagram không dính logo.
- **Tích hợp AdMob & Analytics**: Tải quảng cáo Banner, Interstitial Ad ngầm không gây giật lag và theo dõi chỉ số với Firebase Analytics.

---

## Công nghệ & Kiến trúc

- **Ngôn ngữ**: [Kotlin](https://kotlinlang.org/)
- **Kiến trúc**: MVVM (Model-View-ViewModel)
- **Bất đồng bộ**: Kotlin Coroutines
- **Mạng (Networking)**: Retrofit
- **Quản lý file**: Android System `DownloadManager`
- **Quảng cáo**: Google Mobile Ads SDK (AdMob)
- **Phân tích**: Firebase Analytics (BoM)

---


## Hướng dẫn cài đặt & Thiết lập

### 1. Yêu cầu hệ thống
- Android Studio Jellyfish / Ladybug (2024.1+) trở lên
- JDK 17
- Android SDK Min: API 24 (Android 7.0+) | Target: API 37

### 2. Cấu hình `local.properties`
Tạo hoặc mở file `local.properties` ở thư mục gốc của dự án và thêm các thông số:

```properties
sdk.dir=/path/to/your/android/sdk

# AdMob App ID & Ad Unit IDs (Test IDs)
ADMOB_APP_ID=ca-app-pub-3940256099942544~3347511713
APP_OPEN_ID=ca-app-pub-3940256099942544/9257395921
BANNER_MAIN_ID=ca-app-pub-3940256099942544/6300978111
ADAPTIVE_BANNER_ID=ca-app-pub-3940256099942544/9214589741
INTERSTITIAL_DOWNLOAD_ID=ca-app-pub-3940256099942544/1033173712
```

### 3. Cấu hình Firebase Analytics
1. Tải file `google-services.json` từ dự án Firebase Console của bạn (với package name `com.pureclip.app`).
2. Đặt file `google-services.json` vào thư mục `app/` (Tham khảo mẫu tại `app/google-services.json.example`).

---

## Biên dịch & Chạy dự án (Build & Run)

Mở Terminal tại thư mục gốc và chạy lệnh:

```bash
# Biên dịch phiên bản Debug
./gradlew assembleDebug

# Chạy Unit Tests
./gradlew test
```

---

## Giấy phép (License)

Dự án được phân phối dưới giấy phép **MIT License**.
