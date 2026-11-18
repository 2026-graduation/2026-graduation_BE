package com.ssambbong.gymjjak.demo;

import com.ssambbong.gymjjak.global.infrastructure.demo.DemoOnboardingResetAspect;
import com.ssambbong.gymjjak.global.infrastructure.demo.DemoOnboardingResetProperties;
import com.ssambbong.gymjjak.onboarding.application.port.in.OnboardingUsecase;
import com.ssambbong.gymjjak.onboarding.application.port.out.OnboardingCacheEvictionPort;
import com.ssambbong.gymjjak.onboarding.application.port.out.OnboardingPort;
import com.ssambbong.gymjjak.onboarding.application.port.out.UserPortFromOnboarding;
import com.ssambbong.gymjjak.onboarding.application.service.OnboardingService;
import com.ssambbong.gymjjak.user.application.command.LoginCommand;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DemoOnboardingResetTest {

    @Test
    @DisplayName("대상 아이디가 비어 있으면(기본값) 어떤 계정도 초기화하지 않는다")
    void properties_empty_default() {
        DemoOnboardingResetProperties properties = new DemoOnboardingResetProperties(List.of());

        assertThat(properties.shouldReset("member1@gymjjak.test")).isFalse();
        assertThat(properties.shouldReset(null)).isFalse();
    }

    @Test
    @DisplayName("지정한 아이디만 초기화 대상이며 공백은 무시한다")
    void properties_match_trimmed_usernames() {
        DemoOnboardingResetProperties properties =
                new DemoOnboardingResetProperties(List.of(" member1@gymjjak.test ", "", "member2@gymjjak.test"));

        assertThat(properties.shouldReset("member1@gymjjak.test")).isTrue();
        assertThat(properties.shouldReset("member2@gymjjak.test")).isTrue();
        assertThat(properties.shouldReset("member3@gymjjak.test")).isFalse();
    }

    @Test
    @DisplayName("온보딩 초기화는 설문 삭제, 완료 상태 되돌리기, 캐시 삭제를 수행한다")
    void service_reset_existing_user() {
        OnboardingPort onboardingPort = mock(OnboardingPort.class);
        UserPortFromOnboarding userPort = mock(UserPortFromOnboarding.class);
        OnboardingCacheEvictionPort cacheEvictionPort = mock(OnboardingCacheEvictionPort.class);
        OnboardingService service = new OnboardingService(onboardingPort, userPort, cacheEvictionPort);
        when(userPort.findIdByUsername("member1@gymjjak.test")).thenReturn(Optional.of(7L));

        service.resetOnboardingByUsername("member1@gymjjak.test");

        verify(onboardingPort).deleteByUserId(7L);
        verify(userPort).resetOnboarding(7L);
        verify(cacheEvictionPort).evictMyOnboarding(7L);
    }

    @Test
    @DisplayName("존재하지 않는 아이디면 아무것도 하지 않는다")
    void service_reset_unknown_user() {
        OnboardingPort onboardingPort = mock(OnboardingPort.class);
        UserPortFromOnboarding userPort = mock(UserPortFromOnboarding.class);
        OnboardingCacheEvictionPort cacheEvictionPort = mock(OnboardingCacheEvictionPort.class);
        OnboardingService service = new OnboardingService(onboardingPort, userPort, cacheEvictionPort);
        when(userPort.findIdByUsername("nobody@gymjjak.test")).thenReturn(Optional.empty());

        service.resetOnboardingByUsername("nobody@gymjjak.test");

        verifyNoInteractions(onboardingPort, cacheEvictionPort);
    }

    @Test
    @DisplayName("대상 계정이 로그인하면 로그인 처리 전에 초기화하고, 다른 계정은 건드리지 않는다")
    void aspect_resets_before_login_only_for_target() throws Throwable {
        OnboardingUsecase usecase = mock(OnboardingUsecase.class);
        DemoOnboardingResetAspect aspect = new DemoOnboardingResetAspect(
                new DemoOnboardingResetProperties(List.of("member1@gymjjak.test")), usecase);
        ProceedingJoinPoint target = mock(ProceedingJoinPoint.class);
        ProceedingJoinPoint other = mock(ProceedingJoinPoint.class);

        aspect.resetOnboardingBeforeLogin(target, new LoginCommand("member1@gymjjak.test", "pw"));
        aspect.resetOnboardingBeforeLogin(other, new LoginCommand("member2@gymjjak.test", "pw"));

        InOrder order = inOrder(usecase, target);
        order.verify(usecase).resetOnboardingByUsername("member1@gymjjak.test");
        order.verify(target).proceed();
        verify(usecase, never()).resetOnboardingByUsername("member2@gymjjak.test");
        verify(other).proceed();
    }
}
