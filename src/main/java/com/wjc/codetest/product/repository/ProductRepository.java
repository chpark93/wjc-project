package com.wjc.codetest.product.repository;

import com.wjc.codetest.product.model.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * [Code Review #32]
 * 문제: 인터페이스에 @Repository 어노테이션 사용
 * 원인: Spring Data JPA는 JpaRepository를 상속받은 인터페이스를 자동으로 스캔하여 프록시 구현체를 생성하므로 @Repository 어노테이션은 불필요하며 중복
 * 개선안:
 *   1) @Repository 제거
 *   2) 어노테이션을 제거해도 기능에는 영향이 없으며, 코드가 간결해짐
 * 검증:
 *   1) @Repository 제거 후 Spring Bean 등록 정상 동작 확인
 *   2) 의존성 주입 정상 동작 확인
 *   3) Spring Data JPA 자동 스캔 동작 확인
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * [Code Review #24]
     * 문제: 메서드 파라미터 이름이 실제 의미와 불일치
     * 원인: Spring Data JPA의 메서드 네이밍 컨벤션은 파라미터 이름을 기준으로 쿼리를 생성하므로, 파라미터 이름이 실제 필드명과 다르면 코드 가독성이 저하
     * 개선안:
     *   1) 매개변수명 name -> category로 변경
     * 검증:
     *   1) 파라미터 이름 변경 후 쿼리 생성 정상 동작 확인
     *   2) 코드 가독성 향상 확인 (파라미터 이름과 필드명 일치)
     *   3) 코드 리뷰 시 네이밍 일관성 확인
     */
    Page<Product> findAllByCategory(String name, Pageable pageable);

    /**
     * [Code Review #30]
     * 문제:
     *   1) JPQL 쿼리에 정렬 기준 없음
     *   2) Product와 Category 도메인이 분리되지 않음
     * 원인:
     *   1) 정렬 기준이 없으면 결과 순서가 보장되지 않아 일관성 없는 결과 발생
     *   2) Category를 String으로 관리하여 카테고리 자체의 속성을 표현할 수 없음
     * 개선안:
     *   1) 단기: ORDER BY 절 추가 - @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category ASC")
     *   2) 장기: Category Entity 분리
     *     - Category Entity 생성
     *     - Product와 Category를 연관 관계로 연결
     *     - CategoryRepository 생성하여 독립적인 카테고리 관리
     *   3) 도메인 분리 시 개선점:
     *     - 카테고리 자체의 속성 관리 가능
     *     - 카테고리 목록 조회 시 Product 테이블 접근 불필요
     *     - 카테고리별 통계 쿼리 최적화 가능
     *     - 존재하지 않는 카테고리 입력 방지
     * 검증:
     *   1) 단기: ORDER BY 추가 후 결과 순서 일관성 확인
     *   2) 장기: Category 엔티티 분리 후 다음 확인
     *     - 카테고리 목록 조회 시 Product 테이블 조회하지 않는지 확인
     *     - 존재하지 않는 카테고리로 Product 생성 시 FK 제약조건 위반 확인
     */
    @Query("SELECT DISTINCT p.category FROM Product p")
    List<String> findDistinctCategories();
}
