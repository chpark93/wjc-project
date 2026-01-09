package com.wjc.codetest.product.controller;

import com.wjc.codetest.product.model.domain.Product;
import com.wjc.codetest.product.model.request.CreateProductRequest;
import com.wjc.codetest.product.model.request.GetProductListRequest;
import com.wjc.codetest.product.model.request.UpdateProductRequest;
import com.wjc.codetest.product.model.response.ProductListResponse;
import com.wjc.codetest.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [Code Review #11]
 * 문제:
 *   1) 행위(get, create, delete)가 포함된 Restful하지 않은 설계
 *   2) URL에 버전 관리 없음
 * 원인:
 *   1) REST API는 리소스 중심 설계를 원칙으로 하며, URL에 행위를 포함하면 API 확장성과 일관성이 저하
 *   2) 버전 관리가 없으면 API 변경 시 하위 호환성 문제가 발생 가능
 * 개선안:
 *   1) @RequestMapping("/api/v1/products") 추가, URL을 리소스 중심으로 변경
 *     - /api/v1/products/{id}, /api/v1/products
 *   2) 트레이드오프: 버전 관리를 추가하면 URL이 길어지지만, API 확장 및 업데이트에 대한 유연성을 확보할 수 있음
 *   3) 선택: URL 버전 관리 방식
 *     - 선택 근거:
 *       a) 헤더 버전 관리보다 명시적이고, 브라우저에서 직접 테스트 가능하며, 캐싱 및 라우팅 설정이 용이함
 * 검증:
 *   1) URL 패턴 일관성 확인 (엔드포인트가 /api/v1/products로 시작)
 *   2) 버전 변경 시 기존 클라이언트 영향 분석
 *   3) URL에 행위가 포함되지 않았는지 확인 (get, create, delete 제거)
 *   4) Restful 원칙 준수 여부 확인 (리소스 중심 설계)
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    /**
     * [Code Review #12]
     * 문제:
     *   1) Entity를 직접 반환 타입으로 사용
     *   2) URL에 행위 포함
     * 원인:
     *   1) Entity를 직접 반환 -> API 스펙이 Entity 구조에 종속되어 도메인 모델 변경이 API 스펙에 직접적인 영향을 줌
     *   2) URL에 행위(get)가 포함되어 Restful하지 않음
     * 개선안:
     *   1) ProductResponse DTO 생성 후 반환 타입 변경
     *   2) URL을 /api/v1/products/{id}로 변경
     *   3) 트레이드오프: DTO를 추가로 생성해야 하지만, API 스펙과 도메인 모델의 분리로 유지보수성 향상
     *   4) 선택: Response DTO 패턴
     *     - 선택 근거:
     *       a) Entity 직접 노출 시 순환 참조, 민감 정보 노출 위험이 있으며, DTO 사용 시 API 스펙과 도메인 모델을 독립적으로 관리 가능
     * 검증:
     *   1) ProductResponse DTO 생성 후 API 응답 스펙 확인 (필요한 필드만 노출)
     *   2) Entity 필드 추가 시 API 스펙 변경 없이 동작하는지 확인
     *   3) 순환 참조 발생 시나리오 테스트 (양방향 관계 시)
     *   4) API 문서 생성 시 DTO 구조 반영 확인
     */
    @GetMapping(value = "/get/product/by/{productId}")
    public ResponseEntity<Product> getProductById(@PathVariable(name = "productId") Long productId){
        Product product = productService.getProductById(productId);
        return ResponseEntity.ok(product);
    }

    /**
     * [Code Review #14]
     * 문제:
     *   1) Create 성공 시 200 OK 반환
     *   2) @RequestBody에 @Valid 누락
     *   3) URL에 행위 포함
     *   4) 인증/인가 체크 누락
     * 원인:
     *   1) HTTP 스펙에 따르면 리소스 생성 성공 시 201 Created를 반환해야 함
     *   2) @Valid 없이는 입력 검증이 수행되지 않아 잘못된 데이터가 서비스 레이어까지 전달됨
     *   3) URL에 행위(create)가 포함되어 Restful하지 않음
     *   4) 인증되지 않은 사용자도 상품 생성이 가능하여 보안 위험 존재
     * 개선안:
     *   1) ResponseEntity.status(HttpStatus.CREATED).body() 사용
     *   2) @Valid 추가
     *   3) URL을 /api/v1/products로 변경
     *   4) 인증 체크: @PreAuthorize 추가
     *     - 인증된 사용자만 상품 생성 가능하도록 제한
     *     - 인증되지 않은 사용자는 401 Unauthorized 반환
     *   5) 트레이드오프:
     *     - @Valid: 추가적인 라이브러리 의존성이 필요하지만, 컨트롤러 레벨에서 입력 검증을 수행하여 잘못된 데이터가 서비스 레이어로 전달되는 것을 방지할 수 있음
     *     - 인증 체크: 모든 요청에 인증 확인 오버헤드가 발생하지만, 무단 접근을 원천 차단하여 보안을 강화할 수 있음
     *   6) 선택: HTTP 201 Created 상태 코드 + @Valid 검증 + 인증 체크
     *     - 선택 근거:
     *       a) HTTP 표준 준수로 클라이언트가 생성 성공을 명확히 인지 가능
     *       b) @Valid를 통해 잘못된 데이터가 비즈니스 로직에 도달하기 전에 차단 가능
     *       c) 인증 체크로 보안 강화
     * 검증:
     *   1) 생성 성공 시 HTTP 201 Created 반환 확인
     *   2) @Valid 코드 로직 추가 후, 잘못된 요청 데이터 전송 시 400 Bad Request 반환 확인
     *   3) 검증 실패 시 예외 메시지 형식 확인
     *   4) 인증되지 않은 사용자 요청 시 401 Unauthorized 반환 확인
     */
    @PostMapping(value = "/create/product")
    public ResponseEntity<Product> createProduct(@RequestBody CreateProductRequest dto){
        Product product = productService.create(dto);
        return ResponseEntity.ok(product);
    }

    /**
     * [Code Review #20]
     * 문제:
     *   1) 삭제 작업에 POST 메서드 사용
     *   2) 200 OK 반환
     *   3) URL에 행위 포함
     *   4) 인증/인가 체크 누락
     * 원인:
     *   1) HTTP 메서드의 멱등성 원칙에 따르면 삭제는 DELETE 메서드를 사용해야 하며, POST는 멱등하지 않아 동일한 요청을 여러 번 보낼 경우 예상치 못한 동작이 발생할 수 있음
     *   2) 삭제 성공 시 본문이 없는 204 No Content를 반환하는 것이 표준
     *   3) URL에 행위(delete)가 포함되어 Restful하지 않음
     *   4) 상품 소유자 확인 없이 누구나 삭제 가능하여 보안 위험 존재
     * 개선안:
     *   1) @DeleteMapping 사용
     *   2) ResponseEntity.noContent().build() 반환
     *   3) URL을 /api/v1/products/{id}로 변경
     *   4) 논리삭제 구현: deleted 플래그(boolean) 필드를 추가하여 데이터 복구 가능하도록 구현
     *     - 장점: 데이터 복구 가능, 삭제 이력 추적 가능
     *     - 단점: 데이터베이스 용량 증가, 조회 쿼리에 WHERE deleted = false 조건 추가 필요
     *   5) 인증 체크: @PreAuthorize 추가
     *     - 인증된 사용자만 상품 삭제 가능하도록 제한
     *     - 인증되지 않은 사용자는 401 Unauthorized 반환
     *   6) 트레이드오프:
     *     - 논리삭제: 데이터 복구와 이력 관리가 가능하지만 DB 용량이 증가하고, 물리삭제는 용량 관리에 유리하지만 데이터 복구가 불가능함
     *     - 인증 체크: 모든 요청에 인증 확인 오버헤드가 발생하지만, 무단 접근을 원천 차단하여 보안을 강화할 수 있음
     *   7) 선택: 논리삭제 방식 + 인증 체크
     *     - 선택 근거:
     *       a) 실 운영을 기준으로 데이터 복구와 이력 추적이 필요
     *       b) 인증 체크로 보안 강화
     * 검증:
     *   1) DELETE 메서드로 변경 후 삭제 정상 동작 확인
     *   2) 삭제 성공 시 HTTP 204 No Content 반환 확인
     *   3) 동일한 DELETE 요청 재전송 시 멱등성 확인
     *   4) 삭제된 데이터 조회 시 제외되는지 확인
     *   5) 인증되지 않은 사용자 요청 시 401 Unauthorized 반환 확인
     */
    @PostMapping(value = "/delete/product/{productId}")
    public ResponseEntity<Boolean> deleteProduct(@PathVariable(name = "productId") Long productId){
        productService.deleteById(productId);
        return ResponseEntity.ok(true);
    }

    /**
     * [Code Review #17]
     * 문제:
     *   1) 수정 작업에 POST 메서드 사용
     *   2) @RequestBody에 @Valid 누락
     *   3) URL에 행위 포함
     *   4) 인증/인가 체크 누락
     * 원인:
     *   1) 리소스 수정은 PUT(전체 교체) 또는 PATCH(부분 수정) 메서드를 사용해야 하며, POST는 리소스 생성에 사용됨
     *   2) 입력 검증이 누락되어 잘못된 데이터가 전달될 수 있음
     *   3) URL에 행위(update)가 포함되어 Restful하지 않음
     *   4) 상품 소유자 확인 없이 누구나 수정 가능하여 보안 위험 존재
     * 개선안:
     *   1) @PatchMapping 또는 @PutMapping 사용
     *   2) @Valid 추가
     *   3) URL을 /api/v1/products/{id}로 변경
     *   4) 인증 체크: @PreAuthorize("isAuthenticated()") 추가
     *     - 인증된 사용자만 상품 수정 가능하도록 제한
     *     - 인증되지 않은 사용자는 401 Unauthorized 반환
     *   5) 트레이드오프:
     *     - PATCH vs PUT: PATCH는 부분 수정에 적합하고, PUT은 전체 교체에 적합
     *     - 인증 체크: 모든 요청에 인증 확인 오버헤드가 발생하지만, 무단 접근을 원천 차단하여 보안을 강화할 수 있음
     *   6) 선택: @PatchMapping + Controller 인증 체크 + Service 소유자 권한 체크
     *     - 선택 근거:
     *       a) 현재 UpdateProductRequest가 일부 필드만 수정하는 구조이므로 부분 수정에 적합한 PATCH가 적절
     *       b) PUT은 모든 필드를 필수로 받아 전체 교체하는 경우에 사용
     *       c) 인증 체크로 보안 강화
     * 검증:
     *   1) PATCH 메서드로 변경 후 부분 수정 정상 동작 확인
     *   2) @Valid 추가 후 검증 실패 시 400 Bad Request 반환 확인
     *   3) 일부 필드만 전송 시 해당 필드만 수정되는지 확인
     *   4) 존재하지 않는 ID 수정 시도 시 404 Not Found 반환 확인
     *   5) 수정되지 않은 필드는 기존 값 유지 확인
     *   6) 인증되지 않은 사용자 요청 시 401 Unauthorized 반환 확인
     */
    @PostMapping(value = "/update/product")
    public ResponseEntity<Product> updateProduct(@RequestBody UpdateProductRequest dto){
        Product product = productService.update(dto);
        return ResponseEntity.ok(product);
    }

    /**
     * [Code Review #22]
     * 문제:
     *   1) 조회 작업에 POST 메서드 사용
     *   2) Entity를 Response에 직접 포함
     * 원인:
     *   1) 조회 작업은 GET 메서드를 사용해야 하며, POST는 리소스 생성에 사용됨
     *   2) RequestBody로 페이징 파라미터를 받는 것은 Restful하지 않음
     * 개선안:
     *   1) @GetMapping 사용
     *   2) Query Parameter로 페이징 정보 전달
     *   3) ProductResponse DTO 사용
     *   4) 예: @GetMapping("/api/v1/products/search?category={category}&page={page}&size={size}")
     *   5) 트레이드오프: Query Parameter를 사용하면 URL이 길어지지만, GET 요청의 캐싱 이점을 활용할 수 있음
     *   6) 선택: GET + Query Parameter 방식
     *     - 선택 근거:
     *       a) 브라우저 / 프록시 캐싱 활용 가능
     *       b) RequestBody는 GET에서 사용하지 않는 것이 표준
     * 검증:
     *   1) GET 메서드로 변경 후 조회 정상 동작 확인
     *   2) Query Parameter로 페이징 정보 전달 확인 (page, size)
     *   3) GET 요청 캐싱 가능 여부 확인 (브라우저 / 프록시 캐시)
     *   4) 페이징 파라미터 검증
     *   5) 빈 결과 반환 시 빈 배열 반환 확인
     */
    @PostMapping(value = "/product/list")
    public ResponseEntity<ProductListResponse> getProductListByCategory(@RequestBody GetProductListRequest dto){
        Page<Product> productList = productService.getListByCategory(dto);
        return ResponseEntity.ok(new ProductListResponse(productList.getContent(), productList.getTotalPages(), productList.getTotalElements(), productList.getNumber()));
    }

    /**
     * [Code Review #28]
     * 문제: 메서드명이 실제 동작과 불일치 (getProductListByCategory인데 카테고리 목록을 반환)
     * 원인: 메서드명이 Product 목록 조회를 의미하지만, 실제로는 카테고리 목록을 반환하여 메서드명과 일치하지 않는 다른 작업 수행
     * 개선안:
     *   1) 메서드명을 getUniqueCategories()로 변경하여 실제 동작과 일치시킴
     *   2) URL을 /api/v1/products/categories로 변경 -> Product에 종속된 카테고리 리소스임을 명확히 표현
     * 검증:
     *   1) 메서드명 변경 후 의도가 명확히 전달되는지 확인
     *   2) 메서드명이 실제 동작과 일치하는지 확인 (카테고리 목록 반환)
     *   3) URL이 리소스 계층 구조를 올바르게 표현하는지 확인
     *   4) API 문서에서 메서드명과 URL 일관성 확인
     */
    @GetMapping(value = "/product/category/list")
    public ResponseEntity<List<String>> getProductListByCategory(){
        List<String> uniqueCategories = productService.getUniqueCategories();
        return ResponseEntity.ok(uniqueCategories);
    }
}
