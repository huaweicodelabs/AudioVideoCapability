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

import com.huawei.wearablewearengine.utils.LogUtil;
import ohos.aafwk.ability.Ability;
import ohos.rpc.IRemoteBroker;
import ohos.rpc.IRemoteObject;
import ohos.rpc.MessageParcel;
import ohos.rpc.RemoteObject;
import ohos.rpc.MessageOption;

public class ProcessResponseRemote extends RemoteObject implements IRemoteBroker {
    private Ability ability;
    private final int REMOTE_COMMAND = 0;
    private String remoteDeviceId;
    private DistributedNotificationPlugin distributedNotificationPlugin;
    private boolean isFirstTime = false;

    public ProcessResponseRemote(Ability ability) {
        super("Audio Player Remote");
        this.ability = ability;
        distributedNotificationPlugin = DistributedNotificationPlugin.getInstance();
    }

    @Override
    public IRemoteObject asObject() {
        return this;
    }

    @Override
    public boolean onRemoteRequest(int code, MessageParcel data, MessageParcel reply, MessageOption option) {
        if (code == REMOTE_COMMAND) {
            LogUtil.debug("TAG","Response_command begin");
            String command = data.readString();
            String imageURL = data.readString();
            String songName = data.readString();
            String mode = data.readString();
            String status = data.readString();
            String singerName = data.readString();
            String errorMsg = data.readString();
            int position = data.readInt();
            String deviceId = data.readString();
            remoteDeviceId = deviceId;
            LogUtil.debug("TAG","Response_command: "+command);
            LogUtil.debug("TAG","Response_command: "+imageURL);
            LogUtil.debug("TAG","Response_command: "+songName);
            LogUtil.debug("TAG","Response_command: "+mode);
            LogUtil.debug("TAG","Response_command: "+status);
            LogUtil.debug("TAG","Response_command: "+singerName);
            LogUtil.debug("TAG","Response_command: "+errorMsg);
            LogUtil.debug("TAG","Response_command: "+position);
            LogUtil.debug("TAG","Response_remote_deviceid: "+remoteDeviceId);

            distributedNotificationPlugin.publishEvent(command, imageURL, songName, mode, status, singerName, errorMsg, position);
            LogUtil.debug("TAG","Response_command completed");

            return true;
        }
        return false;
    }
}