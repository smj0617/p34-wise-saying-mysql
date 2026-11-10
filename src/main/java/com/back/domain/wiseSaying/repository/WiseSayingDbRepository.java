package com.back.domain.wiseSaying.repository;

import com.back.domain.wiseSaying.entity.WiseSaying;
import com.back.global.db.DbConfig;
import com.back.standard.dto.Page;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 명언을 MySQL 의 wise_saying 테이블에 저장한다.
// JDBC·SQL 코드는 이 클래스에만 있다. 컨트롤러·서비스는 DB 를 모르고 WiseSayingRepository 만 본다.
//   - 번호: AUTO_INCREMENT. 지운 번호는 다시 쓰지 않고, 재시작해도 이어진다.
//   - 검색·페이징: WHERE ... LIKE, ORDER BY id DESC, LIMIT/OFFSET 으로 DB 가 계산한다.
//   - 빌드: DB 의 모든 명언을 buildDir/data.json 으로 내보낸다.
public class WiseSayingDbRepository implements WiseSayingRepository {

    private static final String SCHEMA_FILE = "schema.sql";
    private static final String BUILD_FILE_NAME = "data.json";
    private static final String COLUMNS = "id, content, author";

    private final DbConfig dbConfig;
    private final Path buildDir;

    public WiseSayingDbRepository(DbConfig dbConfig, Path buildDir) {
        this.dbConfig = dbConfig;
        this.buildDir = buildDir;
        createTableIfNotExists();
    }

    @Override
    public WiseSaying save(WiseSaying wiseSaying) {
        if (wiseSaying.isNew()) {
            int newId = (int) insert("""
                    INSERT INTO wise_saying
                    SET createDate = NOW(),
                        modifyDate = NOW(),
                        content = ?,
                        author = ?
                    """, wiseSaying.getContent(), wiseSaying.getAuthor());
            wiseSaying.setId(newId);
            return wiseSaying;
        }

        update("""
                UPDATE wise_saying
                SET modifyDate = NOW(),
                    content = ?,
                    author = ?
                WHERE id = ?
                """, wiseSaying.getContent(), wiseSaying.getAuthor(), wiseSaying.getId());
        return wiseSaying;
    }

    @Override
    public List<WiseSaying> findAll() {
        return selectList("SELECT " + COLUMNS + " FROM wise_saying ORDER BY id ASC");
    }

    @Override
    public Optional<WiseSaying> findById(int id) {
        return selectList("SELECT " + COLUMNS + " FROM wise_saying WHERE id = ?", id)
                .stream()
                .findFirst();
    }

    @Override
    public boolean delete(int id) {
        return update("DELETE FROM wise_saying WHERE id = ?", id) > 0;
    }

    @Override
    public int count() {
        return (int) selectLong("SELECT COUNT(*) FROM wise_saying");
    }

    // 인터페이스의 default(findAll 후 자바에서 거르기) 대신 SQL 로 검색·페이징한다.
    @Override
    public Page<WiseSaying> findForList(String keywordType, String keyword, int page, int pageSize) {
        SearchCondition cond = SearchCondition.of(keywordType, keyword);

        int totalCount = (int) selectLong("SELECT COUNT(*) FROM wise_saying WHERE " + cond.where(), cond.params());

        List<Object> params = new ArrayList<>(List.of(cond.params()));
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        List<WiseSaying> content = selectList(
                "SELECT " + COLUMNS + " FROM wise_saying WHERE " + cond.where() + " ORDER BY id DESC LIMIT ? OFFSET ?",
                params.toArray()
        );

        return new Page<>(content, totalCount, page, pageSize);
    }

    @Override
    public void build() {
        Path buildFile = buildDir.resolve(BUILD_FILE_NAME);

        try {
            Files.createDirectories(buildDir);
            Files.writeString(buildFile, WiseSayingJson.toJsonArray(findAll()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // 테스트용: 테이블을 비우고 AUTO_INCREMENT 도 1부터 다시 시작한다.
    public void truncate() {
        update("TRUNCATE wise_saying");
    }

    // ── 검색 조건 ─────────────────────────────

    // keywordType 에 따라 WHERE 절과 바인딩 값을 만든다. 검색어가 없으면 모두 통과(1 = 1).
    private record SearchCondition(String where, Object[] params) {

        static SearchCondition of(String keywordType, String keyword) {
            if (keyword.isEmpty()) return new SearchCondition("1 = 1", new Object[0]);

            String like = escapeLike(keyword);

            return switch (keywordType) {
                case "author" -> new SearchCondition("author LIKE CONCAT('%', ?, '%')", new Object[]{like});
                case "content" -> new SearchCondition("content LIKE CONCAT('%', ?, '%')", new Object[]{like});
                default -> new SearchCondition(
                        "(content LIKE CONCAT('%', ?, '%') OR author LIKE CONCAT('%', ?, '%'))",
                        new Object[]{like, like}
                );
            };
        }

        // 검색어에 든 % _ \ 는 LIKE 의 특수 문자가 아니라 글자 그대로 찾도록 \ 를 붙인다.
        private static String escapeLike(String keyword) {
            return keyword
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
        }
    }

    // ── JDBC ─────────────────────────────
    // 콘솔 앱이라 요청이 드물어서, 작업마다 커넥션을 열고 바로 닫는다.

    private void createTableIfNotExists() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(SCHEMA_FILE)) {
            if (in == null) throw new IllegalStateException(SCHEMA_FILE + " 을 찾을 수 없습니다.");

            update(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbConfig.jdbcUrl(), dbConfig.username(), dbConfig.password());
    }

    private long insert(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(stmt, params);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL 실행 실패: " + sql, e);
        }
    }

    private int update(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bind(stmt, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("SQL 실행 실패: " + sql, e);
        }
    }

    private List<WiseSaying> selectList(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bind(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                List<WiseSaying> wiseSayings = new ArrayList<>();
                while (rs.next()) {
                    wiseSayings.add(new WiseSaying(rs.getInt("id"), rs.getString("content"), rs.getString("author")));
                }
                return wiseSayings;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL 실행 실패: " + sql, e);
        }
    }

    private long selectLong(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bind(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL 실행 실패: " + sql, e);
        }
    }

    private void bind(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
}
