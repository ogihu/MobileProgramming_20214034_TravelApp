# MobileProgramming_20214034_TravelApp
모바일프로그래밍 기말 여행 기록 앱 (학번 20214034)

## 실행 방법
1. Android Studio → **Sync Project with Gradle Files**
2. `local.properties`에 **카카오 JavaScript 앱 키** 추가 (Git 커밋 금지):
   ```properties
   KAKAO_JAVASCRIPT_KEY=발급받은_JavaScript키
   ```
3. [Kakao Developers](https://developers.kakao.com/) 콘솔 설정
   - **제품 설정 → 카카오맵** 활성화 (ON)
   - **앱 설정 → 플랫폼 → Web** 추가
   - 사이트 도메인: `https://appassets.androidplatform.net`
4. Android Studio → **Sync → Rebuild Project** → Run ▶

### 지도 표시 방식
- **WebView + 카카오맵 JavaScript API** (x86_64 에뮬레이터에서도 동작)
- `MapFragment`만 WebView 방식이며, 목록/DB/사진/GPS 기능은 Kotlin 코드 그대로 유지

## APK 제출
- **Build → Build APK(s)**
- 경로: `app/build/outputs/apk/debug/app-debug.apk`

## 패키지 구조
```
com.example.travelapp_20214034_hero/
├── app/              MainActivity, BottomNavigation
├── data/             TravelItem, TravelDbHelper (SQLiteOpenHelper)
├── ui/
│   ├── list/         HomeFragment, TravelAdapter, TravelViewHolder
│   ├── detail/       DetailActivity
│   ├── addedit/      AddEditActivity
│   └── map/          MapFragment (카카오맵 WebView)
└── common/           TravelExtras, ImageFileHelper, PhotoExifHelper
```

## 구현 기능 체크리스트
| 기능 | 상태 |
|------|------|
| SQLite CRUD (SQLiteOpenHelper) | ✅ |
| RecyclerView + Adapter/ViewHolder | ✅ |
| Fragment 2개 + BottomNavigation + 백스택 | ✅ |
| 옵션 메뉴 3개 + About | ✅ |
| 컨텍스트 메뉴 (목록 롱프레스) | ✅ |
| AddEdit / Detail + Intent | ✅ |
| 갤러리·카메라 + FileProvider | ✅ |
| 사진 내부 저장 (`files/photos`) | ✅ |
| Geocoder 여행지 검색 | ✅ |
| 코루틴 + ProgressBar | ✅ |
| 카카오맵 WebView 마커 | ✅ |
| GPS EXIF → 위도·경도 자동 입력 | ✅ |
| 삭제 시 내부 사진 파일 정리 | ✅ |
| RecyclerView DiffUtil | ✅ |
| Activity Result 목록 갱신 | ✅ |

## 제출
- GitHub URL + APK
- 마감: **6월 15일 23:59** (이후 push 미반영)
# MobileProgramming_20214034_TravelApp
紐⑤컮?쇳봽濡쒓렇?섎컢 湲곕쭚 ?ы뻾 湲곕줉 ??(?숇쾲 20214034)

## ?ㅽ뻾 諛⑸쾿
1. Android Studio ??**Sync Project with Gradle Files**
2. `local.properties`??**移댁뭅??JavaScript ????* (Git 而ㅻ컠 湲덉?):
   ```properties
   KAKAO_JAVASCRIPT_KEY=諛쒓툒諛쏆?_JavaScript??
   ```
3. [Kakao Developers](https://developers.kakao.com/) 肄섏넄 ?ㅼ젙
   - **?쒗뭹 ?ㅼ젙 ??移댁뭅?ㅻ㏊** ?쒖꽦??(ON)
   - **???ㅼ젙 ???뚮옯????Web** 異붽?
   - ?ъ씠???꾨찓?? `https://appassets.androidplatform.net`
4. Android Studio ??**Sync ??Rebuild Project** ??Run ??

### 吏???쒖떆 諛⑹떇
- **WebView + 移댁뭅?ㅻ㏊ JavaScript API** (x86_64 ?먮??덉씠?곗뿉?쒕룄 ?숈옉)
- `MapFragment`留?WebView 諛⑹떇, ?섎㉧吏 ??湲곕뒫? ?숈씪

## APK ?쒖텧
- **Build ??Build APK(s)**
- 寃쎈줈: `app/build/outputs/apk/debug/app-debug.apk`

## ?⑦궎吏 援ъ“ (湲곕뒫蹂?

```
com.example.travelapp_20214034_hero/
?쒋?? app/              MainActivity, BottomNavigation (CH05)
?쒋?? data/             TravelItem, TravelDbHelper (CH11 SQLite)
?쒋?? ui/
??  ?쒋?? list/         HomeFragment, TravelAdapter, ViewHolder (CH05 RecyclerView, CH06 而⑦뀓?ㅽ듃 硫붾돱)
??  ?쒋?? detail/       DetailActivity (CH09 Intent)
??  ?쒋?? addedit/      AddEditActivity (CH04 ?꾩젽, CH09 Intent, Geocoder)
??  ?붴?? map/          MapFragment (移댁뭅?ㅻ㏊ SDK)
?붴?? common/           TravelExtras, ImageFileHelper, PhotoExifHelper (CH06 ?뚯씪쨌EXIF GPS)
```

## 援ы쁽 湲곕뒫 泥댄겕由ъ뒪??
| 湲곕뒫 | ?곹깭 |
|------|------|
| SQLite CRUD (SQLiteOpenHelper) | ??|
| RecyclerView + Adapter/ViewHolder | ??|
| Fragment 2 + BottomNav + 諛깆뒪??| ??|
| ?듭뀡 硫붾돱 3媛?+ About | ??|
| 而⑦뀓?ㅽ듃 硫붾돱 (紐⑸줉 濡깊봽?덉뒪) | ??|
| AddEdit / Detail + Intent | ??|
| 媛ㅻ윭由?룹뭅硫붾씪 + FileProvider | ??|
| ?ъ쭊 ?대? ???(`files/photos`) | ??|
| Geocoder ?ы뻾吏 寃??| ??|
| 肄붾（??+ ProgressBar | ??|
| 移댁뭅?ㅻ㏊ WebView 留덉빱 | ??(JavaScript ?ㅒ톆eb ?꾨찓???ㅼ젙) |
| GPS EXIF ???꽷룰꼍???먮룞 ?낅젰 | ??|
| ??젣 ???대? ?ъ쭊 ?뚯씪 ?뺣━ | ??|
| RecyclerView DiffUtil | ??|
| Activity Result 紐⑸줉 媛깆떊 | ??|

## res 由ъ냼??(?대쫫 洹쒖튃)
| ?뚯씪 | 湲곕뒫 |
|------|------|
| `activity_main.xml` | 硫붿씤 ??|
| `fragment_home.xml` | ?ы뻾 紐⑸줉 |
| `item_travel.xml` | 紐⑸줉 移대뱶 |
| `activity_add_edit.xml` | 異붽?쨌?섏젙 |
| `activity_detail.xml` | ?곸꽭 |
| `fragment_map.xml` | 吏??|
| `menu_main.xml` | ?듭뀡 硫붾돱 |
| `menu_travel_context.xml` | 而⑦뀓?ㅽ듃 硫붾돱 |
| `menu_bottom_nav.xml` | ?섎떒 ??|

## Git 而ㅻ컠 ?덉떆 (湲곕뒫蹂?遺꾨━ ??
```bash
git add app/src/main/java/.../data/
git commit -m "feat: data ?⑦궎吏 SQLite CRUD (CH11)"

git add app/src/main/java/.../ui/list/
git commit -m "refactor: ui/list 紐⑸줉 RecyclerView 紐⑤뱢"

git add app/src/main/java/.../ui/detail/ app/src/main/java/.../ui/addedit/
git commit -m "refactor: ui/detail, ui/addedit Activity 遺꾨━"

git add app/src/main/java/.../app/ app/src/main/AndroidManifest.xml
git commit -m "refactor: app MainActivity 諛?Manifest 寃쎈줈 ?뺣━"

git add app/src/main/java/.../common/
git commit -m "refactor: common ?⑦궎吏 (Extras, ImageFileHelper)"
```

## ?쒖텧
- GitHub URL + APK
- 留덇컧: **6??15??23:59** (?댄썑 push 誘몃컲??
