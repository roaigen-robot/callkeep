/*
 * Copyright (c) 2016-2019 The CallKeep Authors (see the AUTHORS file)
 * SPDX-License-Identifier: ISC, MIT
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package io.wazo.callkeep;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.TelecomManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashMap;
import java.util.UUID;

import static io.wazo.callkeep.Constants.*;

import io.flutter.embedding.engine.plugins.broadcastreceiver.BroadcastReceiverControlSurface;
import io.flutter.plugin.common.MethodChannel;

@TargetApi(Build.VERSION_CODES.M)
public class VoiceConnection extends Connection  {
    private boolean isMuted = false;
    private HashMap<String, String> handle;
    private Context context;
    private static final String TAG = "RNCK:VoiceConnection";

    VoiceConnection(Context context, HashMap<String, String> handle) {
        super();
        this.handle = handle;
        this.context = context;

        String number = handle.get(EXTRA_CALL_NUMBER);
        String name = handle.get(EXTRA_CALLER_NAME);

        if (number != null) {
            setAddress(Uri.parse(number), TelecomManager.PRESENTATION_ALLOWED);
        }
        if (name != null && !name.equals("")) {
            setCallerDisplayName(name, TelecomManager.PRESENTATION_ALLOWED);
        }
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onShowIncomingCallUi() {
        String uuid = handle.get(EXTRA_CALL_UUID);
        String callerName = handle.get(EXTRA_CALLER_NAME);
        int notificationId = uuid.hashCode();
        Resources res = context.getResources();

        // Setup Fullscreen Noti
        Intent fullScreenIntent = new Intent(context, CallKeepIncomingFullNotiActivity.class);
        fullScreenIntent.putExtra(EXTRA_CALLER_NAME, callerName);
        fullScreenIntent.putExtra(EXTRA_CALL_UUID, uuid);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, notificationId, fullScreenIntent, PendingIntent.FLAG_IMMUTABLE);

        // Setup RemoteView Noti
        RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.callkeep_incoming_noti);
        remoteView.setTextViewText(R.id.caller_text, callerName + "에게 전화가 왔어요");

        Intent answerIntent = new Intent(context, VoiceConnection.NotiReceiver.class);
        answerIntent.putExtra("CALL_ANSWER", true);
        answerIntent.putExtra(EXTRA_CALL_UUID, uuid);
        PendingIntent pendingAnswerIntent = PendingIntent.getBroadcast(context, UUID.randomUUID().hashCode(), answerIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        remoteView.setOnClickPendingIntent(R.id.answer_button, pendingAnswerIntent);

        Intent rejectIntent = new Intent(context, VoiceConnection.NotiReceiver.class);
        rejectIntent.putExtra("CALL_REJECT", true);
        rejectIntent.putExtra(EXTRA_CALL_UUID, uuid);
        PendingIntent pendingRejectIntent = PendingIntent.getBroadcast(context, UUID.randomUUID().hashCode(), rejectIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        remoteView.setOnClickPendingIntent(R.id.reject_button, pendingRejectIntent);

        // Setup Notification channel
        String channelId = context.getPackageName() + ".callkeep.callnew";
        Log.d("onShowIncomingCallUi", "Set NotificationChannel : " + channelId);
        NotificationChannel channel = new NotificationChannel(channelId, "전화 수신", NotificationManager.IMPORTANCE_HIGH);
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        channel.setSound(ringtoneUri, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        NotificationManager mgr = context.getSystemService(NotificationManager.class);
        mgr.createNotificationChannel(channel);

        // Setup Notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(res.getIdentifier("ic_launcher","drawable", context.getPackageName()))
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setCustomContentView(remoteView)
                .setCustomBigContentView(remoteView)
                .setCustomHeadsUpContentView(remoteView)
                .setOngoing(true);
        Notification notification = notificationBuilder.build();
        notification.flags |= Notification.FLAG_INSISTENT;
        mgr.notify(notificationId, notification);
        Log.d("onShowIncomingCallUi", "Show Notification callUuid : " + uuid + " / id: " + notificationId);
    }

    @Override
    public void onExtrasChanged(Bundle extras) {
        super.onExtrasChanged(extras);
        HashMap attributeMap = (HashMap<String, String>)extras.getSerializable("attributeMap");
        if (attributeMap != null) {
            handle = attributeMap;
        }
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState state) {
        if (state.isMuted() == this.isMuted) {
            return;
        }

        this.isMuted = state.isMuted();
        sendCallRequestToActivity(isMuted ? ACTION_MUTE_CALL : ACTION_UNMUTE_CALL, handle);
    }

    @Override
    public void onAnswer() {
        super.onAnswer();
        Log.d(TAG, "onAnswer called");
        Log.d(TAG, "onAnswer ignored");
    }
    
    @Override
    public void onAnswer(int videoState) {
        super.onAnswer(videoState);
        Log.d(TAG, "onAnswer videoState called: " + videoState);

        setConnectionCapabilities(getConnectionCapabilities() | Connection.CAPABILITY_HOLD);
        setAudioModeIsVoip(true);

        sendCallRequestToActivity(ACTION_ANSWER_CALL, handle);
        sendCallRequestToActivity(ACTION_AUDIO_SESSION, handle);
        Log.d(TAG, "onAnswer videoState executed");
    }

    @Override
    public void onPlayDtmfTone(char dtmf) {
        try {
            handle.put("DTMF", Character.toString(dtmf));
        } catch (Throwable exception) {
            Log.e(TAG, "Handle map error", exception);
        }
        sendCallRequestToActivity(ACTION_DTMF_TONE, handle);
    }

    @Override
    public void onDisconnect() {
        super.onDisconnect();
        setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
        sendCallRequestToActivity(ACTION_END_CALL, handle);
        Log.d(TAG, "onDisconnect executed");
        try {
            ((VoiceConnectionService) context).deinitConnection(handle.get(EXTRA_CALL_UUID));
        } catch(Throwable exception) {
            Log.e(TAG, "Handle map error", exception);
        }
        destroy();
    }

    public void reportDisconnect(int reason) {
        super.onDisconnect();
        switch (reason) {
            case 1:
                setDisconnected(new DisconnectCause(DisconnectCause.ERROR));
                break;
            case 2:
            case 5:
                setDisconnected(new DisconnectCause(DisconnectCause.REMOTE));
                break;
            case 3:
                setDisconnected(new DisconnectCause(DisconnectCause.BUSY));
                break;
            case 4:
                setDisconnected(new DisconnectCause(DisconnectCause.ANSWERED_ELSEWHERE));
                break;
            case 6:
                setDisconnected(new DisconnectCause(DisconnectCause.MISSED));
                break;
            default:
                break;
        }
        ((VoiceConnectionService)context).deinitConnection(handle.get(EXTRA_CALL_UUID));
        destroy();
    }

    @Override
    public void onAbort() {
        super.onAbort();
        setDisconnected(new DisconnectCause(DisconnectCause.REJECTED));
        sendCallRequestToActivity(ACTION_END_CALL, handle);
        Log.d(TAG, "onAbort executed");
        try {
            ((VoiceConnectionService) context).deinitConnection(handle.get(EXTRA_CALL_UUID));
        } catch(Throwable exception) {
            Log.e(TAG, "Handle map error", exception);
        }
        destroy();
    }

    @Override
    public void onHold() {
        super.onHold();
        this.setOnHold();
        sendCallRequestToActivity(ACTION_HOLD_CALL, handle);
    }

    @Override
    public void onUnhold() {
        super.onUnhold();
        sendCallRequestToActivity(ACTION_UNHOLD_CALL, handle);
        setActive();
    }

    @Override
    public void onReject() {
        super.onReject();
        setDisconnected(new DisconnectCause(DisconnectCause.REJECTED));
        sendCallRequestToActivity(ACTION_END_CALL, handle);
        Log.d(TAG, "onReject executed");
        try {
            ((VoiceConnectionService) context).deinitConnection(handle.get(EXTRA_CALL_UUID));
        } catch(Throwable exception) {
            Log.e(TAG, "Handle map error", exception);
        }
        destroy();
    }

    /*
     * Send call request to the RNCallKeepModule
     */
    private void sendCallRequestToActivity(final String action, @Nullable final HashMap attributeMap) {
        final VoiceConnection instance = this;
        final Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(action);
                if (attributeMap != null) {
                    Bundle extras = new Bundle();
                    extras.putSerializable("attributeMap", attributeMap);
                    intent.putExtras(extras);
                }
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        });
    }

    public static class NotiReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String uuid = intent.getStringExtra(EXTRA_CALL_UUID);
            if(uuid == null || uuid.isEmpty()) {
                Log.d("VoiceConnection$NotiReceiver", "Not noti..");
                return;
            }
            if(intent.getBooleanExtra("CALL_ANSWER",false)) {
                handleCall(context, uuid,true);
            } else if (intent.getBooleanExtra("CALL_REJECT", false)) {
                handleCall(context, uuid,false);
            }
        }

        public static void handleCall(Context context, String uuid, boolean answer) {
            int notificationId = uuid.hashCode();
            Log.d("VoiceConnection$NotiReceiver", "Handle call : (" + answer + ") callUuid : " + uuid + " / id : " + notificationId);
            CallKeepModule lastCallKeep = CallKeepModule.LastCallKeep();
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(uuid.hashCode());
            if (answer) {
                Log.d("VoiceConnection$NotiReceiver", "CALL CALL_ANSWER : " + uuid);
                notificationManager.cancelAll();
                lastCallKeep.answerIncomingCall(uuid);
            } else {
                Log.d("VoiceConnection$NotiReceiver", "CALL REJECT : " + uuid);
                lastCallKeep.rejectCall(uuid);
            }
        }
    }
}