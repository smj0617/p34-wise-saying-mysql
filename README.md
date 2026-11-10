# 명언 앱 ⑧ MySQL 전환 (15단계)

명언 앱 ⑦(파일 영속성)에서 이어서, 명언을 파일 대신 **MySQL** 에 저장하도록 리포지터리 구현체만 갈아 끼웠습니다.

## 1. MySQL 띄우기

```bash
docker run --name mysql-1 -p 127.0.0.1:3306:3306 -e TZ=Asia/Seoul -e MYSQL_ROOT_PASSWORD=<비밀번호> -d mysql:8.4
```

## 2. DB 만들기 (개발 DB · 테스트 DB 분리)

```bash
docker exec mysql-1 mysql -uroot -p<비밀번호> -e "
CREATE DATABASE IF NOT EXISTS wise_saying_dev  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS wise_saying_test CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

| 용도 | DB 이름 | 쓰는 곳 |
|---|---|---|
| 개발(실제 실행) | `wise_saying_dev` | `Main` → `new App(sc)` |
| 테스트 | `wise_saying_test` | `AppTestRunner` (매 테스트 전 `TRUNCATE`) |

## 3. 테이블

테이블 생성 SQL 은 [`src/main/resources/schema.sql`](src/main/resources/schema.sql) 에 있습니다.
`WiseSayingDbRepository` 가 시작할 때 이 SQL 을 실행하므로(`CREATE TABLE IF NOT EXISTS`) 따로 만들지 않아도 됩니다.

```sql
CREATE TABLE IF NOT EXISTS wise_saying (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    createDate DATETIME NOT NULL,
    modifyDate DATETIME NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
```

- 번호는 `AUTO_INCREMENT` 라서 지운 번호를 다시 쓰지 않고, 재시작해도 이어집니다.

## 4. 접속 정보 (소스에 비밀번호 하드코딩 없음)

`DbConfig.load(profile)` 가 아래 순서로 읽고, 뒤의 값이 앞의 값을 덮어씁니다.

1. `src/main/resources/db.properties` — host·port·username·profile 별 DB 이름 (git 에 포함, 비밀번호 없음)
2. 프로젝트 루트의 `db.local.properties` — 비밀번호 등 내 PC 전용 값 (**`.gitignore` 대상**)
3. 환경변수 `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`, `DB_NAME`

```bash
cp db.local.properties.example db.local.properties   # 그리고 db.password= 에 비밀번호 입력
# 또는
export DB_PASSWORD=<비밀번호>
```

## 5. 실행 · 테스트

```bash
./gradlew run     # 개발 DB 로 실행
./gradlew test    # 테스트 DB 로 통합 테스트
```

## 6. 리포지터리를 어떻게 갈아 끼웠나

```
WiseSayingController ─▶ WiseSayingService ─▶ WiseSayingRepository (인터페이스)
                                               ├─ WiseSayingFileRepository  (⑦ 파일)
                                               └─ WiseSayingDbRepository    (⑧ MySQL)  ◀ 지금 사용
```

- 컨트롤러·서비스 코드는 **바꾸지 않았습니다.** 둘 다 `WiseSayingRepository` 인터페이스만 압니다.
- `App` 생성자에서 만드는 구현체 한 줄을 `new WiseSayingFileRepository(dbDir)` → `new WiseSayingDbRepository(dbConfig, buildDir)` 로 바꿨습니다.
  (그래서 `App` 생성자 인자가 저장 폴더 → DB 접속 정보 + 빌드 폴더로 바뀌었습니다.)
- JDBC·SQL 코드는 `WiseSayingDbRepository` 에만 있습니다.
- 검색·페이징은 인터페이스의 default 메서드(자바에서 거르기)를 오버라이드해 **SQL 로 처리**합니다.
  - `WHERE content LIKE CONCAT('%', ?, '%')` (검색어의 `%` `_` `\` 는 이스케이프)
  - `ORDER BY id DESC LIMIT ? OFFSET ?` + `SELECT COUNT(*)` 로 전체 페이지 수 계산
- 빌드(`빌드` 명령)는 DB 의 모든 명언을 `db/wiseSaying/data.json`(테스트는 `test-db/wiseSaying/data.json`)으로 내보냅니다.

## 7. 테스트

- `WiseSayingControllerTest` — 기존 통합 테스트(등록·목록·삭제·수정·검색·페이징) 그대로 통과
- `WiseSayingDbTest` — DB 저장, 재시작 후 유지·번호 이어짐, 삭제 번호 재사용 안 함, 수정 반영, 특수문자, LIKE 이스케이프, `data.json` 빌드
- 매 테스트 전에 `AppTestRunner.clearTestDb()` 가 테스트 DB 의 `wise_saying` 을 `TRUNCATE` 합니다.
