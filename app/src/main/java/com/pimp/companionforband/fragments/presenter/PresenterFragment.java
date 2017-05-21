package com.pimp.companionforband.fragments.presenter;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.pimp.companionforband.R;
import com.pimp.companionforband.activities.main.MainActivity;
import com.pimp.companionforband.utils.band.BandUtils;
import com.pimp.companionforband.utils.band.subscription.Band1SubscriptionTask;
import com.pimp.companionforband.utils.band.subscription.Band2SubscriptionTask;

public class PresenterFragment extends Fragment {

    String TAG = PresenterFragment.class.getSimpleName();

    public static BandAccelerometerEventListener bandAccelerometerEventListener;
    public static BandGsrEventListener bandGsrEventListener;
    public static BandHeartRateEventListener bandHeartRateEventListener;

    private TextView heartRateTextView;

    private void redisterBandListener(){
        try {
            MainActivity.client.getSensorManager().
                    registerAccelerometerEventListener(bandAccelerometerEventListener, SampleRate.MS128);
            MainActivity.client.getSensorManager().
                    registerGsrEventListener(bandGsrEventListener, GsrSampleRate.MS200);
            MainActivity.client.getSensorManager().
                    registerHeartRateEventListener(bandHeartRateEventListener);
        } catch (BandIOException e) {
            BandUtils.handleBandException(e);
        } catch (InvalidBandVersionException e) {
            MainActivity.appendToUI(e.getMessage(), "Style.ALERT");
        } catch (BandException e) {
            MainActivity.appendToUI(e.getMessage(), "Style.ALERT");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_presenter, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");
        heartRateTextView = (TextView) view.findViewById(R.id.heartRateTextView);
        bandAccelerometerEventListener = new BandAccelerometerEventListener() {
            @Override
            public void onBandAccelerometerChanged(BandAccelerometerEvent bandAccelerometerEvent) {
                Log.d(
                        TAG,
                        bandAccelerometerEvent.getAccelerationX() + " " +
                        bandAccelerometerEvent.getAccelerationY() + " " +
                        bandAccelerometerEvent.getAccelerationZ()
                );
            }
        };
        bandGsrEventListener = new BandGsrEventListener() {
            @Override
            public void onBandGsrChanged(BandGsrEvent bandGsrEvent) {
                Log.d(
                        TAG,
                        bandGsrEvent.getResistance() + " "
                );
            }
        };
        bandHeartRateEventListener = new BandHeartRateEventListener() {
            @Override
            public void onBandHeartRateChanged(BandHeartRateEvent bandHeartRateEvent) {
                Log.d(
                        TAG,
                        bandHeartRateEvent.getHeartRate() + " "
                );

                final int heartRate = bandHeartRateEvent.getHeartRate();
                MainActivity.sActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        heartRateTextView.setText(heartRate + " BPM");
                    }
                });

            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        redisterBandListener();
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.d(TAG, "onPause");
        try {
            MainActivity.client.getSensorManager().unregisterAccelerometerEventListener(bandAccelerometerEventListener);
            MainActivity.client.getSensorManager().unregisterHeartRateEventListener(bandHeartRateEventListener);
            MainActivity.client.getSensorManager().unregisterGsrEventListener(bandGsrEventListener);
        } catch (BandIOException e) {
            BandUtils.handleBandException(e);
        }
    }
}
