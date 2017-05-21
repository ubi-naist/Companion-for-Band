package com.pimp.companionforband.fragments.presenter;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
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

    private static final float ACC_MAX = 3.0f;
    private static final float ACC_MIN = 0.8f;
    private static final float GSR_MAX = 4000f;
    private static final float GSR_MIN = 2000f;
    private static final float HR_MAX = 150f;
    private static final float HR_MIN = 60f;

    private static final int LISTENER_INTERVAL = 5000;

    private static String[] labels = {"MOVE", "GSR", "HR"};
    private static int[] colors = {R.color.md_orange_A400, R.color.md_blue_300, R.color.md_red_400};

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

            float normalizedAcc = (accVal - ACC_MIN) / (ACC_MAX - ACC_MIN);
            float normalizedGsr = (gsrVal - GSR_MIN) / (GSR_MAX - GSR_MIN);
            float normalizedHeart = (heartVal - HR_MIN) / (HR_MAX - HR_MIN);

            float[] vals = {normalizedAcc, normalizedGsr, normalizedHeart};

            if(sensorChart.getLineData() == null){
                LineData lineData = new LineData();

                for(int i=0; i<3; i++){
                    ArrayList<Entry> list = new ArrayList<>();
                    list.add(new Entry(0, vals[i]));
                    LineDataSet lineDataSet = new LineDataSet(list, labels[i]);

                    int color = ContextCompat.getColor(MainActivity.sContext, colors[i]);
                    lineDataSet.setColor(color);

                    // lineDataSet.setDrawCircleHole(false);
                    lineDataSet.setCircleColor(color);

                    lineDataSet.setLineWidth(2.0f);
                    lineData.addDataSet(lineDataSet);

                    lineDataSet.setDrawValues(false);
                }
                sensorChart.setData(lineData);
                sensorChart.notifyDataSetChanged();
                sensorChart.moveViewToX(lineData.getEntryCount());
            }else{
                LineData lineData = sensorChart.getLineData();
                float xVal = lineData.getEntryCount() / 3.0f  * LISTENER_INTERVAL / 60000f;
                for(int i=0; i<3; i++){
                    lineData.addEntry(new Entry(xVal, vals[i]), i);
                }

                lineData.notifyDataChanged();
                sensorChart.notifyDataSetChanged();
                sensorChart.moveViewToX(lineData.getEntryCount());
            }
        }
    };

    private void initGraph(){
        sensorChart.setNoDataText("No Sensor Data...");

        Description description = sensorChart.getDescription();
        description.setEnabled(false);

        int whiteColor = ContextCompat.getColor(MainActivity.sContext, R.color.md_white_1000);

        XAxis xAxis = sensorChart.getXAxis();
        xAxis.setTextColor(whiteColor);

        YAxis axisLeft = sensorChart.getAxisLeft();
        axisLeft.setEnabled(false);

        YAxis axisRight = sensorChart.getAxisRight();
        axisRight.setEnabled(false);

        Legend legend = sensorChart.getLegend();
        legend.setTextColor(whiteColor);
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

        sensorChart = (LineChart) view.findViewById(R.id.sensors_graph);
        initGraph();

        bandSensorObserver = new BandSensorObserver(MainActivity.client, LISTENER_INTERVAL);
        bandSensorObserver.setHeartRateListener(bandHeartRateEventListener);
        bandSensorObserver.setSensorsViewListener(sensorsViewListener);

        heartRateTextView = (TextView) view.findViewById(R.id.heartRateTextView);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        bandSensorObserver.setSensorsViewListener(sensorsViewListener);
        bandSensorObserver.startObserve();
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.d(TAG, "onPause");
        bandSensorObserver.stopObserve();
    }
}
