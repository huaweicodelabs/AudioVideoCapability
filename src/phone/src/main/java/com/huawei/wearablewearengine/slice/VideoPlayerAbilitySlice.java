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
import com.huawei.wearablewearengine.controller.SendResponseRemote;
import com.huawei.wearablewearengine.manager.MediaPlayerPlugin;
import com.huawei.wearablewearengine.model.PlayItemModel;
import com.huawei.wearablewearengine.service.MediaPlayerServiceAbility;
import com.huawei.wearablewearengine.utils.CommonFunctions;
import com.huawei.wearablewearengine.utils.Constants;
import com.huawei.wearablewearengine.utils.DateUtils;
import com.huawei.wearablewearengine.utils.LogUtil;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.ability.IAbilityConnection;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.Text;
import ohos.agp.components.RoundProgressBar;
import ohos.agp.components.Slider;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.DependentLayout;
import ohos.agp.components.surfaceprovider.SurfaceProvider;
import ohos.agp.graphics.Surface;
import ohos.agp.graphics.SurfaceOps;
import ohos.agp.window.service.WindowManager;
import ohos.bundle.AbilityInfo;
import ohos.bundle.ElementName;
import ohos.bundle.IBundleManager;
import ohos.distributedschedule.interwork.DeviceInfo;
import ohos.distributedschedule.interwork.DeviceManager;
import ohos.distributedschedule.interwork.IDeviceStateCallback;
import ohos.media.common.sessioncore.AVElement;
import ohos.rpc.IRemoteObject;
import ohos.rpc.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.huawei.wearablewearengine.utils.Constants.ERROR;
import static com.huawei.wearablewearengine.utils.Constants.PAUSE;
import static com.huawei.wearablewearengine.utils.Constants.PLAY;
import static com.huawei.wearablewearengine.utils.Constants.ONLINE_VIDEO;
import static com.huawei.wearablewearengine.utils.Constants.PING;
import static com.huawei.wearablewearengine.utils.LogUtil.TAG_LOG;

public class VideoPlayerAbilitySlice extends AbilitySlice implements SurfaceOps.Callback, DistributedNotificationPlugin.DistributedNotificationEventListener {
    private static final String TAG = VideoPlayerAbilitySlice.class.getSimpleName();
    private MediaPlayerPlugin mediaPlayerPlugin;
    private SurfaceProvider surfaceProvider;
    private Surface surface;
    private Text mSongTitle;
    private Text mStartTime;
    private Text mEndTime;
    private Image playButton;
    private Image forwardButton;
    private Image rewindButton;
    private Image nextButton;
    private Image previousButton;
    private Image backButton;

    private RoundProgressBar progressBar;
    private int currentPosition = 0;
    private DistributedNotificationPlugin distributedNotificationPlugin;

    public ArrayList<AVElement> onlineVideosAVElements = new ArrayList<>();
    public ArrayList<PlayItemModel> onlineVideosList = new ArrayList<>();

    private int timeElapsed;
    private int timeRemaining;
    private int finalTime;

    private DependentLayout mTimeLayout;
    private DependentLayout mFooterLayout;
    private Timer timer = new Timer();
    private ProgressTimerTask progressTimerTask;
    private static final int TIME_DELAY = 500;
    private static final int TIME_LOOP = 1000;
    private boolean mSurfaceClicked = true;
    private DirectionalLayout mHeaderLayout;

    private DeviceInfo watchDeviceInfo;
    private String localDeviceId= "";
    private SendResponseRemote responseRemoteProxy;

    private boolean mBufferStart = false;
    private Slider mTimeProgressbar;
    private static final int PROGRESS_RUNNING_TIME = 1000;

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);

        CommonFunctions.getInstance().setMarkLockAsScreenOn();

        // Hide status bar
        this.getWindow().addFlags(WindowManager.LayoutConfig.MARK_ALLOW_EXTEND_LAYOUT);
        this.getWindow().addFlags(WindowManager.LayoutConfig.MARK_FULL_SCREEN);

        super.setUIContent(ResourceTable.Layout_ability_video_player);

        if (intent != null) {
            if (intent.hasParameter("OnlineVideoAVElements")) {
                onlineVideosAVElements = intent.getSequenceableArrayListParam("OnlineVideoAVElements");
                LogUtil.debug(LogUtil.TAG_LOG,"Size_OnlineVideoAVElements: "+onlineVideosAVElements.size());
            }
            if (intent.hasParameter("OnlineVideoList")) {
                onlineVideosList = intent.getSerializableParam("OnlineVideoList");
                LogUtil.debug(LogUtil.TAG_LOG,"Size_OnlineVideoList: "+onlineVideosList.size());
            }
            if (intent.hasParameter("CurrentPosition")) {
                currentPosition =  intent.getIntParam("CurrentPosition",0);
            }

            if (intent.hasParameter("DeviceId")) {
                Object obj = intent.getParams().getParam("DeviceId");
                if (obj instanceof String) {
                    String deviceId = (String) obj;
                    localDeviceId = deviceId;
                    LogUtil.debug(TAG_LOG,"Received deviceId from Intent: "+localDeviceId);

                    if (localDeviceId != null && !localDeviceId.isEmpty()) {
                        startService();
                    }
                }
            }
        }

        setupUI();
        initData();
    }

    @Override
    public void onActive() {
        super.onActive();
    }

    @Override
    public void surfaceCreated(SurfaceOps surfaceOps) {
        if (surfaceProvider.getSurfaceOps().isPresent()) {
            surface = surfaceProvider.getSurfaceOps().get().getSurface();
            LogUtil.info(TAG, "surface set");
        }
        if (onlineVideosList != null && onlineVideosList.size() >= 1) {
            play(currentPosition);
        }
        LogUtil.info(TAG, "surface created");
    }

    @Override
    public void surfaceChanged(SurfaceOps surfaceOps, int i, int i1, int i2) {
        LogUtil.info(TAG, "surface updated with (" + i + "," + i1 + "," + i2 + ")" );
    }

    @Override
    public void surfaceDestroyed(SurfaceOps surfaceOps) {
        LogUtil.info(TAG, "surface destroyed");
    }

    private void initData() {
        mediaPlayerPlugin = new MediaPlayerPlugin(this, new MediaPlayerPlugin.MediaPlayerCallback() {
            @Override
            public void onPrepared() {
                getUITaskDispatcher().asyncDispatch(() -> {
                    mTimeProgressbar.setMaxValue(mediaPlayerPlugin.getDuration());
                    mTimeProgressbar.setProgressValue(mediaPlayerPlugin.getCurrentTime());
                    LogUtil.debug(TAG_LOG,"Duration: "+mediaPlayerPlugin.getDuration());;
                    LogUtil.debug(TAG_LOG,"CurrentTime: "+mediaPlayerPlugin.getCurrentTime());;
                });
            }

            @Override
            public void onPlayBackComplete() {
                pause();
            }

            @Override
            public void onBuffering(int percent) {
                getUITaskDispatcher().asyncDispatch(() -> {
                    if(!mBufferStart){
                        mBufferStart = true;
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                startProgressTaskTimer();
                            }
                        }, 500);
                    }
                });
            }

            @Override
            public void onError(int errorType, int errorCode) {
                getUITaskDispatcher().asyncDispatch(() -> {
                    LogUtil.error(TAG, "onError" + errorType + ", skip to the next audio");
                    CommonFunctions.getInstance().showToast(VideoPlayerAbilitySlice.this,"Video play error. Error code: "+errorCode+" and Error type: "+errorType);

                    sendResponseRemote(ERROR, "Video play error. Error code: "+errorCode+" and Error type: "+errorType);
                });
            }
        });

        distributedNotificationPlugin = DistributedNotificationPlugin.getInstance();
        distributedNotificationPlugin.setEventListener(this);
    }

    private void setupUI() {
        surfaceProvider = (SurfaceProvider) findComponentById(ResourceTable.Id_surface_provider);
        surfaceProvider.pinToZTop(false);
        WindowManager.getInstance().getTopWindow().get().setTransparent(true);
        surfaceProvider.getSurfaceOps().get().addCallback(this);

        mSongTitle = (Text) findComponentById(ResourceTable.Id_title);
        progressBar = (RoundProgressBar) findComponentById(ResourceTable.Id_round_progress_bar);
        backButton = (Image) findComponentById(ResourceTable.Id_back_button);
        playButton = (Image) findComponentById(ResourceTable.Id_play_button);
        forwardButton = (Image) findComponentById(ResourceTable.Id_forward_button);
        rewindButton = (Image) findComponentById(ResourceTable.Id_rewind_button);
        nextButton = (Image) findComponentById(ResourceTable.Id_next_button);
        previousButton = (Image) findComponentById(ResourceTable.Id_prev_button);
        mStartTime = (Text) findComponentById(ResourceTable.Id_start_time_song);
        mEndTime = (Text) findComponentById(ResourceTable.Id_end_time_song);
        mTimeProgressbar = (Slider) findComponentById(ResourceTable.Id_time_duration_progressbar);
        mTimeLayout = (DependentLayout ) findComponentById(ResourceTable.Id_time_layout);
        mFooterLayout = (DependentLayout ) findComponentById(ResourceTable.Id_footer_layout);
        mHeaderLayout = (DirectionalLayout) findComponentById(ResourceTable.Id_header_layout);

        playButton.setPixelMap(ResourceTable.Media_play);
        previousButton.setPixelMap(ResourceTable.Media_ic_previous);
        nextButton.setPixelMap(ResourceTable.Media_ic_next);

        surfaceProvider.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {

                if(!mSurfaceClicked){
                    mSurfaceClicked = true;
                    mFooterLayout.setVisibility(Component.HIDE);
                    mHeaderLayout.setVisibility(Component.HIDE);
                }else {
                    mSurfaceClicked = false;
                    mTimeLayout.setVisibility(Component.VISIBLE);
                    mFooterLayout.setVisibility(Component.VISIBLE);
                    mHeaderLayout.setVisibility(Component.VISIBLE);
                }

            }
        });

        mTimeProgressbar.setValueChangedListener(new Slider.ValueChangedListener() {
            @Override
            public void onProgressUpdated(Slider slider, int value, boolean b) {
                LogUtil.debug(TAG_LOG,"Res VideoPlayerSlice.onProgressUpdated i - "+value);
                LogUtil.debug(TAG_LOG,"Res VideoPlayerSlice.onProgressUpdated finalTime - "+finalTime);
                getUITaskDispatcher().asyncDispatch(() ->
                        mStartTime.setText(DateUtils.msToString(value)));
            }

            @Override
            public void onTouchStart(Slider slider) {

            }

            @Override
            public void onTouchEnd(Slider slider) {
                int mDraggedTime = slider.getProgress();
                if (slider.getProgress() == mediaPlayerPlugin.getDuration()) {
                    mediaPlayerPlugin.stop();
                } else {
                    if (mediaPlayerPlugin.getCurrentTime() >= mDraggedTime) {
                        mediaPlayerPlugin.rewindSong(mDraggedTime);
                    } else {
                        mediaPlayerPlugin.forwardSong(mDraggedTime);
                    }
                }
            }
        });

        backButton.setClickedListener(component -> {
            setResultToPlayerSlice();
        });

        playButton.setClickedListener(component -> {
            if (mediaPlayerPlugin.isPlaying()) {
                pause();
            } else {
                play();
            }
        });

        nextButton.setClickedListener(component -> {
            play(currentPosition + 1);
        });
        previousButton.setClickedListener(component -> play(currentPosition - 1));
    }

    private void play(int position) {
        if (progressTimerTask != null) {
            progressTimerTask.cancel();
        }
        mBufferStart = false;

        int maxPosition = onlineVideosList.size() - 1;
        if (position > maxPosition) {
            position = 0;
        } else if (position < 0) {
            position = maxPosition;
        }
        currentPosition = position;

        getUITaskDispatcher().asyncDispatch(() -> {
            mStartTime.setText("00:00:00");
            mEndTime.setText("00:00:00");
            mTimeProgressbar.setMinValue(0);

            String title = onlineVideosList.get(currentPosition).getTitle();
            mSongTitle.setText(title);
            playButton.setPixelMap(ResourceTable.Media_pause_icon);
        });

        mediaPlayerPlugin.release();
        mediaPlayerPlugin.startPlayVideo(onlineVideosList.get(position).getSongUrl(), surface);

        pauseSongUI();
    }

    private void play() {
        boolean flag = mediaPlayerPlugin.startPlayer();
        if (!flag) {
            play(currentPosition);
        } else {
            pauseSongUI();
        }
    }

    private void playSongUI() {
        getUITaskDispatcher().asyncDispatch(() -> {
            playButton.setPixelMap(ResourceTable.Media_play);
        });

        sendResponseRemote(PAUSE, "");
    }

    private void pauseSongUI() {
        getUITaskDispatcher().asyncDispatch(() -> {
            playButton.setPixelMap(ResourceTable.Media_pause_icon);
        });

        sendResponseRemote(PLAY, "");
    }

    private void pause() {
        mediaPlayerPlugin.pausePlay();
        playSongUI();
    }

    private void updateSeekBarTime(){
        LogUtil.debug(TAG_LOG,"Res PlayerListSlice.updateSeekBarTime 1 ");
        // get current position
        timeElapsed = mediaPlayerPlugin.getCurrentTime();
        finalTime = mediaPlayerPlugin.getDuration();
        LogUtil.debug(TAG_LOG,"Res PlayerListSlice.updateSeekBarTime 1 finalTime - "+finalTime);
        LogUtil.debug(TAG_LOG,"Res PlayerListSlice.updateSeekBarTime 1 timeElapsed - "+timeElapsed);

        getUITaskDispatcher().asyncDispatch(() -> {
            // set seekbar progress using time played
            mTimeProgressbar.setMaxValue((int) finalTime);
            mTimeProgressbar.setProgressValue((int) timeElapsed);

            // set time remaining in minutes and seconds
            timeRemaining = finalTime - timeElapsed;

            mEndTime.setText(DateUtils.msToString(finalTime));
        });
    }

    private void startProgressTaskTimer() {
        if (progressTimerTask != null) {
            progressTimerTask.cancel();
        }
        progressTimerTask = new ProgressTimerTask();
        timer.schedule(progressTimerTask, TIME_DELAY, TIME_LOOP);
    }

    class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            updateSeekBarTime();
        }
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
    public void onEventReceive(String result, String mode, int position, String deviceID) {
        LogUtil.info(TAG, result);
        if (currentPosition != position) {
            currentPosition = position;
        }
        switch (result) {
            case Constants.PLAY:
            case Constants.NEXT:
            case Constants.PREVIOUS:
                play(currentPosition);
                break;
            case Constants.PAUSE:
                pause();
                break;
            case Constants.FORWARD:
                mediaPlayerPlugin.seek();
                break;
            case Constants.REWIND:
                mediaPlayerPlugin.back();
                break;
            case Constants.PING:
                CommonFunctions.getInstance().showToast(VideoPlayerAbilitySlice.this,"Pinging from watch");
                sendResponseRemote(PING, "");
                break;
            default:
                // Default case
                break;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (progressTimerTask != null) {
            progressTimerTask.cancel();
        }
        surfaceProvider.getSurfaceOps().get().removeCallback(this);
        surfaceProvider.removeFromWindow();
        surfaceProvider.release();
        mediaPlayerPlugin.release();
    }

    private void startService() {
        LogUtil.debug(TAG_LOG,"MediaPlayerServiceAbility - Phone Calling");
        Intent intent = new Intent();
        Operation operation = new Intent.OperationBuilder()
                .withDeviceId("")
                .withBundleName(getBundleName())
                .withAbilityName(MediaPlayerServiceAbility.class.getName())
                .withFlags(Intent.FLAG_ABILITYSLICE_MULTI_DEVICE)
                .build();
        intent.setOperation(operation);
        startAbility(intent);
        LogUtil.debug(TAG_LOG,"MediaPlayerServiceAbility - Phone startAbility");

        fetchOnlineWatchDevice();
    }

    private void fetchOnlineWatchDevice() {
        List<DeviceInfo> deviceInfoList = DeviceManager.getDeviceList(DeviceInfo.FLAG_GET_ONLINE_DEVICE);
        if (deviceInfoList.isEmpty()) {
            CommonFunctions.getInstance().showToast(this,"No device found");
            LogUtil.debug(LogUtil.TAG_LOG,"No device found");
        } else {
            deviceInfoList.forEach(deviceInformation -> {
                LogUtil.info(TAG_LOG, "Found devices - Name " + deviceInformation.getDeviceName());
                if (deviceInformation.getDeviceType() == DeviceInfo.DeviceType.SMART_WATCH
                        &&
                        (watchDeviceInfo == null || !deviceInformation.getDeviceId().equals(watchDeviceInfo.getDeviceId()))
                ) {
                    watchDeviceInfo = deviceInformation;
                    LogUtil.info(TAG_LOG, "Found device - Type " + deviceInformation.getDeviceType());
                    LogUtil.info(TAG_LOG, "Found device - State " + deviceInformation.getDeviceState());
                    LogUtil.info(TAG_LOG, "Found device - Id remote " + deviceInformation.getDeviceId());
                    LogUtil.info(TAG_LOG, "Found device - Id local " + localDeviceId);

                    if (localDeviceId.contains(watchDeviceInfo.getDeviceId())) {
                        LogUtil.info(TAG_LOG, "Device Id match");
                        connectToRemoteService();
                    } else {
                        LogUtil.info(TAG_LOG, "Device Id not match");
                    }
                }
            });
        }
    }

    private void connectToRemoteService() {
        LogUtil.info(TAG_LOG, "connectToRemoteService - from Phone Start");
        Intent intent = new Intent();
        Operation operation = new Intent.OperationBuilder()
                .withDeviceId(watchDeviceInfo.getDeviceId())
                .withBundleName(Constants.PACKAGE_NAME)
                .withAbilityName(Constants.ACK_RESPONSE_SERVICEABILITY)
                .withFlags(Intent.FLAG_ABILITYSLICE_MULTI_DEVICE)
                .build();
        intent.setOperation(operation);
        try {
            List<AbilityInfo> abilityInfos = getBundleManager().queryAbilityByIntent(intent, IBundleManager.GET_BUNDLE_DEFAULT, 0);
            if (abilityInfos != null && !abilityInfos.isEmpty()) {
                connectAbility(intent, iAbilityConnection);
                LogUtil.info(TAG_LOG, "connectToRemoteService - from Phone connectAbility");
                LogUtil.info(TAG_LOG, "connect service on phone with id " + watchDeviceInfo.getDeviceId() );
            } else {
                CommonFunctions.getInstance().showToast(this,"Cannot connect service on watch");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private IAbilityConnection iAbilityConnection = new IAbilityConnection() {
        @Override
        public void onAbilityConnectDone(ElementName elementName, IRemoteObject iRemoteObject, int i) {
            LogUtil.info(TAG_LOG, "ability connect done!");
            responseRemoteProxy = new SendResponseRemote(iRemoteObject, watchDeviceInfo.getDeviceId());

            sendResponseRemote(PLAY, "");
        }

        @Override
        public void onAbilityDisconnectDone(ElementName elementName, int i) {
            LogUtil.info(TAG_LOG, "ability disconnect done!");
            disconnectAbility(iAbilityConnection);
        }
    };

    private IDeviceStateCallback iDeviceStateCallback = new IDeviceStateCallback() {
        @Override
        public void onDeviceOffline(String s, int i) {
            startService();
        }

        @Override
        public void onDeviceOnline(String s, int i) {
            startService();
        }
    };

    private void sendResponseRemote(String status, String errorMsg) {
        String singer = "";
        if (responseRemoteProxy != null) {
            if (localDeviceId != null && !localDeviceId.isEmpty()) {
                if (onlineVideosList != null && onlineVideosList.size() >= 1) {
                    String title = "";
                    String thumbnail="";

                    if (onlineVideosList != null && onlineVideosList.size() >= 1) {
                        if (onlineVideosList.get(currentPosition).getSinger() != null
                                && !onlineVideosList.get(currentPosition).getSinger().isEmpty()) {
                            singer = onlineVideosList.get(currentPosition).getSinger();
                            title = onlineVideosList.get(currentPosition).getTitle();
                            thumbnail = onlineVideosList.get(currentPosition).getThumbnailUrl();
                        }
                    }

                    responseRemoteProxy.remoteControl(Constants.ACKNOWLEDGE, thumbnail, title, ONLINE_VIDEO, status, singer, errorMsg, currentPosition);
                }
            }
        }
    }

    public int getBasicTransTime(int currentTime) {
        return currentTime / PROGRESS_RUNNING_TIME * PROGRESS_RUNNING_TIME;
    }

    @Override
    protected void onBackPressed() {
        setResultToPlayerSlice();
    }

    private void setResultToPlayerSlice() {
        if (progressTimerTask != null) {
            progressTimerTask.cancel();
        }
        surfaceProvider.getSurfaceOps().get().removeCallback(this);
        surfaceProvider.removeFromWindow();
        surfaceProvider.release();
        if (mediaPlayerPlugin != null) {
            mediaPlayerPlugin.release();
        }

        Intent intent = new Intent();

        intent.setParam("Mode", ONLINE_VIDEO);
        intent.setParam("DeviceId", localDeviceId);
        intent.setParam("CurrentPosition", currentPosition);

        setResult(intent);
        terminate();
    }
}