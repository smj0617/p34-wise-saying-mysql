package com.back.global.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

// DB 접속 정보. 소스에 비밀번호를 쓰지 않고, 아래 순서로 읽어 뒤에 읽은 값이 앞의 값을 덮어쓴다.
//   1. classpath 의 db.properties        ← host·port·username·profile 별 DB 이름 (git 에 올림)
//   2. 프로젝트 루트의 db.local.properties ← 비밀번호 등 내 PC 전용 값 (git 에 올리지 않음)
//   3. 환경변수 DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD, DB_NAME
public record DbConfig(String host, int port, String dbName, String username, String password) {

    private static final String CLASSPATH_FILE = "db.properties";
    private static final Path LOCAL_FILE = Path.of("db.local.properties");

    // profile: "dev"(실제 실행) 또는 "test"(테스트)
    public static DbConfig load(String profile) {
        Properties props = new Properties();
        loadClasspath(props);
        loadLocalFile(props);

        return new DbConfig(
                read(props, "DB_HOST", "db.host", "localhost"),
                Integer.parseInt(read(props, "DB_PORT", "db.port", "3306")),
                read(props, "DB_NAME", "db." + profile + ".name", null),
                read(props, "DB_USERNAME", "db.username", "root"),
                read(props, "DB_PASSWORD", "db.password", "")
        );
    }

    public String jdbcUrl() {
        return "jdbc:mysql://%s:%d/%s".formatted(host, port, dbName);
    }

    // 로그 등에 찍혀도 비밀번호가 보이지 않게 한다.
    @Override
    public String toString() {
        return "DbConfig[url=%s, username=%s]".formatted(jdbcUrl(), username);
    }

    private static String read(Properties props, String envName, String propName, String defaultValue) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) return env;

        String value = props.getProperty(propName, defaultValue);
        if (value == null) throw new IllegalStateException(propName + " 설정이 없습니다.");
        return value;
    }

    private static void loadClasspath(Properties props) {
        try (InputStream in = DbConfig.class.getClassLoader().getResourceAsStream(CLASSPATH_FILE)) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void loadLocalFile(Properties props) {
        if (!Files.exists(LOCAL_FILE)) return;

        try (Reader reader = Files.newBufferedReader(LOCAL_FILE)) {
            props.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
