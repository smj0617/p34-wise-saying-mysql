package com.back.domain.wiseSaying.repository;

import com.back.domain.wiseSaying.entity.WiseSaying;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// 명언 하나를 파일 하나로 저장한다.
//   <dbDir>/1.json, <dbDir>/2.json ...  ← 명언
//   <dbDir>/lastId.txt                   ← 마지막으로 만든 번호
//   <dbDir>/data.json                    ← 빌드 결과 (모든 명언을 배열로)
// 파일을 다루는 코드는 이 클래스에만 있다. 컨트롤러·서비스는 파일을 모른다.
public class WiseSayingFileRepository implements WiseSayingRepository {

    private static final String LAST_ID_FILE_NAME = "lastId.txt";
    private static final String BUILD_FILE_NAME = "data.json";

    private final Path dbDir;

    // 실제 실행은 db/wiseSaying, 테스트는 테스트 전용 폴더를 넘긴다.
    public WiseSayingFileRepository(Path dbDir) {
        this.dbDir = dbDir;
    }

    @Override
    public WiseSaying save(WiseSaying wiseSaying) {
        if (wiseSaying.isNew()) {
            int newId = getLastId() + 1;
            wiseSaying.setId(newId);
            saveLastId(newId);
        }

        // 등록이든 수정이든 {번호}.json 을 통째로 다시 쓴다.
        writeFile(wiseSayingFile(wiseSaying.getId()), WiseSayingJson.toJson(wiseSaying));
        return wiseSaying;
    }

    @Override
    public List<WiseSaying> findAll() {
        if (!Files.exists(dbDir)) return List.of();

        try (Stream<Path> files = Files.list(dbDir)) {
            return files
                    .filter(this::isWiseSayingFile)
                    .map(file -> WiseSayingJson.fromJson(readFile(file)))
                    .sorted(Comparator.comparingInt(WiseSaying::getId))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<WiseSaying> findById(int id) {
        Path file = wiseSayingFile(id);
        if (!Files.exists(file)) return Optional.empty();

        return Optional.of(WiseSayingJson.fromJson(readFile(file)));
    }

    // 파일만 지운다. lastId.txt 는 그대로 두므로 지운 번호는 다시 쓰이지 않는다.
    @Override
    public boolean delete(int id) {
        try {
            return Files.deleteIfExists(wiseSayingFile(id));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int count() {
        return findAll().size();
    }

    @Override
    public void build() {
        writeFile(dbDir.resolve(BUILD_FILE_NAME), WiseSayingJson.toJsonArray(findAll()));
    }

    // ── 파일 이름 규칙 ─────────────────────────────

    private Path wiseSayingFile(int id) {
        return dbDir.resolve(id + ".json");
    }

    // "3.json" 처럼 숫자로만 된 이름만 명언 파일로 본다. (data.json, lastId.txt 는 제외)
    private boolean isWiseSayingFile(Path file) {
        return file.getFileName().toString().matches("\\d+\\.json");
    }

    // ── 마지막 번호 ─────────────────────────────

    // 파일이 없으면 아직 한 번도 등록하지 않은 것 → 0
    private int getLastId() {
        Path file = dbDir.resolve(LAST_ID_FILE_NAME);
        if (!Files.exists(file)) return 0;

        return Integer.parseInt(readFile(file).trim());
    }

    private void saveLastId(int lastId) {
        writeFile(dbDir.resolve(LAST_ID_FILE_NAME), String.valueOf(lastId));
    }

    // ── 파일 읽기·쓰기 (UTF-8) ─────────────────────────────

    private String readFile(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // 폴더가 없으면 먼저 만들고 쓴다. 같은 이름의 파일이 있으면 덮어쓴다.
    private void writeFile(Path file, String text) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
