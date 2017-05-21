package com.pimp.companionforband.fragments.presenter;

import android.hardware.SensorListener;
import android.os.Handler;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.InvalidBandVersionException;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.GsrSampleRate;
import com.microsoft.band.sensors.SampleRate;
import com.pimp.companionforband.utils.band.BandUtils;

import java.util.ArrayList;

/**
 * Created by yuta- on 2017/05/21.
 */

public class BandSensorObserver {


    private BandHeartRateEventListener heartRateListener;
    private SensorsViewListener sensorsViewListener;

    private BandAccelerometerEventListener bandAccelerometerEventListener;
    private BandGsrEventListener bandGsrEventListener;
    private BandHeartRateEventListener bandHeartRateEventListener;

    private ArrayList<BandAccelerometerEvent> accelerometerEvents;
    private ArrayList<BandGsrEvent> gsrEvents;
    private ArrayList<BandHeartRateEvent> heartRateEvents;

    private Handler observeHandler;
    private Runnable observeRunnable;

    private int callbackInterval;
    private BandClient client;

    public BandSensorObserver(BandClient client, final int callbackInterval){
        this.callbackInterval = callbackInterval;

        this.client = client;

        this.bandAccelerometerEventListener = new BandAccelerometerEventListener() {
            @Override
            public void onBandAccelerometerChanged(BandAccelerometerEvent bandAccelerometerEvent) {
                // Log.d("test1", "accx " + bandAccelerometerEvent.getAccelerationX());
                accelerometerEvents.add(bandAccelerometerEvent);
            }
        };
        this.bandGsrEventListener = new BandGsrEventListener() {
            @Override
            public void onBandGsrChanged(BandGsrEvent bandGsrEvent) {
                // Log.d("test1", "gsr " + bandGsrEvent.getResistance());
                gsrEvents.add(bandGsrEvent);
            }
        };
        this.bandHeartRateEventListener = new BandHeartRateEventListener() {
            @Override
            public void onBandHeartRateChanged(BandHeartRateEvent bandHeartRateEvent) {
                heartRateEvents.add(bandHeartRateEvent);
                if(heartRateListener != null){
                    heartRateListener.onBandHeartRateChanged(bandHeartRateEvent);
                }
            }
        };

        this.observeHandler = new Handler();
        this.observeRunnable = new Runnable() {
            @Override
            public void run() {
                if(sensorsViewListener != null){
                    float accVal = 0.0f;
                    if(accelerometerEvents.size() > 0){
                        for(BandAccelerometerEvent event:accelerometerEvents){
                            float acc = (float) Math.sqrt(
                                    event.getAccelerationX() * event.getAccelerationX() +
                                            event.getAccelerationY() * event.getAccelerationY() +
                                            event.getAccelerationZ() * event.getAccelerationZ()
                            );
                            accVal += acc;
                        }
                        accVal /= (float)accelerometerEvents.size();
                    }

                    float gsrVal = 0.0f;
                    if(gsrEvents.size() > 0){
                        for(BandGsrEvent event:gsrEvents){
                            int gsr = event.getResistance();
                            gsrVal += gsr;
                        }
                        gsrVal /= (float)gsrEvents.size();
                    }

                    float heartVal = 0.0f;

                    if(heartRateEvents.size() > 0){
                        for(BandHeartRateEvent event:heartRateEvents){
                            int heart = event.getHeartRate();
                            heartVal += heart;
                        }
                        heartVal /= (float)heartRateEvents.size();
                    }

                    sensorsViewListener.onSensorChanged(accVal, gsrVal, heartVal);
                }
                accelerometerEvents = new ArrayList<>();
                gsrEvents = new ArrayList<>();
                heartRateEvents = new ArrayList<>();

                observeHandler.postDelayed(observeRunnable, callbackInterval);
            }
        };
    }

    public void startObserve() {
        this.accelerometerEvents = new ArrayList<>();
        this.gsrEvents = new ArrayList<>();
        this.heartRateEvents = new ArrayList<>();
        try {
            this.client.getSensorManager().
                    registerAccelerometerEventListener(bandAccelerometerEventListener, SampleRate.MS128);
            this.client.getSensorManager().
                    registerGsrEventListener(bandGsrEventListener, GsrSampleRate.MS200);
            this.client.getSensorManager().
                    registerHeartRateEventListener(bandHeartRateEventListener);
        } catch (BandIOException e) {
            BandUtils.handleBandException(e);
        } catch (InvalidBandVersionException e) {
            // BandUtils.handleBandException(e);
        } catch (BandException e) {
            BandUtils.handleBandException(e);
        }
        observeHandler.postDelayed(observeRunnable, callbackInterval);
    }

    public void stopObserve(){
        try {
            this.client.getSensorManager().unregisterAccelerometerEventListener(bandAccelerometerEventListener);
            this.client.getSensorManager().unregisterHeartRateEventListener(bandHeartRateEventListener);
            this.client.getSensorManager().unregisterGsrEventListener(bandGsrEventListener);
        } catch (BandIOException e) {
            BandUtils.handleBandException(e);
        }
        observeHandler.removeCallbacks(observeRunnable);
    }

    public void setHeartRateListener(BandHeartRateEventListener heartRateListener){
        this.heartRateListener = heartRateListener;
    }

    public void setSensorsViewListener(SensorsViewListener sensorsViewListener){
        this.sensorsViewListener = sensorsViewListener;
    }

    public interface SensorsViewListener{
        void onSensorChanged(float accVal, float gsrVal, float heartVal);
    }
}
