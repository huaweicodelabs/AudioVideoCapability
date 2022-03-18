/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2021. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.wearablewearengine.slice;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.value.LottieAnimationViewData;
import com.huawei.wearablewearengine.ResourceTable;
import com.huawei.wearablewearengine.utils.Constants;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;

import java.util.Timer;
import java.util.TimerTask;

import static com.huawei.wearablewearengine.utils.Constants.TIMER_MILLISECOND;

/**
 * SplashAbilitySlice ability slice part of Main ability.
 * Showing splash screen.
 */
public class SplashAbilitySlice extends AbilitySlice {

    /**
     * Abilityslice lifecycle method onStart.
     * @param intent as Intent
     */
    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_splash);

        LottieAnimationView lv = (LottieAnimationView)findComponentById(ResourceTable.Id_animationView);
        LottieAnimationViewData data = new LottieAnimationViewData();
        data.setUrl(Constants.LOTTIE_ANIMATION_URL);
        data.autoPlay = true;
        data.setRepeatCount(10); // specify repetition count
        lv.setAnimationData(data);

        navigateToOnlineDeviceScreen();
    }

    /**
     * Navigate to app online device screen.
     */
    private void navigateToOnlineDeviceScreen() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                present(new DashboardAbilitySlice(), new Intent());
                terminate();
            }
        }, TIMER_MILLISECOND);
    }

    /**
     * Abilityslice lifecycle method onActive.
     */
    @Override
    public void onActive() {
        super.onActive();
    }

    /**
     * Abilityslice lifecycle method onForeground.
     * @param intent as Intent
     */
    @Override
    public void onForeground(Intent intent) {
        super.onForeground(intent);
    }
}
