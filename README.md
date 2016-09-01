#ScaleVideoView
实现可缩放的视频播放器，提供实时截图功能。

### 布局:
```
<com.zr.example.view.QGVideoView  
	android:id="@+id/video_portrait"  
    android:layout_centerInParent="true"  
    android:background="#000000"  
    android:layout_width="match_parent"  
    android:layout_height="200dp" />  
```

### 代码中设置
```
 		mRealVideoView = (QGVideoView)findViewById(R.id.video_portrait);
        // 设置播放模式：直播 or 点播
        mRealVideoView.setBufferStrategy(QGVideoView.VIDEO_MODE_LIVE);
        // 设置是否允许缩放
        mRealVideoView.setCanScale(true);
        mRealVideoView.startSession(mVideoPath);
```

具体实现请看代码，效果如图所示

![演示](https://github.com/yushiwo/ScaleVideoView/blob/master/art/demo.gif?raw=true)
