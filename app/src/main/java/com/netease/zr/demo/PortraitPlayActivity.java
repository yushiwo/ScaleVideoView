package com.netease.zr.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.netease.zr.myapplication.R;
import com.zr.ijk.util.ToastUtil;
import com.zr.ijk.view.QGVideoView;


public class PortraitPlayActivity extends AppCompatActivity implements View.OnClickListener{

    private String mVideoPath;
    private QGVideoView mRealVideoView;
    private Button mSnapButton;
    private ImageView mSnapImageView;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portrait_play);

        mContext = this;

        mVideoPath = getIntent().getStringExtra("videoPath");
        Intent intent = getIntent();
        String intentAction = intent.getAction();
        if (!TextUtils.isEmpty(intentAction) && intentAction.equals(Intent.ACTION_VIEW)) {
            mVideoPath = intent.getDataString();
        }

        initView();

        setListeners();
    }


    private void initView(){
        mSnapButton = (Button)findViewById(R.id.btn_snap);
        mSnapImageView = (ImageView) findViewById(R.id.img_snap);
        mRealVideoView = (QGVideoView)findViewById(R.id.video_portrait);
        // 设置播放模式：直播 or 点播
        mRealVideoView.setBufferStrategy(QGVideoView.VIDEO_MODE_LIVE);
        // 设置是否允许缩放
        mRealVideoView.setCanScale(true);
        mRealVideoView.startSession(mVideoPath);
    }

    private void setListeners(){

        mSnapButton.setOnClickListener(this);

        // 播放控件设置点击监听，当控件可以缩放时，响应singleTap事件；否则响应onClick事件
        mRealVideoView.setOnSingleTapListener(new QGVideoView.OnSingleTapListener() {
            @Override
            public void onSingleTap() {
                ToastUtil.showToast(mContext, "点击了播放控件！");
            }
        });

        mRealVideoView.setOnRealTimeVideoChangeListner(new QGVideoView.OnRealTimeVideoChangeListener() {
            @Override
            public void onChange(String state) {
                ToastUtil.showToast(mContext, state);
            }
        });

    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.btn_snap) {
            if (mRealVideoView.isPlaying()) {
                mSnapImageView.setImageBitmap(mRealVideoView.getSnapShot());
            } else {
                ToastUtil.showToast(mContext, "请在视频播放状态下截图");
            }

        }
    }
}
