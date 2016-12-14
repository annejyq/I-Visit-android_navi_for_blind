package com.example.anne.testvisit;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class TripInformation extends AppCompatActivity {

    TextView EstimatedTimeArrival, EstimatedObstacleCount, BlindFriendly, CompanyRec;
    Button StartMyTrip;
    String StartPoint, EndPoint;
    boolean GetSuggestionFlag = true;
    Speaker speaker;
    int starting_hour, starting_minutes;
    float start_time;

    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = MainActivity.mmDevice;

    final byte delimiter =33;
    int readBufferPosition = 0;

    Calendar c = Calendar.getInstance();

    long time1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trip_information);

        EstimatedTimeArrival = (TextView) findViewById(R.id.estitime);
        EstimatedObstacleCount = (TextView) findViewById(R.id.estiobstacle);
        BlindFriendly = (TextView) findViewById(R.id.friendly);
        CompanyRec = (TextView) findViewById(R.id.company);

        StartMyTrip = (Button) findViewById(R.id.starttrip);

        speaker = new Speaker(this);

        final Handler handler = new Handler();

        final class workerThread implements Runnable {

            private String btMsg;

            public workerThread(String msg) {
                btMsg = msg;
            }

            public void run() {
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
                                    readBufferPosition = 0;

                                    //The variable data now contains our full command
                                    handler.post(new Runnable() {
                                        public void run() {
                                            Log.d("ACK of Bluetooth Server", data);
                                        }
                                    });

                                    workDone = true;
                                    break;

                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }

                            if (workDone) {
                                mmSocket.close();
                                break;
                            }

                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }
        }


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            StartPoint = extras.getString("Start_from");
            EndPoint = extras.getString("Destination");
        }

        StartMyTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speaker.allow(true);
                speaker.speak("Long click to start navigation instructions. ");
            }
        });

        StartMyTrip.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                (new Thread(new workerThread("start"))).start();

                time1 = System.currentTimeMillis();
                Log.e("time1", String.valueOf(time1));

                goToLocation();
                return true;
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        if (GetSuggestionFlag) {

            // Query Mongo DB for Travel Suggestion
            new GetSuggestion().execute(StartPoint, EndPoint);
            GetSuggestionFlag = false;
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        speaker.destroy();
    }

    private void goToLocation() {

        Intent intent = new Intent(this, Guide.class);

        Log.e("start-time",Float.toString(start_time));

        //Parameters passed to Guide: Source, Destination, Start_Time
        intent.putExtra("Start Point", StartPoint);
        intent.putExtra("Destination", EndPoint);
        intent.putExtra("Start Time", start_time);
        intent.putExtra("Current Time", time1);
        //intent.putExtra("Travel Time", "0");

        startActivity(intent);

    }

    int i=0;

    class GetSuggestion extends AsyncTask<String, Void, String> {
        String Time_Of_Arrival;
        String Obstacle;
        String FriendlyPercentage;
        String IndependencyPercentage;


        protected String doInBackground(String... params) {
            String Cloud_Url = "http://ec2-54-214-131-233.us-west-2.compute.amazonaws.com/suggestions";
            String source = params[0];
            String destination = params[1];
            String Result = "";
            try {
                URL CloudUrl = new URL(Cloud_Url);
                HttpURLConnection CloudHttp = (HttpURLConnection) CloudUrl.openConnection();
                CloudHttp.setConnectTimeout(20000);
                CloudHttp.setDoInput(true);
                CloudHttp.setDoOutput(true);
                CloudHttp.setRequestMethod("POST");
                CloudHttp.setRequestProperty("Content-Type", "application/json");

                String Source_Destination = "{" + '"' + "Source" + '"' + ":" + '"' + source + '"' + ", " + '"' + "Destination" + '"' + ":" + '"' + destination + '"' + "}";

                Log.d("Body Data", Source_Destination);

                CloudHttp.setFixedLengthStreamingMode(Source_Destination.getBytes().length);
                OutputStream SendtoCloud = CloudHttp.getOutputStream();

                PrintWriter out = new PrintWriter(SendtoCloud);
                out.print(Source_Destination);
                out.close();

                int responseCode = CloudHttp.getResponseCode();
                System.out.println("Response Code of EC2 Cloud: " + responseCode);
                StringBuilder response = new StringBuilder();

                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(CloudHttp.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    Result = response.toString();
                    Log.e("Response Content", Result);

                } else {
                    Log.e("Response", "Error");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return Result;
        }

        protected void onPostExecute(String Response) {

            boolean flag = false;

            speaker.allow(true);

            String result[] = Response.split(": ");

            Obstacle = result[2].split(",")[0];
            if (Obstacle.equals("\"NA\"")) {
                Obstacle = "Not Applicable";
                EstimatedObstacleCount.setText("N/A");
            } else {
                Obstacle = Obstacle.split("\"")[1];
                Obstacle = String.valueOf(Math.round(Float.valueOf(Obstacle)));
                EstimatedObstacleCount.setText(Obstacle);
            }

            String Blind = result[3].split(",")[0];
            if (Blind.equals("\"NA\"")) {
                BlindFriendly.setText("N/A");
                flag = true;
            } else {
                String FriendlyNumber = result[3].split(",")[0];
                FriendlyNumber = FriendlyNumber.split("\"")[1];
                FriendlyNumber = String.valueOf(Double.valueOf(FriendlyNumber) * 100);
                System.out.print(FriendlyNumber);
                FriendlyPercentage = String.valueOf(FriendlyNumber) + "%";
                BlindFriendly.setText(FriendlyPercentage);
            }

            String Time = result[4].split(",")[0];
            if (Time.equals("\"NA\"")) {
                Time_Of_Arrival = "Not applicable";
                EstimatedTimeArrival.setText("N/A");
            } else {
                String Timevalue = result[4].split(",")[0];
                Timevalue = Timevalue.split("\"")[1];
                int TimeValue = Math.round(Float.valueOf(Timevalue));
                Time_Of_Arrival = TimeValue + " minutes";
                EstimatedTimeArrival.setText(Time_Of_Arrival);

            }

            String Independent = result[5].split(" ")[0];
            if (Independent.equals("\"NA\"")) {
                CompanyRec.setText("N/A");
            } else {
                String Independency = Independent.split("\"")[1];
                Independency = String.valueOf(Double.valueOf(Independency) * 100);
                System.out.print(Independency);
                IndependencyPercentage = Independency + "%";
                CompanyRec.setText(IndependencyPercentage);
            }

            String timeofestimatebygoogle = Time_Of_Arrival;

            String say1 = "The estimated time of arrival is" + Time_Of_Arrival;
            String say2 = "The estimated number of obstacle is" + Obstacle;
            String say3 = FriendlyPercentage + "people think this trip is blind friendly";
            String say4 = IndependencyPercentage + "people think they can finish the trip independently";


            if (flag) {
                new SaySomething().execute("Sorry, no one has traveled the same route. "+
                        "The estimated time of arrival suggested by google is" + timeofestimatebygoogle+ "." +
                        "The other information need to be collected.");
                Log.e("try","try");
                flag = false;
            } else {
                speakInformation(say2, say1, say3, say4);
            }

        }
    }

    public void speakInformation(String s1, String s2, String s3, String s4) {
        final String[] say=new String[5];
        say[0] = null;
        say[1] = s1;
        say[2] = s2;
        say[3] = s3;
        say[4] = s4;
        final Handler handler = new Handler();
        Timer timer = new Timer();


        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if (i < 5) {
                            try {
                                TripInformation.SaySomething performBackgroundTask = new TripInformation.SaySomething();
                                // PerformBackgroundTask this class is the class that extends AsynchTask
                                Log.e("value of i",String.valueOf(i));
                                performBackgroundTask.execute(say[i]);
                                i = i + 1;
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                            }
                        }
                        /*else {
                            //FinishTrip.setEnabled(true);
                            Log.e("button", "enabled");
                        }*/
                    }

                    });
                }
            };

            timer.schedule(doAsynchronousTask, 0, 5000);
        }

        //execute in every 50000 ms



    class SaySomething extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... params) {
            String information = params[0];
            speaker.allow(true);
            speaker.speak(information);
                return information;
        }
        protected void onPostExecute(String str) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
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


}
