package com.example.anne.testvisit;

import android.content.Intent;
import android.os.AsyncTask;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class Feedback extends AppCompatActivity implements RecognitionListener {

    Speaker speaker;
    SpeechRecognizer mSpeechRecognizer;

    Button FriendlyQuestion, CompanyQuestion;
    TextView Completed;

    boolean Question1_Enable, Question2_Enable = false;
    boolean Question1_Answered, Question2_Answered = false;

    String destination, StartPoint, TravelTime, ObstacleCount;
    String SendResult, Question1_Result, Question2_Result;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback);

        speaker = new Speaker(this);

        FriendlyQuestion = (Button) findViewById(R.id.friendlyquestion);
        CompanyQuestion = (Button) findViewById(R.id.companyquestion);
        Completed = (TextView) findViewById(R.id.missioncomplete);
        //TestAnswer = (TextView) findViewById(R.id.testanswer);

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(this);



        FriendlyQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speaker.allow(true);
                String Question1 = "Do you think this is a blind friendly trip? Answer yes or no with long click";
                speaker.speak(Question1);
            }
        });

        CompanyQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speaker.allow(true);
                String Question2 = "Do you think you can complete the trip independently? Answer yes or no with long click";
                speaker.speak(Question2);

            }
        });

        //just for testing ObstacleCount="0";

    }

    @Override
    protected void onStart() {
        super.onStart();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            destination = extras.getString("Destination");
            StartPoint = extras.getString("Start Point");
            TravelTime = extras.getString("Travel Time");
            ObstacleCount = extras.getString("Obstacle");
            Log.e("Traveltime_feedback",TravelTime);
            Log.e("ObstacleCount_feedback", ObstacleCount);

            //Log.e("source&destination", StartPoint+EndPoint);
        }

        FriendlyQuestion.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Question1_Enable = true;
                switch (v.getId()) {
                    case R.id.friendlyquestion:
                        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

                        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

                        //Start Recognizing
                        mSpeechRecognizer.startListening(intent);
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        CompanyQuestion.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Question2_Enable = true;
                switch (v.getId()) {
                    case R.id.companyquestion:
                        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

                        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

                        //Start Recognizing
                        mSpeechRecognizer.startListening(intent);
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speaker.destroy();
        Destroy();
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Toast.makeText(this, "Ready to Record", Toast.LENGTH_SHORT);
    }

    @Override
    public void onBeginningOfSpeech() {
        Toast.makeText(this, "Start Recording", Toast.LENGTH_SHORT);
    }

    @Override
    public void onEndOfSpeech() {
        Toast.makeText(this, "End Recording", Toast.LENGTH_SHORT);
    }

    //System Data Record
    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.v("DEBUG","onBufferReceived");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.v("DEBUG","receive : " + rmsdB + "dB");
    }

    //Error
    @Override
    public void onError(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                Toast.makeText(this, "Fail to save audio", Toast.LENGTH_LONG).show();
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                Toast.makeText(this, "Android Client Error", Toast.LENGTH_LONG).show();
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                Toast.makeText(this, "Fail to Match", Toast.LENGTH_LONG).show();
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                Toast.makeText(this, "No Input!", Toast.LENGTH_LONG).show();
                break;
            default:
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.v("DEBUG","onEvent");
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.v("DEBUG","onPartialResults");
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> recData = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        final String[] str_items = recData.toArray(new String[recData.size()]);
        SendResult = str_items[0];
        if (Question1_Enable) {
            Question1_Result = SendResult;
            //TestStart.setText(StartPoint);
            Question1_Answered = true;
            Question1_Enable = false;
        } else if (Question2_Enable) {
            Question2_Result = SendResult;
            //TestEnd.setText(EndPoint);
            Question2_Answered = true;
            Question2_Enable = false;
        }
        if(Question2_Answered && Question1_Answered) {
            new SendFeedback().execute(StartPoint, destination, TravelTime,
                    ObstacleCount, Question1_Result, Question2_Result);
        }

        System.out.println(SendResult);

        for (int i=0; i<str_items.length; i++) {
            System.out.println(str_items[i]);
        }
    }

    public void Destroy() {
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.stopListening();
        }
    }



    class SendFeedback extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... params) {
            String Cloud_Url = "http://ec2-54-214-131-233.us-west-2.compute.amazonaws.com/feedback";
            String source = params[0];
            String destination = params[1];
            String journeytime = params[2];
            String obstaclecount = params[3];
            String blindfriend = params[4];
            String independent = params[5];
            String blindfriendly, independency = "";

            if (blindfriend.equals("yes")) {
                blindfriendly = "1";
            } else {
                blindfriendly = "0";
            }

            if (independent.equals("yes")) {
                independency = "1";
            } else {
                independency = "0";
            }

            String Result = "";
            try {
                URL CloudUrl = new URL(Cloud_Url);
                HttpURLConnection CloudHttp = (HttpURLConnection) CloudUrl.openConnection();
                CloudHttp.setConnectTimeout(20000);
                CloudHttp.setDoInput(true);
                CloudHttp.setDoOutput(true);
                CloudHttp.setRequestMethod("POST");
                CloudHttp.setRequestProperty("Content-Type", "application/json");

                String Body_Data = "{" + '"' + "Source" + '"' + ":" + '"' + source + '"' + ", " + '"' + "Destination" + '"' + ":" + '"' + destination + '"' + ", " +
                        '"' + "JourneyTime" + '"' + ":" + '"' + journeytime + '"' + ", " + '"' + "ObstacleCount" + '"' + ":" + '"' + obstaclecount + '"' + ", " +
                        '"' + "BlindFriendly" + '"' + ":" + '"' + blindfriendly + '"' + ", " + '"' + "Independency" + '"' + ":" + '"' + independency + '"'+ "}";

                Log.d("Body Data", Body_Data);

                CloudHttp.setFixedLengthStreamingMode(Body_Data.getBytes().length);
                OutputStream SendtoCloud = CloudHttp.getOutputStream();

                PrintWriter out = new PrintWriter(SendtoCloud);
                out.print(Body_Data);
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
            Completed.setText("Thank you for using our application!");
            speaker.allow(true);
            speaker.speak("Thank you for using our application");
        }
    }


}
