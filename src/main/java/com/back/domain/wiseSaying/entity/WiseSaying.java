package com.back.domain.wiseSaying.entity;

public class WiseSaying {

    private int id;
    private String content;
    private String author;

    public WiseSaying(String content, String author) {
        this.content = content;
        this.author = author;
    }

    // 파일에서 읽어 올 때처럼, 번호가 이미 정해진 명언을 다시 만들 때 쓴다.
    public WiseSaying(int id, String content, String author) {
        this(content, author);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // 아직 저장소에 저장되지 않아 번호가 없는 상태
    public boolean isNew() {
        return id == 0;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
