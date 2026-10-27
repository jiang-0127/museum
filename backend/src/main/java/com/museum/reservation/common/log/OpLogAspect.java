package com.museum.reservation.common.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.museum.reservation.common.IpUtil;
import com.museum.reservation.module.system.entity.OperationLog;
import com.museum.reservation.module.system.mapper.OperationLogMapper;
import com.museum.reservation.security.CurrentUser;
import com.museum.reservation.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OpLogAspect {

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;
        Object result;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            errorMessage = e.getMessage();
            throw e;
        } finally {
            try {
                saveLog(joinPoint, opLog, success, errorMessage, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("保存操作日志失败: {}", e.getMessage());
            }
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, OpLog opLog, boolean success,
                         String errorMessage, long duration) {
        HttpServletRequest request = currentRequest();
        LoginUser user = CurrentUser.get();
        OperationLog entity = new OperationLog();
        if (user != null) {
            entity.setUserId(user.getId());
            entity.setUsername(user.getUsername());
        } else {
            entity.setUsername("anonymous");
        }
        entity.setModule(opLog.module());
        entity.setOperation(opLog.operation());
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        entity.setMethod(signature.getDeclaringType().getSimpleName() + "." + signature.getName());
        if (opLog.saveParams()) {
            entity.setParams(maskSensitive(serializeArgs(joinPoint.getArgs())));
        }
        entity.setIp(IpUtil.getIp(request));
        entity.setDurationMs(duration);
        entity.setSuccess(success ? 1 : 0);
        entity.setErrorMsg(errorMessage == null ? null : errorMessage.substring(0, Math.min(errorMessage.length(), 500)));
        operationLogMapper.insert(entity);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            Object[] filtered = Arrays.stream(args)
                    .filter(arg -> !(arg instanceof HttpServletRequest) && !(arg instanceof HttpServletResponse))
                    .toArray();
            String json = objectMapper.writeValueAsString(filtered);
            return json.length() > 2000 ? json.substring(0, 2000) + "..." : json;
        } catch (Exception e) {
            return null;
        }
    }

    private String maskSensitive(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("(\"(?:password|passwordHash|oldPassword|newPassword)\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(\\d{6})(\\d{8})(\\d{3}[0-9Xx])", "$1********$3");
    }
}
