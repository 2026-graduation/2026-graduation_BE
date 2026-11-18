package com.ssambbong.gymjjak.onboarding.application.port.out;

import com.ssambbong.gymjjak.onboarding.adapter.out.persistence.OnboardingSurveyJpaEntity;
import com.ssambbong.gymjjak.onboarding.domain.model.OnboardingSurvey;
import com.ssambbong.gymjjak.onboarding.domain.model.Region;

import java.util.Optional;

public interface OnboardingPort {

    boolean existsByUserId(Long userId);

    Long saveRegion(Region region);

    Long saveOnboardingSurvey(OnboardingSurvey onboardingSurvey);

    Optional<MyOnboardingView> findMyOnboardingByUserId(Long userId);

    void updateRegion(Long regionId, Region region);

    Long updateOnboardingSurvey(OnboardingSurvey onboardingSurvey);

    /** 데모 초기화용: 사용자의 온보딩 설문과 선호 지역을 삭제한다. */
    void deleteByUserId(Long userId);

}
