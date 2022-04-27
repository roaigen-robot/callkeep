package io.wazo.callkeep;

import static io.wazo.callkeep.Constants.EXTRA_CALLER_NAME;
import static io.wazo.callkeep.Constants.EXTRA_CALL_UUID;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

public class CallKeepIncomingFullNotiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.callkeep_incoming_fullnoti);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            turnScreenOnAndKeyguardOff();
        }
        Intent intent = getIntent();
        CallKeepModule lastCallKeep = CallKeepModule.LastCallKeep();
        // Update views
        String callerName = intent.getStringExtra(EXTRA_CALLER_NAME);
        TextView textView = findViewById(R.id.caller_text);
        textView.setText(callerName + "에게 전화가 왔어요");

        Context context = getApplicationContext();
        String uuid = intent.getStringExtra(EXTRA_CALL_UUID);
        findViewById(R.id.answer_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VoiceConnection.NotiReceiver.handleCall(context,uuid,true);
            }
        });
        findViewById(R.id.reject_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VoiceConnection.NotiReceiver.handleCall(context,uuid,false);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            turnScreenOffAndKeyguardOn();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void turnScreenOnAndKeyguardOff() {
        setShowWhenLocked(true);
        setTurnScreenOn(true);

        Boolean requestUnlock = false;
        if (requestUnlock) {
            KeyguardManager manager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
            manager.requestDismissKeyguard(this, null);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void turnScreenOffAndKeyguardOn() {
        setShowWhenLocked(false);
        setTurnScreenOn(false);
    }

}