# MobileProgramming_20214034_TravelApp
모바일프로그래밍 기말 여행 기록 앱 (학번 20214034)

## 실행 방법
1. Android Studio에서 프로젝트 열기 → Gradle Sync
2. `local.properties`에 Google Maps API 키 추가 (지도 탭용, Git 커밋 금지):
   ```
   MAPS_API_KEY=발급받은_키
   ```
3. Run ▶

## 구현 요약
- SQLiteOpenHelper CRUD (`TravelDbHelper`)
- RecyclerView + Adapter/ViewHolder
- Fragment 2개 (목록 / 지도) + BottomNavigationView + 백스택
- AddEditActivity (추가·수정), DetailActivity (상세)
- 갤러리·카메라 Intent, Glide, 옵션·컨텍스트 메뉴, 삭제 AlertDialog
- 코루틴 + ProgressBar (목록 로딩)
