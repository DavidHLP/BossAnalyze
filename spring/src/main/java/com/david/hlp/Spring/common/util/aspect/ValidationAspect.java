package com.david.hlp.Spring.common.util.aspect;

import com.david.hlp.Spring.common.util.annotation.validatestring.ValidateString;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
public class ValidationAspect {

    @Around("execution(* com.david.hlp.Spring.**.controller..*(..))")
    public Object validateStringParameters(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(ValidateString.class) && args[i] instanceof String) {
                String value = (String) args[i];
                if (value == null || value.isEmpty() || "null".equals(value) || "undefined".equals(value)) {
                    args[i] = null;
                }
            }
        }
        // 使用修改后的参数执行原始方法
        return joinPoint.proceed(args);
    }
}