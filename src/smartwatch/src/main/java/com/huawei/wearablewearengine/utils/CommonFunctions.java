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

import com.huawei.wearablewearengine.ResourceTable;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.agp.utils.LayoutAlignment;
import ohos.agp.window.dialog.ToastDialog;
import ohos.agp.window.service.WindowManager;
import ohos.app.Context;
import ohos.wifi.WifiDevice;

import static com.huawei.wearablewearengine.utils.Constants.TOAST_DURATION_MILLISECOND;
import static ohos.data.search.schema.PhotoItem.TAG;

public class CommonFunctions {

    private static volatile CommonFunctions sInstance = null;

    /**
     * Initialize Instance.
     * @return return Instance
     */
    public static CommonFunctions getInstance() {
        if (sInstance == null) {
            synchronized (CommonFunctions.class) {
                if (sInstance == null) {
                    sInstance = new CommonFunctions();
                }
            }
        }
        return sInstance;
    }

    /**
     * Wifi connection check.
     * @param context reference value
     * @return return boolean value whether internet connected true, else false.
     */
    public boolean queryNetworkStatus(Context context){
        WifiDevice mWifiDevice = WifiDevice.getInstance(context);
        boolean isConnected = mWifiDevice.isConnected();
        boolean isWifiActive = mWifiDevice.isWifiActive();
        LogUtil.warn(TAG, "===isConnected===="+isConnected);
        LogUtil.warn(TAG, "===isWifiActive===="+isWifiActive);
        if (isConnected) {
            return true;
        }else{
            return false;
        }
    }

    public void showToast(Context context, String msg) {
        DirectionalLayout layout = (DirectionalLayout) LayoutScatter.getInstance(context)
                .parse(ResourceTable.Layout_layout_toast, null, false);
        Text text = (Text) layout.findComponentById(ResourceTable.Id_msg_toast);
        text.setText(msg);
        new ToastDialog(context)
                .setComponent(layout)
                .setAlignment(LayoutAlignment.CENTER)
                .setDuration(TOAST_DURATION_MILLISECOND)
                .setSize(DirectionalLayout.LayoutConfig.MATCH_CONTENT, DirectionalLayout.LayoutConfig.MATCH_CONTENT)
                .show();
    }

    public void setMarkLockAsScreenOn() {
        WindowManager wm = WindowManager.getInstance();
        wm.getTopWindow().get().addFlags(WindowManager.LayoutConfig.MARK_SCREEN_ON_ALWAYS);
    }
}
