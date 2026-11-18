package com.ssambbong.gymjjak.global.infrastructure.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 시연용: 로그인할 때마다 온보딩 정보를 초기화할 로그인 아이디 목록.
 * <p>
 * 기본값은 비어 있어 아무 동작도 하지 않는다. 로컬 시연 PC에서만
 * {@code DEMO_ONBOARDING_RESET_USERNAMES=member1@gymjjak.test,member2@gymjjak.test} 처럼 지정한다.
 */
@Component
public class DemoOnboardingResetProperties {

    private final List<String> usernames;

    public DemoOnboardingResetProperties(
            @Value("${app.demo.onboarding-reset.usernames:}") List<String> usernames
    ) {
        this.usernames = usernames.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
    }

    public boolean shouldReset(String username) {
        return username != null && usernames.contains(username.trim());
    }
}
