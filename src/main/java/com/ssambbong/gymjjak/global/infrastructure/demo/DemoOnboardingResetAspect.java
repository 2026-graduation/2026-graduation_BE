package com.ssambbong.gymjjak.global.infrastructure.demo;

import com.ssambbong.gymjjak.onboarding.application.port.in.OnboardingUsecase;
import com.ssambbong.gymjjak.user.application.command.LoginCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 시연용: 지정된 계정이 로그인하면 로그인 처리 직전에 온보딩 정보를 지워,
 * 로그인할 때마다 온보딩이 다시 진행되도록 한다. 대상 계정이 없으면(기본값) 아무 일도 하지 않는다.
 * 로그인 서비스 코드를 건드리지 않기 위해 AOP로 끼워 넣는다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DemoOnboardingResetAspect {

    private final DemoOnboardingResetProperties properties;
    private final OnboardingUsecase onboardingUsecase;

    @Around("execution(* com.ssambbong.gymjjak.user.application.port.in.UserCommandUseCase.login(..)) && args(command)")
    public Object resetOnboardingBeforeLogin(ProceedingJoinPoint joinPoint, LoginCommand command) throws Throwable {
        if (properties.shouldReset(command.username())) {
            log.info("event=demo_onboarding_reset_before_login username={}", command.username());
            onboardingUsecase.resetOnboardingByUsername(command.username());
        }
        return joinPoint.proceed();
    }
}
