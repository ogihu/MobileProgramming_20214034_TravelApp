# MobileProgramming_20214034_TravelApp
모바일프로그래밍 기말 여행 기록 앱 (학번 20214034)

## 실행 방법
1. Android Studio에서 프로젝트 열기 → **Sync Project with Gradle Files**
2. `local.properties`에 Google Maps API 키 추가 (**Git 커밋 금지**):
   ```properties
   MAPS_API_KEY=발급받은_키
   ```
3. Maps SDK for Android 사용 설정 + API 키에 **패키지명·SHA-1** 제한 등록:
   ```bash
   gradlew signingReport
   ```
   - 패키지: `com.example.travelapp_20214034_hero`
4. Run ▶ (에뮬레이터 또는 실기기)

## APK 빌드 (제출용)
- **Build → Build Bundle(s) / APK(s) → Build APK(s)**
- 출력: `app/build/outputs/apk/debug/app-debug.apk`

## 구현 요약 (수업·기말 대응)
| 항목 | 구현 |
|------|------|
| CH11 SQLiteOpenHelper CRUD | `data/TravelDbHelper.kt` |
| CH05 RecyclerView + Adapter/ViewHolder | `ui/home/` |
| CH05 Fragment + BottomNavigation | `HomeFragment`, `MapFragment` |
| CH06 옵션 메뉴 | `menu_main.xml`, `MainActivity` |
| CH06 컨텍스트 메뉴 | `registerForContextMenu` + `onContextItemSelected` |
| CH06 AlertDialog | 삭제 확인 |
| CH06 파일 처리 | `util/ImageFileHelper.kt` (내부 저장) |
| CH09 Intent | Activity 전환, 갤러리/카메라 |
| CH10 코루틴 | DB/저장/검색 IO + ProgressBar |
| 지도 API | `MapFragment` + Geocoder 여행지 검색 |

## 폴더 구조
```
data/          SQLite (CH11)
ui/home/       RecyclerView, 컨텍스트 메뉴
ui/map/        Google Maps
util/          사진 내부 저장
MainActivity   Fragment + 옵션 메뉴
AddEditActivity  추가·수정·Geocoder
DetailActivity   상세
```

## 제출
- GitHub Repository URL
- APK (또는 zip / 이메일)
- 마감: **6월 15일 23:59** (이후 커밋 미반영)
