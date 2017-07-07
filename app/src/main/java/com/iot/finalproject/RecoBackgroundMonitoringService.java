package com.iot.finalproject;

/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2015 Perples, Inc.
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


import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.perples.recosdk.RECOBeacon;
import com.perples.recosdk.RECOBeaconManager;
import com.perples.recosdk.RECOBeaconRegion;
import com.perples.recosdk.RECOBeaconRegionState;
import com.perples.recosdk.RECOErrorCode;
import com.perples.recosdk.RECOMonitoringListener;
import com.perples.recosdk.RECOServiceConnectListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

/**
 * RECOBackgroundMonitoringService is to monitor regions in the background.
 *
 * RECOBackgroundMonitoringService는 백그라운드에서 monitoring을 수행합니다.
 */
public class RecoBackgroundMonitoringService extends Service implements RECOMonitoringListener, RECOServiceConnectListener {
    /**
     * We recommend 1 second for scanning, 10 seconds interval between scanning, and 60 seconds for region expiration time.
     * 1초 스캔, 10초 간격으로 스캔, 60초의 region expiration time은 당사 권장사항입니다.
     */
    private long mScanDuration = 1*1000L;
    private long mSleepDuration = 10*1000L;
    private long mRegionExpirationTime = 60*1000L;
    private int mNotificationID = 9999;

    private RECOBeaconManager mRecoManager;
    private ArrayList<RECOBeaconRegion> mRegions;

    @Override
    public void onCreate() {
        Log.i("BackMonitoringService", "onCreate()");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("BackMonitoringService", "onStartCommand()");
        /**
         * Create an instance of RECOBeaconManager (to set scanning target and ranging timeout in the background.)
         * If you want to scan only RECO, and do not set ranging timeout in the backgournd, create an instance:
         * 		mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), true, false);
         * WARNING: False enableRangingTimeout will affect the battery consumption.
         *
         * RECOBeaconManager 인스턴스틀 생성합니다. (스캔 대상 및 백그라운드 ranging timeout 설정)
         * RECO만을 스캔하고, 백그라운드 ranging timeout을 설정하고 싶지 않으시다면, 다음과 같이 생성하시기 바랍니다.
         * 		mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), true, false);
         * 주의: enableRangingTimeout을 false로 설정 시, 배터리 소모량이 증가합니다.
         */
        mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), MainActivity.SCAN_RECO_ONLY, MainActivity.ENABLE_BACKGROUND_RANGING_TIMEOUT);
        this.bindRECOService();
        //this should be set to run in the background.
        //background에서 동작하기 위해서는 반드시 실행되어야 합니다.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i("BackMonitoringService", "onDestroy()");
        this.tearDown();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i("BackMonitoringService", "onTaskRemoved()");
        super.onTaskRemoved(rootIntent);
    }

    private void bindRECOService() {
        Log.i("BackMonitoringService", "bindRECOService()");

        mRegions = new ArrayList<>();

        Integer UUIDMaj = 11;
        this.generateBeaconRegion(UUIDMaj, 111, "1");
        this.generateBeaconRegion(UUIDMaj, 112, "2");
        this.generateBeaconRegion(UUIDMaj, 113, "3");
        this.generateBeaconRegion(UUIDMaj, 114, "4");
        this.generateBeaconRegion(UUIDMaj, 115, "5");
        this.generateBeaconRegion(UUIDMaj, 116, "6");

        mRecoManager.setMonitoringListener(this);
        mRecoManager.bind(this);
    }

    private void generateBeaconRegion(Integer maj, Integer mir, String RegsionId) {
        Log.i("BackMonitoringService", "generateBeaconRegion()");

        RECOBeaconRegion recoRegion;

        recoRegion = new RECOBeaconRegion(MainActivity.RECO_UUID, maj, mir, RegsionId);
        recoRegion.setRegionExpirationTimeMillis(mRegionExpirationTime);
        mRegions.add(recoRegion);
    }

    private void startMonitoring() {
        Log.i("BackMonitoringService", "startMonitoring()");

        mRecoManager.setScanPeriod(mScanDuration);
        mRecoManager.setSleepPeriod(mSleepDuration);

        for(RECOBeaconRegion region : mRegions) {
            try {
                mRecoManager.startMonitoringForRegion(region);
            } catch (RemoteException e) {
                Log.e("BackMonitoringService", "RemoteException has occured while executing RECOManager.startMonitoringForRegion()");
                e.printStackTrace();
            } catch (NullPointerException e) {
                Log.e("BackMonitoringService", "NullPointerException has occured while executing RECOManager.startMonitoringForRegion()");
                e.printStackTrace();
            }
        }
    }

    ////////////////////
    ////////////////////
    ////////////////////

    private void showMessage(Context context, String title, String msg, String ticker, String code) {
//비콘 신호 수신시 메시지 전송....
        NotificationManager mManager = (NotificationManager)context.getSystemService(Activity.NOTIFICATION_SERVICE);
        PendingIntent pendingIntent ;
        int notifyID= (int)java.lang.System.currentTimeMillis();
        pendingIntent = PendingIntent.getActivity(
                context,
                notifyID,
                new Intent(context, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("code",code),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Builder mBuilder;
        android.support.v4.app.NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
        style.setBigContentTitle(title).bigText(msg);
        java.lang.System.currentTimeMillis();

        mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.b)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)//선택하면 사라진다.
                .setTicker(ticker)
                .setVibrate(new long[]{500, 500})
                .setStyle(style);

        mManager.notify(notifyID, mBuilder.build());
    }

    public String todayDateTime() {
        //오늘 일자 반환..
        SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        Date current = new Date();
        String date = formater.format(current);
        String nowDate = date;
        return nowDate;
    }

    private String getRecoRegTime() {
        // 비콘을 읽은 일자를 SharedPreferences에서 읽어 온다.
        SharedPreferences prefs = getSharedPreferences(MainActivity.APP_NBAME, 0);

        String regTime ="";
        regTime = prefs.getString(MainActivity.RECO_REG_TIME, "");
        if (regTime.isEmpty()) return "";
        else return regTime;
    }
    ////////////////////
    ////////////////////
    ////////////////////

    private void stopMonitoring() {
        Log.i("BackMonitoringService", "stopMonitoring()");

        for(RECOBeaconRegion region : mRegions) {
            try {
                mRecoManager.stopMonitoringForRegion(region);
            } catch (RemoteException e) {
                Log.e("BackMonitoringService", "RemoteException has occured while executing RECOManager.stopMonitoringForRegion()");
                e.printStackTrace();
            } catch (NullPointerException e) {
                Log.e("BackMonitoringService", "NullPointerException has occured while executing RECOManager.stopMonitoringForRegion()");
                e.printStackTrace();
            }
        }
    }

    private void tearDown() {
        Log.i("BackMonitoringService", "tearDown()");
        this.stopMonitoring();

        try {
            mRecoManager.unbind();
        } catch (RemoteException e) {
            Log.e("BackMonitoringService", "RemoteException has occured while executing unbind()");
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceConnect() {
        Log.i("BackMonitoringService", "onServiceConnect()");
        Toast.makeText(this, "reco connected", Toast.LENGTH_SHORT).show();
        this.startMonitoring();
        //Write the code when RECOBeaconManager is bound to RECOBeaconService

    }

    @Override
    public void didDetermineStateForRegion(RECOBeaconRegionState state, RECOBeaconRegion region) {
        Log.i("BackMonitoringService", "didDetermineStateForRegion()");
        //Write the code when the state of the monitored region is changed
        Toast.makeText(this, "didDetermineStateForRegion", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void didEnterRegion(RECOBeaconRegion region, Collection<RECOBeacon> beacons) {
        /**
         * For the first run, this callback method will not be called.
         * Please check the state of the region using didDetermineStateForRegion() callback method.
         *
         * 최초 실행시, 이 콜백 메소드는 호출되지 않습니다.
         * didDetermineStateForRegion() 콜백 메소드를 통해 region 상태를 확인할 수 있습니다.
         */

        //Get the region and found beacon list in the entered region
        Log.i("BackMonitoringService", "didEnterRegion() - " + region.getUniqueIdentifier());
//        this.popupNotification("Inside of " + region.getUniqueIdentifier());
        //Write the code when the device is enter the region

        String recoRegTime = getRecoRegTime();
        String todayTime = todayDateTime();

        Toast.makeText(this, todayTime + " didEnterRegion", Toast.LENGTH_SHORT).show();

//        if(!recoRegTime.equals(todayTime)) {
////오늘 비콘 신호를 처음 읽는 다면
//            showMessage(this,"제목","내용", "Ticket", "구분값");
//        }
//        else
//        {
//            showMessage(this,"제목","오늘 들어 왔다.", "Ticket", "구분값");
//        }
    }

    @Override
    public void didExitRegion(RECOBeaconRegion region) {
        /**
         * For the first run, this callback method will not be called.
         * Please check the state of the region using didDetermineStateForRegion() callback method.
         *
         * 최초 실행시, 이 콜백 메소드는 호출되지 않습니다.
         * didDetermineStateForRegion() 콜백 메소드를 통해 region 상태를 확인할 수 있습니다.
         */

        Log.i("BackMonitoringService", "didExitRegion() - " + region.getUniqueIdentifier());
//        this.popupNotification("Outside of " + region.getUniqueIdentifier());
        //Write the code when the device is exit the region
        Toast.makeText(this, "didExitRegion", Toast.LENGTH_SHORT).show();
        showMessage(this,"제목","didExitRegion", "Ticket", "구분값");

    }

    @Override
    public void didStartMonitoringForRegion(RECOBeaconRegion region) {
        Log.i("BackMonitoringService", "didStartMonitoringForRegion() - " + region.getUniqueIdentifier());
        //Write the code when starting monitoring the region is started successfully
        Toast.makeText(this, "didStartMonitoringForRegion", Toast.LENGTH_SHORT).show();
        showMessage(this,"제목","didStartMonitoringForRegion", "Ticket", "구분값");

    }

    private void popupNotification(String msg) {
        Log.i("BackMonitoringService", "popupNotification()");
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(new Date());
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(msg + " " + currentTime)
                        .setContentText(msg);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        builder.setStyle(inboxStyle);
        nm.notify(mNotificationID, builder.build());
        mNotificationID = (mNotificationID - 1) % 1000 + 9000;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // This method is not used
        return null;
    }

    @Override
    public void onServiceFail(RECOErrorCode errorCode) {
        //Write the code when the RECOBeaconService is failed.
        //See the RECOErrorCode in the documents.
        return;
    }

    @Override
    public void monitoringDidFailForRegion(RECOBeaconRegion region, RECOErrorCode errorCode) {
        //Write the code when the RECOBeaconService is failed to monitor the region.
        //See the RECOErrorCode in the documents.
        return;
    }
}
