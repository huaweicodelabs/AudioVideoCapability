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

package com.huawei.wearablewearengine.service;

import com.huawei.wearablewearengine.controller.ProcessResponseRemote;
import com.huawei.wearablewearengine.utils.LogUtil;
import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;
import ohos.rpc.IRemoteObject;

import static com.huawei.wearablewearengine.utils.LogUtil.TAG_LOG;

public class MediaPlayerServiceAbility extends Ability {
    private ProcessResponseRemote processResponseRemote = new ProcessResponseRemote(this);

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        LogUtil.debug(TAG_LOG,"MediaPlayerServiceAbility - Watch Started");
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtil.debug(TAG_LOG,"MediaPlayerServiceAbility - Watch Stop");
    }

    @Override
    protected IRemoteObject onConnect(Intent intent) {
        super.onConnect(intent);
        return processResponseRemote.asObject();
    }
}
