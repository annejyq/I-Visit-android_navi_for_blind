package com.example.anne.testvisit;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import android.os.Handler;

public class Guide extends AppCompatActivity {

    public long arrived=0;
    public String api_key="AIzaSyD3d31DDRuBs8KzAf3NbflYTIMVkq8O5CA";
    //destination is got from another class
    Speaker mspeaker;
    String destination, StartPoint;

    Button FinishTrip;
    int finish_hour, finish_minutes;
    float start_time, finish_time;

    long time1, time2;
    String time;


    public String travel_time, Obstacle_Count = "0", Moving_Obstacle = "0";



    Calendar d = Calendar.getInstance();

    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = MainActivity.mmDevice;
    final byte delimiter =33;
    int readBufferPosition = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide);

        mspeaker = new Speaker(this);
        FinishTrip = (Button) findViewById(R.id.stoptrip);

        //FinishTrip.setEnabled(false);

        final Handler handler = new Handler();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            destination = extras.getString("Destination");
            StartPoint = extras.getString("Start Point");
            start_time = extras.getFloat("Start Time");

            time1 = extras.getLong("Current Time");

            destination = destination.replace(" ","%20");
            //Log.e("source&destination", StartPoint+EndPoint);
        }

        class test extends AsyncTask<String, Void, String>  {

            private String btMsg;


            protected String doInBackground(String... params){
                btMsg = params[0];
                sendBtMsg(btMsg);
                while (!Thread.currentThread().isInterrupted()) {
                    int bytesAvailable;
                    boolean workDone = false;

                    try {
                        InputStream mmInputStream;
                        mmInputStream = mmSocket.getInputStream();
                        bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {

                            byte[] packetBytes = new byte[bytesAvailable];
                            Log.e("IVisit recv byte", "bytes available");
                            byte[] readBuffer = new byte[1024];
                            mmInputStream.read(packetBytes);

                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    Log.e("databt_guide", data);
                                    readBufferPosition = 0;
                                    Obstacle_Count = data;

                                    //The variable data now contains our full command
                                    /*handler.post(new Runnable() {
                                        public void run() {
                                            //myLabel.setText(data);
                                            Obs_temp = data;
                                        }
                                    });*/

                                    workDone = true;
                                    break;


                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }

                            if (workDone) {
                                mmSocket.close();
                                return Obstacle_Count;
                                //break;
                            }

                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                return Obstacle_Count;
            }
            /*protected void onPostExecute(String obs) {
                Obstacle_Count = obs;
                Log.e("obstacle_count_guide", Obstacle_Count);

            }*/
        }



        FinishTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mspeaker.allow(true);
                mspeaker.speak("Long click to finish navigation.");
            }
        });

        FinishTrip.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                new test().execute("finish", Obstacle_Count);

                time2 = System.currentTimeMillis();
                Log.e("time2", String.valueOf(time2));

                time = String.valueOf((time2 - time1)/(1000*60));
                Log.e("time",time);

                //System.out.print(Obstacle_Count);

                travel_time = time;

                Log.e("traveltime_guide", travel_time);



                goToFeedback();
                return true;
            }
        });



        callAsynchronousTask();
        // user arrives at destination
        System.out.println("arrived");

    }


    public void callAsynchronousTask() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if (arrived == 0) {
                            try {
                                Guide.getLocation performBackgroundTask = new Guide.getLocation();
                                // PerformBackgroundTask this class is the class that extends AsynchTask
                                performBackgroundTask.execute(arrived);
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                            }
                        } /*else {
                            //FinishTrip.setEnabled(true);
                            //Log.e("button", "enabled");
                        }*/

                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 50000); //execute in every 50000 ms
    }


    private void goToFeedback() {
        Intent intent = new Intent(this, Feedback.class);

        try{Thread.sleep(4000);}catch (Exception e){
        }
        //需要传递的参数：起点，终点，总时间，障碍数量
        intent.putExtra("Travel Time", time);
        intent.putExtra("Start Point", StartPoint);
        intent.putExtra("Destination", destination);
        intent.putExtra("Obstacle", Obstacle_Count);

        Log.e("Obstacle_guide",Obstacle_Count);
        startActivity(intent);
        //Log.e("putExtras", StartPoint+EndPoint);
    }

    class getLocation extends AsyncTask<Long,Void,Long> {
        String latitude;
        String longitude;
        double mlatitude;
        String time_sugg;
        String dir_suggest;
        double mlongitude;
        float tar_latitude=0;
        float tar_longitude=0;


        protected Long doInBackground(Long...params) {
            String GoogleGeourl = "https://www.googleapis.com/geolocation/v1/geolocate?key="+api_key;
            //while()
            try {
                URL GoogleGeoUrl = new URL(GoogleGeourl);
                HttpURLConnection GeoHttp = (HttpURLConnection) GoogleGeoUrl.openConnection();
                GeoHttp.setConnectTimeout(20000);
                GeoHttp.setDoInput(true);
                GeoHttp.setDoOutput(true);
                GeoHttp.setRequestMethod("POST");
                GeoHttp.setRequestProperty("Content-Length", "0");
                GeoHttp.setRequestProperty("Content-Type", "application/json");
                GeoHttp.setRequestProperty("Host", "www.googleapis.com");

                OutputStream SendtoGoogle = GeoHttp.getOutputStream();
                SendtoGoogle.flush();
                SendtoGoogle.close();
                GeoHttp.connect();

                int responseCode = GeoHttp.getResponseCode();
                System.out.println("Res Code:" + responseCode);
                StringBuilder response = new StringBuilder();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(GeoHttp.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    System.out.println(response);
                    String Res = response.toString();
                    String result[] = Res.split(": ");
                    latitude = result[2].split(",")[0];
                    longitude = result[3].split(" \\}")[0];
                    mlatitude= Double.valueOf(latitude);
                    Log.e("latitude",latitude+"   "+longitude);
                    mlongitude=Double.valueOf(longitude);

                } else {
                    System.out.println("Error");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (Math.abs(mlatitude-tar_latitude)<0.01 && Math.abs(mlongitude-tar_longitude)<0.01){
                arrived=1;
                return arrived;
            }else{
                String GuideUrl="https://maps.googleapis.com/maps/api/directions/json?origin="+latitude+","+
                        longitude+"&destination="+destination+"&mode=walking&key="+api_key;
                try{
                    System.out.println(GuideUrl);
                    URL GoogleMapURL= new URL(GuideUrl);
                    HttpURLConnection LocHttp = (HttpURLConnection) GoogleMapURL.openConnection();
                    LocHttp.setConnectTimeout(20000);
                    LocHttp.setDoInput(true);
                    LocHttp.setDoOutput(true);
                    LocHttp.setRequestMethod("POST");
                    OutputStream SendtoGoogle = LocHttp.getOutputStream();
                    SendtoGoogle.flush();
                    SendtoGoogle.close();
                    LocHttp.connect();

                    int responseCode = LocHttp.getResponseCode();
                    System.out.println("Location Res Code:" + responseCode);
                    StringBuilder response2 = new StringBuilder();
                    if (responseCode == 200) {
                        BufferedReader in2 = new BufferedReader(
                                new InputStreamReader(LocHttp.getInputStream()));
                        String inputLine;
                        while ((inputLine = in2.readLine()) != null){
                            response2.append(inputLine);
                        }
                        in2.close();
                        String Res = response2.toString();
                        String des[]=Res.split("end_location");
                        String des_lat[] = des[1].split("lat");
                        String des_lng[] = des_lat[1].split("lng");
                        String result[] = des_lng[1].split("steps");
                        String api_la=des_lng[0];
                        String api_lng=result[0];
                        String des_latitude=api_la.substring(4,12);
                        String des_longitude=api_lng.substring(4,18);
                        tar_latitude=Float.valueOf(des_latitude);
                        tar_longitude=Float.valueOf(des_longitude);
                        System.out.println(tar_latitude+","+tar_longitude);
                        String next_step[]=Res.split("html_instructions");
                        String suggest=next_step[1].split(",")[0];
                        suggest=suggest.replace("\\u003c","");
                        suggest=suggest.replace("b\\u003e","");
                        dir_suggest=suggest.replace("/","");
                        dir_suggest=dir_suggest.substring(5,dir_suggest.length()-1);

                        System.out.println(dir_suggest);

                        time_sugg=next_step[0].split("duration")[1];
                        String t_sugg=time_sugg.split("text")[1];
                        time_sugg=t_sugg.split(",")[0];
                        time_sugg=time_sugg.substring(5,time_sugg.length()-1);

                        System.out.println(time_sugg);

                    }else{
                        System.out.println("Bad response");
                    }
                }catch(Exception e){
                    System.out.println("guide error");
                }
                arrived=0;
                return arrived;
            }
        }
        protected void onPostExecute(Long Result){
            System.out.println("time:"+time_sugg+",path:"+dir_suggest);
            mspeaker.allow(true);
            mspeaker.speak("time:"+time_sugg+",path:"+dir_suggest);
        }
    }

    public void sendBtMsg(String msg2send) {
        //UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID
        try {

            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            //if (!mmSocket.isConnected()) {
            if (mmSocket != null){
                mmSocket.connect();
                Log.e("Socket Name",msg2send);
            }

            String msg = msg2send;
            //msg += "\n";
            OutputStream mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(msg.getBytes());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        mspeaker.destroy();
        arrived = 1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mspeaker.destroy();
        arrived = 1;
    }

}
