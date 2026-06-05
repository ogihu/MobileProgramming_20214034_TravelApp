# MobileProgramming_20214034_TravelApp
모바일프로그래밍 기말 여행 기록 앱 (학번 20214034)

## 실행 방법
1. Android Studio → **Sync Project with Gradle Files**
2. `local.properties`에 **카카오 네이티브 앱 키** (Git 커밋 금지):
   ```properties
   KAKAO_NATIVE_APP_KEY=발급받은_네이티브앱키
   ```
3. [Kakao Developers](https://developers.kakao.com/) 콘솔 설정 (스크린샷 기준 앱: 모바일프로그래밍기말정영웅)
   - **앱 설정 → 플랫폼 → Android** 추가
   - 패키지명: `com.example.travelapp_20214034_hero`
   - 키 해시 (이 PC debug): `EJjb8E/YxiaBuOOuokzQlttO4WY=`
   - 앱 지도 탭 오류 화면에도 동일 값이 표시됨
4. **제품 설정 → 카카오맵** 활성화
5. 네이티브 앱 키: `300e9c1bc71739828edbba12bc49230c` (`local.properties`에만, Git 금지)
6. Android Studio → **Sync → Rebuild Project** → Run ▶

## APK 제출
- **Build → Build APK(s)**
- 경로: `app/build/outputs/apk/debug/app-debug.apk`

## 패키지 구조 (기능별)

```
com.example.travelapp_20214034_hero/
├── app/              MainActivity, BottomNavigation (CH05)
├── data/             TravelItem, TravelDbHelper (CH11 SQLite)
├── ui/
│   ├── list/         HomeFragment, TravelAdapter, ViewHolder (CH05 RecyclerView, CH06 컨텍스트 메뉴)
│   ├── detail/       DetailActivity (CH09 Intent)
│   ├── addedit/      AddEditActivity (CH04 위젯, CH09 Intent, Geocoder)
│   └── map/          MapFragment (카카오맵 SDK)
└── common/           TravelExtras, ImageFileHelper, PhotoExifHelper (CH06 파일·EXIF GPS)
```

## 구현 기능 체크리스트
| 기능 | 상태 |
|------|------|
| SQLite CRUD (SQLiteOpenHelper) | ✅ |
| RecyclerView + Adapter/ViewHolder | ✅ |
| Fragment 2 + BottomNav + 백스택 | ✅ |
| 옵션 메뉴 3개 + About | ✅ |
| 컨텍스트 메뉴 (목록 롱프레스) | ✅ |
| AddEdit / Detail + Intent | ✅ |
| 갤러리·카메라 + FileProvider | ✅ |
| 사진 내부 저장 (`files/photos`) | ✅ |
| Geocoder 여행지 검색 | ✅ |
| 코루틴 + ProgressBar | ✅ |
| 카카오맵 마커 | ✅ (키·콘솔 설정 필요) |
| GPS EXIF → 위·경도 자동 입력 | ✅ |
| 삭제 시 내부 사진 파일 정리 | ✅ |
| RecyclerView DiffUtil | ✅ |
| Activity Result 목록 갱신 | ✅ |

## res 리소스 (이름 규칙)
| 파일 | 기능 |
|------|------|
| `activity_main.xml` | 메인 탭 |
| `fragment_home.xml` | 여행 목록 |
| `item_travel.xml` | 목록 카드 |
| `activity_add_edit.xml` | 추가·수정 |
| `activity_detail.xml` | 상세 |
| `fragment_map.xml` | 지도 |
| `menu_main.xml` | 옵션 메뉴 |
| `menu_travel_context.xml` | 컨텍스트 메뉴 |
| `menu_bottom_nav.xml` | 하단 탭 |

## Git 커밋 예시 (기능별 분리 시)
```bash
git add app/src/main/java/.../data/
git commit -m "feat: data 패키지 SQLite CRUD (CH11)"

git add app/src/main/java/.../ui/list/
git commit -m "refactor: ui/list 목록 RecyclerView 모듈"

git add app/src/main/java/.../ui/detail/ app/src/main/java/.../ui/addedit/
git commit -m "refactor: ui/detail, ui/addedit Activity 분리"

git add app/src/main/java/.../app/ app/src/main/AndroidManifest.xml
git commit -m "refactor: app MainActivity 및 Manifest 경로 정리"

git add app/src/main/java/.../common/
git commit -m "refactor: common 패키지 (Extras, ImageFileHelper)"
```

## 제출
- GitHub URL + APK
- 마감: **6월 15일 23:59** (이후 push 미반영)
