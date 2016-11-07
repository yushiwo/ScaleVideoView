package com.zr.ijk.view;



import tv.danmaku.ijk.media.player.IMediaPlayer;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import com.zr.ijk.view.widget.VideoView;


public class QGVideoView extends FrameLayout implements IMediaPlayer.OnPreparedListener,IMediaPlayer.OnCompletionListener,
        VideoView.OnBufferingStartListener, VideoView.OnBufferingEndListener, IMediaPlayer.OnErrorListener, VideoView.OnStartListener,
        VideoView.OnPauseListener, IMediaPlayer.OnNetworkErrorListener, VideoView.OnRenderingStartListener, VideoView.OnAudioRenderingStartListener{
    private VideoView mVideoView;

    /**----------- 控件的尺寸数值 -----------**/
    private int originalWidth;   //最初的宽度
    private int originalHeight;   //最初的高度
    private int originalLeft;
    private int originalTop;
    private int originalRight;
    private int originalBottom;

    /**---------- 内部videiview的位置数值 ---------*/
    private int currentLeft;
    private int currentTop;
    private int currentRight;
    private int currentBottom;
    private int currentWidth;
    private int currentHeight;

    /**--------------- 常量设定 ------------------*/
    public static final int VIDEO_MODE_LIVE = 0;  //直播低延时
    public static final int VIDEO_MODE_RECORD = 1; //点播抗抖动

    public static final String VIDEO_STATUS_PREPARE = "prepare"; //播放器准备就绪
    public static final String VIDEO_STATUS_STOP = "stop"; //停止播放
    public static final String VIDEO_STATUS_RENDERING_START = "renderingstart"; //播放器渲染首帧
    public static final String VIDEO_AUDIO_STATUS_RENDERING_START = "audiorenderingstart"; //播放器渲染首帧
    public static final String VIDEO_STATUS_COMPLETE = "complete"; //播放完成
    public static final String VIDEO_STATUS_BUFFERING_START = "bufferstart"; //开始缓冲
    public static final String VIDEO_STATUS_BUFFERING_END = "bufferend"; //结束缓冲
    public static final String VIDEO_STATUS_FAILED = "failed"; //播放出错
    public static final String VIDEO_STATUS_PAUSE = "pause"; //暂停
    public static final String VIDEO_STATUS_START = "start"; //取消暂停


    private int pointerCount = 0; //手指的个数
    private float oldDist = 0;
    private int sensitivity = 2;  //移动图像时的灵敏度，值越大越不灵敏
    private Point prePoint = new Point(0,0);

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    private double scale = 1;
    private double CHANGE_RANGE = 0.1;  //缩放的灵敏度，越大越灵敏（放大或者缩小上一次的CHANGE_RANGE倍）
    private final int FLIP_SENSITIVITY = 20;  //滑动动画的灵敏度 越大越灵敏
    private VelocityTracker mVelocityTracker = null;
    private int FLIP_ANIM_DURATION = 500;    //滑动动画的持续时间
    private ValueAnimator xFlipAnim;
    private ValueAnimator yFlipAnim;
    private boolean shouldMoveVideoView = true;       //防止缩放的时候画面跳动（跳动的原因：最后一根手指离开时触发了滑动事件）
    private float maxScale = 5;      //最大放大倍数

    private boolean canScale = false;  //是否可缩放标志位，默认为false

    /** 当前View大小是否变化过的标志位 */
    private int onSizeChangedNewWidth = -2;
    private int onSizeChangedNewHeight = -2;

    /**
     * 判断是否支持手势缩放
     * @return
     */
    public boolean isCanScale() {
        return canScale;
    }

    /**
     * 设置是否支持手势缩放
     * @param canScale
     */
    public void setCanScale(boolean canScale) {
        this.canScale = canScale;
    }

    public void setFullScreenBtnPressed(boolean b){
        mVideoView.setFullScreenBtnPressed(b);
    }

    public void setBackBtnPressed(boolean b){
        mVideoView.setBackBtnPressed(b);
    }

    public void setPlayRecord(boolean b){
        mVideoView.setPlayRecord(b);
    }

    public QGVideoView(Context context) {
        super(context);
        init(context);

    }

    public QGVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public QGVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context){
        mVideoView = new VideoView(context);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        this.addView(mVideoView,lp);
        this.setClickable(true);

        mVideoView.setOnPreparedListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setOnBufferingEndListener(this);
        mVideoView.setOnBufferingStartListener(this);
        mVideoView.setOnErrorListener(this);
        mVideoView.setOnStartListener(this);
        mVideoView.setOnPauseListener(this);
        mVideoView.setOnNetworkErrorListener(this);
        mVideoView.setOnRenderingStartListener(this);
        mVideoView.setOnAudioRenderingStartListener(this);
    }

    //尺寸改变的时候更新边界值
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d("zrrr", "onSizeChanged w = " + w + "; oldw = " + oldw);
        super.onSizeChanged(w, h, oldw, oldh);
        onSizeChangedNewWidth = w;
        onSizeChangedNewHeight = h;
        initSizeValues();
    }

    /**
     * 开始播放视频
     * @param url
     */
    public void startSession(String url){
        mVideoView.setVideoPath(url);
        mVideoView.requestFocus();
        mVideoView.start();
    }

    /**
     * 结束视频播放
     */
    public void stopSession(){
        mVideoView.stopPlayback();
        if(this.mOnRealTimeVideoChangeListener!=null){
            this.mOnRealTimeVideoChangeListener.onChange(VIDEO_STATUS_STOP);
        }
    }

    /**
     * 设置播放器缓冲策略。 0:直播低延时； 1:点播抗抖动
     * @param bufferStrategy 缓冲策略
     */
    public void setBufferStrategy(int bufferStrategy){
        mVideoView.setBufferStrategy(bufferStrategy);
    }

    /**
     * seek
     * @param msec
     */
    public void seekTo(long msec){
        mVideoView.seekTo(msec);
    }

    /**
     * 获取视频当前播放的进度
     * @return
     */
    public int getCurrentPosition(){
        return mVideoView.getCurrentPosition();
    }

    /**
     * 是否在播放
     * @return
     */
    public boolean isPlaying(){
        return mVideoView.isPlaying();
    }

    /**
     * 暂停
     */
    public void pause(){
        mVideoView.pause();
    }

    /**
     * 继续播
     */
    public void start(){
        mVideoView.start();
    }

    /**
     * 设置视频播放buffering的view
     * @param mediaBufferingIndicator
     */
    public void setMediaBufferingIndicator(View mediaBufferingIndicator){
        mVideoView.setMediaBufferingIndicator(mediaBufferingIndicator);
    }

    /**
     * 设置音量
     * @param valume  音量大小,范围(0.0 ~ 1.0)
     */
    public void setVolume(float valume){
        mVideoView.setVolume(valume);
    }

    /**
     * 获取当前播放视频截图
     * @return 截图的bitmap，截图失败返回null
     */
    public Bitmap getSnapShot(){
        return mVideoView.getSnapShot();
    }

    /**
     * 重置，回到原始的大小
     */
    public void resetScale(){
        scale = 1;
        int x = this.getWidth()/2;
        int y = this.getHeight()/2;
        changeSize(x, y);
    }

    private void initSizeValues(){

        Log.d("zrrr", "initSizeValues");
        //修改获取的初始位置的对象
        ViewTreeObserver vto = mVideoView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.d("zrrr", "onGlobalLayout");
                currentLeft = mVideoView.getLeft();
                currentTop = mVideoView.getTop();
                currentRight = mVideoView.getRight();
                currentBottom = mVideoView.getBottom();
                currentHeight = mVideoView.getHeight();
                currentWidth = mVideoView.getWidth();

                if(originalWidth != onSizeChangedNewWidth && originalHeight != onSizeChangedNewHeight){
                    originalLeft  = mVideoView.getLeft();
                    originalTop  = mVideoView.getTop();
                    originalRight  = mVideoView.getRight();
                    originalBottom  = mVideoView.getBottom();
                    originalHeight  = mVideoView.getHeight();
                    originalWidth  = mVideoView.getWidth();
                }
            }
        });
    }

    @SuppressLint("NewApi")
    public boolean onTouchEvent(MotionEvent me){
        if(!canScale){
            return super.onTouchEvent(me);
        }
        switch(me.getAction()&MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_DOWN :
                Log.d("zrr", "ACTION_DOWN");
                pointerCount = 1;
                if(mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                }
                else {
                    mVelocityTracker.clear();
                }
                mVelocityTracker.addMovement(me);
                prePoint.x = (int) me.getX();
                prePoint.y = (int) me.getY();

                mFirstPointerPoint.x = prePoint.x;
                mFirstPointerPoint.y = prePoint.y;

                shouldMoveVideoView = true;
                break;
            case MotionEvent.ACTION_MOVE :
                Log.d("zrr", "ACTION_MOVE");
                mVelocityTracker.addMovement(me);
                int xOffset = getXOffset(me.getX());
                int yOffset = getYOffset(me.getY());
                if(pointerCount == 1){
                    if(shouldMoveVideoView){
                        moveVedioView(xOffset,yOffset);
                    }
                }else{  //多个手指的时候进行缩放
                    float newDist = (float)Math.sqrt((me.getX(0) - me.getX(1))*(me.getX(0) - me.getX(1)) + (me.getY(0) - me.getY(1))*(me.getY(0) - me.getY(1)));
                    float x = (me.getX(0)+me.getX(1))/2;
                    float y = (me.getY(0)+me.getY(1))/2;
                    float dist = newDist-oldDist;
                    if(dist > 2)
                        zoomOut(x,y);
                    else if(dist < -2)
                        zoomIn(x,y);
                    oldDist = newDist;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN :
                Log.d("zrr", "ACTION_POINTER_DOWN");
                mVideoView.setmZoom(true);
                pointerCount += 1;
                if(pointerCount == 2){
                    Log.d("zrr", "ACTION_POINTER_DOWNc 111");
                    oldDist = (float)Math.sqrt((me.getX(0) - me.getX(1)) * (me.getX(0) - me.getX(1)) + (me.getY(0) - me.getY(1)) * (me.getY(0) - me.getY(1)));
                    shouldMoveVideoView = false;       //缩放时不能移动
                }
                break;
            case MotionEvent.ACTION_POINTER_UP :
                Log.d("zrr", "ACTION_POINTER_UP");
                pointerCount -= 1;
                mVideoView.setmZoom(false);
                break;
            case MotionEvent.ACTION_UP :
                Log.d("zrr", "ACTION_UP");
                mVelocityTracker.computeCurrentVelocity(FLIP_SENSITIVITY);
                if(shouldMoveVideoView) {
                    startFlipAnimation((int) mVelocityTracker.getXVelocity(), (int) mVelocityTracker.getYVelocity());
                }
                if(shouldTriggerSingleTap(me) && mOnSingleTapListener != null) {
                    mOnSingleTapListener.onSingleTap();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                Log.d("zrr", "ACTION_CANCEL");
                mVideoView.setmZoom(false);
                if(mVelocityTracker != null){
                    mVelocityTracker.clear();
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
        }
        return true;
    }

    private int getXOffset(float curX){
        int x = (int)curX;
        int offset = x - prePoint.x;
        prePoint.x = x;
        return offset;
    }
    private int getYOffset(float curY){
        int y = (int)curY;
        int offset = y - prePoint.y;
        prePoint.y = y;
        return offset;
    }

    //xOffset,yOffset为videoview移动的偏移量
    private void moveVedioView(int xOffset, int yOffset) {
        Log.d("zrr", "moveVedioView" + xOffset + ":" + yOffset);
        if (xOffset > sensitivity || yOffset > sensitivity || xOffset < -sensitivity || yOffset < -sensitivity) {
            currentLeft = currentLeft + xOffset;
            currentTop = currentTop + yOffset;
            currentRight = currentRight + xOffset;
            currentBottom = currentBottom + yOffset;
            if (currentLeft > originalLeft) {
                currentLeft = originalLeft;
                currentRight = currentLeft + currentWidth;
            }
            if (currentTop > originalTop) {
                currentTop = originalTop;
                currentBottom = currentTop + currentHeight;
            }
            if (currentRight < originalRight) {
                currentRight = originalRight;
                currentLeft = currentRight - currentWidth;
            }
            if (currentBottom < originalBottom) {
                currentBottom = originalBottom;
                currentTop = currentBottom - currentHeight;
            }
            mVideoView.layout(currentLeft, currentTop, currentRight, currentBottom);
        }
    }

    //滑动动画
    @SuppressLint("NewApi")
    private void startFlipAnimation(int xOffset, int yOffset){
        if(xFlipAnim == null && yFlipAnim == null){
            Interpolator interpolator = new DecelerateInterpolator();
            xFlipAnim = ValueAnimator.ofInt(xOffset,0);
            xFlipAnim.setInterpolator(interpolator);
            xFlipAnim.setDuration(FLIP_ANIM_DURATION);
            yFlipAnim = ValueAnimator.ofInt(yOffset,0);
            yFlipAnim.setInterpolator(interpolator);
            yFlipAnim.setDuration(FLIP_ANIM_DURATION);
            xFlipAnim.addUpdateListener(new AnimatorUpdateListener(){
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    moveVedioView((Integer)animation.getAnimatedValue(),0);
                }
            });
            yFlipAnim.addUpdateListener(new AnimatorUpdateListener(){
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    moveVedioView(0,(Integer)animation.getAnimatedValue());
                }
            });
        }
        if(xFlipAnim.isRunning())
            xFlipAnim.cancel();
        xFlipAnim.setIntValues(xOffset,0);
        xFlipAnim.start();
        if(yFlipAnim.isRunning())
            yFlipAnim.cancel();
        yFlipAnim.setIntValues(yOffset,0);
        yFlipAnim.start();
    }

    /**
     * x,y为放大缩小的中心
     *
     * @param x
     * @param y
     * @return
     */
    public boolean changeSize(float x, float y) {
        Log.d("zrr", "changeSize");
        mVideoView.setmZoom(true);
        float xScale = (float) ((x - currentLeft) / currentWidth); //currentLeft为当前图像在屏幕左边的位置，currentLeft<=0
        float yScale = (float) ((y - currentTop) / currentHeight);  //currentTop为当前图像在图片顶部的位置，currentTop<=0
        int xDist = (int) (originalWidth * scale) - currentWidth;
        int yDist = (int) (originalHeight * scale) - currentHeight;
        currentLeft = (int) (currentLeft - xDist * xScale);
        currentTop = (int) (currentTop - yDist * yScale);
        currentRight = (int) (currentRight + (1 - xScale) * xDist);
        currentBottom = (int) (currentBottom + (1 - yScale) * yDist);
        if (currentLeft > originalLeft) {
            currentLeft = originalLeft;
            currentRight = currentLeft + currentWidth + xDist;
        }
        if (currentTop > originalTop) {
            currentTop = originalTop;
            currentBottom = currentTop + currentHeight + yDist;
        }
        if (currentRight < originalRight) {
            currentRight = originalRight;
            currentLeft = currentRight - currentWidth - xDist;
        }
        if (currentBottom < originalBottom) {
            currentBottom = originalBottom;
            currentTop = currentBottom - currentHeight - yDist;
        }
        mVideoView.layout(currentLeft, currentTop, currentRight, currentBottom);
        currentHeight = (int) (originalHeight * scale);
        currentWidth = (int) (originalWidth * scale);

        if(currentWidth == originalWidth && currentHeight == originalHeight){
            mVideoView.setScaleStatus(false, currentWidth, currentHeight);
        }else{
            mVideoView.setScaleStatus(true, currentWidth, currentHeight);
        }

        return true;
    }

    public void zoomOut(float x, float y){
        Log.d("zrr", "zoomOut scale = " + scale);
        if(scale == maxScale){  //已经是最大方法倍数，则不进行缩放操作
            return;
        }
        scale += CHANGE_RANGE;
        if(scale <= maxScale) {
            changeSize(x, y);
        } else {
            scale = maxScale;
            changeSize(x, y);
        }
    }

    @SuppressLint("NewApi")
    public void zoomIn(float x,float y){
        Log.d("zrr", "Zoomin scale = " + scale);
        if(scale == 1) {  //已经是原始大小，则不进行缩放操作
            resetScale();
            mVideoView.requestLayout();
            return;
        }
        scale -= CHANGE_RANGE;
        if(scale >= 1){
            changeSize(x,y);
        }else {
            scale = 1;
            changeSize(x,y);
        }
    }

    /**
     * 设置最大的放大倍数
     * @param maxScale
     */
    public void setMaxScale(float maxScale){
        this.maxScale=maxScale;
    }

    /**
     * 设置缩放的灵敏度
     * @param sensitivity
     */
    public void setZoomSensitivity(int sensitivity){
        this.CHANGE_RANGE = sensitivity / 10.0;
    }

    /**
     * 开始缓冲
     */
    @Override
    public void onBufferingEnd() {
        if(this.mOnRealTimeVideoChangeListener!=null){
            this.mOnRealTimeVideoChangeListener.onChange(VIDEO_STATUS_BUFFERING_END);
        }
    }

    /**
     * 结束缓冲
     */
    @Override
    public void onBufferingStart() {
        if(this.mOnRealTimeVideoChangeListener!=null){
            this.mOnRealTimeVideoChangeListener.onChange(VIDEO_STATUS_BUFFERING_START);
        }
    }

    /**
     * 播放完成
     * @param arg0
     */
    @Override
    public void onCompletion(IMediaPlayer arg0) {
        if(this.mOnRealTimeVideoChangeListener!=null){
            this.mOnRealTimeVideoChangeListener.onChange(VIDEO_STATUS_COMPLETE);
        }
    }

    /**
     * 准备播放
     * @param arg0
     */
    @Override
    public void onPrepared(IMediaPlayer arg0) {
        if(this.mOnRealTimeVideoChangeListener!=null){
            this.mOnRealTimeVideoChangeListener.onChange(VIDEO_STATUS_PREPARE);
        }
    }

    /**
     * 播放出错
     * @param iMediaPlayer
     * @param i
     * @param i1
     * @return
     */
    @Override
    public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
        if(this.mOnRealTimeVideoChangeListener!=null){
            this.mOnRealTimeVideoChangeListener.onChange(VIDEO_STATUS_FAILED);
        }
        return false;
    }

    /**
     * 播放暂停
     */
    @Override
    public void onPause() {
        if(this.mOnRealTimeVideoChangeListener!=null){
            this.mOnRealTimeVideoChangeListener.onChange(VIDEO_STATUS_PAUSE);
        }
    }

    /**
     * 播放继续
     */
    @Override
    public void onStart() {
        if(this.mOnRealTimeVideoChangeListener!=null){
            this.mOnRealTimeVideoChangeListener.onChange(VIDEO_STATUS_START);
        }
    }

    /**
     * 网络错误回调
     * @param iMediaPlayer
     * @param i
     * @param i1
     * @return
     */
    @Override
    public boolean onNetworkError(IMediaPlayer iMediaPlayer, int i, int i1) {
        if(this.mOnRealTimeVideoChangeListener!=null){
            this.mOnRealTimeVideoChangeListener.onChange("networkerror");
        }
        return false;
    }

    /**
     * 首帧绘制回调
     */
    @Override
    public void onRenderingStart() {
        if(this.mOnRealTimeVideoChangeListener!=null){
            this.mOnRealTimeVideoChangeListener.onChange(VIDEO_STATUS_RENDERING_START);
        }
    }

    @Override
    public void onAudioRenderingStart() {
        if(this.mOnRealTimeVideoChangeListener!=null){
            this.mOnRealTimeVideoChangeListener.onChange(VIDEO_AUDIO_STATUS_RENDERING_START);
        }
    }

    private static class Point{
        int x,y;
        Point(int x,int y){
            this.x=x;
            this.y=y;
        }
    }

    public interface OnRealTimeVideoChangeListener{
        public void onChange(String state);
    }
    private OnRealTimeVideoChangeListener mOnRealTimeVideoChangeListener;
    public void setOnRealTimeVideoChangeListner(OnRealTimeVideoChangeListener listener){
        this.mOnRealTimeVideoChangeListener = listener;

    }

    public void setOnSingleTapListener(OnSingleTapListener listener){
        this.mOnSingleTapListener = listener;
    }
    private OnSingleTapListener mOnSingleTapListener;
    public interface OnSingleTapListener{
        public void onSingleTap();
    }


    private Point mFirstPointerPoint = new Point(0,0); //ACTION_DOWN的时候更新
    private final int MOVE_SENSITIVITY = 30; //移动的灵敏度 越大也不灵敏（用来判断手指是否发生了移动，是否应该触发singleTap事件）
    //根据手指move的距离判断是否应该触发singleTap事件
    private boolean shouldTriggerSingleTap(MotionEvent me){
        int xDist = (int)me.getX() - mFirstPointerPoint.x;
        int yDist = (int)me.getY() - mFirstPointerPoint.y;
        int dist = (int) Math.sqrt(xDist * xDist + yDist * yDist);
        return dist < MOVE_SENSITIVITY;
    }
}

