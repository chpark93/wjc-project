package com.wjc.codetest.product.service;

import com.wjc.codetest.product.model.request.CreateProductRequest;
import com.wjc.codetest.product.model.request.GetProductListRequest;
import com.wjc.codetest.product.model.domain.Product;
import com.wjc.codetest.product.model.request.UpdateProductRequest;
import com.wjc.codetest.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * [Code Review #31]
 * 문제:
 *   1) 조회 메서드에 @Transactional(readOnly = true) 미적용
 *   2) Query와 Command 로직이 하나의 Service에 혼재
 * 원인:
 *   1) 읽기 전용 트랜잭션 미사용 시 JPA가 불필요한 체킹을 수행하여 메모리 낭비 발생
 *   2) Query와 Command 로직이 같은 클래스에 있어 책임이 불명확하고, 조회 성능 최적화 시 Command 로직에 영향 가능
 * 개선안:
 *   1) 단기: 조회 메서드에 @Transactional(readOnly = true) 추가
 *   2) 장기: CQRS 패턴 적용
 *     - ProductQueryService: Query 전용 (readOnly 트랜잭션)
 *     - ProductCommandService: Command 전용 (쓰기 트랜잭션)
 *   3) 트레이드오프:
 *     - readOnly: 쓰기 작업 불가능하지만, 메모리 사용량 감소 및 조회 성능 향상
 *     - CQRS: 클래스 수 증가 및 구조 복잡도 증가하지만, 조회 / 쓰기 독립적 최적화 및 확장성 향상
 *   4) 선택: 단기 readOnly 적용 + 장기 CQRS 패턴
 *     - 선택 근거:
 *       a) readOnly는 즉시 적용 가능하며 성능 개선 효과가 명확함
 *       b) 트래픽 증가 시 Query / Command 독립적 스케일링 가능
 * 검증:
 *   1) @Transactional(readOnly = true) 적용 후 Dirty Checking 비활성화 확인
 *   2) readOnly 트랜잭션 내 쓰기 작업 시 예외 발생 확인
 *   3) Query / Command Service 분리 확인, 각 Service의 책임 명확성 확인
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * [Code Review #15]
     * 문제:
     *   1) Service 레이어에 @Transactional 미적용
     *   2) 권한 체크 로직 부재
     *   3) Entity 직접 반환
     * 원인:
     *   1) 현재는 단일 save() 호출로 Repository 레벨 트랜잭션으로 동작하지만, 비즈니스 로직 확장 시 트랜잭션 경계가 불명확해져 일관성이 지켜지지않는 문제 발생 가능
     *   2) 권한 체크 없이 상품 생성 가능
     *   3) Entity 직접 반환 시 API 스펙이 도메인 모델에 종속됨
     * 개선안:
     *   1) @Transactional 추가 -> Service 메서드 전체를 하나의 트랜잭션으로 묶어 원자성 보장
     *   2) 권한 체크 로직 추가
     *   3) ProductResponse DTO로 변환하여 반환
     *   4) 트레이드오프:
     *     - @Transactional: 약간의 오버헤드 발생하지만, 로직 확장 시 안정성 보장
     *     - DTO 변환: 변환 로직 추가 필요하지만, API 스펙과 도메인 모델 독립성 확보
     *   5) 선택: @Transactional + 권한 체크 + DTO 반환
     *     - 선택 근거:
     *       a) 트랜잭션은 비즈니스 로직 확장 시 필수적
     *       b) 권한 체크로 보안 강화
     *       c) DTO 사용으로 API 스펙 안정성 확보
     * 검증:
     *   1) @Transactional 적용 후 예외 발생 시 롤백 확인
     *   2) 트랜잭션 커밋 / 롤백 확인
     *   3) 비즈니스 로직 추가 후에도 트랜잭션 경계 유지 확인
     *   4) ProductResponse DTO 반환 확인
     */
    public Product create(CreateProductRequest dto) {
        Product product = new Product(dto.getCategory(), dto.getName());
        return productRepository.save(product);
    }

    /**
     * [Code Review #13]
     * 문제:
     *   1) RuntimeException 사용
     *   2) @Transactional(readOnly = true) 미적용
     *   3) Entity 직접 반환
     * 원인:
     *   1) RuntimeException -> 예외 원인 파악 어려움
     *   2) 조회 메서드에 readOnly 미적용 시 불필요한 Dirty Checking 발생
     *   3) Entity 직접 반환 시 API 스펙이 도메인 모델에 종속됨
     * 개선안:
     *   1) CustomException 생성하여 GlobalExceptionHandler에서 404 매핑
     *   2) @Transactional(readOnly = true) 추가
     *   3) ProductResponse DTO로 변환하여 반환
     *   4) Optional.isPresent() + get() 대신 orElseThrow() 사용
     *     - 변경 전: if (!optional.isPresent()) throw new RuntimeException("product not found"); return optional.get();
     *     - 변경 후: return repository.findById(id).orElseThrow(() -> new ProductNotFoundException())
     *   5) 트레이드오프:
     *     - CustomException: 예외 클래스 추가 필요하지만, 예외 타입별 HTTP 상태 코드 매핑 가능
     *     - DTO 변환: 변환 로직 추가 필요하지만, API 스펙과 도메인 모델 독립성 확보
     *   6) 선택: CustomException + readOnly + DTO 반환 + orElseThrow
     *     - 선택 근거:
     *       a) CustomException으로 예외 처리 중앙화
     *       b) readOnly로 조회 성능 최적화
     *       c) orElseThrow로 코드 간결성 향상
     * 검증:
     *   1) ProductNotFoundException 생성 후 GlobalExceptionHandler에서 404 반환 확인
     *   2) 존재하지 않는 ID 조회 시 404 Not Found 반환 확인
     *   3) readOnly 적용 후 Dirty Checking 비활성화 확인
     *   4) ProductResponse DTO 반환 확인
     */
    public Product getProductById(Long productId) {
        Optional<Product> productOptional = productRepository.findById(productId);
        if (!productOptional.isPresent()) {
            throw new RuntimeException("product not found");
        }
        return productOptional.get();
    }

    /**
     * [Code Review #18]
     * 문제:
     *   1) product.setCategory(), product.setName() 사용으로 캡슐화 위반
     *   2) 불필요한 save() 호출 (Dirty Checking 미활용)
     *   3) DTO에 id 포함 -> Path Variable로 받아야 함
     *   4) Service 레이어에 @Transactional 미적용
     *   5) 권한 체크 로직 부재
     *   6) Entity 직접 반환
     * 원인:
     *   1) setter 직접 호출 시 도메인 로직이 Service에 분산되어 응집도 저하
     *   2) JPA Dirty Checking은 트랜잭션 커밋 시 자동으로 UPDATE 쿼리 실행하므로 save() 불필요
     *   3) Rest API에서는 리소스 ID를 Path Variable로 받는 것이 표준
     *   4) 현재는 save() 호출로 동작하지만, save() 제거 시 Service 레벨 트랜잭션 없으면 Dirty Checking 미동작
     *   5) 인증된 사용자라도 타인의 상품 수정 가능
     *   6) Entity 직접 반환 시 API 스펙이 도메인 모델에 종속됨
     * 개선안:
     *   1) Product 엔티티에 update(String category, String name) 비즈니스 메서드 추가
     *     - 변경 전: product.setCategory(dto.getCategory()); product.setName(dto.getName());
     *     - 변경 후: product.update(dto.getCategory(), dto.getName());
     *   2) save() 제거 -> Dirty Checking 활용
     *   3) 메서드 시그니처 변경: update(Long id, UpdateProductRequest dto)
     *   4) @Transactional 추가 -> Dirty Checking 활성화 및 로직 확장 시 원자성 보장
     *   5) 권한 체크 로직 추가: 상품 소유자만 수정 가능하도록 검증
     *   6) ProductResponse DTO로 변환하여 반환
     *   7) 트레이드오프:
     *     - Entity에 메서드 추가 필요하지만, 도메인 로직 응집도 향상
     *     - Dirty Checking: save() 제거로 코드 간결하지만, @Transactional 필수
     *     - 권한 체크: 검증 로직 추가 필요하지만, 보안 강화
     *     - DTO 변환: 변환 로직 추가 필요하지만, API 스펙과 도메인 모델 독립성 확보
     *   8) 선택: 비즈니스 메서드 + Dirty Checking + @Transactional + 권한 체크 + DTO 반환
     *     - 선택 근거:
     *       a) 비즈니스 메서드로 도메인 로직 Entity에 집중
     *       b) Dirty Checking으로 불필요한 save() 제거
     *       c) 권한 체크로 보안 강화
     *       d) DTO 사용으로 API 스펙 안정성 확보
     * 검증:
     *   1) Product.update() 메서드 추가 후 정상 동작 확인
     *   2) save() 제거 후 UPDATE 쿼리 정상 실행 확인
     *   3) 쿼리 로그에서 UPDATE 쿼리 발생 시점 확인 (트랜잭션 커밋 시)
     *   4) Dirty Checking 동작 확인
     *   5) 권한 없는 사용자 수정 시도 시 403 Forbidden 반환 확인
     *   6) 트랜잭션 범위 내에서만 변경사항 반영 확인
     *   7) ProductResponse DTO 반환 확인
     */
    public Product update(UpdateProductRequest dto) {
        Product product = getProductById(dto.getId());
        product.setCategory(dto.getCategory());
        product.setName(dto.getName());
        Product updatedProduct = productRepository.save(product);
        return updatedProduct;

    }

    /**
     * [Code Review #21]
     * 문제:
     *   1) 불필요한 조회 후 삭제로 인한 성능 저하
     *   2) 물리 삭제 사용으로 인한 데이터 복구 불가
     *   3) Service 레이어에 @Transactional 미적용
     *   4) 권한 체크 로직 부재
     * 원인:
     *   1) 조회 후 삭제 시 불필요한 SELECT 쿼리 발생
     *   2) 물리 삭제 시 데이터 복구 및 이력 추적 불가능
     *   3) 현재는 Repository 레벨 트랜잭션으로 동작하지만, 논리 삭제 구현 시 조회 + 수정이 하나의 트랜잭션으로 묶여야 함
     *   4) 인증된 사용자라도 타인의 상품 삭제 가능
     * 개선안:
     *   1) 논리 삭제 구현: deleted 플래그 추가
     *     - Product 엔티티에 deleted(boolean) 필드 추가
     *     - 조회 시 WHERE deleted = false 조건 추가
     *   2) @Transactional 추가 -> 조회 + deleted 플래그 변경을 하나의 트랜잭션으로 보장
     *   3) 권한 체크 로직 추가: 상품 소유자만 삭제 가능하도록 검증
     *   4) 트레이드오프:
     *     - 논리 삭제: DB 용량 증가하지만, 데이터 복구 및 이력 추적 가능
     *     - 권한 체크: 검증 로직 추가 필요하지만, 보안 강화
     *   5) 선택: 논리 삭제 + @Transactional + 권한 체크
     *     - 선택 근거:
     *       a) 운영 환경에서 데이터 복구 및 이력 추적 필요
     *       b) 권한 체크로 보안 강화
     * 검증:
     *   1) 논리 삭제 후 deleted = true로 변경 확인
     *   2) 삭제된 데이터 조회 시 제외되는지 확인
     *   3) 권한 없는 사용자 삭제 시도 시 403 Forbidden 반환 확인
     *   4) 동일한 DELETE 요청 재전송 시 멱등성 확인
     */
    public void deleteById(Long productId) {
        Product product = getProductById(productId);
        productRepository.delete(product);
    }

    /**
     * [Code Review #23]
     * 문제:
     *   1) 페이징 파라미터 검증 부재
     *   2) 정렬 기준 하드코딩
     *   3) Offset 기반 페이징으로 대량 데이터 조회 시 성능 저하
     *   4) @Transactional(readOnly = true) 미적용
     *   5) Entity 직접 반환 (Page<Product>)
     * 원인:
     *   1) page, size 검증 없이 음수나 과도한 값 전달 가능
     *   2) 정렬 기준이 하드코딩되어 유연성 저하
     *   3) Offset 기반 페이징은 OFFSET 계산 비용 증가 (예: OFFSET 100000 LIMIT 10은 100010개 행 스캔)
     *   4) 조회 메서드에 readOnly 미적용 시 불필요한 Dirty Checking 발생
     *   5) Entity 직접 반환 시 API 스펙이 도메인 모델에 종속됨
     * 개선안:
     *   1) 페이징 파라미터 검증 추가
     *     - page < 0 -> 0으로 보정
     *     - size <= 0 또는 size > 100 -> 기본값(10) 또는 최대값(100) 적용
     *   2) 정렬 기준을 파라미터로 받도록 변경
     *   3) @Transactional(readOnly = true) 추가
     *   4) Page<ProductResponse> DTO로 변환하여 반환
     *   5) 대량 데이터 조회 성능 개선:
     *     - Cursor 기반 페이징
     *       a) 구현: WHERE id > lastId AND category = ? ORDER BY id ASC LIMIT size
     *       b) 장점: OFFSET 계산 불필요, 일관된 성능, 실시간 데이터 변경에 강함
     *       c) 단점: 특정 페이지 직접 이동 불가, 정렬 기준 변경 시 복잡도 증가
     *       d) 적용 시기: 데이터 수백만건 이상
     *     - Redis 캐싱
     *       a) 구현: @Cacheable("productsByCategory") 또는 RedisTemplate
     *       b) 장점: 조회 속도 대폭 향상, DB 부하 감소
     *       c) 단점: 캐시 무효화 전략 필요, 데이터 정합성 관리 필요
     *       d) 적용 시기: 자주 조회되고 변경 빈도 낮은 데이터
     *     - 인덱스 최적화
     *       a) 구현: CREATE INDEX idx_category ON product(category)
     *       b) 장점: 조회 성능 향상
     *       c) 단점: 인덱스 저장 공간 필요
     *       d) 적용 시기: category 기준 조회가 빈번한 경우
     *   6) 트레이드오프:
     *     - 파라미터 검증: 검증 로직 추가로 코드 복잡해지지만, 데이터 무결성과 안정성 보장
     *     - Cursor 페이징: 특정 페이지 이동 불가하지만, 대량 데이터 조회 성능 대폭 향상
     *     - Redis 캐싱: 캐시 관리 복잡도 증가하지만, 조회 성능 대폭 향상
     *     - DTO 변환: 변환 로직 추가 필요하지만, API 스펙과 도메인 모델 독립성 확보
     *   7) 선택: 파라미터 검증 + readOnly + DTO 반환 + 인덱스 최적화 (단기) + Cursor 페이징 / Redis 캐싱 (장기)
     *     - 선택 근거:
     *       a) 파라미터 검증으로 안정성 확보
     *       b) 인덱스는 즉시 적용 가능하며 효과가 명확함
     *       c) Cursor 페이징과 Redis 캐싱은 트래픽 증가 시 적용
     * 검증:
     *   1) 음수 page 전달 시 0으로 보정 확인
     *   2) 0 또는 음수 size 전달 시 기본값(10) 적용 확인
     *   3) 과도한 size 전달 시 최대값(100) 적용 확인
     *   4) 정렬 기준 파라미터로 다양한 정렬 옵션 지원 확인
     *   5) readOnly 적용 후 Dirty Checking 비활성화 확인
     *   6) Page<ProductResponse> DTO 반환 확인
     *   7) Cursor 기반 페이징: 수백만건 데이터에서 Offset vs Cursor 응답시간 비교
     *   8) Redis 캐싱: 캐시 히트율 측정, 상품 생성 / 수정 / 삭제 시 캐시 무효화 확인
     *   9) 인덱스: EXPLAIN 쿼리로 Index Scan 확인, 조회 성능 개선 측정
     */
    public Page<Product> getListByCategory(GetProductListRequest dto) {
        PageRequest pageRequest = PageRequest.of(dto.getPage(), dto.getSize(), Sort.by(Sort.Direction.ASC, "category"));
        return productRepository.findAllByCategory(dto.getCategory(), pageRequest);
    }

    /**
     * [Code Review #29]
     * 문제:
     *   1) 캐싱 전략 부재
     *   2) @Transactional(readOnly = true) 미적용
     *   3) 빈 결과 처리 전략 미명시
     * 원인:
     *   1) 카테고리 목록은 자주 변경되지 않는 데이터이므로 매번 DB 조회 시 성능 낭비
     *   2) 조회 메서드에 readOnly 미적용 시 불필요한 체킹 발생
     *   3) 빈 결과 반환 시 null 반환 가능성
     * 개선안:
     *   1) @Cacheable 추가
     *      - 구현: @Cacheable("categories")
     *      - TTL 설정: 1시간 (카테고리 변경 빈도 고려)
     *      - 캐시 무효화: 상품 생성 / 수정 / 삭제 시 @CacheEvict 사용
     *   2) @Transactional(readOnly = true) 추가
     *   3) 빈 결과 처리: Collections.emptyList() 반환 보장
     *   4) 트레이드오프:
     *      - 캐싱: 메모리 사용량 증가 및 캐시 관리 복잡도 증가하지만, 조회 성능 대폭 향상
     *   5) 선택: @Cacheable + readOnly + 빈 결과 처리
     *      - 선택 근거:
     *          a) 카테고리는 변경 빈도가 낮아 캐싱 효과가 큼
     *          b) readOnly로 조회 성능 최적화
     *          c) 빈 결과 처리로 null 방지
     * 검증:
     *   1) @Cacheable 적용 후 첫 조회는 DB, 이후는 캐시에서 조회 확인
     *   2) 성능 테스트: 캐싱 전후 조회 시간 비교 (캐싱 전: 50ms, 캐싱 후: 5ms 이하)
     *   3) 상품 생성 / 수정 / 삭제 시 캐시 무효화 확인
     *   4) 빈 결과 반환 시 빈 리스트 확인 (null 아님)
     *   5) 캐시 TTL 만료 후 재조회 확인
     *   6) readOnly 적용 후 Dirty Checking 비활성화 확인
     */
    public List<String> getUniqueCategories() {
        return productRepository.findDistinctCategories();
    }
}
