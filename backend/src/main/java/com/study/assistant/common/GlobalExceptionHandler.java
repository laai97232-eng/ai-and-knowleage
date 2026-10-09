package com.study.assistant.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 前端 history 路由回退用的页面，构建产物在 static/index.html */
    private static final Resource INDEX = new ClassPathResource("static/index.html");

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> biz(BizException e) {
        return ResponseEntity.status(e.getCode()).body(R.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> valid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("参数错误");
        return ResponseEntity.badRequest().body(R.fail(400, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> readable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(R.fail(400, "请求格式不正确"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<R<Void>> upload(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest().body(R.fail(400, "文件不能超过 20MB"));
    }

    /**
     * 找不到静态资源。前端用 vue-router 的 history 模式，/login、/admin/courses 这类地址
     * 服务端并没有对应文件，这里回退到 index.html 交给前端路由；
     * 但 /api、上传文件以及 /assets 下的真实资源仍然按 404 处理。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> notFound(NoResourceFoundException e) {
        String path = "/" + e.getResourcePath();
        boolean backendPath = path.startsWith("/api") || path.startsWith("/uploads") || path.startsWith("/assets");
        if (!backendPath && INDEX.exists()) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(INDEX);
        }
        return ResponseEntity.status(404).body(R.fail(404, "资源不存在"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> other(Exception e) {
        log.error("unhandled", e);
        return ResponseEntity.internalServerError().body(R.fail(500, "服务器内部错误"));
    }
}
