package com.wjc.codetest.product.model.request;

import lombok.Getter;
import lombok.Setter;

/**
 * [Code Review #25]
 * 문제:
 *   1) 페이징 파라미터에 검증 어노테이션 누락
 *   2) POST 메서드로 조회 요청
 *   3) @Setter 사용으로 인한 보안 위험
 * 원인:
 *   1) page와 size 파라미터에 대한 검증이 없으면 음수나 0이 전달될 수 있음
 *   2) 조회 작업은 GET 메서드를 사용해야 하며, Query Parameter로 전달하는 것이 Restful함
 *   3) @Setter로 인해 역직렬화 후 값 변경 가능
 * 개선안:
 *   1) @Min 어노테이션 추가
 *   2) GET 메서드로 변경하여 Query Parameter 사용
 *   3) @Setter 제거
 *   4) 검증 어노테이션 추가로 잘못된 페이징 파라미터 방지
 * 검증:
 *   1) @Min 추가 후 음수 값 전달 시 400 Bad Request 확인
 *   2) GET 메서드로 변경 후 Query Parameter 전달 확인
 *   3) @Setter 제거 후 역직렬화 정상 동작 확인
 *   4) 외부에서 set 메서드 호출 시 컴파일 오류 확인
 */
@Getter
@Setter
public class GetProductListRequest {
    private String category;
    private int page;
    private int size;
}
