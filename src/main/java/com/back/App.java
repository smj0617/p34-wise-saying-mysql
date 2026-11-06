package com.back;

import com.back.domain.system.controller.SystemController;
import com.back.domain.wiseSaying.controller.WiseSayingController;
import com.back.domain.wiseSaying.repository.WiseSayingFileRepository;
import com.back.domain.wiseSaying.repository.WiseSayingRepository;
import com.back.domain.wiseSaying.service.WiseSayingService;

import java.nio.file.Path;
import java.util.Scanner;

public class App {

    // 실제 실행(Main)에서 명언을 저장하는 폴더
    public static final Path DB_DIR = Path.of("db", "wiseSaying");

    private static final int SAMPLE_COUNT = 10;

    private final Scanner sc;
    private final SystemController systemController;
    private final WiseSayingController wiseSayingController;

    // 실제 실행(Main): db/wiseSaying 을 쓰고, 샘플 명언은 만들지 않는다.
    public App(Scanner sc) {
        this(sc, DB_DIR, false);
    }

    // 테스트는 dbDir 에 테스트 전용 폴더를 넘긴다.
    // 검색·페이징 테스트는 makeSampleData = true 로, 폴더가 비어 있으면 샘플 10개를 만들고 시작한다.
    public App(Scanner sc, Path dbDir, boolean makeSampleData) {
        this.sc = sc;

        // 바뀐 곳은 이 한 줄뿐: 메모리 저장소 대신 파일 저장소를 만들어 넘긴다.
        // 서비스·컨트롤러는 WiseSayingRepository 인터페이스만 알기 때문에 고칠 필요가 없다.
        WiseSayingRepository wiseSayingRepository = new WiseSayingFileRepository(dbDir);
        WiseSayingService wiseSayingService = new WiseSayingService(wiseSayingRepository);

        if (makeSampleData) {
            wiseSayingService.makeSampleDataIfEmpty(SAMPLE_COUNT);
        }

        this.systemController = new SystemController();
        this.wiseSayingController = new WiseSayingController(sc, wiseSayingService);
    }

    public void run() {
        System.out.println("== 명언 앱 ==");

        while (true) {
            System.out.print("명령) ");
            Rq rq = new Rq(sc.nextLine());

            switch (rq.getActionName()) {
                case "종료" -> {
                    systemController.actionExit();
                    return;
                }
                case "등록" -> wiseSayingController.actionWrite();
                case "목록" -> wiseSayingController.actionList(rq);
                case "삭제" -> wiseSayingController.actionDelete(rq);
                case "수정" -> wiseSayingController.actionModify(rq);
                case "빌드" -> wiseSayingController.actionBuild();
                default -> System.out.println("알 수 없는 명령입니다.");
            }
        }
    }
}
