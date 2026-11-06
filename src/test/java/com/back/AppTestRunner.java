package com.back;

import com.back.standard.util.TestUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class AppTestRunner {

    // 테스트 전용 저장 폴더. 실제 실행 폴더(db/wiseSaying)는 건드리지 않는다.
    public static final Path TEST_DB_DIR = Path.of("test-db", "wiseSaying");

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
    // 같은 테스트 안에서 run 을 두 번 부르면 = 프로그램을 껐다 다시 켠 것 (같은 폴더를 다시 읽음).
    public static String run(String input) {
        return run(input, scanner -> new App(scanner, TEST_DB_DIR, false).run());
    }

    // 검색·페이징 테스트용: 폴더가 비어 있으면 샘플 명언 10개를 만들고 시작
    public static String runWithSampleData(String input) {
        return run(input, scanner -> new App(scanner, TEST_DB_DIR, true).run());
    }

    // 테스트 전용 폴더를 통째로 지운다. 매 테스트 전에 불러서 번호가 1번부터 시작하게 한다.
    // 폴더 안 파일을 먼저 지워야 폴더가 지워지므로, 깊은 경로부터(역순) 지운다.
    public static void clearTestDb() {
        if (!Files.exists(TEST_DB_DIR)) return;

        try (Stream<Path> paths = Files.walk(TEST_DB_DIR)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(AppTestRunner::deleteFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteFile(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
