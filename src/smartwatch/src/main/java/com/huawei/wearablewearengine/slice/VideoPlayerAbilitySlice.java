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
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.Image;
import ohos.agp.components.Text;
import ohos.agp.components.surfaceprovider.SurfaceProvider;
import ohos.agp.graphics.Surface;
import ohos.agp.graphics.SurfaceOps;
import ohos.agp.window.service.WindowManager;
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
import ohos.rpc.IRemoteObject;
import ohos.rpc.RemoteException;

import java.util.ArrayList;
import java.util.List;

import static com.huawei.wearablewearengine.utils.Constants.PREVIOUS;
import static com.huawei.wearablewearengine.utils.Constants.PAUSE;
import static com.huawei.wearablewearengine.utils.Constants.PLAY;
import static com.huawei.wearablewearengine.utils.Constants.NEXT;
import static com.huawei.wearablewearengine.utils.Constants.ONLINE_VIDEO;
import static com.huawei.wearablewearengine.utils.Constants.ONLINE_DEVICE_SLICE;
import static com.huawei.wearablewearengine.utils.Constants.MAIN_ABILITY;
import static com.huawei.wearablewearengine.utils.Constants.PACKAGE_NAME;
import static com.huawei.wearablewearengine.utils.Constants.PLAYBACKCOMPLETE;
import static com.huawei.wearablewearengine.utils.Constants.PING;
import static com.huawei.wearablewearengine.utils.Constants.RESUME;
import static com.huawei.wearablewearengine.utils.Constants.REQUEST_CODE;
import static com.huawei.wearablewearengine.utils.LogUtil.TAG_LOG;

public class VideoPlayerAbilitySlice extends AbilitySlice implements SurfaceOps.Callback, DistributedNotificationPlugin.DistributedNotificationEventListener {
    private Image img_previous;
    private Image img_play;
    private Image img_next;
    private Image img_cast;
    private com.andexert.library.RippleView rippleview_previous;
    private com.andexert.library.RippleView rippleview_play;
    private com.andexert.library.RippleView rippleview_next;
    private com.andexert.library.RippleView rippleview_cast;
    private Text title;
    private boolean mSurfaceClicked = true;
    private SurfaceProvider surfaceProvider;
    private Surface surface;

    private ArrayList<PlayItemModel> onlineSongsList = new ArrayList<>();
    private PlayItemModel playItemModel = new PlayItemModel();
    private int currentPosition = 0;
    private boolean isCasting = false;
    private String mStreaming = "";
    private String currentDeviceId = "";

    private MediaPlayerPlugin mediaPlayerPlugin;
    private SendRequestRemote requestRemoteProxy;
    private static final int EVENT_STATE_CHANGE = 10001;
    private boolean isPlaying= false;
    private DistributedNotificationPlugin distributedNotificationPlugin;
    private PreferenceUtil preferenceUtil;
    private DirectionalLayout layout_title;
    private DirectionalLayout player_controller;
    private DirectionalLayout bottom_layout;

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);

        CommonFunctions.getInstance().setMarkLockAsScreenOn();

        // Hide status bar
        this.getWindow().addFlags(WindowManager.LayoutConfig.MARK_ALLOW_EXTEND_LAYOUT);
        this.getWindow().addFlags(WindowManager.LayoutConfig.MARK_FULL_SCREEN);

        super.setUIContent(ResourceTable.Layout_ability_video_player);

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

        initView();
        initPlayer();
    }

    private void initView() {
        surfaceProvider = (SurfaceProvider) findComponentById(ResourceTable.Id_surface_provider);
        surfaceProvider.pinToZTop(false);
        WindowManager.getInstance().getTopWindow().get().setTransparent(true);
        surfaceProvider.getSurfaceOps().get().addCallback(this);

        rippleview_previous = (com.andexert.library.RippleView) findComponentById(ResourceTable.Id_ripple_previous);
        rippleview_play = (com.andexert.library.RippleView) findComponentById(ResourceTable.Id_ripple_play);
        rippleview_next = (com.andexert.library.RippleView) findComponentById(ResourceTable.Id_ripple_next);
        rippleview_cast = (com.andexert.library.RippleView) findComponentById(ResourceTable.Id_ripple_cast);

        img_previous = (Image) findComponentById(ResourceTable.Id_img_previous);
        img_play = (Image) findComponentById(ResourceTable.Id_img_play);
        img_next = (Image) findComponentById(ResourceTable.Id_img_next);
        img_cast = (Image) findComponentById(ResourceTable.Id_img_cast);

        layout_title = (DirectionalLayout) findComponentById(ResourceTable.Id_layout_title);
        player_controller = (DirectionalLayout) findComponentById(ResourceTable.Id_player_controller);
        bottom_layout = (DirectionalLayout) findComponentById(ResourceTable.Id_bottom_layout);

        title = (Text) findComponentById(ResourceTable.Id_title);

        rippleview_previous.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                if (isCasting) {
                    if (requestRemoteProxy != null) {
                        sendRequestRemote(currentPosition-1,PREVIOUS);
                    } else {
                        CommonFunctions.getInstance().showToast(VideoPlayerAbilitySlice.this,"Device not connected");
                    }
                } else {
                    if (mStreaming.contains(ONLINE_VIDEO)) {
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
                        CommonFunctions.getInstance().showToast(VideoPlayerAbilitySlice.this,"Device not connected");
                    }
                } else {
                    if (mStreaming.contains(ONLINE_VIDEO)) {
                        if (onlineSongsList != null && onlineSongsList.size() >= 1) {
                            if (mediaPlayerPlugin.isPlaying()) {
                                mediaPause();
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
                        CommonFunctions.getInstance().showToast(VideoPlayerAbilitySlice.this,"Device not connected");
                    }
                } else {
                    if (mStreaming.contains(ONLINE_VIDEO)) {
                        if (onlineSongsList != null && onlineSongsList.size() >= 1) {
                            play(currentPosition+1);
                        }
                    }
                }
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
                    intent.setParam("Streaming", ONLINE_VIDEO);
                    presentForResult(new OnlineDeviceAbilitySlice(), intent, REQUEST_CODE);
                    mediaPause();
                }
            }
        });

        surfaceProvider.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                if(!mSurfaceClicked){
                    mSurfaceClicked = true;
                    layout_title.setVisibility(Component.HIDE);
                    player_controller.setVisibility(Component.HIDE);
                    bottom_layout.setVisibility(Component.HIDE);
                }else {
                    mSurfaceClicked = false;
                    layout_title.setVisibility(Component.VISIBLE);
                    player_controller.setVisibility(Component.VISIBLE);
                    bottom_layout.setVisibility(Component.VISIBLE);
                }

            }
        });

        distributedNotificationPlugin = DistributedNotificationPlugin.getInstance();
        distributedNotificationPlugin.setEventListener(this);
        distributedNotificationPlugin.subscribeEvent();

        DeviceManager.registerDeviceStateCallback(iDeviceStateCallback);

        preferenceUtil = PreferenceUtil.getInstance();

        isCasting = preferenceUtil.getIsCasting();

        if (isCasting) {
            img_cast.setPixelMap(ResourceTable.Media_ic_distribute_white);
        } else {
            img_cast.setPixelMap(ResourceTable.Media_ic_distribute_grey);
        }
    }

    private void initPlayer() {
        mediaPlayerPlugin = new MediaPlayerPlugin(this, new MediaPlayerPlugin.MediaPlayerCallback() {
            @Override
            public void onPrepared() {
            }

            @Override
            public void onPlayBackComplete() {
                mediaPause();
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
                    if (mStreaming.contains(ONLINE_VIDEO)) {
                        getUITaskDispatcher().asyncDispatch(() -> {
                            LogUtil.error(TAG_LOG, "Mediaplayer onError" + errorType + ", skip to the next video");
                            CommonFunctions.getInstance().showToast(VideoPlayerAbilitySlice.this,"Video play error. Error code: "+errorCode+" and Error type: "+errorType);
                        });
                    }
                }
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceOps surfaceOps) {
        if (surfaceProvider.getSurfaceOps().isPresent()) {
            surface = surfaceProvider.getSurfaceOps().get().getSurface();
            LogUtil.info(LogUtil.TAG_LOG, "surface set");
        }
        if (mStreaming.contains(ONLINE_VIDEO)) {
            if (onlineSongsList != null && onlineSongsList.size() >= 1) {
                play(currentPosition, PLAY);
            }
        }
        LogUtil.info(LogUtil.TAG_LOG, "surface created");
    }

    @Override
    public void surfaceChanged(SurfaceOps surfaceOps, int i, int i1, int i2) {
        LogUtil.info("TAG", "surface updated with (" + i + "," + i1 + "," + i2 + ")" );
    }

    @Override
    public void surfaceDestroyed(SurfaceOps surfaceOps) {
        LogUtil.info("TAG", "surface destroyed");
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
                    .createKvManager(new KvManagerConfig(VideoPlayerAbilitySlice.this))
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
                CommonFunctions.getInstance().showToast(VideoPlayerAbilitySlice.this,"Device offline");
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

        release();
    }

    private void release() {
        surfaceProvider.getSurfaceOps().get().removeCallback(this);
        surfaceProvider.removeFromWindow();
        surfaceProvider.release();

        DeviceManager.unregisterDeviceStateCallback(iDeviceStateCallback);
        if (mediaPlayerPlugin != null) {
            mediaPlayerPlugin.release();
        }
    }

    @Override
    protected void onBackPressed() {
        super.onBackPressed();
        release();
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
            case Constants.PREVIOUS:
            case Constants.PAUSE:
            case Constants.ERROR:
            case Constants.COMPLETE:
                processResponse(status, mode, imageURL, songName, singerName, errorMsg, position);
                break;
            case PLAYBACKCOMPLETE:
                if (mStreaming.contains(ONLINE_VIDEO)) {
                    play(currentPosition+1, PLAY);
                }
                break;
            case RESUME:
                isPlaying = true;
                handlingUI();
                break;
            case PING:
                CommonFunctions.getInstance().showToast(VideoPlayerAbilitySlice.this,"Pinged response from phone");
                processResponse(status, mode, imageURL, songName, singerName, errorMsg, position);
                break;
            default:
                // Default case
                break;
        }
    }

    private void setCurrentSongsDetails(String mode, int position) {
        getUITaskDispatcher().asyncDispatch(() -> {
            title.setText(onlineSongsList.get(position).getTitle());
        });
    }

    private void processResponse(String status, String mode, String imageURL, String songName, String singerName, String errorMsg, int position) {
        if (!status.contains(Constants.ERROR)) {
            getUITaskDispatcher().asyncDispatch(() -> {
                title.setText(songName);

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
                CommonFunctions.getInstance().showToast(VideoPlayerAbilitySlice.this,errorMsg);
            });
        }
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
                        mediaPlayerPlugin.release();
                    } else {
                        img_cast.setPixelMap(ResourceTable.Media_ic_distribute_grey);
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

    private void handlingUI() {
        getUITaskDispatcher().asyncDispatch(() -> {
            if (isPlaying) {
                img_play.setPixelMap(ResourceTable.Media_pause_icon);
            } else {
                img_play.setPixelMap(ResourceTable.Media_play);
            }
        });
    }

    private void sendRequestRemote(int position, String action) {
        int maxPosition = onlineSongsList.size() - 1;
        if (position > maxPosition) {
            position = 0;
        } else if (position < 0) {
            position = maxPosition;
        }
        currentPosition = position;

        setCurrentSongsDetails(ONLINE_VIDEO, currentPosition);

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
                requestRemoteProxy.remoteControl(action, ONLINE_VIDEO, position);
            } else {
                CommonFunctions.getInstance().showToast(VideoPlayerAbilitySlice.this,"Device not connected");
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

        if (mStreaming.contains(ONLINE_VIDEO)) {
            mediaPlayerPlugin.release();
            mediaPlayerPlugin.startPlay(onlineSongsList.get(currentPosition).getSongUrl(),surface);
        }
        setCurrentSongsDetails(ONLINE_VIDEO, currentPosition);
        isPlaying = true;
        handlingUI();
    }

    private void mediaPause() {
        mediaPlayerPlugin.pausePlay();
        isPlaying = false;
        handlingUI();
    }
}
