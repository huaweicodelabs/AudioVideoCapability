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

import com.huawei.wearablewearengine.controller.DistributedNotificationPlugin;
import com.huawei.wearablewearengine.model.PlayItemModel;
import com.huawei.wearablewearengine.utils.CommonFunctions;
import com.huawei.wearablewearengine.utils.Constants;
import com.huawei.wearablewearengine.utils.LogUtil;
import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;
import ohos.event.notification.NotificationRequest;
import ohos.media.common.Source;
import ohos.media.player.Player;
import ohos.powermanager.PowerManager;
import ohos.rpc.IRemoteObject;

/**
 * PlayerServiceAbility service class for playing audio in both foreground and background.
 */
public class PlayerServiceAbility extends Ability {
    private Player mPlayer;
    private static String TAG = "Player";
    private Source source;
    private String status = "";
    private int position = 0;
    private String errorMsg = "";
    private PlayItemModel playItemModel = new PlayItemModel();
    private DistributedNotificationPlugin distributedNotificationPlugin = DistributedNotificationPlugin.getInstance();

    /**
     * Ability lifecycle method onStart.
     * @param intent as Intent
     */
    @Override
    public void onStart(Intent intent) {
        CommonFunctions.getInstance().setMarkLockAsScreenOn();

        LogUtil.warn(TAG, "--------PlayerServiceAbility::onStart-----");
        super.onStart(intent);
        NotificationRequest request = new NotificationRequest(1005);
        NotificationRequest.NotificationNormalContent content = new NotificationRequest.NotificationNormalContent();
        NotificationRequest.NotificationContent notificationContent = new NotificationRequest.NotificationContent(content);
        request.setContent(notificationContent);
        keepBackgroundRunning(1006,request);
        PowerManager powerManager = new PowerManager();
        PowerManager.RunningLock runningLock = powerManager.createRunningLock("test",PowerManager.RunningLockType.BACKGROUND);
        runningLock.lock(5000000);
    }

    /**
     * Ability lifecycle method onBackground.
     */
    @Override
    public void onBackground() {
        super.onBackground();
        LogUtil.warn(TAG, "--------PlayerServiceAbility::onBackground-----");
    }

    /**
     * Ability lifecycle method onCommand.
     * @param intent as Intent
     * @param restart as boolean
     * @param startId as Intent
     */
    @Override
    public void onCommand(Intent intent, boolean restart, int startId) {
        errorMsg = "";
        if (intent != null) {
            if (intent.hasParameter("PlayItemModel")) {
                playItemModel = intent.getSerializableParam("PlayItemModel");
                LogUtil.debug("TAG"," onCommand serviceparam SongURL "+playItemModel.getSongUrl());
                source = new Source(playItemModel.getSongUrl());
            } else {
                LogUtil.debug("TAG"," onCommand service: no param ");
            }
            if (intent.hasParameter("Status")) {
                status = intent.getStringParam("Status");
                LogUtil.debug("TAG"," onCommand serviceparam Status "+status);
            }
            if (intent.hasParameter("Position")) {
                position = intent.getIntParam("Position", 0);
                LogUtil.debug("TAG"," onCommand serviceparam Position "+position);
            }
        } else {
            LogUtil.debug("TAG"," onCommand intent null ");
        }

        if (status.contains(Constants.PLAY)) {
            if (mPlayer != null) {
                LogUtil.warn(TAG, "Resume player");
                mPlayer.play();
                status = Constants.RESUME;
                sendDataToEvent();
                return;
            }
            mPlayer = new Player(this);
            mPlayer.setPlayerCallback(iPlayerCallback);

            if (!mPlayer.setSource(source)) {
                LogUtil.warn(TAG, "Set audio source failed");
                status = Constants.ERROR;
            }

            if (!mPlayer.prepare()) {
                LogUtil.warn(TAG, "Prepare audio file failed");
                status = Constants.ERROR;
            }

            if (mPlayer.play()) {
                LogUtil.warn(TAG, "Play success");
                errorMsg = "";
                sendDataToEvent();
            } else {
                LogUtil.warn(TAG, "Play failed");
                status = Constants.ERROR;
                sendDataToEvent();
            }
        } else if (status.contains(Constants.NEXT) ||
                status.contains(Constants.PREVIOUS)) {
            if (mPlayer != null) {
                LogUtil.warn(TAG, "Stop and release old player");
                if (mPlayer.isNowPlaying()) {
                    mPlayer.stop();
                }
                mPlayer.release();
            }
            mPlayer = new Player(this);
            mPlayer.setPlayerCallback(iPlayerCallback);

            if (!mPlayer.setSource(source)) {
                LogUtil.warn(TAG, "Set audio source failed");
                status = Constants.ERROR;
            }

            if (!mPlayer.prepare()) {
                LogUtil.warn(TAG, "Prepare audio file failed");
                status = Constants.ERROR;
            }

            if (mPlayer.play()) {
                LogUtil.warn(TAG, "Play success");
                errorMsg = "";
                sendDataToEvent();
            } else {
                LogUtil.warn(TAG, "Play failed");
                status = Constants.ERROR;
                sendDataToEvent();
            }
        } else if (status.contains(Constants.PAUSE)) {
            if (mPlayer.isNowPlaying()) {
                mPlayer.pause();
                sendDataToEvent();
            }
        } else if (status.contains(Constants.STOP)) {
            if (mPlayer.isNowPlaying()) {
                mPlayer.stop();
            }
            mPlayer.release();
            sendDataToEvent();
        }
    }

    /**
     * IRemoteObject onconnect method.
     * @param intent as Intent
     * @return null
     */
    @Override
    public IRemoteObject onConnect(Intent intent) {
        LogUtil.warn(TAG, "----PlayerServiceAbility::onConnect-----");
        return null;
    }

    /**
     * Ability lifecycle method onDisconnect.
     * @param intent as Intent
     */
    @Override
    public void onDisconnect(Intent intent) {
        LogUtil.warn(TAG, "----PlayerServiceAbility::onDisconnect-----");

        if (mPlayer != null) {
            if (mPlayer.isNowPlaying()) {
                mPlayer.pause();
                sendDataToEvent();
            }
        }
    }

    /**
     * Ability lifecycle method onStop.
     */
    @Override
    public void onStop() {
        super.onStop();
        LogUtil.warn(TAG, "--------PlayerServiceAbility::onStop-----");

        if (mPlayer != null) {
            if (mPlayer.isNowPlaying()) {
                mPlayer.pause();
            }
            mPlayer.release();
            sendDataToEvent();
        }
    }

    private void sendDataToEvent() {
        if (playItemModel != null) {
            distributedNotificationPlugin.publishEvent(status,playItemModel.getThumbnailUrl(),playItemModel.getTitle(),playItemModel.getMode(),status, playItemModel.getSinger(), errorMsg, position);
        }
    }

    /**
     * IPlayerCallback for player.
     */
    Player.IPlayerCallback iPlayerCallback = new Player.IPlayerCallback() {
        @Override
        public void onPrepared() {
            LogUtil.warn(TAG, "==============Complete the preparation");
        }

        @Override
        public void onMessage(int i, int i1) {
            LogUtil.warn(TAG, "==============Receive Messages");
        }

        @Override
        public void onError(int errorType, int errorCode) {
            LogUtil.warn(TAG, "==============throw error" );
            errorMsg = "Audio play error. Error code: "+errorCode+" and Error type: "+errorType;
            sendDataToEvent();
        }

        @Override
        public void onResolutionChanged(int i, int i1) {
            LogUtil.warn(TAG, "==============onResolutionChanged" );
        }

        @Override
        public void onPlayBackComplete() {
            LogUtil.warn(TAG, "==============onPlayBackComplete" );
            status = Constants.PLAYBACKCOMPLETE;
            sendDataToEvent();
        }

        @Override
        public void onRewindToComplete() {
            LogUtil.warn(TAG, "==============onRewindToComplete" );
        }

        @Override
        public void onBufferingChange(int i) {
            LogUtil.warn(TAG, "==============onBufferingChange" );
        }

        @Override
        public void onNewTimedMetaData(Player.MediaTimedMetaData mediaTimedMetaData) {
        }

        @Override
        public void onMediaTimeIncontinuity(Player.MediaTimeInfo mediaTimeInfo) {
        }
    };
}