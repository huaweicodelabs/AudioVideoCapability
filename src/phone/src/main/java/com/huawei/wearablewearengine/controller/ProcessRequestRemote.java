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

package com.huawei.wearablewearengine.controller;

import com.huawei.wearablewearengine.MainAbility;
import com.huawei.wearablewearengine.utils.Constants;
import com.huawei.wearablewearengine.utils.LogUtil;
import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.rpc.IRemoteBroker;
import ohos.rpc.IRemoteObject;
import ohos.rpc.MessageParcel;
import ohos.rpc.RemoteObject;
import ohos.rpc.MessageOption;

import static com.huawei.wearablewearengine.utils.Constants.PLAYER_LIST_ACTION;

public class ProcessRequestRemote extends RemoteObject implements IRemoteBroker {
    private Ability ability;
    private final int REMOTE_COMMAND = 0;
    private String remoteDeviceId;
    private DistributedNotificationPlugin distributedNotificationPlugin;

    public ProcessRequestRemote(Ability ability) {
        super("Audio Player Remote");
        this.ability = ability;
        distributedNotificationPlugin = DistributedNotificationPlugin.getInstance();
    }

    @Override
    public IRemoteObject asObject() {
        return this;
    }

    // Sets an entry for receiving requests.
    @Override
    public boolean onRemoteRequest(int code, MessageParcel data, MessageParcel reply, MessageOption option) {
        if (code == REMOTE_COMMAND) {
            String command = data.readString();
            String mode = data.readString();
            int position = data.readInt();
            String deviceId = data.readString();
            remoteDeviceId = deviceId;

            LogUtil.debug("TAG","Request_remote_deviceid: "+remoteDeviceId);
            LogUtil.debug("TAG","Request_command: "+command);
            if (Constants.START.equals(command)) {
                LogUtil.debug("TAG","Request_command First Time");
                openAbility();

                distributedNotificationPlugin.subscribeEvent();
                distributedNotificationPlugin.publishEvent(command, mode, position, deviceId);
            } else {
                LogUtil.debug("TAG","Request_command Second Time");
                distributedNotificationPlugin.publishEvent(command, mode, position, deviceId);
            }

            return true;
        }
        return false;
    }

    private void openAbility() {
        Intent intent = new Intent();
        Operation operation = new Intent.OperationBuilder()
                .withBundleName(ability.getBundleName())
                .withAbilityName(MainAbility.class.getName())
                .withAction(PLAYER_LIST_ACTION)
                .build();
        intent.setOperation(operation);
        intent.setParam("DeviceId",remoteDeviceId);
        ability.startAbility(intent);
    }
}
