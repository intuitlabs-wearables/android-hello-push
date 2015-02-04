/*
 * Copyright (c) 2015 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.intuitlabs.wear.pushreceiver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.intuit.intuitwear.exceptions.IntuitWearException;
import com.intuit.intuitwear.notifications.IWearAndroidNotificationSender;
import com.intuit.intuitwear.notifications.IWearNotificationSender;
import com.intuit.intuitwear.notifications.IWearNotificationType;
import com.intuit.mobile.png.sdk.PushNotifications;
import com.intuit.mobile.png.sdk.UserTypeEnum;

import java.lang.ref.WeakReference;


/**
 * GCMIntentService extends Google's {@link GCMBaseIntentService}, setting up the communication with
 * the Push Notification Gateway. Here we are adding the registration with Intuit's Notification
 * Server and also implement a few callbacks.
 */
public class GCMIntentService extends GCMBaseIntentService {
    private static final String LOG_TAG = GCMIntentService.class.getSimpleName();
    private static final String MSG_KEY = GCMIntentService.class.getName() + "_MSG_KEY";

    /**
     * Handler that can display incoming messages, if the registered Activity is still around.
     */
    private static class MyHandler extends Handler {
        //Using a weak reference means we won't prevent garbage collection
        private final WeakReference<MainActivity> myClassWeakReference;

        public MyHandler(final MainActivity myClassInstance) {
            myClassWeakReference = new WeakReference<>(myClassInstance);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handleMessage(final Message msg) {
            MainActivity activity = myClassWeakReference.get();
            if (activity != null && msg !=null) {
                activity.showMessage(msg.getData().getString(MSG_KEY, ""));
            }
        }
    }

    private static MyHandler handler;

    public static void setHandler(final MainActivity a) {
        handler = new MyHandler(a);
    }


    /**
     * This method triggers the main registration flow. It should be called each
     * time the application is started to ensure the app stays in sync with the
     * Push Notification servers.
     *
     * @param context         {@link android.content.Context} Android Context
     * @param receiver_id     {@link String} Type of userId, represents intuit id, Mobile Number, Email, uniquely identifies the user
     * @param receiver_groups {@link String[]} The groups to which the userId may belong, allowing for groups messages
     */
    protected static void register(final Context context,
                                   final String receiver_id,
                                   final String[] receiver_groups) {
        PushNotifications.register(
                context,
                MainActivity.GCM_PROJECT_NUMBER,
                receiver_id,
                receiver_groups,
                UserTypeEnum.OTHER,
                MainActivity.INTUIT_SENDER_ID,
                false);
    }

    /**
     * Default Constructor, requires Google GCM PROJECT_NUMBER,
     * which must be your GCM Project number and statically available.
     */
    public GCMIntentService() {
        super(MainActivity.GCM_PROJECT_NUMBER);
    }


    /*
     * This callback method is invoked when GCM delivers a notification to the device.
     *
     * Assuming that the json encoded message is a valid (see IntuitWear JSONSchema) document,
     * we acquire an instance of a {@link IWearNotificationSender.Factory} to create a NotificationSender,
     * which will send the generated notification to the wearable device.
     *
     * @param context {@link Context} Application context
     * @param intent {@link Intent} received with the push notification
     */
    @Override
    protected void onMessage(final Context context, final Intent intent) {
        Log.v(LOG_TAG, "Received onMessage call. Will now display a notification");
        final String message = intent.getStringExtra("payload").replaceAll("[\r\n]+$", "");
        final IWearNotificationSender.Factory iWearSender = IWearNotificationSender.Factory.getsInstance();
        try {
            IWearAndroidNotificationSender androidNotificationSender =
                    (IWearAndroidNotificationSender) iWearSender.createNotificationSender(IWearNotificationType.ANDROID, this, message);
            androidNotificationSender.sendNotification(this);
        } catch (IntuitWearException e) {
            e.printStackTrace();
        }

        if (handler != null) {
            Message m = new Message();
            Bundle b = new Bundle();
            b.putString(MSG_KEY, message);
            m.setData(b);
            handler.sendMessage(m);
        }
    }


    /*
     * This callback method is invoked after a successful registration with GCM.
     * Here we are passing the new registrationId to the PNG SDK.
     * The SDK will send the registrationId along with any user and userGroup mappings to the PNG servers.
     */
    @Override
    protected void onRegistered(final Context context, final String regId) {
        Log.i(LOG_TAG, "Received onRegistered call. Updating the PNG servers.");
        PushNotifications.updateServer(context, regId);
    }

    /**
     * Callback called upon a GCM error.
     *
     * @param context {@link Context} Application context
     * @param msg     {@link String} Error string
     */
    @Override
    protected void onError(final Context context, final String msg) {
        Log.e(LOG_TAG, "Error related to GCM: " + msg);
    }

    /**
     * Callback called when device is unregistered from GCM.
     *
     * @param context {@link Context} Application context
     * @param msg     Unregister message
     */
    @Override
    protected void onUnregistered(final Context context, final String msg) {
        Log.i(LOG_TAG, "Received unregistered call");
    }
}

