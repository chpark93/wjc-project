package com.wjc.codetest.product.model.response;

import com.wjc.codetest.product.model.domain.Product;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * [Code Review #26]
 * 문제:
 *   1) Response DTO에 Entity를 직접 포함
 *   2) @Setter 사용으로 인한 불변성 위반
 * 원인:
 *   1) Entity를 Response에 포함하면 API 스펙이 Entity 구조에 종속됨
 *   2) @Setter로 인해 응답 생성 후 외부에서 데이터 변경 가능
 * 개선안:
 *   1) ProductResponse DTO를 생성하여 Product 대신 사용
 *   2) @Setter 제거 (Response DTO는 불변 객체로 설계)
 *   3) DTO 클래스 생성 필요하나 API 계약과 도메인 모델 분리로 유지보수성 향상
 * 검증:
 *   1) ProductResponse DTO 생성 후 API 응답 스펙 확인
 *   2) 순환 참조 및 N+1 문제 발생 여부 확인
 */
/**
 * <p>
 *
 * </p>
 *
 * @author : 변영우 byw1666@wjcompass.com
 * @since : 2025-10-27
 */
@Getter
@Setter
public class ProductListResponse {
    private List<Product> products;
    private int totalPages;
    private long totalElements;
    private int page;

    /**
     * [Code Review #27]
     * 문제: 생성자에서 Entity 리스트를 직접 받아 저장
     * 원인: Entity를 직접 받으면 지연 로딩 시 N+1 문제와 순환 참조 위험 존재
     * 개선안:
     *   1) ProductResponse 리스트를 받도록 변경
     *   2) DTO 변환 로직 필요하나 API 계약과 도메인 모델 분리로 유지보수성 향상
     * 검증:
     *   1) ProductResponse 리스트로 변경 후 생성자 시그니처 확인
     *   2) DTO 변환 로직 추가 후 정상 동작 확인
     *   3) N+1 문제 및 순환 참조 발생 여부 확인
     */
    public ProductListResponse(List<Product> content, int totalPages, long totalElements, int number) {
        this.products = content;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.page = number;
    }
}
