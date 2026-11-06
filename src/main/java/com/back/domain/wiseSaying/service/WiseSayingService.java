package com.back.domain.wiseSaying.service;

import com.back.domain.wiseSaying.entity.WiseSaying;
import com.back.domain.wiseSaying.repository.WiseSayingRepository;
import com.back.standard.dto.Page;

import java.util.Optional;

// 순수 비즈니스 로직. 콘솔 입출력 없음, 파일 처리 없음.
// 저장소가 메모리든 파일이든 이 클래스는 WiseSayingRepository 인터페이스만 보고 일한다.
public class WiseSayingService {

    private final WiseSayingRepository wiseSayingRepository;

    public WiseSayingService(WiseSayingRepository wiseSayingRepository) {
        this.wiseSayingRepository = wiseSayingRepository;
    }

    public WiseSaying write(String content, String author) {
        return wiseSayingRepository.save(new WiseSaying(content, author));
    }

    public Page<WiseSaying> findForList(String keywordType, String keyword, int page, int pageSize) {
        return wiseSayingRepository.findForList(keywordType, keyword, page, pageSize);
    }

    public Optional<WiseSaying> findById(int id) {
        return wiseSayingRepository.findById(id);
    }

    public boolean delete(int id) {
        return wiseSayingRepository.delete(id);
    }

    public void modify(WiseSaying wiseSaying, String content, String author) {
        wiseSaying.setContent(content);
        wiseSaying.setAuthor(author);
        wiseSayingRepository.save(wiseSaying);
    }

    public void build() {
        wiseSayingRepository.build();
    }

    // 명언이 하나도 없을 때만 "명언 1 / 작자미상 1" ~ "명언 n / 작자미상 n" 을 만든다.
    public void makeSampleDataIfEmpty(int count) {
        if (wiseSayingRepository.count() > 0) return;

        for (int i = 1; i <= count; i++) {
            write("명언 " + i, "작자미상 " + i);
        }
    }
}
