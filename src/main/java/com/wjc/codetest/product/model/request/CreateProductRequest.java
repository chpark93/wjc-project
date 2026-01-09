package com.wjc.codetest.product.model.request;

import lombok.Getter;
import lombok.Setter;

/**
 * [Code Review #16]
 * 문제:
 *   1) @Setter 사용으로 역직렬화 후 값 변경 가능
 *   2) DTO 검증 어노테이션 누락
 *   3) 불필요한 생성자 존재
 * 원인:
 *   1) @Setter로 인해 역직렬화 후 외부에서 필드 변경 가능
 *   2) DTO 필드에 검증 Validation 어노테이션 코드 누락
 *   3) @Setter가 있으면 Jackson은 기본 생성자 + setter 방식을 사용하므로 파라미터가 있는 생성자는 호출되지 않음
 * 개선안:
 *   1) @Setter 제거 -> Jackson이 파라미터가 있는 생성자를 사용하도록 변경
 *   2) 검증 어노테이션 추가
 *   3) category만 받는 생성자 제거 (name 필수이므로 불필요)
 *   4) 검증 어노테이션 추가 시 의존성이 필요하지만, 입력 데이터 무결성 보장 가능
 * 검증:
 *   1) @Setter 제거 후 Jackson이 생성자를 통해 역직렬화하는지 확인
 *   2) @NotBlank 추가 후 null/빈 문자열 요청 시 400 Bad Request 확인
 *   3) @Size 추가 후 길이 초과 시 400 Bad Request 확인
 */
@Getter
@Setter
public class CreateProductRequest {
    private String category;
    private String name;

    public CreateProductRequest(String category) {
        this.category = category;
    }

    public CreateProductRequest(String category, String name) {
        this.category = category;
        this.name = name;
    }
}
