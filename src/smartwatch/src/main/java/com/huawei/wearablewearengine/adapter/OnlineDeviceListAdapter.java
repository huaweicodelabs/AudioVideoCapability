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

package com.huawei.wearablewearengine.adapter;

import com.andexert.library.RippleView;
import com.huawei.wearablewearengine.ResourceTable;
import com.huawei.wearablewearengine.utils.CommonFunctions;
import com.huawei.wearablewearengine.utils.LogUtil;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.Text;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.LayoutScatter;
import ohos.app.Context;
import ohos.distributedschedule.interwork.DeviceInfo;

import java.util.ArrayList;
import java.util.List;

public class OnlineDeviceListAdapter extends BaseItemProvider {

    private final List<DeviceInfo> onlineDeviceList;
    private final Context context;
    private final DeviceItemListener listener;

    public interface DeviceItemListener {
        void onSelectedDeviceItem(DeviceInfo deviceInfo);
    }

    public OnlineDeviceListAdapter(Context context, DeviceItemListener listener) {
        this.onlineDeviceList = new ArrayList<>();
        this.context = context;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return onlineDeviceList.size();
    }

    @Override
    public Object getItem(int position) {
        if (position >= 0 && position < onlineDeviceList.size()) {
            return onlineDeviceList.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Component getComponent(int position, Component convertComponent, ComponentContainer componentContainer) {
        final Component component;
        final ViewHolder viewHolder;
        if (convertComponent == null) {
            component = LayoutScatter.getInstance(context).parse(
                    ResourceTable.Layout_item_online_device,
                    null,
                    false);
            viewHolder = new ViewHolder();
            viewHolder.deviceName = (Text) component.findComponentById(ResourceTable.Id_device_name);
            viewHolder.tv_deviceid = (Text) component.findComponentById(ResourceTable.Id_tv_device_id);
            viewHolder.deviceTypeIv = (Image) component.findComponentById(ResourceTable.Id_device_type_iv);
            viewHolder.ripple_layout = (RippleView) component.findComponentById(ResourceTable.Id_ripple_layout);

            component.setTag(viewHolder);
        } else {
            component = convertComponent;
            viewHolder = (ViewHolder) component.getTag();
        }
        DeviceInfo deviceInfo =  onlineDeviceList.get(position);
        if (deviceInfo != null) {
            viewHolder.deviceName.setText(deviceInfo.getDeviceName());
            viewHolder.ripple_layout.setClickedListener(component1 -> {
                listener.onSelectedDeviceItem(deviceInfo);
            });

            try {
                String deviceId = encryptDeviceId(deviceInfo.getDeviceId(), 4, deviceInfo.getDeviceId().length()-4, '*');
                viewHolder.tv_deviceid.setText(deviceId);
                LogUtil.debug(LogUtil.TAG_LOG,"Encrypted deviceid: "+deviceId);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (deviceInfo.getDeviceType().equals(DeviceInfo.DeviceType.SMART_PHONE)) {
                viewHolder.deviceTypeIv.setPixelMap(ResourceTable.Media_ic_phone);
            } else if (deviceInfo.getDeviceType().equals(DeviceInfo.DeviceType.SMART_CAR)) {
                viewHolder.deviceTypeIv.setPixelMap(ResourceTable.Media_ic_car);
            } else if (deviceInfo.getDeviceType().equals(DeviceInfo.DeviceType.SMART_PAD)) {
                viewHolder.deviceTypeIv.setPixelMap(ResourceTable.Media_ic_tablet);
            } else if (deviceInfo.getDeviceType().equals(DeviceInfo.DeviceType.SMART_TV)) {
                viewHolder.deviceTypeIv.setPixelMap(ResourceTable.Media_ic_tv);
            } else if (deviceInfo.getDeviceType().equals(DeviceInfo.DeviceType.SMART_WATCH)) {
                viewHolder.deviceTypeIv.setPixelMap(ResourceTable.Media_ic_watch);
            }
        }
        return component;
    }

    public void updateDeviceItems(List<DeviceInfo> deviceInfoList) {
        onlineDeviceList.clear();
        onlineDeviceList.addAll(deviceInfoList);
        notifyDataChanged();
    }

    private static class ViewHolder {
        private Text deviceName;
        private Text tv_deviceid;
        private Image deviceTypeIv;
        private RippleView ripple_layout;
    }

    private String encryptDeviceId(String str, int sPOS, int ePOS, char specChar) throws Exception {

        if (str == null || str.equals("")) {
            return "";
        }

        if (sPOS < 0) {
            sPOS = 0;
        }

        if (ePOS > str.length()) {
            ePOS = str.length();
        }

        if (sPOS > ePOS) {
            CommonFunctions.getInstance().showToast(context, "End value cannot be greater than start value");
            return "";
        }

        int maskLength = ePOS - sPOS;

        if (maskLength == 0) {
            return str;
        }

        StringBuilder sbMaskString = new StringBuilder(maskLength);

        if (maskLength >= 5) {
            sbMaskString = sbMaskString.append("****");
        } else {
            for (int i = 0; i < maskLength; i++) {
                sbMaskString.append(specChar);
            }
        }

        return str.substring(0, sPOS)
                + sbMaskString.toString()
                + str.substring(sPOS + maskLength);
    }
}
