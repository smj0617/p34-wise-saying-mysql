package com.back;

import com.back.domain.wiseSaying.entity.WiseSaying;
import com.back.domain.wiseSaying.repository.WiseSayingDbRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static com.back.AppTestRunner.TEST_BUILD_DIR;
import static com.back.AppTestRunner.TEST_DB_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

// 15단계(MySQL 영속성)·10단계(빌드) 테스트.
// 한 테스트 안에서 AppTestRunner.run 을 두 번 부르면 "프로그램을 껐다 다시 켠 것"과 같다.
// 매 테스트 전에 테스트 DB 의 wise_saying 테이블을 TRUNCATE 한다.
class WiseSayingDbTest {

    @BeforeEach
    void clearTestDb() {
        AppTestRunner.clearTestDb();
    }

    // DB 에 실제로 무엇이 들어 있는지는 리포지터리로 직접 확인한다.
    private WiseSayingDbRepository repository() {
        return new WiseSayingDbRepository(TEST_DB_CONFIG, TEST_BUILD_DIR);
    }

    private String readBuildFile() throws IOException {
        return Files.readString(TEST_BUILD_DIR.resolve("data.json"));
    }

    @Test
    @DisplayName("등록하면 wise_saying 테이블에 id·content·author 가 저장된다")
    void t1() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                종료
                """);

        WiseSaying wiseSaying = repository().findById(1).orElseThrow();

        assertThat(wiseSaying.getContent()).isEqualTo("현재를 사랑하라.");
        assertThat(wiseSaying.getAuthor()).isEqualTo("작자미상");
        assertThat(repository().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("재시작해도 명언이 목록에 남는다")
    void t2() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                등록
                과거에 집착하지 마라.
                작자미상
                종료
                """);

        String out = AppTestRunner.run("""
                목록
                종료
                """);

        assertThat(out).containsSubsequence(
                "2 / 작자미상 / 과거에 집착하지 마라.",
                "1 / 작자미상 / 현재를 사랑하라."
        );
    }

    @Test
    @DisplayName("재시작 후 등록하면 번호가 이어진다")
    void t3() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                종료
                """);

        String out = AppTestRunner.run("""
                등록
                과거에 집착하지 마라.
                작자미상
                종료
                """);

        assertThat(out).contains("2번 명언이 등록되었습니다.");
    }

    @Test
    @DisplayName("수정하면 DB 가 갱신되고, 재시작해도 수정된 내용이 보인다")
    void t4() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                수정?id=1
                현재와 자신을 사랑하라.
                홍길동
                종료
                """);

        WiseSaying wiseSaying = repository().findById(1).orElseThrow();
        assertThat(wiseSaying.getContent()).isEqualTo("현재와 자신을 사랑하라.");
        assertThat(wiseSaying.getAuthor()).isEqualTo("홍길동");

        String out = AppTestRunner.run("""
                목록
                종료
                """);

        assertThat(out)
                .contains("1 / 홍길동 / 현재와 자신을 사랑하라.")
                .doesNotContain("1 / 작자미상 / 현재를 사랑하라.");
    }

    @Test
    @DisplayName("삭제하면 DB 에서도 지워지고, 재시작해도 목록에 없다")
    void t5() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                등록
                과거에 집착하지 마라.
                작자미상
                삭제?id=1
                종료
                """);

        assertThat(repository().findById(1)).isEmpty();
        assertThat(repository().findById(2)).isPresent();

        String out = AppTestRunner.run("""
                목록
                종료
                """);

        assertThat(out)
                .contains("2 / 작자미상 / 과거에 집착하지 마라.")
                .doesNotContain("1 / 작자미상 / 현재를 사랑하라.");
    }

    @Test
    @DisplayName("마지막 번호를 삭제하고 재시작해도, 삭제한 번호는 다시 쓰지 않는다")
    void t6() {
        AppTestRunner.run("""
                등록
                현재를 사랑하라.
                작자미상
                등록
                과거에 집착하지 마라.
                작자미상
                삭제?id=2
                종료
                """);

        String out = AppTestRunner.run("""
                등록
                미래를 꿈꿔라.
                작자미상
                종료
                """);

        assertThat(out)
                .contains("3번 명언이 등록되었습니다.")
                .doesNotContain("2번 명언이 등록되었습니다.");
    }

    @Test
    @DisplayName("명언에 따옴표·역슬래시가 있어도 저장 후 그대로 읽힌다")
    void t7() {
        AppTestRunner.run("""
                등록
                "나"는 C:\\temp 에 있다.
                작자미상
                종료
                """);

        String out = AppTestRunner.run("""
                목록
                종료
                """);

        assertThat(out).contains("1 / 작자미상 / \"나\"는 C:\\temp 에 있다.");
    }

    @Test
    @DisplayName("검색어의 % _ 는 LIKE 특수 문자가 아니라 글자 그대로 찾는다")
    void t8() {
        String out = AppTestRunner.run("""
                등록
                할인율 100%
                작자미상
                등록
                할인율 100
                작자미상
                목록?keywordType=content&keyword=100%
                종료
                """);

        assertThat(out)
                .contains("1 / 작자미상 / 할인율 100%")
                .doesNotContain("2 / 작자미상 / 할인율 100\n");
    }

    @Test
    @DisplayName("빌드: DB 의 모든 명언을 배열 형태의 data.json 으로 만들고 안내를 출력한다")
    void t9() throws IOException {
        String out = AppTestRunner.run("""
                등록
                명언 1
                작가 1
                등록
                명언 2
                작가 2
                빌드
                종료
                """);

        assertThat(out).contains("data.json 파일의 내용이 갱신되었습니다.");
        assertThat(readBuildFile()).isEqualToIgnoringWhitespace("""
                [
                  {
                    "id": 1,
                    "content": "명언 1",
                    "author": "작가 1"
                  },
                  {
                    "id": 2,
                    "content": "명언 2",
                    "author": "작가 2"
                  }
                ]
                """);
    }

    @Test
    @DisplayName("빌드: 삭제한 명언은 data.json 에 들어가지 않는다")
    void t10() throws IOException {
        AppTestRunner.run("""
                등록
                명언 1
                작가 1
                등록
                명언 2
                작가 2
                삭제?id=1
                빌드
                종료
                """);

        assertThat(readBuildFile())
                .contains("\"content\": \"명언 2\"")
                .doesNotContain("\"content\": \"명언 1\"");
    }

    @Test
    @DisplayName("테스트는 개발 DB 가 아닌 테스트 전용 DB·폴더를 쓴다")
    void t11() {
        assertThat(TEST_DB_CONFIG.dbName()).isEqualTo("wise_saying_test");
        assertThat(TEST_BUILD_DIR).isNotEqualTo(App.BUILD_DIR);
    }
}
