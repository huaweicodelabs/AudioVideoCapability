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

import com.bumptech.glide.Glide;
import com.huawei.wearablewearengine.ResourceTable;
import com.huawei.wearablewearengine.interfaces.Adapterlistener;
import com.huawei.wearablewearengine.model.PlayItemModel;
import com.huawei.wearablewearengine.utils.LogUtil;

import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.Image;
import ohos.agp.components.Text;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.app.Context;

import java.util.ArrayList;

public class SongsListAdapter extends BaseItemProvider {

    private ArrayList<PlayItemModel> songsList;
    private Context context;
    private Adapterlistener listener;


    public SongsListAdapter(Context context, ArrayList<PlayItemModel> songsList, Adapterlistener listener) {
        this.context = context;
        this.songsList = songsList;
        this.listener = listener;
    }

    // Used to save the child components in ListContainer.
    public class SettingHolder {
        Image songIma;
        Text songText;
        DirectionalLayout item_dl;

        SettingHolder(Component component) {
            songIma = (Image) component.findComponentById(ResourceTable.Id_ima_song);
            songText = (Text) component.findComponentById(ResourceTable.Id_text_song);
            item_dl = (DirectionalLayout) component.findComponentById(ResourceTable.Id_item_dl);
        }

    }

    @Override
    public int getCount() {
        return songsList == null ? 0 : songsList.size();

    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Component getComponent(int position, Component component, ComponentContainer componentContainer) {
        final Component cpt;
        SettingHolder holder;
        PlayItemModel song = songsList.get(position);
        if (component == null) {
            cpt = LayoutScatter.getInstance(context).parse(ResourceTable.Layout_layout_item_song, null, false);
            holder = new SettingHolder(cpt);
            // Bind the obtained child components to the ListContainer instance.
            cpt.setTag(holder);
        } else {
            cpt = component;
            // Fill data for the child components bound to the ListContainer instance obtained from the cache.
            holder = (SettingHolder) cpt.getTag();
        }
        holder.songIma.setPixelMap(song.getImageId());
        holder.songText.setText(song.getTitle());

        holder.item_dl.setClickedListener(new Component.ClickedListener() {
            @Override
            public void onClick(Component component) {
                listener.onItemClick(song, position);
            }
        });

        LogUtil.error(LogUtil.TAG_LOG,"image_url--->"+songsList.get(position).getThumbnailUrl());
        if (!song.getThumbnailUrl().isEmpty()) {
            Glide.with(context)
                    .load(song.getThumbnailUrl())
                    .placeholder(ResourceTable.Media_OttSplash)
                    .error(ResourceTable.Media_OttSplash)
                    .into(holder.songIma);
        } else {
            Glide.with(context)
                    .load(ResourceTable.Media_OttSplash)
                    .into(holder.songIma);
        }

        return cpt;
    }
}
