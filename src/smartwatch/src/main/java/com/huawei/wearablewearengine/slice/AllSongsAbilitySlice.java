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
import com.huawei.wearablewearengine.adapter.SongsListAdapter;
import com.huawei.wearablewearengine.interfaces.Adapterlistener;
import com.huawei.wearablewearengine.model.PlayItemModel;
import com.huawei.wearablewearengine.network.RestApiInterface;
import com.huawei.wearablewearengine.network.RestClient;
import com.huawei.wearablewearengine.utils.CommonFunctions;
import com.huawei.wearablewearengine.utils.LogUtil;
import com.huawei.wearablewearengine.utils.PreferenceUtil;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.animation.AnimatorProperty;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.Image;
import ohos.agp.components.Text;
import ohos.agp.components.Component;
import ohos.agp.components.ListContainer;
import ohos.global.resource.Entry;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;

import static com.huawei.wearablewearengine.utils.Constants.ONLINE_AUDIO;
import static com.huawei.wearablewearengine.utils.Constants.ONLINE_VIDEO;
import static com.huawei.wearablewearengine.utils.Constants.MAIN_ABILITY;
import static com.huawei.wearablewearengine.utils.Constants.PACKAGE_NAME;
import static com.huawei.wearablewearengine.utils.Constants.CONTROLLER_SLICE;
import static com.huawei.wearablewearengine.utils.Constants.VIDEO_PLAY_SLICE;
import static com.huawei.wearablewearengine.utils.LogUtil.TAG_LOG;

public class AllSongsAbilitySlice extends AbilitySlice implements Adapterlistener {
    private ListContainer songListContainer;
    private SongsListAdapter songsListAdapter;
    private ArrayList<PlayItemModel> onlineSongsList;

    private RestApiInterface apiInterface;
    private String songsFormat = "";
    private Text title;
    private Image loadingBtn;
    private DirectionalLayout loadingImgContent;
    private AnimatorProperty mAnimatorProperty;
    private PreferenceUtil preferenceUtil;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_allsongs);

        if (intent != null) {
            if (intent.hasParameter("format")) {
                songsFormat = intent.getStringParam("format");
            }
        }

        preferenceUtil = PreferenceUtil.getInstance();

        initView();
        animatorPropertyHandle();
    }

    private void initView() {
        songListContainer = (ListContainer) findComponentById(ResourceTable.Id_song_list_container);
        title = (Text) findComponentById(ResourceTable.Id_title);

        loadingBtn = (Image) findComponentById(ResourceTable.Id_imgLoading);
        loadingImgContent = (DirectionalLayout) findComponentById(ResourceTable.Id_loading_img_content);

        apiInterface = RestClient.getClient().create(RestApiInterface.class);

        if (songsFormat.equalsIgnoreCase("audio")) {
            title.setText("Audio List");
            initLocalFile();
        } else if (songsFormat.equalsIgnoreCase("video")) {
            title.setText("Video List");
            initLocalFile();
        } else {
            CommonFunctions.getInstance().showToast(this, "No data found");
        }
    }

    private void initLocalFile() {
        onlineSongsList = new ArrayList<>();

        Entry[] entries = new Entry[0];
        try {
            entries = getResourceManager().getRawFileEntry("smartwatch/resources/rawfile/").getEntries();
            LogUtil.info(LogUtil.TAG_LOG, String.valueOf(entries.length));
            for (Entry entry: entries) {
                LogUtil.info(LogUtil.TAG_LOG, entry.getPath());
                if (songsFormat.equalsIgnoreCase("audio")) {
                    if (entry.getPath().toString().contains(".mp3")
                    || entry.getPath().toString().contains(".aac")) {
                        onlineSongsList.add(new PlayItemModel(
                                ResourceTable.Media_OttSplash,
                                entry.getPath().substring(0, entry.getPath().lastIndexOf(".")),
                                "",
                                "smartwatch/resources/rawfile/"+entry.getPath().toString(),
                                "Album",
                                "",
                                "HarmonyOS",
                                "",
                                ONLINE_AUDIO
                        ));
                    }
                } else {
                    if (entry.getPath().toString().contains(".mp4")) {
                        onlineSongsList.add(new PlayItemModel(
                                ResourceTable.Media_OttSplash,
                                entry.getPath().substring(0, entry.getPath().lastIndexOf(".")),
                                "",
                                "smartwatch/resources/rawfile/"+entry.getPath().toString(),
                                "Huawei",
                                "",
                                "HarmonyOS",
                                "",
                                ONLINE_VIDEO
                        ));
                    }
                }
            }
            songsListAdapter = new SongsListAdapter(getContext(), onlineSongsList, AllSongsAbilitySlice.this::onItemClick);
            songListContainer.setItemProvider(songsListAdapter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initApiCall(String songsFormat) {
        handlingLoadingProgress();

        LogUtil.error(TAG_LOG, "--------songsFormate-------" + songsFormat);
        Call<ArrayList<PlayItemModel>> call;
        if (songsFormat.equalsIgnoreCase("audio")) {
            call = apiInterface.getOnLineSongsLists();
        } else {
            call = apiInterface.getOnLineVideosLists();
        }

        call.enqueue(new Callback<ArrayList<PlayItemModel>>() {
            @Override
            public void onResponse(Call<ArrayList<PlayItemModel>> call, Response<ArrayList<PlayItemModel>> response) {

                LogUtil.error(TAG_LOG, "--------list_size-------" + response.body().size());

                onlineSongsList = new ArrayList<>();

                for (int i = 0; i < response.body().size(); i++) {
                    String mode = "";
                    if (songsFormat.equalsIgnoreCase("audio")) {
                        mode = ONLINE_AUDIO;
                    } else {
                        mode = ONLINE_VIDEO;
                    }

                    onlineSongsList.add(new PlayItemModel(
                            ResourceTable.Media_OttSplash,
                            response.body().get(i).getTitle(),
                            response.body().get(i).getThumbnailUrl(),
                            response.body().get(i).getSongUrl(),
                            response.body().get(i).getAlbum(),
                            response.body().get(i).getSinger(),
                            response.body().get(i).getCategory(),
                            response.body().get(i).getDuration(),
                            mode
                    ));
                }

                songsListAdapter = new SongsListAdapter(getContext(), onlineSongsList, AllSongsAbilitySlice.this::onItemClick);
                songListContainer.setItemProvider(songsListAdapter);
                handlingLoadingProgress();
            }

            @Override
            public void onFailure(Call<ArrayList<PlayItemModel>> call, Throwable throwable) {
                handlingLoadingProgress();
                LogUtil.error(TAG_LOG, "--------Call failed-------" + throwable);

                call.cancel();
            }
        });
    }

    @Override
    public void onItemClick(PlayItemModel playItemModel, int position) {
        if (songsFormat.equalsIgnoreCase("audio")) {
            preferenceUtil.putCastingStatus(false, "");

            Intent intent = new Intent();
            Operation operation = new Intent.OperationBuilder()
                    .withDeviceId("")
                    .withBundleName(PACKAGE_NAME)
                    .withAbilityName(MAIN_ABILITY)
                    .withAction(CONTROLLER_SLICE)
                    .build();
            intent.setOperation(operation);
            intent.setParam("OnlineVideoList", onlineSongsList);
            intent.setParam("SelectedSongDetails", playItemModel);
            intent.setParam("CurrentPosition", position);
            intent.setParam("isCasting", false);
            intent.setParam("Streaming", ONLINE_AUDIO);
            startAbility(intent);
        } else {
            preferenceUtil.putCastingStatus(false, "");

            Intent intent = new Intent();
            Operation operation = new Intent.OperationBuilder()
                    .withDeviceId("")
                    .withBundleName(PACKAGE_NAME)
                    .withAbilityName(MAIN_ABILITY)
                    .withAction(VIDEO_PLAY_SLICE)
                    .build();
            intent.setOperation(operation);
            intent.setParam("OnlineVideoList", onlineSongsList);
            intent.setParam("SelectedSongDetails", playItemModel);
            intent.setParam("CurrentPosition", position);
            intent.setParam("isCasting", false);
            intent.setParam("Streaming", ONLINE_VIDEO);
            startAbility(intent);
        }
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

    /**
     * Handling loading progress bar.
     */
    private void handlingLoadingProgress() {
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
    }

    @Override
    protected void onBackPressed() {
        super.onBackPressed();
    }
}
