package com.back.domain.wiseSaying.repository;

import com.back.domain.wiseSaying.entity.WiseSaying;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// WiseSaying ↔ JSON 문자열 변환 (JSON 라이브러리 없이 직접).
// 파일을 열고 닫는 일은 하지 않는다. 문자열만 만들고 읽는다.
public final class WiseSayingJson {

    private WiseSayingJson() {
    }

    // {
    //   "id": 1,
    //   "content": "명언 1",
    //   "author": "작가 1"
    // }
    public static String toJson(WiseSaying wiseSaying) {
        return """
                {
                  "id": %d,
                  "content": "%s",
                  "author": "%s"
                }""".formatted(
                wiseSaying.getId(),
                escape(wiseSaying.getContent()),
                escape(wiseSaying.getAuthor())
        );
    }

    // [ {명언1}, {명언2} ] — 각 객체를 두 칸 들여 쓴다.
    public static String toJsonArray(List<WiseSaying> wiseSayings) {
        if (wiseSayings.isEmpty()) return "[]";

        String body = wiseSayings.stream()
                .map(WiseSayingJson::toJson)
                .map(json -> json.indent(2).stripTrailing())
                .collect(Collectors.joining(",\n"));

        return "[\n" + body + "\n]";
    }

    public static WiseSaying fromJson(String json) {
        int id = Integer.parseInt(readNumber(json, "id"));
        String content = readString(json, "content");
        String author = readString(json, "author");

        return new WiseSaying(id, content, author);
    }

    // ── 읽기 ─────────────────────────────

    // "id": 1  → "1"
    private static String readNumber(String json, String key) {
        Matcher matcher = Pattern
                .compile("\"" + key + "\"\\s*:\\s*(-?\\d+)")
                .matcher(json);

        if (!matcher.find()) throw new IllegalArgumentException(key + " 값을 찾을 수 없습니다: " + json);
        return matcher.group(1);
    }

    // "content": "명언 \"1\""  → 명언 "1"
    // 따옴표 안에서 \" 는 값의 일부이므로, 이스케이프되지 않은 " 를 만날 때까지 읽는다.
    private static String readString(String json, String key) {
        Matcher matcher = Pattern
                .compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
                .matcher(json);

        if (!matcher.find()) throw new IllegalArgumentException(key + " 값을 찾을 수 없습니다: " + json);
        return unescape(matcher.group(1));
    }

    // ── 이스케이프 ─────────────────────────────
    // 명언에 " 나 \ 가 들어 있으면 그대로 쓰면 JSON 이 깨지므로 앞에 \ 를 붙인다.
    // 순서 주의: \ 를 먼저 바꿔야, 뒤에서 붙인 \ 가 다시 바뀌지 않는다.

    private static String escape(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private static String unescape(String text) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(++i);
                sb.append(next == 'n' ? '\n' : next);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
