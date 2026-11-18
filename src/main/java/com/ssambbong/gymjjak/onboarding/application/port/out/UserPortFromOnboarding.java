package com.ssambbong.gymjjak.onboarding.application.port.out;

public interface UserPortFromOnboarding {
    boolean existsById(Long userId);

    void completeOnboarding(Long userId);

    /** 데모 초기화용: 로그인 아이디로 사용자 ID를 찾는다. */
    java.util.Optional<Long> findIdByUsername(String username);

    /** 데모 초기화용: 사용자의 온보딩 완료 상태를 되돌린다. */
    void resetOnboarding(Long userId);

}
