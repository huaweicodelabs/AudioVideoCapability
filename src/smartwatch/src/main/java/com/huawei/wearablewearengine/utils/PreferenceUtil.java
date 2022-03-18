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

import ohos.aafwk.ability.Ability;
import ohos.data.DatabaseHelper;
import ohos.data.preferences.Preferences;

/**
 * Local Shared Preference Class.
 */
public class PreferenceUtil {
    private static volatile PreferenceUtil sInstance = null;
    public static final String APP_PREFERENCE_NAME = "OTTMusicStationHapPreference";
    public static final String KEY_PLAYS_STATUS_PARAM = "CurrentStatus";
    public static final String KEY_MODE_PARAM = "CurrentMode";
    public static final String KEY_SONG_NAME_PARAM = "CurrentSongName";
    public static final String KEY_SONG_THUMBNAIL_PARAM = "CurrentSongLogo";
    public static final String KEY_SONG_URI_PARAM = "CurrentSongURI";
    public static final String KEY_SONG_SINGER_NAME_PARAM = "CurrentSongSingerName";
    public static final String KEY_CASTING_PARAM = "CurrentSongCasting";
    public static final String KEY_DEVICE_ID_PARAM = "CurrentDeviceId";
    public static final String KEY_VOLUME_CONTROL_PARAM = "VolumeControlStatus";

    private static Ability contextAbility = null;
    /**
     * Creating instance.
     * @return instance value
     */
    public static PreferenceUtil getInstance() {
        if (sInstance == null) {
            synchronized (PreferenceUtil.class) {
                if (sInstance == null) {
                    sInstance = new PreferenceUtil();
                }
            }
        }
        return sInstance;
    }

    /**
     * Registering databaseHelper.
     * @return Preferences object
     */
    public Preferences getApplicationPref() {
        DatabaseHelper databaseHelper = new DatabaseHelper(contextAbility);
        return databaseHelper.getPreferences(APP_PREFERENCE_NAME);
    }

    /**
     * Registering ability.
     * @param ctxAbility as Ability
     */
    public void register(Ability ctxAbility) {
        contextAbility = ctxAbility;
    }

    /**
     * Storing Current Radio Song.
     * @param status as String
     * @param songName as String
     * @param logourl as String
     * @param uri as String
     * @param singerName as String
     * @param mode as String
     * @param isCasting as boolean
     */
    public void putCurrentSongDetails(String status, String songName, String logourl, String uri, String singerName, String mode, boolean isCasting) {
        Preferences modifier = getApplicationPref();
        modifier.putString(KEY_PLAYS_STATUS_PARAM, status);
        modifier.putString(KEY_SONG_NAME_PARAM, songName);
        modifier.putString(KEY_SONG_THUMBNAIL_PARAM, logourl);
        modifier.putString(KEY_SONG_URI_PARAM, uri);
        modifier.putString(KEY_SONG_SINGER_NAME_PARAM, singerName);
        modifier.putString(KEY_MODE_PARAM, mode);
        modifier.putBoolean(KEY_CASTING_PARAM, isCasting);
        modifier.flush();
    }

    /**
     * Storing Cast Status.
     * @param isCasting as boolean
     * @param deviceId as String
     */
    public void putCastingStatus(boolean isCasting, String deviceId) {
        Preferences modifier = getApplicationPref();
        modifier.putBoolean(KEY_CASTING_PARAM, isCasting);
        modifier.putString(KEY_DEVICE_ID_PARAM, deviceId);
        modifier.flush();
    }

    /**
     * Storing Current Volume Status.
     * @param isMuted as boolean
     */
    public void putVolumeControlStatus(boolean isMuted) {
        Preferences modifier = getApplicationPref();
        modifier.putBoolean(KEY_VOLUME_CONTROL_PARAM, isMuted);
        modifier.flush();
    }

    /**
     * Retrieving Current Playing Status.
     * @return status value
     */
    public String getCurrentPlayingStatus() {
        return getApplicationPref().getString(KEY_PLAYS_STATUS_PARAM, "");
    }

    /**
     * Retrieving Current Song Name.
     * @return song name
     */
    public String getCurrentSongName() {
        return getApplicationPref().getString(KEY_SONG_NAME_PARAM, "");
    }

    /**
     * Retrieving Current Song Logo.
     * @return thumbnail
     */
    public String getCurrentSongLogo() {
        return getApplicationPref().getString(KEY_SONG_THUMBNAIL_PARAM, "");
    }

    /**
     * Retrieving Current Song URI.
     * @return song uri
     */
    public String getCurrentSongURI() {
        return getApplicationPref().getString(KEY_SONG_URI_PARAM, "");
    }

    /**
     * Retrieving Current Song Singer Name.
     * @return singer name
     */
    public String getCurrentSongSingerName() {
        return getApplicationPref().getString(KEY_SONG_SINGER_NAME_PARAM, "");
    }

    /**
     * Retrieving Current Song Mode.
     * @return song mode
     */
    public String getCurrentSongMode() {
        return getApplicationPref().getString(KEY_MODE_PARAM, "");
    }

    /**
     * Retrieving Current Song Mode.
     * @return isCasting as boolean value
     */
    public boolean getIsCasting() {
        return getApplicationPref().getBoolean(KEY_CASTING_PARAM, false);
    }

    /**
     * Retrieving Current Song Mode.
     * @return volume muted or unmuted
     */
    public boolean getIsVolumeMuted() {
        return getApplicationPref().getBoolean(KEY_VOLUME_CONTROL_PARAM, false);
    }

    /**
     * Retrieving Device Id.
     * @return device id
     */
    public String getPrefDeviceID() {
        return getApplicationPref().getString(KEY_DEVICE_ID_PARAM, "");
    }
}