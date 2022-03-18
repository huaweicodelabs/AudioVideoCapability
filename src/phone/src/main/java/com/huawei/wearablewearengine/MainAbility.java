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

package com.huawei.wearablewearengine;

import com.huawei.wearablewearengine.model.GrantPermissionModel;
import com.huawei.wearablewearengine.slice.DashboardPlayerAbilitySlice;
import com.huawei.wearablewearengine.slice.SplashAbilitySlice;
import com.huawei.wearablewearengine.utils.Constants;
import com.huawei.wearablewearengine.utils.LogUtil;
import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;
import ohos.bundle.IBundleManager;
import org.greenrobot.eventbus.EventBus;

public class MainAbility extends Ability {
    public static final int REQUEST_CODE = 1;

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setMainRoute(SplashAbilitySlice.class.getName());

        initActionRoute();
    }

    private void initActionRoute() {
        addActionRoute(Constants.PLAYER_LIST_ACTION, DashboardPlayerAbilitySlice.class.getName());
    }

    @Override
    public void onRequestPermissionsFromUserResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            LogUtil.debug(LogUtil.TAG_LOG, "Permission is required to proceed");
            for (int i=0;i<permissions.length;i++) {
                EventBus.getDefault().post(new GrantPermissionModel(permissions[i],grantResults[i] == IBundleManager.PERMISSION_GRANTED));
            }
        }
    }
}
