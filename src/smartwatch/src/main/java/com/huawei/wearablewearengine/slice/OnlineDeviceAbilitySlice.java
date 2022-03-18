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

import com.huawei.wearablewearengine.MainAbility;
import com.huawei.wearablewearengine.ResourceTable;
import com.huawei.wearablewearengine.adapter.OnlineDeviceListAdapter;
import com.huawei.wearablewearengine.model.GrantPermissionModel;
import com.huawei.wearablewearengine.model.PlayItemModel;
import com.huawei.wearablewearengine.utils.CommonFunctions;
import com.huawei.wearablewearengine.utils.Constants;
import com.huawei.wearablewearengine.utils.LogUtil;
import com.huawei.wearablewearengine.utils.PreferenceUtil;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.Component;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Text;
import ohos.bundle.IBundleManager;
import ohos.distributedschedule.interwork.DeviceInfo;
import ohos.distributedschedule.interwork.DeviceManager;
import ohos.distributedschedule.interwork.IDeviceStateCallback;
import ohos.security.SystemPermission;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import static com.huawei.wearablewearengine.utils.Constants.ONLINE_AUDIO;
import static com.huawei.wearablewearengine.utils.Constants.ONLINE_VIDEO;
import static com.huawei.wearablewearengine.utils.LogUtil.TAG_LOG;

public class OnlineDeviceAbilitySlice extends AbilitySlice {

    private ListContainer onlineDeviceListContainer;
    private DirectionalLayout layout_refresh;
    private DirectionalLayout layout_cancel;
    private OnlineDeviceListAdapter deviceListAdapter;
    private DeviceInfo currentDeviceInfo;
    private static final int EVENT_STATE_CHANGE = 10001;
    private Text title;

    private ArrayList<PlayItemModel> onlineSongsList = new ArrayList<>();
    private PlayItemModel playItemModel = new PlayItemModel();
    private int currentPosition = 0;
    private boolean isCasting = false;
    private String mStreaming = "";
    private PreferenceUtil preferenceUtil;
    private MainAbility mainAbility;

    private IDeviceStateCallback iDeviceStateCallback = new IDeviceStateCallback() {
        @Override
        public void onDeviceOffline(String s, int i) {
            fetchOnlineDevice();
        }

        @Override
        public void onDeviceOnline(String s, int i) {
            fetchOnlineDevice();
        }
    };

    /**
     * Abilityslice lifecycle method onStart.
     * @param intent as Intent
     */
    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_online_device);

        mainAbility = (MainAbility) getAbility();
        mainAbility.setSwipeToDismiss(false);

        preferenceUtil = PreferenceUtil.getInstance();

        CommonFunctions.getInstance().setMarkLockAsScreenOn();

        if (intent != null) {
            if (intent.hasParameter("OnlineVideoList")) {
                onlineSongsList = intent.getSerializableParam("OnlineVideoList");
            }
            if (intent.hasParameter("SelectedSongDetails")) {
                playItemModel = intent.getSerializableParam("SelectedSongDetails");
            }
            if (intent.hasParameter("CurrentPosition")) {
                currentPosition = intent.getIntParam("CurrentPosition",0);
            }
            if (intent.hasParameter("isCasting")) {
                isCasting = intent.getBooleanParam("isCasting",false);
            }
            if (intent.hasParameter("Streaming")) {
                mStreaming = intent.getStringParam("Streaming");
            }
        }

        initView();
    }

    private void initView() {
        onlineDeviceListContainer = (ListContainer) findComponentById(ResourceTable.Id_online_device_list);
        layout_refresh = (DirectionalLayout) findComponentById(ResourceTable.Id_layout_refresh);
        layout_cancel = (DirectionalLayout) findComponentById(ResourceTable.Id_layout_cancel);
        deviceListAdapter = new OnlineDeviceListAdapter(this, this::startController);

        title = (Text) findComponentById(ResourceTable.Id_title);
        title.setText("Online Device");

        requestPermissions(SystemPermission.DISTRIBUTED_DATASYNC);
        EventBus.getDefault().register(this);
        DeviceManager.registerDeviceStateCallback(iDeviceStateCallback);

        layout_refresh.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                currentDeviceInfo = null;
                fetchOnlineDevice();
            }
        });

        layout_cancel.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                mainAbility.setSwipeToDismiss(true);

                Intent intent = new Intent();

                intent.setParam(Constants.DEVICE_ID_KEY, "");
                intent.setParam("OnlineVideoList", onlineSongsList);
                intent.setParam("SelectedSongDetails", playItemModel);
                intent.setParam("CurrentPosition", currentPosition);
                intent.setParam("isCasting", false);

                preferenceUtil.putCastingStatus(false, "");

                if (mStreaming.contains(ONLINE_AUDIO)) {
                    intent.setParam("Streaming", ONLINE_AUDIO);
                } else {
                    intent.setParam("Streaming", ONLINE_VIDEO);
                }
                setResult(intent);
                terminate();
            }
        });
    }

    @Override
    protected void onActive() {
        super.onActive();
        if (deviceListAdapter.getCount() == 0) {
            fetchOnlineDevice();
        }
    }

    private void requestPermissions(String... permissions) {
        for (String permission : permissions) {
            if (verifyCallingOrSelfPermission(permission) != IBundleManager.PERMISSION_GRANTED) {
                requestPermissionsFromUser(new String[] {permission}, MainAbility.REQUEST_CODE);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGrantedPermissionModel(GrantPermissionModel grantPermissionModel) {
        if (grantPermissionModel.permission.equals(SystemPermission.DISTRIBUTED_DATASYNC)
            && grantPermissionModel.isGranted) {
            fetchOnlineDevice();
        }
    }

    private void fetchOnlineDevice() {
        List<DeviceInfo> deviceInfoList = DeviceManager.getDeviceList(DeviceInfo.FLAG_GET_ONLINE_DEVICE);
        if (deviceInfoList.isEmpty()) {
            CommonFunctions.getInstance().showToast(this,"No device found");
            LogUtil.debug(LogUtil.TAG_LOG,"No device found");
        } else {
            deviceInfoList.forEach(deviceInformation -> {
                LogUtil.info(TAG_LOG, "Found devices - Name " + deviceInformation.getDeviceName());
                if (deviceInformation.getDeviceType() == DeviceInfo.DeviceType.SMART_PHONE &&
                        (currentDeviceInfo == null || !deviceInformation.getDeviceId().equals(currentDeviceInfo.getDeviceId()))) {
                    currentDeviceInfo = deviceInformation;
                    LogUtil.info(TAG_LOG, "Found device - Type " + deviceInformation.getDeviceType());
                    LogUtil.info(TAG_LOG, "Found device - State " + deviceInformation.getDeviceState());
                    LogUtil.info(TAG_LOG, "Found device - Id " + deviceInformation.getDeviceId());
                }
            });

            deviceListAdapter.updateDeviceItems(deviceInfoList);
            onlineDeviceListContainer.setItemProvider(deviceListAdapter);
        }
    }

    private void startController(DeviceInfo deviceInfo) {
        mainAbility.setSwipeToDismiss(true);

        Intent intent = new Intent();

        intent.setParam(Constants.DEVICE_ID_KEY, deviceInfo.getDeviceId());
        intent.setParam("OnlineVideoList", onlineSongsList);
        intent.setParam("SelectedSongDetails", playItemModel);
        intent.setParam("CurrentPosition", currentPosition);
        intent.setParam("isCasting", true);

        preferenceUtil.putCastingStatus(true, deviceInfo.getDeviceId());

        if (mStreaming.contains(ONLINE_AUDIO)) {
            intent.setParam("Streaming", ONLINE_AUDIO);
        } else {
            intent.setParam("Streaming", ONLINE_VIDEO);
        }

        setResult(intent);
        terminate();
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        DeviceManager.unregisterDeviceStateCallback(iDeviceStateCallback);
    }
}
