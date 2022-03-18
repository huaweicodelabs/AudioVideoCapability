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
import com.huawei.wearablewearengine.model.PlayItemModel;
import com.huawei.wearablewearengine.utils.Constants;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Image;
import ohos.agp.components.Text;
import ohos.app.Context;

import java.util.List;

public class SongsListAdapter extends BaseItemProvider {
    // ListContainer data set
    private List<PlayItemModel> songList;
    private Context slice;

    public SongsListAdapter(List<PlayItemModel> list, Context slice) {
        this.songList = list;
        this.slice = slice;
    }

    // Used to save the child components in ListContainer.
    public class SettingHolder {
        Image songIma;
        Text songText;

        SettingHolder(Component component) {
            songIma = (Image) component.findComponentById(ResourceTable.Id_ima_song);
            songText = (Text) component.findComponentById(ResourceTable.Id_text_song);
        }

    }

    @Override
    public int getCount() {
        return songList == null ? 0 : songList.size();
    }

    @Override
    public Object getItem(int position) {
        if (songList != null && position >= 0 && position < songList.size()){
            return songList.get(position);
        }
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
        PlayItemModel song = songList.get(position);
        if (component == null) {
            cpt = LayoutScatter.getInstance(slice).parse(ResourceTable.Layout_layout_item_song, null, false);
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
        
        if (song.getMode().toString().equals(Constants.ONLINE_AUDIO)
                || song.getMode().toString().equals(Constants.ONLINE_VIDEO)) {
            Glide.with(slice)
                    .load(song.getThumbnailUrl())
                    .placeholder(ResourceTable.Media_OttSplash)
                    .error(ResourceTable.Media_OttSplash)
                    .into(holder.songIma);
        } else {
            Glide.with(slice)
                    .load(ResourceTable.Media_OttSplash)
                    .into(holder.songIma);
        }

        return cpt;
    }
}
