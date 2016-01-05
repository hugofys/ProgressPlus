package com.hugolee.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.hugolee.progressplus.ProgressPlus;

public class DemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ProgressPlus progressPlus1 = (ProgressPlus) findViewById(R.id.progress1);
        final ProgressPlus progressPlus2 = (ProgressPlus) findViewById(R.id.progress2);


        final EditText edit = (EditText) findViewById(R.id.edit_progress);
        Button btn = (Button) findViewById(R.id.btn_set);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String progress = edit.getText().toString();
                if (!TextUtils.isEmpty(progress) && TextUtils.isDigitsOnly(progress)) {
                    progressPlus1.setProgress(Integer.valueOf(progress));
                    progressPlus2.setProgress(Integer.valueOf(progress));
                }

            }
        });
    }

}
