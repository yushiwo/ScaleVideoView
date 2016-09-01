package com.zr.example.view.widget;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnBufferingUpdateListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnCompletionListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnErrorListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnInfoListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnPreparedListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnSeekCompleteListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnVideoSizeChangedListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnNetworkErrorListener;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.MediaInfo;

/**
 * Displays a video file. The VideoView class can load images from various
 * sources (such as resources or content providers), takes care of computing its
 * measurement from the video so that it can be used in any layout manager, and
 * provides various display options such as scaling and tinting.
 *
 * VideoView also provide many wrapper methods for
 */
public class VideoView extends SurfaceView implements MediaPlayerControl {
    private static final String TAG = VideoView.class.getName();

    private Uri mUri;
    private long mDuration;
    private String mUserAgent;


    public  boolean isFullScreenBtnPressed = false;
    public boolean isFullScreenBtnPressed() {
        return isFullScreenBtnPressed;
    }

    public void setFullScreenBtnPressed(boolean isFullScreenBtnPressed) {
        this.isFullScreenBtnPressed = isFullScreenBtnPressed;
        mVideoLayout = VIDEO_LAYOUT_STRETCH;  //全屏后重新请求流的时候会重新计算视频的宽高，这里设置全屏撑满整个屏幕
    }

    public boolean isBackBtnPressed() {
        return isBackBtnPressed;
    }

    public void setBackBtnPressed(boolean isBackBtnPressed) {
        this.isBackBtnPressed = isBackBtnPressed;
    }

    /** 是否按下全屏按钮 */
    public  boolean isBackBtnPressed = false;

    /** 是否是播放录像 */
    public  boolean isPlayRecord = false;

    public boolean isPlayRecord() {
        return isPlayRecord;
    }

    public void setPlayRecord(boolean isPlayRecord) {
        this.isPlayRecord = isPlayRecord;
    }


    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;
    private static final int STATE_SUSPEND = 6;
    private static final int STATE_RESUME = 7;
    private static final int STATE_SUSPEND_UNSUPPORTED = 8;

    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;

    private int mVideoLayout = VIDEO_LAYOUT_SCALE;
    public static final int VIDEO_LAYOUT_ORIGIN = 0;
    public static final int VIDEO_LAYOUT_SCALE = 1;
    public static final int VIDEO_LAYOUT_STRETCH = 2;
    public static final int VIDEO_LAYOUT_ZOOM = 3;

    private int mVideoScalingMode = VIDEO_SCALING_MODE_FIT_XY;
    public static final int VIDEO_SCALING_MODE_NONE = 0;
    public static final int VIDEO_SCALING_MODE_FIT  = 1;
    public static final int VIDEO_SCALING_MODE_FILL = 2;
    public static final int VIDEO_SCALING_MODE_FULL = 3;
    public static final int VIDEO_SCALING_MODE_FIT_XY = 4;

    private SurfaceHolder mSurfaceHolder = null;
    private IMediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoSarNum;
    private int mVideoSarDen;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private View mMediaBufferingIndicator;
    private OnCompletionListener mOnCompletionListener;
    private OnPreparedListener mOnPreparedListener;
    private OnErrorListener mOnErrorListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnInfoListener mOnInfoListener;
    private OnNetworkErrorListener mOnNetworkErrorListener;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private int mCurrentBufferPercentage;
    private long mSeekWhenPrepared;
    private boolean mCanPause = true;
    private boolean mCanSeekBack = true;
    private boolean mCanSeekForward = true;
    private boolean mOpenMediaCodec = false;
    private Context mContext;

    /** 0:直播低延时; 1:点播防抖动 */
    private int mBufferStrategy = 0;
    /** 默认直播缓存大小，512K */
    private long mBufferSize =  2 * 1024 * 1024;
    /** 默认帧率 */
    private int mFrames = 15;

    private int mCurrentPositionX;
    private int mCurrentPositionY;
    private boolean mChangeFlag = false;
    private boolean mPositionChangeFlag = false;
    private float mScale;
    private int mWidth;
    private int mHeight;

    public boolean ismZoom() {
        return mZoom;
    }

    public void setmZoom(boolean mZoom) {
        this.mZoom = mZoom;
    }
    /** 缩放标志位 */
    private boolean mZoom = false;

    /** 上一次播放位置 */
    private int lastPosition;
    /** 当前播放位置 */
    private int currentPosition;
    /** 定时器 */
    private Timer mTimer;
    /** 定时器任务 */
    private TimerTask mTimerTask;
    /** 定时器状态：开 */
    private static final int TIMER_STATE_ON = 0;
    /** 定时器状态：关 */
    private static final int TIMER_STATE_OFF = 1;
    /** 默认定时器状态：关 */
    private int mTimerState = TIMER_STATE_OFF;
    /** 定时器运行事件间隔，默认为10秒 */
    private int mTimerPeriod = 10000;



    /**
     * 获取定时器运行时间间隔
     * @return
     */
    public int getmTimerPeriod() {
        return mTimerPeriod;
    }

    /**
     * 设置定时器运行时间间隔
     * @param mTimerPeriod
     */
    public void setmTimerPeriod(int mTimerPeriod) {
        this.mTimerPeriod = mTimerPeriod;
    }

    public VideoView(Context context) {
        super(context);
        initVideoView(context);
    }

    public VideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initVideoView(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(isScaling){
            Log.d("zrr", "onMeasure 111");
            setMeasuredDimension(currentWidth, currentHeight);
        }else {
            Log.d("zrr", "onMeasure 222");
            // 为了解决切换全屏时候，重新计算一遍
            setVideoScalingMode(mVideoScalingMode);
            int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
            int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
            setMeasuredDimension(width, height);
        }

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void setVideoScalingMode(int videoScalingMode) {
        Log.d("zrr", "setVideoScalingMode");
        FrameLayout layout = (FrameLayout) this.getParent();
        int parentWidth = layout.getWidth();
        int parentHeight = layout.getHeight();
        Log.d("zrr", "parentWidth = " + parentWidth);
        Log.d("zrr", "parentHeight = " + parentHeight);

        LayoutParams layPara = getLayoutParams();
        int winWidth  = 0;
        int winHeight = 0;
        Rect rect = new Rect();
        this.getWindowVisibleDisplayFrame(rect);//获取状态栏高度
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay(); //获取屏幕分辨率
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) { //new
            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            winWidth  = metrics.widthPixels;
            winHeight = metrics.heightPixels - rect.top;
        }
        else { //old
            try {
                Method mRawWidth  = Display.class.getMethod("getRawWidth");
                Method mRawHeight = Display.class.getMethod("getRawHeight");
                winWidth  = (Integer) mRawWidth.invoke(display);
                winHeight = (Integer) mRawHeight.invoke(display) - rect.top;
            } catch (NoSuchMethodException e) {
                DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
                winWidth  = dm.widthPixels;
                winHeight = dm.heightPixels - rect.top;
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        if(parentHeight != 0 && parentWidth != 0){
            winWidth = parentWidth;
            winHeight = parentHeight;
        }

        float winRatio = (float) winWidth / winHeight;
        if (mVideoWidth > 0 && mVideoHeight > 0) {
            float aspectRatio = (float) (mVideoWidth) / mVideoHeight;
            if (mVideoSarNum > 0 && mVideoSarDen > 0)
                aspectRatio = aspectRatio * mVideoSarNum / mVideoSarDen;
            mSurfaceHeight = mVideoHeight;
            mSurfaceWidth = mVideoWidth;

            if (VIDEO_SCALING_MODE_NONE == videoScalingMode && mSurfaceWidth < winWidth && mSurfaceHeight < winHeight) {  //按照video的原始大小播放
                layPara.width = (int) (mSurfaceHeight * aspectRatio);
                layPara.height = mSurfaceHeight;
            } else if ( VIDEO_SCALING_MODE_FIT == videoScalingMode) { //拉伸(等比例)
                if (winRatio < aspectRatio) {
                    layPara.width  = winWidth;
                    layPara.height = (int)(winWidth / aspectRatio);
                }
                else {
                    layPara.width  = (int)(aspectRatio * winHeight);
                    layPara.height = winHeight;
                }
            } else if (VIDEO_SCALING_MODE_FILL == videoScalingMode){ //满屏
                layPara.width  = winWidth;
                layPara.height = winHeight;
            } else if (VIDEO_SCALING_MODE_FULL == videoScalingMode) { //全屏
                if (winRatio < aspectRatio) {
                    layPara.width  = (int)(winHeight * aspectRatio);
                    layPara.height = winHeight;
                }
                else {
                    layPara.width  = winWidth;
                    layPara.height = (int)(winWidth / aspectRatio);
                }
            } else if (VIDEO_SCALING_MODE_FIT_XY == videoScalingMode){ //相对于父控件拉伸铺满
                layPara.width  = parentWidth;
                layPara.height = parentHeight;
            } else {
                if (winRatio < aspectRatio) {
                    layPara.width  = (int)(aspectRatio * winHeight);
                    layPara.height = winHeight;
                }
                else {
                    layPara.width  = winWidth;
                    layPara.height = (int)(winWidth / aspectRatio);
                }
            }
            setLayoutParams(layPara);
            getHolder().setFixedSize(mSurfaceWidth, mSurfaceHeight);
        }

        mVideoScalingMode = videoScalingMode;
    }



    private void initVideoView(Context ctx) {
        mContext = ctx;
        mVideoWidth = 0;
        mVideoHeight = 0;
        mVideoSarNum = 0;
        mVideoSarDen = 0;
        getHolder().addCallback(mSHCallback);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;
        if (ctx instanceof Activity)
            ((Activity) ctx).setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    public boolean isValid() {
        return (mSurfaceHolder != null && mSurfaceHolder.getSurface().isValid());
    }

    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    public void setVideoURI(Uri uri) {
        mUri = uri;
        mSeekWhenPrepared = 0;
        openVideo();
        requestLayout();
        invalidate();
    }

    public void setUserAgent(String ua) {
        mUserAgent = ua;
    }

    public void stopPlayback() {
        Log.d("zr", "stopPlayback");
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mUri = null;  //防止从设置界面跳转回来出现停止的视频重新播放的情况
            mCurrentState = STATE_IDLE;
            mTargetState = STATE_IDLE;
        }
    }

    private void openVideo() {
        Log.d("zr", "openVideo");
        if (mUri == null || mSurfaceHolder == null) {
            Log.d("zr", "openVideo111");
            if(mUri == null){
                Log.d("zr", "mUri is null");
            }
            if(mSurfaceHolder == null){
                Log.d("zr", "mSurfaceHolder is null");
            }
            return;
        }


        //如果是跳转过来的，则不做操作
        if(isFullScreenBtnPressed && isPlayRecord == false){
            setVideoScalingMode(mVideoScalingMode);
            requestLayout();
            return;
        }

        if(isBackBtnPressed && isPlayRecord == false){
            setVideoScalingMode(mVideoScalingMode);
            requestLayout();
            return;
        }

        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        mContext.sendBroadcast(i);

        release(false);
        try {
            mDuration = -1;
            mCurrentBufferPercentage = 0;
            IjkMediaPlayer ijkMediaPlayer = null;
            if (mUri != null) {
                ijkMediaPlayer = new IjkMediaPlayer();
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 0);
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "timeout", 30*1000*1000);
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-buffer-size", 512*1024);
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 15);
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", (mOpenMediaCodec == true) ? 1 : 0);

//                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user_agent", mUserAgent);

                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 0);
                ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_frame", 0);
            }
            mMediaPlayer = ijkMediaPlayer;
            mMediaPlayer.setBufferStrategy(mBufferStrategy);
            if(mBufferStrategy == 0){  //直播时，设置buffer大小；点播时使用默认的buffersize，为15M
                mMediaPlayer.setBuffer(mBufferSize, mFrames);   //buffer要在setBufferStrategy设置之后设置才生效
            }
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
            mMediaPlayer.setOnNetworkErrorListener(mNetworkErrorListener);
            if (mUri != null)
                mMediaPlayer.setDataSource(mUri.toString());
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
        } catch (IOException ex) {
            DebugLog.e(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer,
                    IMediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (IllegalArgumentException ex) {
            DebugLog.e(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer,
                    IMediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        }
    }


    public void setMediaBufferingIndicator(View mediaBufferingIndicator) {
        if (mMediaBufferingIndicator != null)
            mMediaBufferingIndicator.setVisibility(View.GONE);
        mMediaBufferingIndicator = mediaBufferingIndicator;
    }


    OnVideoSizeChangedListener mSizeChangedListener = new OnVideoSizeChangedListener() {
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height,
                                       int sarNum, int sarDen) {
            DebugLog.dfmt(TAG, "onVideoSizeChanged: (%dx%d)", width, height);
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            mVideoSarNum = sarNum;
            mVideoSarDen = sarDen;
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                setVideoScalingMode(mVideoScalingMode);
            }
        }
    };

    OnPreparedListener mPreparedListener = new OnPreparedListener() {
        public void onPrepared(IMediaPlayer mp) {
            mCurrentState = STATE_PREPARED;
            mTargetState = STATE_PLAYING;

            if (mOnPreparedListener != null)
                mOnPreparedListener.onPrepared(mMediaPlayer);
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();

            long seekToPosition = mSeekWhenPrepared;

            if (seekToPosition != 0)
                seekTo(seekToPosition);
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                setVideoScalingMode(mVideoScalingMode);
                if (mSurfaceWidth == mVideoWidth
                        && mSurfaceHeight == mVideoHeight) {
                    if (mTargetState == STATE_PLAYING) {
                        start();
                    } else if (!isPlaying()
                            && (seekToPosition != 0 || getCurrentPosition() > 0)) {
                    }
                }
            } else if (mTargetState == STATE_PLAYING) {
                start();
            }
        }
    };

    private OnCompletionListener mCompletionListener = new OnCompletionListener() {
        public void onCompletion(IMediaPlayer mp) {
            DebugLog.d(TAG, "onCompletion");
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;
            if (mOnCompletionListener != null)
                mOnCompletionListener.onCompletion(mMediaPlayer);
        }
    };

    private OnErrorListener mErrorListener = new OnErrorListener() {
        public boolean onError(IMediaPlayer mp, int framework_err, int impl_err) {
            DebugLog.dfmt(TAG, "Error: %d, %d", framework_err, impl_err);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;

            if (mOnErrorListener != null) {
                if (mOnErrorListener.onError(mMediaPlayer, framework_err,
                        impl_err))
                    return true;
            }

            return true;
        }
    };

    private OnBufferingUpdateListener mBufferingUpdateListener = new OnBufferingUpdateListener() {
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {
            mCurrentBufferPercentage = percent;
            if (mOnBufferingUpdateListener != null)
                mOnBufferingUpdateListener.onBufferingUpdate(mp, percent);
        }
    };

    private OnInfoListener mInfoListener = new OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer mp, int what, int extra) {
            DebugLog.dfmt(TAG, "onInfo: (%d, %d)", what, extra);
            if (mOnInfoListener != null) {
                mOnInfoListener.onInfo(mp, what, extra);
            } else if (mMediaPlayer != null) {
                if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    DebugLog.dfmt(TAG, "onInfo: (MEDIA_INFO_BUFFERING_START)");
                    if(mOnBufferingStartListener != null){
                        mOnBufferingStartListener.onBufferingStart();  //增加开始缓冲回调
                    }
                    if (mMediaBufferingIndicator != null)
                        mMediaBufferingIndicator.setVisibility(View.VISIBLE);
                } else if (what == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    DebugLog.dfmt(TAG, "onInfo: (MEDIA_INFO_BUFFERING_END)");
                    if(mOnBufferingEndListener != null){
                        mOnBufferingEndListener.onBufferingEnd();   //增加结束缓冲回调
                    }
                    if (mMediaBufferingIndicator != null)
                        mMediaBufferingIndicator.setVisibility(View.GONE);
                }else if (what == IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {  //绘制首帧提醒
                    DebugLog.dfmt(TAG, "onInfo: (MEDIA_INFO_VIDEO_RENDERING_START)");
                    if(mOnRenderingStartListener != null){
                        mOnRenderingStartListener.onRenderingStart();   //增加结束缓冲回调
                    }
                }else if (what == IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START) {
                    DebugLog.dfmt(TAG, "onInfo: (MEDIA_INFO_AUDIO_RENDERING_START)");
                    if(mOnAudioRenderingStartListener != null){
                        mOnAudioRenderingStartListener.onAudioRenderingStart();   //增加结束缓冲回调
                    }
                }
            }

            return true;
        }
    };

    private OnSeekCompleteListener mSeekCompleteListener = new OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(IMediaPlayer mp) {
            DebugLog.d(TAG, "onSeekComplete");
            if (mOnSeekCompleteListener != null)
                mOnSeekCompleteListener.onSeekComplete(mp);
        }
    };

    private OnNetworkErrorListener mNetworkErrorListener = new OnNetworkErrorListener() {
        @Override
        public boolean onNetworkError(IMediaPlayer mp, int network_err, int extra) {
            DebugLog.dfmt(TAG, "NetworkError: %d, %d", network_err, extra);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;

            if (mOnNetworkErrorListener != null) {
                if (mOnNetworkErrorListener.onNetworkError(mMediaPlayer, network_err,
                        extra))
                    return true;
            }

            release(true);
            return true;
        }
    };

    /**
     * 开始缓冲监听接口
     */
    public interface OnBufferingStartListener{
        void onBufferingStart();
    }

    OnBufferingStartListener mOnBufferingStartListener = null;

    public void setOnBufferingStartListener(OnBufferingStartListener listener){
        mOnBufferingStartListener = listener;
    }

    /**
     * 结束缓冲监听接口
     */
    public interface OnBufferingEndListener{
        void onBufferingEnd();
    }

    OnBufferingEndListener mOnBufferingEndListener = null;

    public void setOnBufferingEndListener(OnBufferingEndListener listener){
        mOnBufferingEndListener = listener;
    }

    /**
     * 暂停按钮
     */
    public interface OnPauseListener{
        void onPause();
    }

    OnPauseListener mOnPauseListener = null;

    public void setOnPauseListener(OnPauseListener listener){
        mOnPauseListener = listener;
    }

    /**
     *  首帧绘制回调
     */
    public interface OnRenderingStartListener{
        void onRenderingStart();
    }

    OnRenderingStartListener mOnRenderingStartListener = null;

    public void setOnRenderingStartListener(OnRenderingStartListener listener){
        mOnRenderingStartListener = listener;
    }

    /**
     *  首帧绘制回调
     */
    public interface OnAudioRenderingStartListener{
        void onAudioRenderingStart();
    }

    OnAudioRenderingStartListener mOnAudioRenderingStartListener = null;

    public void setOnAudioRenderingStartListener(OnAudioRenderingStartListener listener){
        mOnAudioRenderingStartListener = listener;
    }

    /**
     * 继续播放按钮
     */
    public interface OnStartListener{
        void onStart();
    }

    OnStartListener mOnStartListener = null;

    public void setOnStartListener(OnStartListener listener){
        mOnStartListener = listener;
    }


    public void setOnPreparedListener(OnPreparedListener l) {
        mOnPreparedListener = l;
    }

    public void setOnCompletionListener(OnCompletionListener l) {
        mOnCompletionListener = l;
    }

    public void setOnErrorListener(OnErrorListener l) {
        mOnErrorListener = l;
    }

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener l) {
        mOnBufferingUpdateListener = l;
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener l) {
        mOnSeekCompleteListener = l;
    }

    public void setOnInfoListener(OnInfoListener l) {
        mOnInfoListener = l;
    }

    public void setOnNetworkErrorListener(OnNetworkErrorListener l) {
        mOnNetworkErrorListener = l;
    }

    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {
        public void surfaceChanged(SurfaceHolder holder, int format, int w,
                                   int h) {
            Log.d("zr", "surfaceChanged");
            DebugLog.dfmt(TAG, "surfaceChanged: (%d, %d)", w, h);

            if (!mZoom)
            {
                mSurfaceHolder = holder;
                if (mMediaPlayer != null) {
                    mMediaPlayer.setDisplay(mSurfaceHolder);
                }

                mSurfaceWidth = w;
                mSurfaceHeight = h;
                boolean isValidState = (mTargetState == STATE_PLAYING);
                boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
                if (mMediaPlayer != null && isValidState && hasValidSize) {
                    if (mSeekWhenPrepared != 0)
                        seekTo(mSeekWhenPrepared);
                    start();
                }
            }
        }

        public void surfaceCreated(SurfaceHolder holder) {
            Log.d("zr", "surfaceCreated");
            mSurfaceHolder = holder;
            if (mMediaPlayer != null && mCurrentState == STATE_SUSPEND
                    && mTargetState == STATE_RESUME) {
                mMediaPlayer.setDisplay(mSurfaceHolder);
                resume();
            } else {
                openVideo();
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d("zr", "surfaceDestroyed");
            mSurfaceHolder = null;
            if (mCurrentState != STATE_SUSPEND && isFullScreenBtnPressed == false &&isBackBtnPressed == false && isPlayRecord == false){
                release(true);
            }
        }
    };

    private void release(boolean cleartargetstate) {
        Log.d("zr", "release");
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (cleartargetstate)
                mTargetState = STATE_IDLE;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        return false;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void start() {
        if(mOnStartListener != null){
            mOnStartListener.onStart();
        }
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        Log.d("zr", "pause");
        if(mOnPauseListener != null){
            mOnPauseListener.onPause();
        }
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        mTargetState = STATE_PAUSED;
    }

    public void resume() {
        Log.d("zr", "resume");
        if (mSurfaceHolder == null && mCurrentState == STATE_SUSPEND) {
            mTargetState = STATE_RESUME;
        } else if (mCurrentState == STATE_SUSPEND_UNSUPPORTED) {
            openVideo();
        }
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            if (mDuration > 0)
                return (int) mDuration;
            mDuration = mMediaPlayer.getDuration();
            return (int) mDuration;
        }
        mDuration = -1;
        return (int) mDuration;
    }

    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            long position = mMediaPlayer.getCurrentPosition();
            return (int) position;
        }
        return 0;
    }

    @Override
    public void seekTo(long msec) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        if (mMediaPlayer != null)
            return mCurrentBufferPercentage;
        return 0;
    }

    public int getVideoWidth() {
        return mVideoWidth;
    }

    public int getVideoHeight() {
        return mVideoHeight;
    }

    /**
     * （播放前初始化）设置缓存策略,默认为0。0:直播低延时；1:点播抗抖动
     * @param bufferStrategy
     */
    public void setBufferStrategy(int bufferStrategy) {
        mBufferStrategy = bufferStrategy;
    }

    protected boolean isInPlaybackState() {
        return (mMediaPlayer != null && mCurrentState != STATE_ERROR
                && mCurrentState != STATE_IDLE && mCurrentState != STATE_PREPARING);
    }

    public boolean canPause() {
        return mCanPause;
    }

    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    public boolean canSeekForward() {
        return mCanSeekForward;
    }

    public void setOpenMediaCodec(boolean enabled) {
        mOpenMediaCodec = enabled;
    }

    public interface OnUrlInvalidListener{
        void onUrlInvalid();
    }

    OnUrlInvalidListener mOnUrlInvalidListener = null;

    public void setOnUrlInvalidListener(OnUrlInvalidListener listener){
        mOnUrlInvalidListener = listener;
    }


    private Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case 1:
                    if(mOnUrlInvalidListener != null){
                        System.out.println("hear me?");
                        mOnUrlInvalidListener.onUrlInvalid();
                    }
                    break;
                case 2:{
                }
            }

            super.handleMessage(msg);

        }

    };

    @Override
    public void snapShot() {

    }

    /**
     * 获取视频截图
     * @return 返回截图的bitmap
     */
    public Bitmap getSnapShot(){
        MediaInfo mediaInfo = mMediaPlayer.getMediaInfo();
        DebugLog.dfmt(TAG, "CodecInfo = %s", mediaInfo.mVideoDecoder);

        if (mediaInfo.mVideoDecoder.equals("MediaCodec")) {
            DebugLog.d(TAG, "================= hardware decode does not support snapShot ==============");
            return null;
        }
        else {
            Bitmap bitmap = Bitmap.createBitmap(mVideoWidth, mVideoHeight, Bitmap.Config.ARGB_4444);
            DebugLog.d(TAG, "================= snapShot ==============");
            if(mMediaPlayer.getSnapshot(bitmap)){
                return bitmap;
            }
        }
        return null;
    }

    @Override
    public void setVolume(float volume) {
        if(mMediaPlayer != null){
            mMediaPlayer.setVolume(volume);
        }

    }

    private boolean isScaling = false;
    private int currentWidth;
    private int currentHeight;
    public void setScaleStatus(Boolean isScaling, int currentWidth, int currentHeight){
        this.isScaling = isScaling;
        this.currentWidth = currentWidth;
        this.currentHeight = currentHeight;
    }

}


