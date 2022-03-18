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

import com.bumptech.glide.Glide;
import com.huawei.wearablewearengine.ResourceTable;
import com.huawei.wearablewearengine.controller.DistributedNotificationPlugin;
import com.huawei.wearablewearengine.controller.SendRequestRemote;
import com.huawei.wearablewearengine.manager.MediaPlayerPlugin;
import com.huawei.wearablewearengine.model.PlayItemModel;
import com.huawei.wearablewearengine.utils.CommonFunctions;
import com.huawei.wearablewearengine.utils.Constants;
import com.huawei.wearablewearengine.utils.LogUtil;
import com.huawei.wearablewearengine.utils.PreferenceUtil;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.ability.IAbilityConnection;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.Text;
import ohos.bundle.AbilityInfo;
import ohos.bundle.ElementName;
import ohos.bundle.IBundleManager;
import ohos.data.distributed.common.KvManagerConfig;
import ohos.data.distributed.common.KvManagerFactory;
import ohos.distributedschedule.interwork.DeviceInfo;
import ohos.distributedschedule.interwork.DeviceManager;
import ohos.distributedschedule.interwork.IDeviceStateCallback;
import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import ohos.eventhandler.InnerEvent;
import ohos.media.audio.AudioManager;
import ohos.media.audio.AudioRemoteException;
import ohos.rpc.IRemoteObject;
import ohos.rpc.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.huawei.wearablewearengine.utils.Constants.PREVIOUS;
import static com.huawei.wearablewearengine.utils.Constants.ONLINE_AUDIO;
import static com.huawei.wearablewearengine.utils.Constants.PAUSE;
import static com.huawei.wearablewearengine.utils.Constants.PLAY;
import static com.huawei.wearablewearengine.utils.Constants.NEXT;
import static com.huawei.wearablewearengine.utils.Constants.ONLINE_VIDEO;
import static com.huawei.wearablewearengine.utils.Constants.VOLUME_ABILITY_SLICE;
import static com.huawei.wearablewearengine.utils.Constants.PLAYERSERVICEABILITY;
import static com.huawei.wearablewearengine.utils.Constants.ONLINE_DEVICE_SLICE;
import static com.huawei.wearablewearengine.utils.Constants.MAIN_ABILITY;
import static com.huawei.wearablewearengine.utils.Constants.PACKAGE_NAME;
import static com.huawei.wearablewearengine.utils.Constants.PLAYBACKCOMPLETE;
import static com.huawei.wearablewearengine.utils.Constants.PING;
import static com.huawei.wearablewearengine.utils.Constants.RESUME;
import static com.huawei.wearablewearengine.utils.Constants.REQUEST_CODE;
import static com.huawei.wearablewearengine.utils.LogUtil.TAG_LOG;

public class PlayerControllerAbilitySlice extends AbilitySlice implements DistributedNotificationPlugin.DistributedNotificationEventListener {

    private Image img_previous;
    private Image img_play;
    private Image img_next;
    private Image img_thumbnail;
    private Image img_volume;
    private Image img_cast;
    private com.andexert.library.RippleView rippleview_previous;
    private com.andexert.library.RippleView rippleview_play;
    private com.andexert.library.RippleView rippleview_next;
    private com.andexert.library.RippleView rippleview_volume;
    private com.andexert.library.RippleView rippleview_cast;
    private String currentDeviceId = "";
    private static final int EVENT_STATE_CHANGE = 10001;
    private SendRequestRemote requestRemoteProxy;
    private boolean isPlaying= false;
    private DistributedNotificationPlugin distributedNotificationPlugin;
    private Text title;
    private Text tv_song_title;
    private Text tv_song_author;

    private ArrayList<PlayItemModel> onlineSongsList = new ArrayList<>();
    private PlayItemModel playItemModel = new PlayItemModel();
    private int currentPosition = 0;
    private boolean isCasting = false;
    private String mStreaming = "";
    private AudioManager audioManager;
    private PreferenceUtil preferenceUtil;

    private Timer timer = new Timer();
    private ProgressTimerTask progressTimerTask;

    private MediaPlayerPlugin mediaPlayerPlugin;

    /**
     * Abilityslice lifecycle method onStart.
     * @param intent as Intent
     */
    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_play_contoller);

        if (intent != null) {
            if (intent.hasParameter("isCasting")) {
                isCasting = intent.getBooleanParam("isCasting",false);
            }
            if (intent.hasParameter("OnlineVideoList")) {
                onlineSongsList = intent.getSerializableParam("OnlineVideoList");
            }
            if (intent.hasParameter("SelectedSongDetails")) {
                playItemModel = intent.getSerializableParam("SelectedSongDetails");
            }
            if (intent.hasParameter("CurrentPosition")) {
                currentPosition = intent.getIntParam("CurrentPosition",0);
            }
            if (intent.hasParameter("Streaming")) {
                mStreaming = intent.getStringParam("Streaming");
            }

            if (intent.hasParameter(Constants.DEVICE_ID_KEY)) {
                Object obj= intent.getParams().getParam(Constants.DEVICE_ID_KEY);
                if (obj instanceof String) {
                    currentDeviceId = (String) obj;
                    connectToRemoteService();
                } else {
                    if (isCasting) {
                        getPhoneDevice();
                    }
                }
            } else {
                if (isCasting) {
                    getPhoneDevice();
                }
            }
        }

        CommonFunctions.getInstance().setMarkLockAsScreenOn();

        initAudio();
        initView();
    }

    private void initView() {
        rippleview_previous = (com.andexert.library.RippleView) findComponentById(ResourceTable.Id_ripple_previous);
        rippleview_play = (com.andexert.library.RippleView) findComponentById(ResourceTable.Id_ripple_play);
        rippleview_next = (com.andexert.library.RippleView) findComponentById(ResourceTable.Id_ripple_next);
        rippleview_volume = (com.andexert.library.RippleView) findComponentById(ResourceTable.Id_ripple_volume);
        rippleview_cast = (com.andexert.library.RippleView) findComponentById(ResourceTable.Id_ripple_cast);

        img_previous = (Image) findComponentById(ResourceTable.Id_img_previous);
        img_play = (Image) findComponentById(ResourceTable.Id_img_play);
        img_next = (Image) findComponentById(ResourceTable.Id_img_next);
        img_thumbnail = (Image) findComponentById(ResourceTable.Id_img_thumbnail);
        img_volume = (Image) findComponentById(ResourceTable.Id_img_volume);
        img_cast = (Image) findComponentById(ResourceTable.Id_img_cast);

        tv_song_author = (Text) findComponentById(ResourceTable.Id_tv_song_author);
        tv_song_title = (Text) findComponentById(ResourceTable.Id_tv_song_title);
        title = (Text) findComponentById(ResourceTable.Id_title);
        title.setText("Controller");
        tv_song_title.setText("Fetching song details...");

        Glide.with(getContext())
                .load(ResourceTable.Media_OttSplash)
                .into(img_thumbnail);

        rippleview_previous.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                if (isCasting) {
                    if (requestRemoteProxy != null) {
                        sendRequestRemote(currentPosition-1,PREVIOUS);
                    } else {
                        CommonFunctions.getInstance().showToast(PlayerControllerAbilitySlice.this,"Device not connected");
                    }
                } else {
                    if (mStreaming.contains(ONLINE_AUDIO)) {
                        if (onlineSongsList != null && onlineSongsList.size() >= 1) {
                            play(currentPosition-1);
                        }
                    }
                }
            }
        });

        rippleview_play.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                if (isCasting) {
                    if (requestRemoteProxy != null) {
                        if (isPlaying) {
                            sendRequestRemote(currentPosition,PAUSE);
                        } else {
                            sendRequestRemote(currentPosition,PLAY);
                        }
                    } else {
                        CommonFunctions.getInstance().showToast(PlayerControllerAbilitySlice.this,"Device not connected");
                    }
                } else {
                    if (mStreaming.contains(ONLINE_AUDIO)) {
                        if (onlineSongsList != null && onlineSongsList.size() >= 1) {
                            if (mediaPlayerPlugin.isPlaying()) {
                                pause();
                            } else {
                                play(currentPosition, PLAY);
                            }
                        }
                    }
                }
            }
        });

        rippleview_next.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                if (isCasting) {
                    if (requestRemoteProxy != null) {
                        sendRequestRemote(currentPosition+1,NEXT);
                    } else {
                        CommonFunctions.getInstance().showToast(PlayerControllerAbilitySlice.this,"Device not connected");
                    }
                } else {
                    if (mStreaming.contains(ONLINE_AUDIO)) {
                        if (onlineSongsList != null && onlineSongsList.size() >= 1) {
                            play(currentPosition+1);
                        }
                    }
                }
            }
        });

        rippleview_volume.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                navigateToVolumeControl();
            }
        });

        rippleview_cast.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                if (!isCasting) {
                    Intent intent = new Intent();
                    Operation operation = new Intent.OperationBuilder()
                            .withDeviceId("")
                            .withBundleName(PACKAGE_NAME)
                            .withAbilityName(MAIN_ABILITY)
                            .withAction(ONLINE_DEVICE_SLICE)
                            .build();
                    intent.setOperation(operation);
                    intent.setParam("isCasting", true);
                    intent.setParam("OnlineVideoList", onlineSongsList);
                    intent.setParam("SelectedSongDetails", playItemModel);
                    intent.setParam("CurrentPosition", currentPosition);
                    if (mStreaming.contains(ONLINE_AUDIO)) {
                        intent.setParam("Streaming", ONLINE_AUDIO);
                    } else {
                        intent.setParam("Streaming", ONLINE_VIDEO);
                    }
                    presentForResult(new OnlineDeviceAbilitySlice(), intent, REQUEST_CODE);
                }
            }
        });

        distributedNotificationPlugin = DistributedNotificationPlugin.getInstance();
        distributedNotificationPlugin.setEventListener(this);
        distributedNotificationPlugin.subscribeEvent();

        DeviceManager.registerDeviceStateCallback(iDeviceStateCallback);

        audioManager = new AudioManager(this);

        preferenceUtil = PreferenceUtil.getInstance();

        isCasting = preferenceUtil.getIsCasting();

        if (isCasting) {
            img_cast.setPixelMap(ResourceTable.Media_ic_distribute_white);
            img_volume.setVisibility(Component.HIDE);
        } else {
            img_cast.setPixelMap(ResourceTable.Media_ic_distribute_grey);
            img_volume.setVisibility(Component.VISIBLE);
            if (mStreaming.contains(ONLINE_AUDIO)) {
                if (onlineSongsList != null && onlineSongsList.size() >= 1) {
                    play(currentPosition, PLAY);
                }
            }
        }
    }

    private void initAudio() {
        mediaPlayerPlugin = new MediaPlayerPlugin(this, new MediaPlayerPlugin.MediaPlayerCallback() {
            @Override
            public void onPrepared() {
                LogUtil.error(TAG_LOG, "Mediaplayer onPrepared");
            }

            @Override
            public void onPlayBackComplete() {
                LogUtil.error(TAG_LOG, "Mediaplayer onPlayBackComplete");
                if (!isCasting) {
                    if (mStreaming.contains(ONLINE_AUDIO)) {
                        play(currentPosition+1, PLAY);
                    }
                }
            }

            @Override
            public void onBuffering(int percent) {
                LogUtil.error(TAG_LOG, "Mediaplayer onBuffering");
                if (percent == 100) {
                    LogUtil.error(TAG_LOG, "Mediaplayer onBuffer complete");
                }
            }

            @Override
            public void onError(int errorType, int errorCode) {
                if (!isCasting) {
                    if (mStreaming.contains(ONLINE_AUDIO)) {
                        getUITaskDispatcher().asyncDispatch(() -> {
                            LogUtil.error(TAG_LOG, "Mediaplayer onError" + errorType + ", skip to the next audio");
                            CommonFunctions.getInstance().showToast(PlayerControllerAbilitySlice.this,"Audio play error. Error code: "+errorCode+" and Error type: "+errorType);
                        });
                    }
                }
            }
        });
    }

    private void connectToRemoteService() {
        Intent intent = new Intent();
        Operation operation = new Intent.OperationBuilder()
                .withDeviceId(currentDeviceId)
                .withBundleName(Constants.PACKAGE_NAME)
                .withAbilityName(Constants.SERVICE_ABILITY_NAME)
                .withFlags(Intent.FLAG_ABILITYSLICE_MULTI_DEVICE)
                .build();
        intent.setOperation(operation);
        try {
            List<AbilityInfo> abilityInfos = getBundleManager().queryAbilityByIntent(intent, IBundleManager.GET_BUNDLE_DEFAULT, 0);
            if (abilityInfos != null && !abilityInfos.isEmpty()) {
                connectAbility(intent, iAbilityConnection);
                LogUtil.info(TAG_LOG, "connect service on phone with id " + currentDeviceId );
            } else {
                CommonFunctions.getInstance().showToast(this,"Cannot connect service on phone");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private IAbilityConnection iAbilityConnection = new IAbilityConnection() {
        @Override
        public void onAbilityConnectDone(ElementName elementName, IRemoteObject iRemoteObject, int i) {
            LogUtil.info(TAG_LOG, "ability connect done!");
            String localDeviceId = KvManagerFactory.getInstance()
                    .createKvManager(new KvManagerConfig(PlayerControllerAbilitySlice.this))
                    .getLocalDeviceInfo()
                    .getId();
            LogUtil.info(TAG_LOG, "DeviceID: localDeviceId: "+localDeviceId);
            LogUtil.info(TAG_LOG, "DeviceID: remoteDeviceId: "+currentDeviceId);
            requestRemoteProxy = new SendRequestRemote(getAbility(), iRemoteObject, localDeviceId);
            sendRequestRemote(currentPosition,Constants.START);
        }

        @Override
        public void onAbilityDisconnectDone(ElementName elementName, int i) {
            LogUtil.info(TAG_LOG, "ability disconnect done!");
            disconnectAbility(iAbilityConnection);
        }
    };

    private EventHandler eventHandler = new EventHandler(EventRunner.current()) {
        @Override
        protected void processEvent(InnerEvent event) {
            if (event.eventId == EVENT_STATE_CHANGE) {
                getPhoneDevice();
            }
        }
    };

    private IDeviceStateCallback iDeviceStateCallback = new IDeviceStateCallback() {
        @Override
        public void onDeviceOffline(String deviceId, int deviceType) {
            if (currentDeviceId.equals(deviceId)) {
                CommonFunctions.getInstance().showToast(PlayerControllerAbilitySlice.this,"Device offline");
                disconnectAbility(iAbilityConnection);
            }
        }

        @Override
        public void onDeviceOnline(String deviceId, int deviceType) {
            eventHandler.sendEvent(EVENT_STATE_CHANGE);
        }
    };

    private void getPhoneDevice() {
        List<DeviceInfo> deviceInfoList = DeviceManager.getDeviceList(DeviceInfo.FLAG_GET_ONLINE_DEVICE);
        if (deviceInfoList.isEmpty()) {
            CommonFunctions.getInstance().showToast(this,"No device found");
            LogUtil.debug(TAG_LOG,"No device found");
        } else {
            deviceInfoList.forEach(deviceInfo -> {
                DeviceInfo.DeviceType deviceType = deviceInfo.getDeviceType();
                String deviceID = deviceInfo.getDeviceId();
                LogUtil.info(TAG_LOG, "Found device " + deviceInfo.getDeviceType());
                if (deviceType == DeviceInfo.DeviceType.SMART_PHONE && !deviceID.equals(this.currentDeviceId)) {
                    this.currentDeviceId = deviceID;
                    connectToRemoteService();
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        DeviceManager.unregisterDeviceStateCallback(iDeviceStateCallback);
        if (mediaPlayerPlugin != null) {
            mediaPlayerPlugin.release();
        }
    }

    @Override
    protected void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onEventSubscribe(String result) {
        LogUtil.info(TAG_LOG, result);
    }

    @Override
    public void onEventPublish(String result) {
        LogUtil.info(TAG_LOG, result);
    }

    @Override
    public void onEventUnSubscribe(String result) {
        LogUtil.info(TAG_LOG, result);
    }

    @Override
    public void onEventReceive(String result, String imageURL, String songName, String mode, String status,String singerName, String errorMsg, int position) {
        LogUtil.debug(TAG_LOG,"onEventReceive");
        LogUtil.info(TAG_LOG, result);
        LogUtil.info(TAG_LOG, imageURL);
        LogUtil.info(TAG_LOG, songName);
        LogUtil.info(TAG_LOG, mode);
        LogUtil.info(TAG_LOG, status);
        LogUtil.info(TAG_LOG, singerName);
        LogUtil.info(TAG_LOG, errorMsg);

        if (currentPosition != position) {
            currentPosition = position;
        }

        mStreaming = mode;
        switch (status) {
            case Constants.ACKNOWLEDGE:
            case Constants.PLAY:
            case Constants.NEXT:
            case PREVIOUS:
            case Constants.PAUSE:
            case Constants.ERROR:
            case Constants.COMPLETE:
                processResponse(status, mode, imageURL, songName, singerName, errorMsg, position);
                break;
            case PLAYBACKCOMPLETE:
                if (mStreaming.contains(ONLINE_AUDIO)) {
                    play(currentPosition+1, PLAY);
                }
                break;
            case RESUME:
                isPlaying = true;
                handlingUI();
                break;
            case PING:
                CommonFunctions.getInstance().showToast(PlayerControllerAbilitySlice.this,"Pinged response from phone");
                processResponse(status, mode, imageURL, songName, singerName, errorMsg, position);
                break;
            default:
                // Default case
                break;
        }
    }

    private void setCurrentSongsDetails(String mode, int position) {
        getUITaskDispatcher().asyncDispatch(() -> {
            if (mode.contains(Constants.OFFLINE_AUDIO)) {
                Glide.with(getContext())
                        .load(ResourceTable.Media_OttSplash)
                        .into(img_thumbnail);
            } else {
                Glide.with(getContext())
                        .load(onlineSongsList.get(position).getThumbnailUrl())
                        .placeholder(ResourceTable.Media_OttSplash)
                        .error(ResourceTable.Media_OttSplash)
                        .into(img_thumbnail);
            }

            tv_song_author.setTruncationMode(Text.TruncationMode.AUTO_SCROLLING);
            tv_song_author.setAutoScrollingCount(7);
            tv_song_author.startAutoScrolling();

            tv_song_title.setTruncationMode(Text.TruncationMode.AUTO_SCROLLING);
            tv_song_title.setAutoScrollingCount(7);
            tv_song_title.startAutoScrolling();

            tv_song_title.setText(onlineSongsList.get(position).getTitle());
            tv_song_author.setText(onlineSongsList.get(position).getSinger());
        });
    }

    private void processResponse(String status, String mode, String imageURL, String songName, String singerName, String errorMsg, int position) {
        if (!status.contains(Constants.ERROR)) {
            getUITaskDispatcher().asyncDispatch(() -> {
                if (mode.contains(Constants.OFFLINE_AUDIO)) {
                    Glide.with(getContext())
                            .load(ResourceTable.Media_OttSplash)
                            .into(img_thumbnail);
                } else {
                    Glide.with(getContext())
                            .load(imageURL)
                            .placeholder(ResourceTable.Media_OttSplash)
                            .error(ResourceTable.Media_OttSplash)
                            .into(img_thumbnail);
                }

                tv_song_author.setTruncationMode(Text.TruncationMode.AUTO_SCROLLING);
                tv_song_author.setAutoScrollingCount(7);
                tv_song_author.startAutoScrolling();

                tv_song_title.setTruncationMode(Text.TruncationMode.AUTO_SCROLLING);
                tv_song_title.setAutoScrollingCount(7);
                tv_song_title.startAutoScrolling();

                tv_song_title.setText(songName);
                tv_song_author.setText(singerName);

                try {
                    int defaultVolumn = audioManager.getVolume(AudioManager.AudioVolumeType.STREAM_MUSIC);
                    if (defaultVolumn == 0) {
                        img_volume.setPixelMap(ResourceTable.Media_ic_no_audio);
                    } else {
                        img_volume.setPixelMap(ResourceTable.Media_ic_audio);
                    }
                } catch (AudioRemoteException e) {
                    e.printStackTrace();
                }

                if (status.contains(Constants.ACKNOWLEDGE)
                        || status.contains(PLAY)
                        || status.contains(RESUME)
                        || status.contains(Constants.NEXT)
                        || status.contains(PREVIOUS)
                        || status.contains(Constants.PING)) {
                    isPlaying = true;

                    img_play.setPixelMap(ResourceTable.Media_pause_icon);
                } else {
                    isPlaying = false;
                    img_play.setPixelMap(ResourceTable.Media_play);
                }
            });
        } else {
            isPlaying = false;
            handlingUI();
            getUITaskDispatcher().asyncDispatch(() -> {
                CommonFunctions.getInstance().showToast(PlayerControllerAbilitySlice.this,errorMsg);
            });
        }
    }

    private void handlingUI() {
        getUITaskDispatcher().asyncDispatch(() -> {
            if (isPlaying) {
                img_play.setPixelMap(ResourceTable.Media_pause_icon);
            } else {
                img_play.setPixelMap(ResourceTable.Media_play);
            }
        });
    }

    /**
     * Play music.
     * @param playItemModel as PlayItemModel object
     * @param status as String
     */
    public void startPlayingMusic(PlayItemModel playItemModel, String status) {
        Intent intent = new Intent();
        intent.setElement(new ElementName("", PACKAGE_NAME, PLAYERSERVICEABILITY));
        intent.setParam("PlayItemModel",playItemModel);
        intent.setParam("Status",status);
        intent.setParam("Position",currentPosition);
        startAbility(intent);
    }

    /**
     * Stop music.
     */
    public void stopPlayingMusic() {
        Intent intent = new Intent();
        intent.setElement(new ElementName("", PACKAGE_NAME, PLAYERSERVICEABILITY));
        intent.setParam("PlayItemModel","");
        intent.setParam("Status",Constants.STOP);
        intent.setParam("Position",currentPosition);
        stopAbility(intent);
    }

    private void sendRequestRemote(int position, String action) {
        int maxPosition = onlineSongsList.size() - 1;
        if (position > maxPosition) {
            position = 0;
        } else if (position < 0) {
            position = maxPosition;
        }
        currentPosition = position;

        String mode = "";

        if (mStreaming.contains(ONLINE_VIDEO)) {
            mode = ONLINE_VIDEO;
        } else {
            mode = ONLINE_AUDIO;
        }

        setCurrentSongsDetails(mode, currentPosition);

        if (action.contains(Constants.ACKNOWLEDGE)
                || action.contains(PLAY)
                || action.contains(RESUME)
                || action.contains(Constants.NEXT)
                || action.contains(PREVIOUS)
                || action.contains(Constants.PING)) {
            isPlaying = true;
        } else {
            isPlaying = false;
        }

        handlingUI();

        if (isCasting) {
            if (requestRemoteProxy != null) {
                requestRemoteProxy.remoteControl(action, mode, position);
            } else {
                CommonFunctions.getInstance().showToast(PlayerControllerAbilitySlice.this,"Device not connected");
            }
        }
    }

    private void play(int position, String status) {
        boolean flag = mediaPlayerPlugin.startPlayer();
        if (!flag) {
            play(position);
        } else {
            isPlaying = false;
            pauseSongUI();
        }
    }

    private void pauseSongUI() {
        getUITaskDispatcher().asyncDispatch(() -> {
            img_play.setPixelMap(ResourceTable.Media_pause_icon);
        });
    }

    private void play(int position) {
        int maxPosition = onlineSongsList.size() - 1;
        if (position > maxPosition) {
            position = 0;
        } else if (position < 0) {
            position = maxPosition;
        }
        currentPosition = position;

        if (mStreaming.contains(ONLINE_AUDIO)) {
            mediaPlayerPlugin.release();
            mediaPlayerPlugin.startPlay(onlineSongsList.get(currentPosition).getSongUrl());
        }
        setCurrentSongsDetails(ONLINE_AUDIO, currentPosition);
        isPlaying = true;
        handlingUI();
    }

    private void pause() {
        mediaPlayerPlugin.pausePlay();
        isPlaying = false;
        handlingUI();
    }

    /**
     * Navigate to volume control screen.
     */
    private void navigateToVolumeControl() {
        Intent intent = new Intent();
        Operation operation = new Intent.OperationBuilder()
                .withAction(VOLUME_ABILITY_SLICE)
                .build();
        intent.setOperation(operation);
        startAbility(intent);
    }

    @Override
    protected void onResult(int requestCode, Intent intent) {
        if (requestCode == REQUEST_CODE) {
            LogUtil.debug(LogUtil.TAG_LOG, "PlayerControllerSlice onResult->" + requestCode);
            LogUtil.debug(LogUtil.TAG_LOG, "PlayerControllerSlice onResult->" + intent.getBooleanParam("isCasting",false));
            LogUtil.debug(LogUtil.TAG_LOG, "PlayerControllerSlice onResult->" + intent.getStringParam("Streaming"));

            if (intent != null) {
                if (intent.hasParameter("isCasting")) {
                    isCasting = intent.getBooleanParam("isCasting",false);
                }

                getUITaskDispatcher().asyncDispatch(() -> {
                    if (isCasting) {
                        img_cast.setPixelMap(ResourceTable.Media_ic_distribute_white);
                        img_volume.setVisibility(Component.HIDE);
                        mediaPlayerPlugin.release();
                    } else {
                        img_cast.setPixelMap(ResourceTable.Media_ic_distribute_grey);
                        img_volume.setVisibility(Component.VISIBLE);
                    }
                });

                if (intent.hasParameter("OnlineVideoList")) {
                    onlineSongsList = intent.getSerializableParam("OnlineVideoList");
                }
                if (intent.hasParameter("SelectedSongDetails")) {
                    playItemModel = intent.getSerializableParam("SelectedSongDetails");
                }
                if (intent.hasParameter("CurrentPosition")) {
                    currentPosition = intent.getIntParam("CurrentPosition",0);
                }

                if (intent.hasParameter("Streaming")) {
                    mStreaming = intent.getStringParam("Streaming");
                }

                if (intent.hasParameter(Constants.DEVICE_ID_KEY)) {
                    Object obj= intent.getParams().getParam(Constants.DEVICE_ID_KEY);
                    if (obj instanceof String) {
                        currentDeviceId = (String) obj;
                        preferenceUtil.putCastingStatus(isCasting, currentDeviceId);

                        if (requestRemoteProxy == null) {
                            LogUtil.debug(TAG_LOG,"Calling connectToRemoteService - requestRemoteProxy null");
                            connectToRemoteService();
                        }
                    }
                }
            }
        }
    }

    private void startProgressTaskTimer() {
        if (progressTimerTask != null) {
            progressTimerTask.cancel();
        }
        progressTimerTask = new ProgressTimerTask();
        timer.schedule(progressTimerTask, Constants.TIME_DELAY, Constants.TIME_LOOP);
    }

    class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (requestRemoteProxy != null) {
                requestRemoteProxy.remoteControl(PING, mStreaming, currentPosition);
                getUITaskDispatcher().asyncDispatch(()-> {
                    CommonFunctions.getInstance().showToast(PlayerControllerAbilitySlice.this,"Pinging to phone");
                });
            } else {
                getUITaskDispatcher().asyncDispatch(()-> {
                    CommonFunctions.getInstance().showToast(PlayerControllerAbilitySlice.this,"Device not connected");
                });
            }
        }
    }
}
