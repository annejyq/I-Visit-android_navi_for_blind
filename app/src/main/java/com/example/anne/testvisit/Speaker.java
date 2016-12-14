package com.example.anne.testvisit;

/**
 * Created by yunqingjiang on 16/12/2.
 */

import java.util.Locale;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.content.Context;


public class Speaker implements OnInitListener {

    private TextToSpeech mSpeech;
    private boolean ready = false;
    private boolean allowed = false;

    public Speaker(Context context) {
        mSpeech = new TextToSpeech(context, this);
    }

    public boolean isAllowed(){
        return allowed;
    }

    public void allow(boolean allowed){
        this.allowed = allowed;
    }



    public void onInit(int status) {
        // TODO Auto-generated method stub
        if (status == TextToSpeech.SUCCESS) {
            mSpeech.setLanguage(Locale.ENGLISH);
            ready = true;
            //String utteranceId = this.hashCode() + "";
            //mSpeech.speak("Welcome", TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        } else {
            ready = false;
        }
    }

    public void speak(String text){
        if(ready&&allowed) {
            String utteranceId = this.hashCode() + "";
            mSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        }
    }

    public void pause(int duration){
        mSpeech.playSilentUtterance(duration, TextToSpeech.QUEUE_ADD, null);
    }

    public void destroy(){
        mSpeech.shutdown();

    }

}


