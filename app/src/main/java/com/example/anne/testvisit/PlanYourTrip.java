package com.example.anne.testvisit;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
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


import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class PlanYourTrip extends AppCompatActivity  implements  RecognitionListener {

    Button start, end, GotoSuggestion;
    Speaker speaker;
    SpeechRecognizer mSpeechRecognizer;
    String SendResult;                  //Result of Speech Recognition
    String StartPoint, EndPoint;
    TextView TestStart, TestEnd;
    boolean StartResult, EndResult = false;
    int i = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.planyourtrip);

        start = (Button) findViewById(R.id.start);
        end = (Button) findViewById(R.id.end);
        GotoSuggestion = (Button) findViewById(R.id.gotosuggestion);

        TestStart = (TextView) findViewById(R.id.teststart);
        TestEnd = (TextView) findViewById(R.id.testend);

        speaker = new Speaker(this);

        //Initialize Speech Recognizer
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(this);

    }

    @Override
    protected void onStart() {
        super.onStart();

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speaker.allow(true);
                String StartFrom = "Long Click to provide your start point";
                speaker.speak(StartFrom);
            }
        });

        end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speaker.allow(true);
                String EndFrom = "Long Click to provide your destination";
                speaker.speak(EndFrom);
            }
        });

        start.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                StartResult = true;
                switch (v.getId()) {
                    case R.id.start:
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

        end.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                EndResult = true;
                switch (v.getId()) {
                    case R.id.end:
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

        GotoSuggestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speaker.allow(true);
                String say1 = "You are going from" + StartPoint + "to" + EndPoint+".";
                String say2 = "Long click to get suggestions.";
                speaker.speak(say1+say2);

                }

            });


        GotoSuggestion.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                goToTripInformation();
                return true;
            }
        });

    }

    /*Method speakInformation and Class SayAnything work together

    to create time interval between instructions of trip information*/



    private void goToTripInformation() {
        Intent intent = new Intent(this, TripInformation.class);

        //Parameters passed to TripInfomation: Source, Destination
        intent.putExtra("Start_from", StartPoint);
        intent.putExtra("Destination", EndPoint);
        startActivity(intent);
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


        if (StartResult) {
            StartPoint = SendResult;
            TestStart.setText(StartPoint);
            StartResult = false;
        } else if (EndResult) {
            EndPoint = SendResult;
            TestEnd.setText(EndPoint);
            EndResult = false;
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
}

