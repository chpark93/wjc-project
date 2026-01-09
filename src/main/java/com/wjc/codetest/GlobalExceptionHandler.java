package com.wjc.codetest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * [Code Review #8]
 * 문제:
 *   1) 특정 패키지만 예외 처리
 *   2) 불필요한 어노테이션 중복 (@ResponseBody, @ResponseStatus)
 *   3) 구체적인 예외 타입별 처리 부재
 *   4) 에러 응답 본문 없음
 * 원인:
 *   1) 특정 패키지만 지정 시 다른 패키지의 예외는 처리되지 않음
 *   2) ResponseEntity 사용 시 @ResponseBody, @ResponseStatus 자동 적용되어 중복
 *   3) RuntimeException만 처리하여 모든 예외가 500으로 처리됨
 *   4) build()만 사용하여 클라이언트가 오류 원인 파악 불가
 * 개선안:
 *   1) @ControllerAdvice value 제거 -> 전체 애플리케이션 예외 처리
 *   2) @ResponseBody, @ResponseStatus 제거 -> ResponseEntity로 통일
 *   3) 구체적인 예외 타입별 핸들러 추가
 *     - CustomException (Business Exception): 404, 400 등
 *     - MethodArgumentNotValidException (Validation Exception): 400
 *     - RuntimeException (Internal Server Error): 500
 *   4) ErrorResponse DTO 생성 및 본문 포함
 *   5) 트레이드오프:
 *     - 전체 패키지 처리: 특정 컨트롤러 예외 처리 불가하지만, 일관된 예외 처리 가능
 *     - 예외 타입별 처리: 코드 복잡도 증가하지만, 적절한 HTTP 상태 코드 및 메시지 제공
 *     - ErrorResponse DTO: DTO를 추가로 생성해야 하지만, 클라이언트 오류 처리 및 디버깅 용이
 *   6) 선택: 전체 패키지 처리 + 예외 타입별 핸들러 + ErrorResponse DTO
 *     - 선택 근거:
 *       a) 일관된 예외 처리로 유지보수성 향상
 *       b) 예외 타입별 적절한 HTTP 상태 코드 매핑
 *       c) ErrorResponse로 클라이언트 디버깅 용이
 * 검증:
 *   1) value 제거 후 전체 패키지 예외 처리 확인
 *   2) CustomException 발생 시 적절한 HTTP 상태 코드 반환 확인 (404, 400 등)
 *   3) MethodArgumentNotValidException 발생 시 400 Bad Request 반환 확인
 *   4) RuntimeException 발생 시 500 Internal Server Error 반환 확인
 *   5) ErrorResponse DTO 본문 포함 확인
 *   6) 로그와 응답 본문의 에러 정보 일관성 확인
 */
@Slf4j
@ControllerAdvice(value = {"com.wjc.codetest.product.controller"})
public class GlobalExceptionHandler {

    /**
     * [Code Review #9]
     * 문제: RuntimeException을 공통 예외 처리로 사용
     * 원인: RuntimeException은 너무 일반적이어서 비즈니스 예외와 시스템 예외 구분 불가
     * 개선안:
     *   1) CustomException 생성 및 처리
     *     - ProductNotFoundException: 404
     *     - InvalidProductException: 400
     *     - UnauthorizedException: 401
     *     - ForbiddenException: 403
     *   2) RuntimeException은 예상치 못한 서버 내부 예외만 처리 (500)
     *   3) 트레이드오프:
     *     - CustomException: 예외 클래스 추가 필요하지만, 예외 타입별 명확한 처리 가능
     *   4) 선택: CustomException + RuntimeException 분리
     *     - 선택 근거:
     *       a) 비즈니스 예외와 시스템 예외 명확히 구분
     *       b) 예외 타입별 적절한 HTTP 상태 코드 매핑
     * 검증:
     *   1) CustomException 발생 시 적절한 HTTP 상태 코드 반환 확인
     *   2) RuntimeException 발생 시 500 반환 확인
     */
    @ResponseBody
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<String> runTimeException(Exception e) {
        log.error("status :: {}, errorType :: {}, errorCause :: {}",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "runtimeException",
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
