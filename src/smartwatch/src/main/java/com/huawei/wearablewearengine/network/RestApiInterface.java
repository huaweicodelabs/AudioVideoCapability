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

package com.huawei.wearablewearengine.network;

import com.huawei.wearablewearengine.model.PlayItemModel;
import retrofit2.Call;
import retrofit2.http.GET;

import java.util.ArrayList;

/**
 * RestApiInterface interface.
 */
public interface RestApiInterface {
    @GET("v3/ebca9282-2486-45c7-bdb8-08df15a979e5")
    Call<ArrayList<PlayItemModel>> getOnLineSongsLists();

    @GET("v3/24789f2d-fefc-4675-9486-d99d58e12c8f")
    Call<ArrayList<PlayItemModel>> getOnLineVideosLists();
}
