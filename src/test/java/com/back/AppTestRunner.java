package com.back;

import com.back.domain.wiseSaying.repository.WiseSayingDbRepository;
import com.back.global.db.DbConfig;
import com.back.standard.util.TestUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.function.Consumer;

public final class AppTestRunner {

    // 테스트 전용 DB(wise_saying_test). 개발 DB(wise_saying_dev)는 건드리지 않는다.
    public static final DbConfig TEST_DB_CONFIG = DbConfig.load("test");

    // 테스트 전용 빌드 폴더. 실제 실행 폴더(db/wiseSaying)는 건드리지 않는다.
    public static final Path TEST_BUILD_DIR = Path.of("test-db", "wiseSaying");

    public static String run(String input, Consumer<Scanner> runApp) {
        var bytes = TestUtil.setOutToByteArray();
        try (Scanner scanner = TestUtil.genScanner(input)) {
            runApp.accept(scanner);
            System.out.flush();
            return bytes.toString(StandardCharsets.UTF_8);
        } finally {
            TestUtil.clearSetOutToByteArray();
        }
    }

    // CRUD 테스트용: 샘플 없이 시작.
    // 같은 테스트 안에서 run 을 두 번 부르면 = 프로그램을 껐다 다시 켠 것 (같은 DB 를 다시 읽음).
    public static String run(String input) {
        return run(input, scanner -> new App(scanner, TEST_DB_CONFIG, TEST_BUILD_DIR, false).run());
    }

    // 검색·페이징 테스트용: 테이블이 비어 있으면 샘플 명언 10개를 만들고 시작
    public static String runWithSampleData(String input) {
        return run(input, scanner -> new App(scanner, TEST_DB_CONFIG, TEST_BUILD_DIR, true).run());
    }

    // 매 테스트 전에 부른다: 테스트 테이블을 TRUNCATE 해서 번호가 1번부터 시작하게 하고, 빌드 파일도 지운다.
    public static void clearTestDb() {
        new WiseSayingDbRepository(TEST_DB_CONFIG, TEST_BUILD_DIR).truncate();

        try {
            Files.deleteIfExists(TEST_BUILD_DIR.resolve("data.json"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
