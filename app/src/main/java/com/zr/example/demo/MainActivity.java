package com.zr.example.demo;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.zr.example.R;


public class MainActivity extends Activity implements OnClickListener{

    private static String mVideoPath = "rtmp://live.hkstv.hk.lxdns.com/live/hks";

    EditText mEditURL;
    Button mPortraitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setListeners();
    }

    private void initView(){
        mEditURL = (EditText)findViewById(R.id.edittext);
        mPortraitButton = (Button)findViewById(R.id.portrait_btn);

        mEditURL.setText(mVideoPath);
    }

    private void setListeners(){
        mPortraitButton.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    };

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.portrait_btn:
                String url = mEditURL.getText().toString().trim();
                Intent intent = new Intent(MainActivity.this, PortraitPlayActivity.class);
                intent.putExtra("videoPath", url);
                startActivity(intent);
                break;

        }
    }

}
