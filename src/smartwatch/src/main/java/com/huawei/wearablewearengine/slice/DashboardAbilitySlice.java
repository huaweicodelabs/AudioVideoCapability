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
import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.Text;

import static com.huawei.wearablewearengine.utils.Constants.MAIN_ABILITY;
import static com.huawei.wearablewearengine.utils.Constants.PACKAGE_NAME;
import static com.huawei.wearablewearengine.utils.Constants.ALL_SONG_ABILITY_SLICE;

public class DashboardAbilitySlice extends AbilitySlice implements Component.ClickedListener {

    private Button btnAudio;
    private Button btnVideo;
    private Text title;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_dashbord);

        initView();
    }

    private void initView() {
        btnAudio=(Button)findComponentById(ResourceTable.Id_btnAudio);
        btnVideo=(Button)findComponentById(ResourceTable.Id_btnVideo);
        title = (Text) findComponentById(ResourceTable.Id_title);
        title.setText("Dashboard");

        btnAudio.setClickedListener(this::onClick);
        btnVideo.setClickedListener(this::onClick);
    }


    @Override
    public void onClick(Component component) {

        switch (component.getId()) {

            case ResourceTable.Id_btnAudio:

                Intent audioIntent = new Intent();
                Operation audioOperation = new Intent.OperationBuilder()
                        .withDeviceId("")
                        .withBundleName(PACKAGE_NAME)
                        .withAbilityName(MAIN_ABILITY)
                        .withAction(ALL_SONG_ABILITY_SLICE)
                        .build();
                audioIntent.setOperation(audioOperation);
                audioIntent.setParam("format", "audio");
                startAbility(audioIntent);

                return;

            case ResourceTable.Id_btnVideo:

                Intent videoIntent = new Intent();
                Operation videoOperation = new Intent.OperationBuilder()
                        .withDeviceId("")
                        .withBundleName(PACKAGE_NAME)
                        .withAbilityName(MAIN_ABILITY)
                        .withAction(ALL_SONG_ABILITY_SLICE)
                        .build();
                videoIntent.setOperation(videoOperation);
                videoIntent.setParam("format", "video");
                startAbility(videoIntent);

                return;

            default:
                // Default case
                return;
        }
    }
}
