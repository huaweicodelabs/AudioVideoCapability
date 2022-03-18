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

package com.huawei.wearablewearengine.manager;

import com.huawei.wearablewearengine.utils.LogUtil;
import ohos.agp.graphics.Surface;
import ohos.app.Context;
import ohos.global.resource.RawFileDescriptor;
import ohos.media.common.Source;
import ohos.media.common.sessioncore.AVElement;
import ohos.media.player.Player;

import java.io.IOException;

/**
 * mediaPlayerPlugin
 *
 * @since 2020-09-14
 */
public class MediaPlayerPlugin implements Player.IPlayerCallback {
    private static final String TAG = MediaPlayerPlugin.class.getSimpleName();

    private static final int REWIND_TIME = 5000;

    private Player player;

    private final Context context;

    private Runnable audioRunnable;

    private final MediaPlayerCallback callback;

    public interface MediaPlayerCallback {
        void onPrepared();
        void onPlayBackComplete();
        void onBuffering(int percent);
        void onError(int errorType, int errorCode);
    }

    /**
     * mediaPlayerPlugin
     *
     * @param sliceContext Context
     */
    public MediaPlayerPlugin(Context sliceContext, MediaPlayerCallback callback) {
        context = sliceContext;
        this.callback = callback;
    }

    /**
     * start
     */
    public synchronized void startPlay() {
        if (player == null) {
            return;
        }
        player.play();
        LogUtil.info(TAG, "start play");
    }

    /**
     * start
     * @return boolean value
     */
    public synchronized boolean startPlayer() {
        if (player == null) {
            return false;
        }
        player.play();
        LogUtil.info(TAG, "start play");
        return true;
    }

    /**
     * pause
     */
    public synchronized void pausePlay() {
        if (player == null) {
            return;
        }
        player.pause();
        LogUtil.info(TAG, "pause play");
    }

    /**
     * pause
     */
    public synchronized void stopPlay() {
        if (player == null) {
            return;
        }
        player.stop();
        LogUtil.info(TAG, "stop play");
    }

    /**
     * Set source,prepare,start
     *
     * @param avElement AVElement
     * @param surface Surface
     */
    public synchronized void startPlayVideo(AVElement avElement, Surface surface) {
        if (player != null) {
            player.stop();
            player.release();
        }

        if (audioRunnable != null) {
            ThreadPoolManager.getInstance().cancel(audioRunnable);
        }

        player = new Player(context);
        player.setPlayerCallback(this);
        audioRunnable = () -> playVideo(avElement, surface);
        ThreadPoolManager.getInstance().execute(audioRunnable);
    }

    /**
     * Set source,prepare,start
     *
     * @param url String
     * @param surface Surface
     */
    public synchronized void startPlayVideo(String url, Surface surface) {
        if (player != null) {
            player.stop();
            player.release();
        }

        if (audioRunnable != null) {
            ThreadPoolManager.getInstance().cancel(audioRunnable);
        }

        player = new Player(context);
        player.setPlayerCallback(this);
        audioRunnable = () -> playVideo(url, surface);
        ThreadPoolManager.getInstance().execute(audioRunnable);
    }

    /**
     * Set source,prepare,start
     * @param avElement AVElement
     *
     */
    public synchronized void startPlay(AVElement avElement) {
        if (player != null) {
            player.stop();
            player.release();
        }

        if (audioRunnable != null) {
            ThreadPoolManager.getInstance().cancel(audioRunnable);
        }

        player = new Player(context);
        player.setPlayerCallback(this);
        audioRunnable = () -> play(avElement);
        ThreadPoolManager.getInstance().execute(audioRunnable);
    }

    /**
     * Set source,prepare,start
     * @param url String
     * @param surface for playing video content
     *
     */
    public synchronized void startPlay(String url, Surface surface) {
        if (player != null) {
            player.stop();
            player.release();
        }

        if (audioRunnable != null) {
            ThreadPoolManager.getInstance().cancel(audioRunnable);
        }

        player = new Player(context);
        player.setPlayerCallback(this);
        audioRunnable = () -> playLocalVideo(url, surface);
        ThreadPoolManager.getInstance().execute(audioRunnable);
    }

    /**
     * Set source,prepare,start
     *
     * @param url String
     *
     */
    public synchronized void startPlay(String url) {
        if (player != null) {
            player.stop();
            player.release();
        }

        if (audioRunnable != null) {
            ThreadPoolManager.getInstance().cancel(audioRunnable);
        }

        player = new Player(context);
        player.setPlayerCallback(this);
        audioRunnable = () -> playLocalVideo(url, null);
        ThreadPoolManager.getInstance().execute(audioRunnable);
    }

    /**
     *  check is playing
     *  @return boolean value
     */
    public synchronized boolean isPlaying() {
        if (player == null) {
            return false;
        }
        return player.isNowPlaying();
    }

    /**
     *  check is playing
     *  @return boolean value
     */
    public synchronized boolean isLooping() {
        if (player == null) {
            return false;
        }
        return player.isSingleLooping();
    }

    private void playVideo(AVElement avElement, Surface surface) {
        Source source = new Source(avElement.getAVDescription().getMediaUri().toString());
        player.setSource(source);
        player.setVideoSurface(surface);
        LogUtil.info(TAG, source.getUri());

        player.prepare();
        if (!player.prepare()) {
            if (this.callback != null) {
                this.callback.onPrepared();
            }
        }
    }

    public void playLocalVideo(String url, Surface surface) {
        RawFileDescriptor rawFileDescriptor = null;
        try {
            rawFileDescriptor = context.getResourceManager().getRawFileEntry(url).openRawFileDescriptor();
            Source source = new Source(rawFileDescriptor.getFileDescriptor(), rawFileDescriptor.getStartPosition(), rawFileDescriptor.getFileSize());
            player.setSource(source);
            if (surface != null) {
                player.setVideoSurface(surface);
            }
            LogUtil.info(TAG, source.getUri());

            player.prepare();
            if (!player.prepare()) {
                if (this.callback != null) {
                    this.callback.onPrepared();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playVideo(String url, Surface surface) {
        Source source = new Source(url);
        player.setSource(source);
        player.setVideoSurface(surface);
        LogUtil.info(TAG, source.getUri());

        player.prepare();
        if (!player.prepare()) {
            if (this.callback != null) {
                this.callback.onPrepared();
            }
        }
    }

    private void play(String url) {
        Source source = new Source(url);
        player.setSource(source);
        LogUtil.info(TAG, source.getUri());

        player.prepare();
        if (!player.prepare()) {
            if (this.callback != null) {
                this.callback.onPrepared();
            }
        }
    }

    private void play(AVElement avElement) {
        Source source = new Source(avElement.getAVDescription().getMediaUri().toString());
        player.setSource(source);
        LogUtil.info(TAG, source.getUri());

        player.prepare();
        if (!player.prepare()) {
            if (this.callback != null) {
                this.callback.onPrepared();
            }
        }
    }

    /**
     * looping
     * @param flag value
     */
    public void looping(boolean flag) {
        if (player == null) {
            return;
        }
        player.enableSingleLooping(flag);
        LogUtil.info(TAG, "looping_enabled: " + player.isSingleLooping());
    }

    /**
     * seek
     * @param seconds value for rewind
     */
    public void goToTime(int seconds) {
        if (player == null) {
            return;
        }
        player.rewindTo(seconds);
        LogUtil.info(TAG, "seek" + player.getCurrentTime());
    }

    public void forwardSong(int seekForwardTime) {
        if (player != null) {
            int currentPosition = player.getCurrentTime();
            if (currentPosition + seekForwardTime <= player.getDuration()) {
                player.rewindTo(currentPosition + seekForwardTime);
            } else {
                player.rewindTo(player.getDuration());
            }
        }
    }

    public void rewindSong(int seekBackwardTime) {
        if (player != null) {
            int currentPosition = player.getCurrentTime();
            if (currentPosition - seekBackwardTime >= 0) {
                player.rewindTo(currentPosition - seekBackwardTime);
            } else {
                player.rewindTo(0);
            }
        }
    }

    /**
     * seek
     */
    public void seek() {
        if (player == null) {
            return;
        }
        player.rewindTo(player.getCurrentTime() + REWIND_TIME);
        LogUtil.info(TAG, "seek" + player.getCurrentTime());
    }

    /**
     * back
     */
    public void back() {
        if (player == null) {
            return;
        }
        player.rewindTo(player.getCurrentTime() - REWIND_TIME);
        LogUtil.info(TAG, "seek" + player.getCurrentTime());
    }

    /**
     * stop player
     */
    public void stop() {
        if (player != null) {
            player.stop();
        }
    }

    /**
     * release player
     */
    public void release() {
        if (player != null) {
            player.stop();
            player.release();
        }
    }

    /**
     * Get current play position
     *
     * @return play position
     */
    public int getCurrentTime() {
        if (player == null) {
            return  0;
        }
        return player.getCurrentTime();
    }

    public int getDuration() {
        if (player == null) {
            return  0;
        }
        return player.getDuration();
    }

    @Override
    public void onPrepared() {
        LogUtil.info(TAG, "onPrepared");
        player.play();
    }

    @Override
    public void onMessage(int type, int extra) {
        LogUtil.info(TAG, "onMessage" + type);
    }

    @Override
    public void onError(int errorType, int errorCode) {
        if (player != null) {
            player.stop();
            player.release();
        }
        if (this.callback != null) {
            this.callback.onError(errorType, errorCode);
        }
    }

    @Override
    public void onResolutionChanged(int width, int height) {
        LogUtil.info(TAG, "onResolutionChanged" + width);
    }

    @Override
    public void onPlayBackComplete() {
        LogUtil.info(TAG, "onPlayBackComplete");
        if (this.callback != null) {
            this.callback.onPlayBackComplete();
        }
    }

    @Override
    public void onRewindToComplete() {
        LogUtil.info(TAG, "onRewindToComplete");
    }

    @Override
    public void onBufferingChange(int percent) {
        LogUtil.info(TAG, "onBufferingChange" + percent);
        if (this.callback != null) {
            this.callback.onBuffering(percent);
        }
    }

    @Override
    public void onNewTimedMetaData(Player.MediaTimedMetaData mediaTimedMetaData) {
        LogUtil.info(TAG, "onNewTimedMetaData" + mediaTimedMetaData.toString());
    }

    @Override
    public void onMediaTimeIncontinuity(Player.MediaTimeInfo mediaTimeInfo) {
        LogUtil.info(TAG, "onNewTimedMetaData" + mediaTimeInfo.toString());
    }
}
