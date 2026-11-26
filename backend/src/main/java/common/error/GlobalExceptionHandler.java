package common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 우리가 던지는 예외: throw new ApiException(ErrorCode.XXX)
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException e) {
        return respond(e.getErrorCode(), e.getMessage());
    }

    // 요청이 잘못된 경우 (깨진 JSON, 타입 불일치, 필수 파라미터 누락)
    @ExceptionHandler({ HttpMessageNotReadableException.class,
                        MethodArgumentTypeMismatchException.class,
                        MissingServletRequestParameterException.class,
                        MissingServletRequestPartException.class })
    public ResponseEntity<ApiError> handleBadRequest(Exception e) {
        return respond(ErrorCode.BAD_REQUEST);
    }

    // 없는 주소
    @ExceptionHandler({ NoHandlerFoundException.class, NoResourceFoundException.class })
    public ResponseEntity<ApiError> handleNotFound(Exception e) {
        return respond(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(Exception e) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(Exception e) {
        return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    // 나머지 전부: 상세 내용은 서버 로그에만 남기고 응답에는 노출하지 않는다
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(Exception e) {
        log.error("처리되지 않은 예외", e);
        return respond(ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ApiError> respond(ErrorCode ec) {
        return respond(ec, ec.getDefaultMessage());
    }

    private ResponseEntity<ApiError> respond(ErrorCode ec, String message) {
        return ResponseEntity.status(ec.getStatus())
                .contentType(MediaType.APPLICATION_JSON)   // Accept 헤더와 상관없이 JSON 고정
                .body(new ApiError(ec.name(), message));
    }
}