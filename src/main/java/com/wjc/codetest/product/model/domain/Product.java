package com.wjc.codetest.product.model.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * [Code Review #1]
 * 문제: @Setter 사용으로 인한 캡슐화 위반 및 @Getter 중복
 * 원인:
 *   1) @Setter는 모든 필드에 setter를 자동 생성하여 불변성 보장 불가
 *   2) 수동 Getter 메서드 중복 정의
 * 개선안:
 *   1) @Setter 제거 후 update(String category, String name) 비즈니스 메서드 추가
 *     - public void update(String category, String name) { this.category = category; this.name = name; }
 *     - Service의 update 메서드에서 product.update(dto.getCategory(), dto.getName()) 형태로 사용
 *   2) 수동 Getter 메서드 삭제
 *   3) 트레이드오프: 코드량은 증가하지만, 도메인 로직이 명확해지고 캡슐화 강화
 *   4) 선택: @Setter 제거 + 비즈니스 메서드 추가
 *     - 선택 근거:
 *       a) 도메인 로직이 Entity에 집중되어 응집도 향상
 *       b) 무분별한 필드 변경 방지
 * 검증:
 *   1) @Setter 제거 후 컴파일 오류 확인
 *   2) 비즈니스 메서드로만 변경 가능한지 확인
 *   3) Service 레이어에서 update() 메서드 호출 확인
 */
@Entity
@Getter
@Setter
public class Product {

    /**
     * [Code Review #2]
     * 문제: GenerationType.AUTO 사용으로 인한 불명확성
     * 원인: AUTO는 DB 방언에 따라 전략이 달라짐
     * 개선안:
     *   1) GenerationType.IDENTITY로 명시적 변경 (MySQL, H2 기준)
     * 검증:
     *   1) H2 환경에서 hibernate_sequence 테이블 생성 여부 확인
     */
    @Id
    @Column(name = "product_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * [Code Review #3]
     * 문제: @Column에 제약조건 명시하지 않음
     * 원인: JPA가 DDL 생성 시 제약조건을 반영하지 못해 DB 스키마가 VARCHAR(255), NULL 허용으로 생성됨
     * 개선안:
     *   1) @Column(nullable = false, length = 50) 추가
     * 검증:
     *   1) DDL 생성 시 NOT NULL, VARCHAR(50) 확인
     *   2) null 값 및 길이 초과 INSERT -> DB 레벨에서 예외 발생 확인
     */
    @Column(name = "category")
    private String category;

    /**
     * [Code Review #4]
     * 문제: @Column에 제약조건 명시하지 않음
     * 원인: JPA가 DDL 생성 시 제약조건을 반영하지 못해 DB 스키마가 VARCHAR(255), NULL 허용으로 생성됨
     * 개선안:
     *   1) @Column(nullable = false, length = 50) 추가
     * 검증:
     *   1) DDL 생성 시 NOT NULL, VARCHAR(50) 확인
     *   2) null 값 및 길이 초과 INSERT -> DB 레벨에서 예외 발생 확인
     */
    @Column(name = "name")
    private String name;

    protected Product() {
    }

    /**
     * [Code Review #5]
     * 문제: 생성자에서 입력 검증 부재
     * 원인: null 체크나 길이 검증이 없어 잘못된 데이터로 엔티티 생성 가능
     * 개선안:
     *   1) 생성자에서 null 체크 및 길이 검증 추가
     *   2) if (category == null || category.isBlank()) throw new IllegalArgumentException()
     *   3) if (name == null || name.isBlank()) throw new IllegalArgumentException()
     *   4) 코드 복잡도 증가하나 조기 오류 감지 가능
     * 검증:
     *   1) null 값 및 빈 문자열로 생성 시 예외 발생 확인
     *   2) DB 제약조건 위반 전 오류 감지 확인
     */
    public Product(String category, String name) {
        this.category = category;
        this.name = name;
    }

    /**
     * [Code Review #6]
     * 문제: @Getter와 수동 Getter 메서드 중복
     * 원인: Lombok @Getter가 이미 getter를 자동 생성하는데 수동으로 중복 정의
     * 개선안:
     *   1) 수동 Getter 메서드 삭제
     * 검증:
     *   1) Lombok @Getter로 동일 기능 제공 확인
     */
    public String getCategory() {
        return category;
    }

    /**
     * [Code Review #7]
     * 문제: @Getter와 수동 Getter 메서드 중복
     * 원인: Lombok @Getter가 이미 getter를 자동 생성하는데 수동으로 중복 정의
     * 개선안:
     *   1) 수동 Getter 메서드 삭제
     * 검증:
     *   1) Lombok @Getter로 동일 기능 제공 확인
     */
    public String getName() {
        return name;
    }
}
