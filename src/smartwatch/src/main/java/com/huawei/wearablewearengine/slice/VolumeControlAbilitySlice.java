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
import com.huawei.wearablewearengine.utils.CommonFunctions;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.ProgressBar;
import ohos.media.audio.AudioManager;
import ohos.media.audio.AudioRemoteException;

import static com.huawei.wearablewearengine.utils.Constants.CONTROLLER_SLICE;

public class VolumeControlAbilitySlice extends AbilitySlice implements Component.ClickedListener {

    private Image imgVolumeMinus;
    private Image imgVolumePlus;
    private Image img_VolumeMute;
    private AudioManager audioManager;
    private ProgressBar progressbar;
    private int currentVolume = 0;
    private boolean isMuted = false;

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_layout_volumecontrol);

        CommonFunctions.getInstance().setMarkLockAsScreenOn();

        initialize();
    }

    /**
     * Initialize view.
     */
    private void initialize() {
        CommonFunctions.getInstance().setMarkLockAsScreenOn();

        progressbar = (ProgressBar) findComponentById(ResourceTable.Id_progressbar);
        imgVolumeMinus = (Image) findComponentById(ResourceTable.Id_imgVolumeMinus);
        imgVolumePlus = (Image) findComponentById(ResourceTable.Id_imgVolumePlus);
        img_VolumeMute = (Image) findComponentById(ResourceTable.Id_img_VolumeMute);

        imgVolumeMinus.setClickedListener(this::onClick);
        imgVolumePlus.setClickedListener(this::onClick);
        img_VolumeMute.setClickedListener(this::onClick);

        initMusicVolumn();
    }

    /**
     * Initialize volume management.
     */
    private void initMusicVolumn() {
        try {
            audioManager = new AudioManager(this);

            int maxVolumn = audioManager.getMaxVolume(AudioManager.AudioVolumeType.STREAM_MUSIC);
            progressbar.setMaxValue(maxVolumn);
            int minVolumn = audioManager.getMinVolume(AudioManager.AudioVolumeType.STREAM_MUSIC);
            progressbar.setMinValue(minVolumn);
            int defaultVolumn = audioManager.getVolume(AudioManager.AudioVolumeType.STREAM_MUSIC);
            progressbar.setProgressValue(defaultVolumn);

            currentVolume = defaultVolumn;

            if (currentVolume == 0) {
                isMuted = true;
                img_VolumeMute.setPixelMap(ResourceTable.Media_ic_no_audio);
            } else {
                isMuted = false;
                img_VolumeMute.setPixelMap(ResourceTable.Media_ic_audio);
            }


        } catch (AudioRemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Component OnClicklistener.
     * @param component as Component
     */
    @Override
    public void onClick(Component component) {
        int id = component.getId();
        if (id == ResourceTable.Id_imgVolumePlus) {
            setMusicVolume(1);
        } else if (id == ResourceTable.Id_imgVolumeMinus) {
            setMusicVolume(-1);
        } else if (id == ResourceTable.Id_img_VolumeMute) {
            if (isMuted) {
                isMuted = false;
                audioManager.setVolume(AudioManager.AudioVolumeType.STREAM_MUSIC, 5);
                img_VolumeMute.setPixelMap(ResourceTable.Media_ic_audio);
                progressbar.setProgressValue(5);
            } else {
                isMuted = true;
                audioManager.setVolume(AudioManager.AudioVolumeType.STREAM_MUSIC, 0);
                img_VolumeMute.setPixelMap(ResourceTable.Media_ic_no_audio);
                progressbar.setProgressValue(0);
            }
        }
    }

    /**
     * Set music volume.
     * @param index as volume level
     */
    private void setMusicVolume(Integer index) {
        audioManager.changeVolumeBy(AudioManager.AudioVolumeType.STREAM_MUSIC, index);
        try {
            int defaultVolumn = audioManager.getVolume(AudioManager.AudioVolumeType.STREAM_MUSIC);
            progressbar.setProgressValue(defaultVolumn);
        } catch (AudioRemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActive() {
        super.onActive();
    }

    @Override
    public void onForeground(Intent intent) {
        super.onForeground(intent);
    }

    /**
     * AbilitySlice lifecycle method onStart.
     */
    @Override
    protected void onBackPressed() {
        super.onBackPressed();
    }
}
