package com.back.domain.wiseSaying.repository;

import com.back.domain.wiseSaying.entity.WiseSaying;
import com.back.standard.dto.Page;

import java.util.List;
import java.util.Optional;

// 저장소가 해 줄 일의 "목록"만 적은 인터페이스.
// 서비스는 이 이름(WiseSayingRepository)만 알고, 실제로 메모리에 두는지 파일에 두는지는 모른다.
// → ⑥에서는 이 이름이 메모리 저장 클래스였고, ⑦에서는 인터페이스로 바뀌었지만 서비스의 필드·생성자는 그대로다.
public interface WiseSayingRepository {

    WiseSaying save(WiseSaying wiseSaying);

    // 번호 오름차순(1, 2, 3 ...)
    List<WiseSaying> findAll();

    Optional<WiseSaying> findById(int id);

    boolean delete(int id);

    int count();

    // 모든 명언을 한 파일(data.json)로 모은다.
    void build();

    // 검색·페이징은 "어디에 저장했나"와 상관없이 findAll() 결과로 똑같이 계산할 수 있어서
    // 구현체마다 다시 쓰지 않도록 default 메서드로 둔다.
    // 최신순으로 검색한 뒤, 그 검색 결과를 기준으로 page 번째 묶음만 잘라 돌려준다.
    default Page<WiseSaying> findForList(String keywordType, String keyword, int page, int pageSize) {
        List<WiseSaying> searched = findAll()
                .reversed()
                .stream()
                .filter(e -> matches(e, keywordType, keyword))
                .toList();

        int skipCount = (page - 1) * pageSize;
        List<WiseSaying> content = searched
                .stream()
                .skip(skipCount)
                .limit(pageSize)
                .toList();

        return new Page<>(content, searched.size(), page, pageSize);
    }

    // 검색어가 없으면 모두 통과. keywordType 에 따라 명언 내용 또는 작가에서 부분 일치로 찾는다.
    private boolean matches(WiseSaying wiseSaying, String keywordType, String keyword) {
        if (keyword.isEmpty()) return true;

        return switch (keywordType) {
            case "author" -> wiseSaying.getAuthor().contains(keyword);
            case "content" -> wiseSaying.getContent().contains(keyword);
            default -> wiseSaying.getContent().contains(keyword) || wiseSaying.getAuthor().contains(keyword);
        };
    }
}
