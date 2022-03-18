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
import com.huawei.wearablewearengine.controller.SendResponseRemote;
import com.huawei.wearablewearengine.manager.MediaPlayerPlugin;
import com.huawei.wearablewearengine.model.PlayItemModel;
import com.huawei.wearablewearengine.adapter.SongsListAdapter;
import com.huawei.wearablewearengine.network.RestApiInterface;
import com.huawei.wearablewearengine.network.RestClient;
import com.huawei.wearablewearengine.service.MediaPlayerServiceAbility;
import com.huawei.wearablewearengine.utils.CommonFunctions;
import com.huawei.wearablewearengine.utils.Constants;
import com.huawei.wearablewearengine.utils.DateUtils;
import com.huawei.wearablewearengine.utils.LogUtil;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.ability.IAbilityConnection;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.animation.Animator;
import ohos.agp.animation.AnimatorProperty;
import ohos.agp.animation.AnimatorValue;
import ohos.agp.utils.Color;
import ohos.bundle.AbilityInfo;
import ohos.bundle.ElementName;
import ohos.bundle.IBundleManager;
import ohos.distributedschedule.interwork.DeviceInfo;
import ohos.distributedschedule.interwork.DeviceManager;
import ohos.distributedschedule.interwork.IDeviceStateCallback;
import ohos.global.resource.Entry;
import ohos.media.common.AVDescription;
import ohos.media.common.sessioncore.AVElement;
import ohos.rpc.IRemoteObject;
import ohos.rpc.RemoteException;
import ohos.utils.PacMap;
import ohos.utils.net.Uri;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.Text;
import ohos.agp.components.RoundProgressBar;
import ohos.agp.components.Slider;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.DependentLayout;
import ohos.agp.components.ListContainer;

import static com.huawei.wearablewearengine.utils.Constants.VIDEO_PLAYER_ACTION;
import static com.huawei.wearablewearengine.utils.Constants.ONLINE_AUDIO;
import static com.huawei.wearablewearengine.utils.Constants.PAUSE;
import static com.huawei.wearablewearengine.utils.Constants.PLAY;
import static com.huawei.wearablewearengine.utils.Constants.ERROR;
import static com.huawei.wearablewearengine.utils.Constants.OFFLINE_AUDIO;
import static com.huawei.wearablewearengine.utils.Constants.ONLINE_VIDEO;
import static com.huawei.wearablewearengine.utils.Constants.VIDEO_ABILITY;
import static com.huawei.wearablewearengine.utils.Constants.PACKAGE_NAME;
import static com.huawei.wearablewearengine.utils.Constants.PING;
import static com.huawei.wearablewearengine.utils.Constants.ACKNOWLEDGE;
import static com.huawei.wearablewearengine.utils.Constants.REQUEST_CODE;
import static com.huawei.wearablewearengine.utils.Constants.TIMER_MILLISECOND;
import static com.huawei.wearablewearengine.utils.LogUtil.TAG_LOG;

public class DashboardPlayerAbilitySlice extends AbilitySlice implements DistributedNotificationPlugin.DistributedNotificationEventListener {

    private Image mPlayButton;
    private Image mStackBackground;
    private Image mThumbnailImage;
    private Image mSongThumbnailImage;
    private Image mPlayBtn;
    private Image mShuffleBtn;
    private Image mNextBtn;
    private Image mLoopBtn;
    private Image mBackButton;
    private Image mPreviousBtn;
    private Image loadingBtn;
    private Text mSongTitle;
    private Text mStartTime;
    private Text mEndTime;
    private Text mSongSinger;
    private Text mSongFilm;
    private Text mSongTitleDetail;
    private Button mOnlineSongsBtn;
    private Button mOnlineVideoBtn;

    private RoundProgressBar progressBar;
    private Slider mTimeProgressbar;

    private DirectionalLayout mSongLayout;
    private DirectionalLayout loadingImgContent;
    private DependentLayout mSongListLayout;
    private DependentLayout mSongDetailLayout;

    private ListContainer listContainer;
    private SongsListAdapter playerAdapter;

    private DistributedNotificationPlugin distributedNotificationPlugin;

    private MediaPlayerPlugin mediaPlayerPlugin;
    private int currentPosition = 0;

    // Media list returned from the media browser service
    private List<AVElement> offlineAudioAVElements = new ArrayList<>();
    private ArrayList<PlayItemModel> offlineSongsList;

    private ArrayList<AVElement> onlineAudioAVElements = new ArrayList<>();
    public ArrayList<AVElement> onlineVideosAVElements = new ArrayList<>();
    private ArrayList<PlayItemModel> onlineSongsList;
    public ArrayList<PlayItemModel> onlineVideosList;

    private String modePlaying = ONLINE_AUDIO;
    private RestApiInterface apiInterface;
    private DeviceInfo watchDeviceInfo;

    private int timeElapsed;
    private int timeRemaining;
    private int finalTime;

    private SendResponseRemote responseRemoteProxy;
    private Timer timer = new Timer();
    private ProgressTimerTask progressTimerTask;
    private static final int TIME_DELAY = 500;
    private static final int TIME_LOOP = 1000;
    private AnimatorValue animatorValue;
    private boolean mShuffleClicked = false;
    private boolean isLooping = false;
    private String localDeviceId= "";
    private AnimatorProperty mAnimatorProperty;
    private boolean mBufferStart = false;

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);

        super.setUIContent(ResourceTable.Layout_ability_dashboard);

        CommonFunctions.getInstance().setMarkLockAsScreenOn();

        if (intent != null) {
            if (intent.hasParameter("DeviceId")) {
                Object obj = intent.getParams().getParam("DeviceId");
                if (obj instanceof String) {
                    String deviceId = (String) obj;
                    localDeviceId = deviceId;
                    LogUtil.debug(TAG_LOG,"Received deviceId from Intent: "+localDeviceId);
                }
            }
        }

        DeviceManager.registerDeviceStateCallback(iDeviceStateCallback);

        initView();
        initApiCall();
        initClickListener();
        initAudio();
    }

    private void initView() {
        mSongTitle = (Text) findComponentById(ResourceTable.Id_list_title_song);
        mStartTime = (Text) findComponentById(ResourceTable.Id_start_time_song);
        mEndTime = (Text) findComponentById(ResourceTable.Id_end_time_song);
        mSongSinger = (Text) findComponentById(ResourceTable.Id_singer_song);
        mSongFilm = (Text) findComponentById(ResourceTable.Id_film_song);
        mSongTitleDetail = (Text) findComponentById(ResourceTable.Id_detail_title_song);

        loadingBtn = (Image) findComponentById(ResourceTable.Id_imgLoading);
        mSongThumbnailImage = (Image) findComponentById(ResourceTable.Id_detail_thumb_image);
        mPlayButton = (Image) findComponentById(ResourceTable.Id_button_list_play_song);
        mBackButton = (Image) findComponentById(ResourceTable.Id_back_button);
        mStackBackground = (Image) findComponentById(ResourceTable.Id_stack_background);
        mThumbnailImage = (Image) findComponentById(ResourceTable.Id_song_thumbnail);
        mPreviousBtn = (Image) findComponentById(ResourceTable.Id_detail_song_previous);
        mPlayBtn = (Image) findComponentById(ResourceTable.Id_detail_song_play);
        mNextBtn = (Image) findComponentById(ResourceTable.Id_detail_song_next);
        mShuffleBtn = (Image) findComponentById(ResourceTable.Id_detail_song_shuffle);
        mLoopBtn = (Image) findComponentById(ResourceTable.Id_detail_song_loop);

        mOnlineSongsBtn = (Button) findComponentById(ResourceTable.Id_list_online_audio_button);
        mOnlineVideoBtn = (Button) findComponentById(ResourceTable.Id_list_online_video_button);

        listContainer = (ListContainer) findComponentById(ResourceTable.Id_list_container);

        loadingImgContent = (DirectionalLayout) findComponentById(ResourceTable.Id_loading_img_content);
        mSongLayout = (DirectionalLayout) findComponentById(ResourceTable.Id_song_layout);
        mSongListLayout = (DependentLayout ) findComponentById(ResourceTable.Id_song_list_layout);
        mSongDetailLayout = (DependentLayout) findComponentById(ResourceTable.Id_song_detail_layout);

        mTimeProgressbar = (Slider) findComponentById(ResourceTable.Id_time_duration_progressbar);
        progressBar = (RoundProgressBar) findComponentById(ResourceTable.Id_round_progress_bar);

        // Enable automatic scrolling to run indefinitely.
        mSongTitle.setAutoScrollingCount(Text.AUTO_SCROLLING_FOREVER);
        // Start automatic text scrolling.
        mSongTitle.startAutoScrolling();

        mPlayButton.setPixelMap(ResourceTable.Media_play);

        // Enable automatic scrolling to run indefinitely.
        mSongTitleDetail.setAutoScrollingCount(Text.AUTO_SCROLLING_FOREVER);
        // Start automatic text scrolling.
        mSongTitleDetail.startAutoScrolling();

        mPlayBtn.setPixelMap(ResourceTable.Media_play);
        mPreviousBtn.setPixelMap(ResourceTable.Media_ic_previous);
        mNextBtn.setPixelMap(ResourceTable.Media_ic_next);
        mShuffleBtn.setPixelMap(ResourceTable.Media_ic_shuffle);
        mLoopBtn.setPixelMap(ResourceTable.Media_ic_oval_loop);

        mOnlineVideoBtn.setTextColor(Color.GRAY);
        mOnlineSongsBtn.setTextColor(Color.WHITE);

        animatorPropertyHandle();
    }

    private void initApiCall() {
        apiInterface = RestClient.getClient().create(RestApiInterface.class);

        boolean isInternetAvailable = CommonFunctions.getInstance().queryNetworkStatus(this);
        if (isInternetAvailable) {
            initLocalFile();
        } else {
            CommonFunctions.getInstance().showToast(this,"No internet connection");
        }
    }

    private void initClickListener() {
        listContainer.setItemClickedListener(new ListContainer.ItemClickedListener() {
            @Override
            public void onItemClicked(ListContainer listContainer, Component component, int i, long l) {
                play(i);
            }
        });

        mPlayButton.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                LogUtil.debug(TAG_LOG,"Res PlayListDetailSlice.onClick - "+mediaPlayerPlugin.isPlaying());
                if (mediaPlayerPlugin.isPlaying()) {
                    if (animatorValue != null) {
                        animatorValue.pause();
                    }
                    pause();
                } else {
                    if (animatorValue != null) {
                        animatorValue.resume();
                    }
                    play();
                }
            }
        });

        mPlayBtn.setClickedListener(component -> {
            if (mediaPlayerPlugin.isPlaying()) {
                if (animatorValue != null) {
                    animatorValue.pause();
                }
                pause();
            } else {
                if (animatorValue != null) {
                    animatorValue.resume();
                }
                play();
            }
        });
        
        mOnlineVideoBtn.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                modePlaying = ONLINE_VIDEO;
                mOnlineVideoBtn.setTextColor(Color.WHITE);
                mOnlineSongsBtn.setTextColor(Color.GRAY);
                playerAdapter = new SongsListAdapter(getOnlineVideos(), getContext());
                listContainer.setItemProvider(playerAdapter);
            }
        });

        mOnlineSongsBtn.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                modePlaying = ONLINE_AUDIO;
                mOnlineVideoBtn.setTextColor(Color.GRAY);
                mOnlineSongsBtn.setTextColor(Color.WHITE);
                playerAdapter = new SongsListAdapter(getOnlineSongs(), getContext());
                listContainer.setItemProvider(playerAdapter);
            }
        });

        mPreviousBtn.setClickedListener(component ->
                previousSong()
        );

        mNextBtn.setClickedListener(component -> nextSong());

        mShuffleBtn.setClickedListener(component -> {
            shuffleSong();
        });

        mLoopBtn.setClickedListener(component -> {
            loopSong();
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

        mSongLayout.setClickedListener(component -> {
            if (modePlaying.equals(ONLINE_VIDEO)) {
                if (onlineVideosAVElements!= null && onlineVideosAVElements.size() >= 1) {
                    mediaPlayerPlugin.release();

                    Intent intent = new Intent();
                    Operation operation = new Intent.OperationBuilder()
                            .withDeviceId("")
                            .withBundleName(PACKAGE_NAME)
                            .withAbilityName(VIDEO_ABILITY)
                            .build();
                    intent.setOperation(operation);
                    intent.setParam("OnlineVideoList", onlineVideosList);
                    intent.setParam("CurrentPosition", currentPosition);
                    intent.setParam("DeviceId", localDeviceId);
                    startAbilityForResult(intent, REQUEST_CODE);
                }
            } else {
                mSongListLayout.setVisibility(Component.HIDE);
                mSongDetailLayout.setVisibility(Component.VISIBLE);

                mStackBackground.setPixelMap(ResourceTable.Media_equalizer);

                if(animatorValue == null){
                    // Create a value animator.
                    animatorValue = new AnimatorValue();
                    // Set the animation duration.
                    animatorValue.setDuration(TIMER_MILLISECOND);
                    // Set the startup delay of the animator.
                    animatorValue.setDelay(1000);
                    // Set the repetition times of the animator.
                    animatorValue.setLoopedCount(AnimatorValue.INFINITE);
                    // Set the curve used by the animator.
                    animatorValue.setCurveType(Animator.CurveType.BOUNCE);
                    // Set the animation process.
                    animatorValue.setValueUpdateListener(new AnimatorValue.ValueUpdateListener() {
                        @Override
                        public void onUpdate(AnimatorValue animatorValue, float value) {
                            mStackBackground.setContentPosition((int) (100 * value), (int) (500 * value));
                        }
                    });
                    // Start the animator.
                    animatorValue.start();
                }else{
                    animatorValue.resume();
                }
            }
        });

        mBackButton.setClickedListener(component -> {
            if (animatorValue != null) {
                animatorValue.release();
            }
            mSongListLayout.setVisibility(Component.VISIBLE);
            mSongDetailLayout.setVisibility(Component.HIDE);
        });
    }

    private void initAudio() {
        mediaPlayerPlugin = new MediaPlayerPlugin(this, new MediaPlayerPlugin.MediaPlayerCallback() {
            @Override
            public void onPrepared() {
                getUITaskDispatcher().asyncDispatch(() -> {
                    mTimeProgressbar.setMaxValue(mediaPlayerPlugin.getDuration());
                    mTimeProgressbar.setProgressValue(mediaPlayerPlugin.getCurrentTime());
                });
            }

            @Override
            public void onPlayBackComplete() {
                getUITaskDispatcher().asyncDispatch(() -> {
                    if (!isLooping) {
                        if(!mShuffleClicked){
                            play(currentPosition + 1);
                        } else{
                            shuffleSong1();
                        }
                    } else {
                        if(!mShuffleClicked){
                            play(currentPosition);
                        } else{
                            shuffleSong1();
                        }
                    }
                });
            }

            @Override
            public void onBuffering(int percent) {
                if(!mBufferStart){
                    mBufferStart = true;
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            startProgressTaskTimer();
                        }
                    }, 500);
                }
            }

            @Override
            public void onError(int errorType, int errorCode) {
                getUITaskDispatcher().asyncDispatch(() -> {
                    LogUtil.error(TAG_LOG, "onError" + errorType + ", skip to the next audio");
                    CommonFunctions.getInstance().showToast(DashboardPlayerAbilitySlice.this,"Audio play error. Error code: "+errorCode+" and Error type: "+errorType);

                    sendResponseRemote(ERROR, "Audio play error. Error code: "+errorCode+" and Error type: "+errorType);
                });
            }
        });

        currentPosition = 0;
        distributedNotificationPlugin = DistributedNotificationPlugin.getInstance();
        distributedNotificationPlugin.setEventListener(this);
    }

    private void initLocalFile() {
        onlineAudioAVElements = new ArrayList<>();
        onlineVideosAVElements = new ArrayList<>();

        Entry[] entries = new Entry[0];
        try {
            entries = getResourceManager().getRawFileEntry("phone/resources/rawfile/").getEntries();
            LogUtil.info(LogUtil.TAG_LOG, String.valueOf(entries.length));
            for (Entry entry: entries) {
                LogUtil.info(LogUtil.TAG_LOG, entry.getPath());
                if (entry.getPath().toString().contains(".mp3")
                        || entry.getPath().toString().contains(".aac")) {
                    PacMap pacMap = new PacMap();
                    pacMap.putString("Category","HarmonyOS");
                    pacMap.putString("Duration","");
                    pacMap.putString("Mode",ONLINE_AUDIO);

                    onlineAudioAVElements.add(
                            new AVElement(new AVDescription.Builder()
                                    .setTitle(entry.getPath().substring(0, entry.getPath().lastIndexOf(".")))
                                    .setIMediaUri(Uri.parse("phone/resources/rawfile/"+entry.getPath().toString()))
                                    .setMediaId("phone/resources/rawfile/"+entry.getPath().toString())
                                    .setSubTitle("Huawei")
                                    .setDescription("HarmonyOS")
                                    .setExtras(pacMap)
                                    .build(),
                                    AVElement.AVELEMENT_FLAG_PLAYABLE));
                } else {
                    if (entry.getPath().toString().contains(".mp4")) {
                        PacMap pacMap = new PacMap();
                        pacMap.putString("Category","HarmonyOS");
                        pacMap.putString("Duration","");
                        pacMap.putString("Mode",ONLINE_VIDEO);

                        onlineVideosAVElements.add(
                                new AVElement(new AVDescription.Builder()
                                        .setTitle(entry.getPath().substring(0, entry.getPath().lastIndexOf(".")))
                                        .setIMediaUri(Uri.parse("phone/resources/rawfile/"+entry.getPath().toString()))
                                        .setMediaId("phone/resources/rawfile/"+entry.getPath().toString())
                                        .setSubTitle("Huawei")
                                        .setDescription("HarmonyOS")
                                        .setExtras(pacMap)
                                        .build(),
                                        AVElement.AVELEMENT_FLAG_PLAYABLE));
                    }
                }
            }

            playerAdapter = new SongsListAdapter(getOnlineSongs(), getContext());
            listContainer.setItemProvider(playerAdapter);

            setCurrentSongsDetails();
            isPlayingUI();
            getOnlineVideos();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getOnLineSongsResponse() {
        handlingLoadingProgress();
        LogUtil.error(TAG_LOG, "--------callretofit inside-------");
        Call<ArrayList<PlayItemModel>> call = apiInterface.getOnLineSongsLists();
        call.enqueue(new Callback<ArrayList<PlayItemModel>>() {
            @Override
            public void onResponse(Call<ArrayList<PlayItemModel>> call, Response<ArrayList<PlayItemModel>> response) {

                LogUtil.error(TAG_LOG, "--------list_size-------"+response.body().size());

                onlineAudioAVElements = new ArrayList<>();

                for (int i = 0; i < response.body().size(); i++) {
                    PacMap pacMap = new PacMap();
                    pacMap.putString("Category",response.body().get(i).getCategory());
                    pacMap.putString("Duration",response.body().get(i).getDuration());
                    pacMap.putString("Mode",response.body().get(i).getMode());

                    onlineAudioAVElements.add(
                            new AVElement(new AVDescription.Builder()
                                    .setTitle(response.body().get(i).getTitle())
                                    .setIMediaUri(Uri.parse(response.body().get(i).getSongUrl()))
                                    .setMediaId(response.body().get(i).getSongUrl())
                                    .setIconUri(Uri.parse(response.body().get(i).getThumbnailUrl()))
                                    .setSubTitle(response.body().get(i).getSinger())
                                    .setDescription(response.body().get(i).getAlbum())
                                    .setExtras(pacMap)
                                    .build(),
                                    AVElement.AVELEMENT_FLAG_PLAYABLE));
                }

                playerAdapter = new SongsListAdapter(getOnlineSongs(), getContext());
                listContainer.setItemProvider(playerAdapter);

                setCurrentSongsDetails();
                isPlayingUI();

                handlingLoadingProgress();
            }

            @Override
            public void onFailure(Call<ArrayList<PlayItemModel>> call, Throwable throwable) {
                call.cancel();

                handlingLoadingProgress();
            }
        });
    }

    private void getOnLineVideosResponse() {
        LogUtil.error(TAG_LOG, "--------callretofit inside-------");
        Call<ArrayList<PlayItemModel>> call = apiInterface.getOnLineVideosLists();
        call.enqueue(new Callback<ArrayList<PlayItemModel>>() {
            @Override
            public void onResponse(Call<ArrayList<PlayItemModel>> call, Response<ArrayList<PlayItemModel>> response) {

                LogUtil.error(TAG_LOG, "--------list_size-------"+response.body().size());

                onlineVideosAVElements = new ArrayList<>();

                for (int i = 0; i < response.body().size(); i++) {
                    PacMap pacMap = new PacMap();
                    pacMap.putString("Category",response.body().get(i).getCategory());
                    pacMap.putString("Duration",response.body().get(i).getDuration());
                    pacMap.putString("Mode",response.body().get(i).getMode());

                    onlineVideosAVElements.add(
                            new AVElement(new AVDescription.Builder()
                                    .setTitle(response.body().get(i).getTitle())
                                    .setIMediaUri(Uri.parse(response.body().get(i).getSongUrl()))
                                    .setMediaId(response.body().get(i).getSongUrl())
                                    .setIconUri(Uri.parse(response.body().get(i).getThumbnailUrl()))
                                    .setSubTitle(response.body().get(i).getSinger())
                                    .setDescription(response.body().get(i).getAlbum())
                                    .setExtras(pacMap)
                                    .build(),
                                    AVElement.AVELEMENT_FLAG_PLAYABLE));
                }

                getOnlineVideos();
            }

            @Override
            public void onFailure(Call<ArrayList<PlayItemModel>> call, Throwable throwable) {
                call.cancel();
            }
        });
    }

    private List<PlayItemModel> getOnlineVideos() {
        onlineVideosList = new ArrayList<>();

        if (onlineVideosAVElements != null && onlineVideosAVElements.size() >= 1) {
            for (int i = 0; i < onlineVideosAVElements.size(); i++) {
                AVElement item = onlineVideosAVElements.get(i);
                String title = item.getAVDescription().getTitle().toString();
                String subtitle = item.getAVDescription().getSubTitle().toString();
                String songURL = item.getAVDescription().getMediaUri().toString();
                String album = item.getAVDescription().getDescription().toString();

                String category = item.getAVDescription().getExtras().getString("Category");
                String duration = item.getAVDescription().getExtras().getString("Duration");

                onlineVideosList.add(new PlayItemModel(
                        ResourceTable.Media_OttSplash,
                        title,
                        "",
                        songURL,
                        album,
                        subtitle,
                        category,
                        duration,
                        ONLINE_VIDEO
                ));
            }
        }
        return onlineVideosList;
    }

    private List<PlayItemModel> getOnlineSongs() {
        onlineSongsList = new ArrayList<>();

        if (onlineAudioAVElements != null && onlineAudioAVElements.size() >= 1) {
            for (int i = 0; i < onlineAudioAVElements.size(); i++) {
                AVElement item = onlineAudioAVElements.get(i);
                String title = item.getAVDescription().getTitle().toString();
                String subtitle = item.getAVDescription().getSubTitle().toString();
                String songURL = item.getAVDescription().getMediaUri().toString();
                String album = item.getAVDescription().getDescription().toString();

                String category = item.getAVDescription().getExtras().getString("Category");
                String duration = item.getAVDescription().getExtras().getString("Duration");

                onlineSongsList.add(new PlayItemModel(
                        ResourceTable.Media_OttSplash,
                        title,
                        "",
                        songURL,
                        album,
                        subtitle,
                        category,
                        duration,
                        ONLINE_AUDIO
                ));
            }
        }
        return onlineSongsList;
    }

    @Override
    public void onActive() {
        super.onActive();
    }

    @Override
    public void onForeground(Intent intent) {
        super.onForeground(intent);
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
        LogUtil.debug(TAG_LOG,"onEventReceive");
        if (deviceID != null && !deviceID.isEmpty()) {
            localDeviceId = deviceID;
            LogUtil.debug(TAG_LOG,"onEventReceive: LocalDeviceId - "+localDeviceId);
        }
        int tempPos = currentPosition;
        if (currentPosition != position) {
            currentPosition = position;
        }
        LogUtil.debug(TAG_LOG,"onEventReceive: Command - "+result);
        modePlaying = mode;
        LogUtil.debug(TAG_LOG,"onEventReceive: Command - "+modePlaying);
        switch (result) {
            case Constants.START:
                if (mode.contains(ONLINE_AUDIO)) {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (mediaPlayerPlugin.isPlaying()) {
                                pause();
                            } else {
                                play();
                            }
                            LogUtil.debug(TAG_LOG,"Controller_from_watch: "+ONLINE_AUDIO);

                            // Multi device conn.
                            startService();
                        }
                    }, TIMER_MILLISECOND);
                } else {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            LogUtil.debug(TAG_LOG,"Controller_from_watch: "+ONLINE_VIDEO);
                            sendDataToVideoPlayer(position);
                        }
                    }, TIMER_MILLISECOND);
                }
                break;
            case Constants.PLAY:
                if (mode.contains(ONLINE_AUDIO)) {
                    if (mediaPlayerPlugin.isPlaying()) {
                        pause();
                    } else {
                        play();
                    }
                } else {
                    LogUtil.debug(TAG_LOG,"Controller_from_watch: "+ONLINE_VIDEO);
                    sendDataToVideoPlayer(position);
                }
                break;
            case Constants.PAUSE:
                pause();
                break;
            case Constants.NEXT:
            case Constants.PREVIOUS:
                if (mode.contains(ONLINE_AUDIO)) {
                    LogUtil.debug(TAG_LOG,"Controller_from_watch: "+ONLINE_AUDIO);
                    if (!isLooping) {
                        if(!mShuffleClicked){
                            play(currentPosition);
                        }   else{
                            shuffleSong1();
                        }
                    } else {
                        currentPosition = tempPos;
                        play(currentPosition);
                    }
                } else {
                    LogUtil.debug(TAG_LOG,"Controller_from_watch: "+ONLINE_VIDEO);
                    sendDataToVideoPlayer(position);
                }
                break;
            case Constants.FINISH:
                System.exit(0);
                break;
            case Constants.PLAYING:
                CommonFunctions.getInstance().showToast(DashboardPlayerAbilitySlice.this, "Playing");
                break;
            case Constants.PING:
                CommonFunctions.getInstance().showToast(DashboardPlayerAbilitySlice.this,"Pinging from watch");
                sendResponseRemote(PING, "");
                break;
            default:
                // Default case
                break;
        }
    }

    private void sendDataToVideoPlayer(int position) {
        mediaPlayerPlugin.release();

        Intent intent = new Intent();
        if (onlineVideosAVElements!= null && onlineVideosAVElements.size() >= 1) {
            currentPosition = position;

            setCurrentSongsDetails();

            Operation operation = new Intent.OperationBuilder()
                    .withDeviceId("")
                    .withBundleName(PACKAGE_NAME)
                    .withAbilityName(VIDEO_ABILITY)
                    .build();
            intent.setOperation(operation);
            intent.setParam("OnlineVideoList", onlineVideosList);
            intent.setParam("CurrentPosition", currentPosition);
            intent.setParam("DeviceId", localDeviceId);
            startAbilityForResult(intent, REQUEST_CODE);
        }
    }

    private void play(int position) {
        if (progressTimerTask != null) {
            progressTimerTask.cancel();
        }
        mBufferStart = false;

        getUITaskDispatcher().asyncDispatch(() -> {
            mStartTime.setText("00:00:00");
            mEndTime.setText("00:00:00");
            mTimeProgressbar.setMinValue(0);
        });

        if (modePlaying.equals(ONLINE_AUDIO)) {
            LogUtil.debug(TAG_LOG,""+ONLINE_AUDIO);

            if (onlineAudioAVElements != null && onlineAudioAVElements.size() >= 1) {
                int maxPosition = onlineAudioAVElements.size() - 1;
                if (position > maxPosition) {
                    position = 0;
                } else if (position < 0) {
                    position = maxPosition;
                }
                currentPosition = position;

                mediaPlayerPlugin.release();
                mediaPlayerPlugin.startPlay(onlineAudioAVElements.get(position));

                setCurrentSongsDetails();
                pauseSongUI();
            }
        } else  if (modePlaying.equals(ONLINE_VIDEO)) {
            mediaPlayerPlugin.release();

            int maxPosition = onlineAudioAVElements.size() - 1;
            if (position > maxPosition) {
                position = 0;
            } else if (position < 0) {
                position = maxPosition;
            }
            currentPosition = position;
            setCurrentSongsDetails();

            LogUtil.debug(TAG_LOG,""+ONLINE_VIDEO);
            Intent intent = new Intent();
            Operation operation = new Intent.OperationBuilder()
                    .withDeviceId("")
                    .withBundleName(PACKAGE_NAME)
                    .withAbilityName(VIDEO_ABILITY)
                    .withAction(VIDEO_PLAYER_ACTION)
                    .build();
            intent.setOperation(operation);
            intent.setParam("OnlineVideoList", onlineVideosList);
            intent.setParam("CurrentPosition", currentPosition);
            intent.setParam("DeviceId", localDeviceId);
            startAbilityForResult(intent, REQUEST_CODE);
        } else {
            LogUtil.debug(TAG_LOG,""+OFFLINE_AUDIO);
            if (offlineAudioAVElements != null && offlineAudioAVElements.size() >= 1) {
                int maxPosition = offlineAudioAVElements.size() - 1;
                if (position > maxPosition) {
                    position = 0;
                } else if (position < 0) {
                    position = maxPosition;
                }
                currentPosition = position;

                mediaPlayerPlugin.release();
                mediaPlayerPlugin.startPlay(offlineAudioAVElements.get(position));

                setCurrentSongsDetails();
                pauseSongUI();

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        startProgressTaskTimer();
                    }
                }, 1000);
            }
        }
    }

    @Override
    protected void onAbilityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case REQUEST_CODE:
                LogUtil.info(TAG_LOG, "PlayerListSlice onAbilityResult->"+requestCode);
                if (resultData != null) {
                    CommonFunctions.getInstance().showToast(DashboardPlayerAbilitySlice.this,"onAbilityResult calling");
                    if (resultData.hasParameter("DeviceId")) {
                        Object obj = resultData.getParams().getParam("DeviceId");
                        if (obj instanceof String) {
                            String deviceId = (String) obj;
                            LogUtil.debug(TAG_LOG,"Received deviceId from Intent: "+deviceId);
                        }
                    }
                    if (resultData.hasParameter("CurrentPosition")) {
                        currentPosition = resultData.getIntParam("CurrentPosition",0);
                    }
                    if (resultData.hasParameter("Mode")) {
                        modePlaying = resultData.getStringParam("Mode");
                        if (modePlaying.equals(ONLINE_VIDEO)) {
                            modePlaying = ONLINE_VIDEO;
                            mOnlineVideoBtn.setTextColor(Color.WHITE);
                            mOnlineSongsBtn.setTextColor(Color.GRAY);
                            playerAdapter = new SongsListAdapter(getOnlineVideos(), getContext());
                        } else {
                            modePlaying = ONLINE_AUDIO;
                            mOnlineVideoBtn.setTextColor(Color.GRAY);
                            mOnlineSongsBtn.setTextColor(Color.WHITE);
                            playerAdapter = new SongsListAdapter(getOnlineSongs(), getContext());
                        }
                        listContainer.setItemProvider(playerAdapter);
                        setCurrentSongsDetails();
                    }
                }
                break;
        }
    }

    @Override
    protected void onResult(int requestCode, Intent intent) {
        LogUtil.info(TAG_LOG, "PlayerListSlice onResult->"+requestCode);
        CommonFunctions.getInstance().showToast(DashboardPlayerAbilitySlice.this,"onResult calling");
        if (requestCode == REQUEST_CODE) {
            if (intent != null) {
                if (intent.hasParameter("DeviceId")) {
                    Object obj = intent.getParams().getParam("DeviceId");
                    if (obj instanceof String) {
                        String deviceId = (String) obj;
                        localDeviceId = deviceId;
                        LogUtil.debug(TAG_LOG,"Received deviceId from Intent: "+localDeviceId);
                    }
                }
                if (intent.hasParameter("CurrentPosition")) {
                    currentPosition = intent.getIntParam("CurrentPosition",0);
                }
                if (intent.hasParameter("Mode")) {
                    modePlaying = intent.getStringParam("Mode");
                    if (modePlaying.equals(ONLINE_VIDEO)) {
                        modePlaying = ONLINE_VIDEO;
                        mOnlineVideoBtn.setTextColor(Color.WHITE);
                        mOnlineSongsBtn.setTextColor(Color.GRAY);
                        playerAdapter = new SongsListAdapter(getOnlineVideos(), getContext());
                    } else {
                        modePlaying = ONLINE_AUDIO;
                        mOnlineVideoBtn.setTextColor(Color.GRAY);
                        mOnlineSongsBtn.setTextColor(Color.WHITE);
                        playerAdapter = new SongsListAdapter(getOnlineSongs(), getContext());
                    }
                    listContainer.setItemProvider(playerAdapter);
                    setCurrentSongsDetails();
                }
            }
        }
    }

    private void setCurrentSongsDetails() {
        getUITaskDispatcher().asyncDispatch(() -> {
            String title = "Song name";
            if (modePlaying.equals(ONLINE_AUDIO)) {
                if (onlineAudioAVElements != null && onlineAudioAVElements.size() >= 1) {
                    AVElement item = onlineAudioAVElements.get(currentPosition);
                    title = item.getAVDescription().getTitle().toString();

                    mSongSinger.setText(onlineSongsList.get(currentPosition).getSinger());
                    mSongFilm.setText(onlineSongsList.get(currentPosition).getAlbum());
                    mSongTitle.setText(title);
                    mSongTitleDetail.setText(title);

                    // Set seekbar progress using time played
                    Glide.with(getContext())
                            .load(ResourceTable.Media_OttSplash)
                            .placeholder(ResourceTable.Media_OttSplash)
                            .error(ResourceTable.Media_OttSplash)
                            .into(mSongThumbnailImage);

                    Glide.with(getContext())
                            .load(ResourceTable.Media_OttSplash)
                            .placeholder(ResourceTable.Media_OttSplash)
                            .error(ResourceTable.Media_OttSplash)
                            .into(mThumbnailImage);
                }
            } else if (modePlaying.equals(ONLINE_VIDEO)) {
                if (onlineVideosAVElements != null && onlineVideosAVElements.size() >= 1) {
                    getUITaskDispatcher().asyncDispatch(() -> {
                        String songName = onlineVideosList.get(currentPosition).getTitle();
                        mSongTitle.setText(songName);
                        String imageURL = onlineVideosList.get(currentPosition).getThumbnailUrl();

                        // Set seekbar progress using time played
                        Glide.with(getContext())
                                .load(imageURL)
                                .placeholder(ResourceTable.Media_OttSplash)
                                .error(ResourceTable.Media_OttSplash)
                                .into(mSongThumbnailImage);
                    });
                }
            } else {
                if (offlineAudioAVElements != null && offlineAudioAVElements.size() >= 1) {
                    AVElement item = offlineAudioAVElements.get(currentPosition);
                    title = item.getAVDescription().getTitle().toString();

                    mSongSinger.setText(offlineSongsList.get(currentPosition).getSinger());
                    mSongFilm.setText(offlineSongsList.get(currentPosition).getAlbum());
                    mSongTitle.setText(title);
                    mSongTitleDetail.setText(title);

                    Glide.with(getContext())
                            .load(ResourceTable.Media_OttSplash)
                            .into(mSongThumbnailImage);
                    Glide.with(getContext())
                            .load(ResourceTable.Media_OttSplash)
                            .into(mThumbnailImage);

                    Glide.with(getContext())
                            .load(ResourceTable.Media_OttSplash)
                            .placeholder(ResourceTable.Media_OttSplash)
                            .error(ResourceTable.Media_OttSplash)
                            .into(mSongThumbnailImage);
                }
            }
        });
    }

    private void isPlayingUI() {
        if (mediaPlayerPlugin != null) {
            boolean flag = mediaPlayerPlugin.startPlayer();
            if (!flag) {
                playSongUI();
            } else {
                pauseSongUI();
            }
        } else {
            playSongUI();
        }
    }

    private void pauseSongUI() {
        getUITaskDispatcher().asyncDispatch(() -> {
            mPlayButton.setPixelMap(ResourceTable.Media_pause_icon);
            mPlayBtn.setPixelMap(ResourceTable.Media_pause_icon);
        });

        sendResponseRemote(PLAY, "");
    }

    private void playSongUI() {
        getUITaskDispatcher().asyncDispatch(() -> {
            mPlayButton.setPixelMap(ResourceTable.Media_play);
            mPlayBtn.setPixelMap(ResourceTable.Media_play);
        });

        sendResponseRemote(PAUSE, "");
    }

    private void play() {
        boolean flag = mediaPlayerPlugin.startPlayer();
        if (!flag) {
            play(currentPosition);
        } else {
            pauseSongUI();
        }
    }

    private void pause() {
        mediaPlayerPlugin.pausePlay();
        playSongUI();
    }

    private void previousSong() {
        if (!isLooping) {
            if(!mShuffleClicked){
                play(currentPosition-1);
            }   else{
                shuffleSong1();
            }
        } else {
            if(!mShuffleClicked){
                play(currentPosition);
            } else{
                shuffleSong1();
            }
        }
    }

    private void nextSong() {
        if (!isLooping) {
            if(!mShuffleClicked){
                play(currentPosition+1);
            }   else{
                shuffleSong1();
            }
        } else {
            if(!mShuffleClicked){
                play(currentPosition);
            } else{
                shuffleSong1();
            }
        }
    }

    private void shuffleSong1() {
        final int min = 1;
        int max = 0;
        if (modePlaying.equals(ONLINE_AUDIO)) {
            if (onlineAudioAVElements != null && onlineAudioAVElements.size() >= 1) {
                if (!isLooping) {
                    max = onlineAudioAVElements.size();
                    final int random = new SecureRandom().nextInt((max - min) + 1) + min;
                    checkRandomValue(random);
                }
            }
        } else if (modePlaying.equals(ONLINE_VIDEO)) {
            if (onlineVideosAVElements != null && onlineVideosAVElements.size() >= 1) {
                max = onlineVideosAVElements.size();
                final int random = new SecureRandom().nextInt((max - min) + 1) + min;
                currentPosition = random;
            }
        } else{
            if (offlineAudioAVElements != null && offlineAudioAVElements.size() >= 1) {
                if (!isLooping) {
                    max = offlineAudioAVElements.size();
                    final int random = new SecureRandom().nextInt((max - min) + 1) + min;
                    checkRandomValue(random);
                }
            }
        }

        play(currentPosition);
    }

    private void checkRandomValue(int random) {
        if (currentPosition != random) {
            currentPosition = random;
        } else {
            random += 1;
            int maxPosition = onlineAudioAVElements.size() - 1;
            if (random > maxPosition) {
                random = 0;
            } else if (random < 0) {
                random = maxPosition;
            }
            currentPosition = random;
        }
    }

    private void shuffleSong() {
        getUITaskDispatcher().asyncDispatch(() -> {
            if(!mShuffleClicked){
                mShuffleBtn.setPixelMap(ResourceTable.Media_ic_shuffle_red);
                mShuffleClicked = true;

            }else{
                mShuffleClicked = false;
                mShuffleBtn.setPixelMap(ResourceTable.Media_ic_shuffle);
            }
        });
    }

    private void loopSong() {
        getUITaskDispatcher().asyncDispatch(() -> {
            if (!isLooping) {
                mediaPlayerPlugin.looping(true);
                isLooping = true;
                mLoopBtn.setPixelMap(ResourceTable.Media_ic_oval_loop_red);
                CommonFunctions.getInstance().showToast(this,"Auto repeat song ON");
            } else {
                mediaPlayerPlugin.looping(false);
                isLooping = false;
                mLoopBtn.setPixelMap(ResourceTable.Media_ic_oval_loop);
                CommonFunctions.getInstance().showToast(this,"Auto repeat song OFF");
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (animatorValue != null) {
            animatorValue.stop();
        }
        DeviceManager.unregisterDeviceStateCallback(iDeviceStateCallback);
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

    private void updateSeekBarTime(){
        LogUtil.debug(TAG_LOG,"Res PlayerListSlice.updateSeekBarTime 1 ");
        // get current position
        timeElapsed = mediaPlayerPlugin.getCurrentTime();
        finalTime = mediaPlayerPlugin.getDuration();
        LogUtil.debug(TAG_LOG,"Res PlayerListSlice.updateSeekBarTime 1 finalTime - "+finalTime);
        LogUtil.debug(TAG_LOG,"Res PlayerListSlice.updateSeekBarTime 1 timeElapsed - "+timeElapsed);

        getUITaskDispatcher().asyncDispatch(() -> {
            // set seekbar progress using time played
            mTimeProgressbar.setMaxValue(finalTime);
            mTimeProgressbar.setProgressValue(timeElapsed);

            // set time remaining in minutes and seconds
            timeRemaining = finalTime - timeElapsed;

            mEndTime.setText(DateUtils.msToString(finalTime));
        });
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
                // Multi device conn.
                if (deviceInformation.getDeviceType() == DeviceInfo.DeviceType.SMART_WATCH
                        || deviceInformation.getDeviceType() == DeviceInfo.DeviceType.SMART_PHONE
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
                getUITaskDispatcher().asyncDispatch(() -> {
                    CommonFunctions.getInstance().showToast(this,"Cannot connect service on watch");
                });
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

            sendResponseRemote(ACKNOWLEDGE, "");
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
                if (modePlaying.equals(ONLINE_AUDIO)) {
                    if (onlineAudioAVElements != null && onlineAudioAVElements.size() >= 1) {
                        AVElement item = onlineAudioAVElements.get(currentPosition);
                        String title = item.getAVDescription().getTitle().toString();

                        if (onlineSongsList != null && onlineSongsList.size() >= 1) {
                            if (onlineSongsList.get(currentPosition).getSinger() != null
                                    && !onlineSongsList.get(currentPosition).getSinger().isEmpty()) {
                                singer = onlineSongsList.get(currentPosition).getSinger();
                            }
                        }

                        responseRemoteProxy.remoteControl(Constants.ACKNOWLEDGE, "", title, ONLINE_AUDIO, status, singer, errorMsg, currentPosition);
                    }
                } else if (modePlaying.equals(ONLINE_VIDEO)) {
                    if (onlineVideosAVElements != null && onlineVideosAVElements.size() >= 1) {
                        AVElement item = onlineVideosAVElements.get(currentPosition);
                        String title = item.getAVDescription().getTitle().toString();

                        if (onlineVideosList != null && onlineVideosList.size() >= 1) {
                            if (onlineVideosList.get(currentPosition).getSinger() != null
                                    && !onlineVideosList.get(currentPosition).getSinger().isEmpty()) {
                                singer = onlineVideosList.get(currentPosition).getSinger();
                            }
                        }

                        responseRemoteProxy.remoteControl(Constants.ACKNOWLEDGE, "", title, ONLINE_VIDEO, status, singer, errorMsg, currentPosition);
                    }
                } else {
                    if (offlineAudioAVElements != null && offlineAudioAVElements.size() >= 1) {
                        AVElement item = offlineAudioAVElements.get(currentPosition);
                        String title = item.getAVDescription().getTitle().toString();

                        if (offlineSongsList != null && offlineSongsList.size() >= 1) {
                            if (offlineSongsList.get(currentPosition).getSinger() != null
                            && !offlineSongsList.get(currentPosition).getSinger().isEmpty()) {
                                singer = offlineSongsList.get(currentPosition).getSinger();
                            }
                        }

                        responseRemoteProxy.remoteControl(Constants.ACKNOWLEDGE, "", title, OFFLINE_AUDIO, status, singer, errorMsg, currentPosition);
                    }
                }
            }
        }
    }

    /**
     * Handling loading progress bar.
     */
    private void handlingLoadingProgress() {
        getUITaskDispatcher().asyncDispatch(() -> {
            if (loadingImgContent.getVisibility() == Component.VISIBLE) {
                loadingImgContent.setVisibility(Component.INVISIBLE);
                if (mAnimatorProperty != null) {
                    mAnimatorProperty.stop();
                }
            } else {
                loadingImgContent.setVisibility(Component.VISIBLE);
                if (mAnimatorProperty != null) {
                    mAnimatorProperty.start();
                }
            }
        });
    }

    /**
     * Handling Animation.
     */
    private void animatorPropertyHandle() {
        mAnimatorProperty = loadingBtn.createAnimatorProperty();
        mAnimatorProperty.rotate(360).setDuration(2000).setLoopedCount(1000);
        loadingBtn.setBindStateChangedListener(new Component.BindStateChangedListener() {
            @Override
            public void onComponentBoundToWindow(Component component) {
                if (mAnimatorProperty != null) {
                    mAnimatorProperty.start();
                }
            }

            @Override
            public void onComponentUnboundFromWindow(Component component) {
            }
        });
    }
}
