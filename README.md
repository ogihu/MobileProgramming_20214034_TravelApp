20214034 정영웅 모바일프로그래밍 기말 텀프로젝트

------------------------------------------------------

개요

여행지, 날짜, 메모, 사진, 위치 정보를 저장하고 지도에서 확인할 수 있는 여행 기록 앱

------------------------------------------------------

사용 기술

- Kotlin
- Android XML UI
- SQLiteOpenHelper
- RecyclerView, ListAdapter, DiffUtil
- Fragment, BottomNavigationView
- ViewBinding
- Coroutine
- Glide
- FileProvider
- Geocoder
- EXIF GPS
- WebView
- Kakao Map JavaScript API

------------------------------------------------------

교수님 조건 확인

- minSdk 26 이상 적용
- Kotlin 기반 Android 앱
- XML 기반 화면 구성
- SQLiteOpenHelper 사용
- Room, Firebase 미사용
- RecyclerView 목록 구현
- Fragment와 BottomNavigationView 사용
- Option Menu, Context Menu 사용
- 갤러리, 카메라, FileProvider 사용
- Kakao Map API 연동

------------------------------------------------------

구현 기능

- 여행 기록 추가, 수정, 삭제
- SQLiteOpenHelper 기반 데이터 저장
- 여행 목록 검색
- 홈 통계 카드 표시
- 최근 여행 칩, 지도 표시 가능 칩 표시
- 갤러리 및 카메라 사진 등록
- 사진 내부 저장 및 이미지 최적화
- 사진 EXIF GPS 정보로 위도/경도 자동 입력
- Geocoder를 이용한 여행지 좌표 검색
- 위도/경도 범위 검증
- 상세 화면 공유 기능
- 상세 화면에서 외부 지도 앱 열기
- 삭제 Undo 기능
- Kakao Map WebView 마커 표시
- 여행 앱 스타일 홈, 상세, 추가/수정 UI

------------------------------------------------------

프로젝트 구조

com.example.travelapp_20214034_hero
- app        MainActivity, Application
- data       SQLiteOpenHelper, TravelItem
- common     이미지 저장, EXIF, 공통 Intent key
- ui
  - list      홈, 목록, 검색, 삭제 Undo
  - addedit   여행 추가/수정
  - detail    여행 상세
  - map       Kakao Map WebView

------------------------------------------------------

실행 설정

local.properties에 Kakao JavaScript 키를 추가

KAKAO_JAVASCRIPT_KEY=발급받은_JavaScript_키

Kakao Developers의 Web 플랫폼에 아래 도메인을 등록

https://appassets.androidplatform.net

------------------------------------------------------

APK 경로

app/build/outputs/apk/debug/app-debug.apk

------------------------------------------------------

제출 파일

- GitHub 프로젝트 URL
- app-debug.apk
