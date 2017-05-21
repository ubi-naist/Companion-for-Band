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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
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

import java.util.ArrayList;

public class PresenterFragment extends Fragment {

    String TAG = PresenterFragment.class.getSimpleName();

    private static TextView heartRateTextView;
    private static LineChart sensorChart;

    private BandSensorObserver bandSensorObserver;

    public static BandHeartRateEventListener bandHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent bandHeartRateEvent) {
            // Log.d("test", "heart " + bandHeartRateEvent.getHeartRate());
            MainActivity.sActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    heartRateTextView.setText(bandHeartRateEvent.getHeartRate() + " BPM");
                }
            });
        }
    };

    public static BandSensorObserver.SensorsViewListener sensorsViewListener = new BandSensorObserver.SensorsViewListener() {
        @Override
        public void onSensorChanged(float accVal, float gsrVal, float heartVal) {
            Log.d("test", "sensor " + accVal +" " + gsrVal + " " + heartVal);

            float normalizedAcc = accVal / 3.0f;
            float normalizedGsr = gsrVal / 4000.0f;
            float normalizedHeart = heartVal / 140f;

            LineData lineData = sensorChart.getLineData();
            int xVal = lineData.getEntryCount();
            lineData.addEntry(new Entry(xVal, normalizedAcc), 0);
            lineData.addEntry(new Entry(xVal, normalizedGsr), 1);
            lineData.addEntry(new Entry(xVal, normalizedHeart), 2);

            lineData.notifyDataChanged();
            sensorChart.notifyDataSetChanged();
            sensorChart.moveViewToX(lineData.getEntryCount());
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_presenter, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        sensorChart = (LineChart) view.findViewById(R.id.sensors_graph);
        LineData lineData = new LineData();

        for(int i=0; i<3; i++){
            ArrayList<Entry> list = new ArrayList<>();
            list.add(new Entry(0,0));
            LineDataSet lineDataSet = new LineDataSet(list, i + "");
            lineData.addDataSet(lineDataSet);
        }

        sensorChart.setData(lineData);

        bandSensorObserver = new BandSensorObserver(MainActivity.client, 5000);
        bandSensorObserver.setHeartRateListener(bandHeartRateEventListener);
        bandSensorObserver.setSensorsViewListener(sensorsViewListener);

        heartRateTextView = (TextView) view.findViewById(R.id.heartRateTextView);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        bandSensorObserver.startObserve();
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.d(TAG, "onPause");
        bandSensorObserver.stopObserve();
    }
}
