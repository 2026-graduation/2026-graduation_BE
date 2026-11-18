package com.ssambbong.gymjjak.onboarding.application.port.in;

import com.ssambbong.gymjjak.onboarding.application.command.RegisterOnboardingCommand;
import com.ssambbong.gymjjak.onboarding.application.command.UpdateOnboardingCommand;
import com.ssambbong.gymjjak.onboarding.application.result.MyOnboardingResult;

public interface OnboardingUsecase {

    void register(RegisterOnboardingCommand command);

    MyOnboardingResult getMyOnboarding(Long userId);

    void updateOnboarding(UpdateOnboardingCommand command);

    /** 데모 초기화용: 로그인 아이디의 온보딩 정보를 지우고 미완료 상태로 되돌린다. */
    void resetOnboardingByUsername(String username);
}
