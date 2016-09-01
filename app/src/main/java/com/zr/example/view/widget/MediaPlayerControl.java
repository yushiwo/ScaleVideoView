package com.zr.example.view.widget;

/**
 * Created by netease on 16/6/3.
 */
public interface MediaPlayerControl {
    void start();

    void pause();

    int getDuration();

    int getCurrentPosition();

    void seekTo(long pos);

    boolean isPlaying();

    int getBufferPercentage();

    boolean canPause();

    boolean canSeekBackward();

    boolean canSeekForward();

    public void snapShot();

    void setVolume(float volume);
}
