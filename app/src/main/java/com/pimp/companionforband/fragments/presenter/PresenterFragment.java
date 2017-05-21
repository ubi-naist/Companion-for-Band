package com.pimp.companionforband.fragments.presenter;

import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
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
    private static final float GSR_MIN = 500f;
    private static final float HR_MAX = 150f;
    private static final float HR_MIN = 60f;

    private static final int LISTENER_INTERVAL = 10000; //msec

    private static String[] labels = {"MOVE", "GSR", "HR"};
    private static int[] colors = {R.color.md_orange_A400, R.color.md_blue_300, R.color.md_red_400};

    private static TextView heartRateTextView;
    private static LineChart sensorChart;

    boolean hideToolbar = false;
    private ImageView expandImageView;

    private static ImageView heartImageView;


    private BandSensorObserver bandSensorObserver;

    public static BandHeartRateEventListener bandHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent bandHeartRateEvent) {
            // Log.d("test", "heart " + bandHeartRateEvent.getHeartRate());
            MainActivity.sActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AnimationSet set = new AnimationSet(true);
                    ScaleAnimation scale = new ScaleAnimation(1, 0.75f, 1, 0.75f);
                    set.addAnimation(scale);
                    TranslateAnimation translate = new TranslateAnimation(0, 12, 0, 12);
                    set.addAnimation(translate);
                    set.setDuration(500);
                    heartImageView.startAnimation(set);

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

            if(accVal == 0.0f){
                normalizedAcc = 0.0f;
            }

            if(gsrVal == 0.0f){
                normalizedGsr = 0.0f;
            }

            if(heartVal == 0.0f){
                normalizedHeart = 0.0f;
            }

            float[] vals = {normalizedAcc, normalizedGsr, normalizedHeart};

            if(sensorChart.getLineData() == null){
                LineData lineData = new LineData();

                for(int i=0; i<3; i++){
                    ArrayList<Entry> list = new ArrayList<>();
                    list.add(new Entry(0, vals[i]));
                    LineDataSet lineDataSet = new LineDataSet(list, labels[i]);

                    int color = ContextCompat.getColor(MainActivity.sContext, colors[i]);
                    lineDataSet.setColor(color);

                    lineDataSet.setCircleColor(color);
                    lineDataSet.setCircleRadius(3.0f);

                    lineDataSet.setLineWidth(2.5f);
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
        int whiteColor = ContextCompat.getColor(MainActivity.sContext, R.color.md_white_1000);
        sensorChart.setNoDataText("No Sensor Data...");

        Description description = sensorChart.getDescription();
        description.setEnabled(false);

        XAxis xAxis = sensorChart.getXAxis();
        xAxis.setTextColor(whiteColor);
        xAxis.setTextSize(15.0f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis axisLeft = sensorChart.getAxisLeft();
        axisLeft.setEnabled(false);

        YAxis axisRight = sensorChart.getAxisRight();
        axisRight.setEnabled(false);

        Legend legend = sensorChart.getLegend();
        legend.setTextColor(whiteColor);
        legend.setTextSize(20.0f);

        sensorChart.setTouchEnabled(false);
        sensorChart.setDragEnabled(false);
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

        expandImageView = (ImageView) view.findViewById(R.id.expandImageView);
        expandImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(hideToolbar){
                    MainActivity.sActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ActionBar actionBar = MainActivity.sActivity.getSupportActionBar();
                            actionBar.show();
                            MainActivity.tabLayout.setVisibility(View.VISIBLE);
                        }
                    });
                }else{
                    MainActivity.sActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ActionBar actionBar = MainActivity.sActivity.getSupportActionBar();
                            actionBar.hide();
                            MainActivity.tabLayout.setVisibility(View.GONE);
                        }
                    });
                }
                hideToolbar = !hideToolbar;
                return false;
            }
        });

        sensorChart = (LineChart) view.findViewById(R.id.sensors_graph);
        initGraph();

        bandSensorObserver = new BandSensorObserver(MainActivity.client, LISTENER_INTERVAL);

        heartImageView = (ImageView) view.findViewById(R.id.heartRateImageView);
        heartRateTextView = (TextView) view.findViewById(R.id.heartRateTextView);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        bandSensorObserver.setHeartRateListener(bandHeartRateEventListener);
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
