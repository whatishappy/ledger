package com.ledger.common.exception;

import com.ledger.common.result.Result;
import com.ledger.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: uri={}, code={}, msg={}", request.getRequestURI(), e.getResultCode().getCode(), e.getMessage());
        return Result.fail(e.getResultCode(), e.getMessage());
    }

    /**
     * 参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return Result.fail(ResultCode.PARAM_VALIDATION_FAILED, message);
    }

    /**
     * 绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return Result.fail(ResultCode.PARAM_VALIDATION_FAILED, message);
    }

    /**
     * 缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.fail(ResultCode.PARAM_VALIDATION_FAILED, "缺少必要参数: " + e.getParameterName());
    }

    /**
     * 请求体不可读
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return Result.fail(ResultCode.PARAM_VALIDATION_FAILED, "请求体格式错误");
    }

    /**
     * 不支持的请求方法
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("不支持的请求方法: {}", e.getMethod());
        return Result.fail(ResultCode.NOT_FOUND, "请求方法不支持: " + e.getMethod());
    }

    /**
     * 认证异常（Token 无效等）
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        return Result.fail(ResultCode.UNAUTHORIZED, e.getMessage());
    }

    /**
     * 权限不足
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.fail(ResultCode.FORBIDDEN);
    }

    /**
     * 非法参数
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.fail(ResultCode.PARAM_VALIDATION_FAILED, e.getMessage());
    }

    /**
     * 参数类型转换异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型转换失败: name={}, value={}", e.getName(), e.getValue());
        return Result.fail(ResultCode.PARAM_VALIDATION_FAILED, "参数类型错误: " + e.getName());
    }

    /**
     * 数据访问异常（SQL异常、连接异常等）
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleDataAccessException(DataAccessException e, HttpServletRequest request) {
        log.error("数据访问异常: uri={}", request.getRequestURI(), e);
        return Result.fail(ResultCode.INTERNAL_SERVER_ERROR, "数据访问异常，请稍后重试");
    }

    /**
     * SQL异常
     */
    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleSQLException(SQLException e, HttpServletRequest request) {
        log.error("SQL异常: uri={}, sqlState={}, errorCode={}", request.getRequestURI(), e.getSQLState(), e.getErrorCode(), e);
        return Result.fail(ResultCode.INTERNAL_SERVER_ERROR, "数据库异常，请稍后重试");
    }

    /**
     * 兜底：其他未捕获异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: uri={}", request.getRequestURI(), e);
        return Result.fail(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
