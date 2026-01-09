package com.wjc.codetest.product.model.request;

import lombok.Getter;
import lombok.Setter;

/**
 * [Code Review #19]
 * 문제:
 *   1) DTO에 id 필드 포함
 *   2) @Setter 사용으로 역직렬화 후 값 변경 가능
 *   3) 검증 어노테이션 누락
 *   4) 불필요한 생성자 존재
 * 원인:
 *   1) Restful API에서는 리소스 ID를 URL Path Variable로 받는 것이 표준
 *   2) @Setter로 인해 역직렬화 후 외부에서 필드 변경 가능
 *   3) DTO 필드에 검증 Validation 어노테이션 코드 누락
 *   4) @Setter가 있으면 Jackson은 기본 생성자 + setter 방식을 사용하므로 파라미터가 있는 생성자는 호출되지 않음
 * 개선안:
 *   1) id 필드 제거 (Path Variable로 받음)
 *   2) @Setter 제거 -> Jackson이 파라미터가 있는 생성자를 사용하도록 변경
 *   3) 검증 어노테이션 추가
 *   4) 불필요한 생성자 제거 (category, name을 받는 생성자만 유지)
 *   5) 검증 어노테이션 추가 시 의존성이 필요하지만, 입력 데이터 무결성 보장 가능
 * 검증:
 *   1) id 필드 제거 후 컨트롤러에서 @PathVariable로 id 받는지 확인
 *   2) @Setter 제거 후 Jackson이 생성자를 통해 역직렬화하는지 확인
 *   3) @NotBlank 추가 후 null/빈 문자열 요청 시 400 Bad Request 확인
 *   4) @Size 추가 후 길이 초과 시 400 Bad Request 확인
 */
@Getter
@Setter
public class UpdateProductRequest {
    private Long id;
    private String category;
    private String name;

    public UpdateProductRequest(Long id) {
        this.id = id;
    }

    public UpdateProductRequest(Long id, String category) {
        this.id = id;
        this.category = category;
    }

    public UpdateProductRequest(Long id, String category, String name) {
        this.id = id;
        this.category = category;
        this.name = name;
    }
}
