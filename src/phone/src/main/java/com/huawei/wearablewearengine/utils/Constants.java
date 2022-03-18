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

package com.huawei.wearablewearengine.utils;


public class Constants {
    public static final String API_BASE_URL = "YOUR_BASE_URL";

    public static final String ONLINE_AUDIO = "online_audio";
    public static final String ONLINE_VIDEO = "online_video";

    public static final String OFFLINE_AUDIO = "offline_audio";

    public static final String ACKNOWLEDGE = "acknowledge";
    public static final String START = "start";
    public static final String PING = "ping";

    public static final String PLAY = "play";
    public static final String PLAYING = "playing";
    public static final String PAUSE = "pause";
    public static final String NEXT = "next";
    public static final String PREVIOUS = "previous";

    public static final String FORWARD = "forward";
    public static final String REWIND = "rewind";

    public static final String COMPLETE = "complete";

    public static final String ERROR = "error";

    public static final String FINISH = "finish";

    public static final String PLAYER_LIST_ACTION = "action.player.list";
    public static final String VIDEO_PLAYER_ACTION = "action.slice.videoplayer";

    public static final int REQUEST_CODE = 0;
    public static final int TIMER_MILLISECOND = 3000;
    public static final int TOAST_DURATION_MILLISECOND = 2000;

    public static final String PACKAGE_NAME = "com.huawei.wearablewearengine";
    public static final String VIDEO_ABILITY = "com.huawei.wearablewearengine.VideoPlayerAbility";
    public static final String ACK_RESPONSE_SERVICEABILITY = "com.huawei.wearablewearengine.service.MediaPlayerServiceAbility";

    private static volatile Constants sInstance = null;

    /**
     * Initialize Instance.
     * @return return Instance
     */
    public static Constants getInstance() {
        if (sInstance == null) {
            synchronized (Constants.class) {
                if (sInstance == null) {
                    sInstance = new Constants();
                }
            }
        }
        return sInstance;
    }
}
