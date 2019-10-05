package com.kerian.mile3;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.ClipData;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.estimote.mustard.rx_goodness.rx_requirements_wizard.Requirement;
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory;
import com.estimote.proximity_sdk.api.EstimoteCloudCredentials;
import com.estimote.proximity_sdk.api.ProximityObserver;
import com.estimote.proximity_sdk.api.ProximityObserverBuilder;
import com.estimote.proximity_sdk.api.ProximityZone;
import com.estimote.proximity_sdk.api.ProximityZoneBuilder;
import com.estimote.proximity_sdk.api.ProximityZoneContext;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

public class MainActivity extends AppCompatActivity {

    private ProximityObserver proximityObserver;

    private TextView SensorNumber, sectorLetter;
    private Button scanningButton;
    private boolean isScanning = false;
    private ImageView player1;
    private ImageView player2;

    private boolean player2isPlaying = false;

    public void setPlayer2isPlaying (boolean play) { player2isPlaying = play; }
    public boolean getPlayer2isPlaying(){ return player2isPlaying; }
    private Player2update p2Update = new Player2update();


    //timer things for replay
    long startTime;
    long delay;
    Timer p2Timer = new Timer();
    private Button toggleP2;

    //arraylist to be populate by array of noteObject class
    ArrayList<NoteObject> taskList = new ArrayList();
    //

    //Piece countdown timer stuff
    Button pieceCountDown;
    private boolean pieceUnderway = false;
    private TextView countDownDisplay, p2CountdownDisplay;

    P2CountDown p2countdown = new P2CountDown();

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    //@TODO Need to upload taskList to FireBase so can pull on play on multiple devices.
    //@TODO Send sensor data from phone to FireBase for player 1 to pull notes for multiple devices
    //@TODO Create a piece timer, set to 5 minutes after clicking a button that says start piece or something. Timer runs and sends finished taskList to FireBase for next performance. use CountDownTimer class, and display timer on phone for all!
    //@TODO need is playing bool in Database to trigger all devices to start playing from DB.


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //layout stuff
        SensorNumber = findViewById(R.id.SectorNumber);
        sectorLetter = findViewById(R.id.scoreObj);
        scanningButton = findViewById(R.id.toggleScanning);

        //image things
        player1 = findViewById(R.id.Player1);
        player2 = findViewById(R.id.Player2);

        //
        startTime = System.currentTimeMillis();
        toggleP2 = findViewById(R.id.toggleP2);

        //piece countdown timer stuff
        pieceCountDown = findViewById(R.id.pieceCountdown);
        countDownDisplay = findViewById(R.id.countDownTimer);

                p2CountdownDisplay = findViewById(R.id.P2countDownTimer);

                //DB things
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference P1Ref = database.getReference("P1");

        //database on change listener
        P1Ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                GenericTypeIndicator<ArrayList<Integer>> t = new GenericTypeIndicator<ArrayList<Integer>>() {};
                ArrayList<Integer> stringArray = snapshot.getValue(t);
                Log.d("app", "Value is: " + stringArray);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.d("app", "Failed to read value.", error.toException());
            }
        });


        EstimoteCloudCredentials cloudCredentials =
                new EstimoteCloudCredentials("mile3interactiveposition-its", "2902b03c7ce2cd2fb8c4b52412587829");

        this.proximityObserver = new ProximityObserverBuilder(getApplicationContext(), cloudCredentials)
                .onError(new Function1<Throwable, Unit>() {
                    @Override
                    public Unit invoke(Throwable throwable) {
                        Log.e("app", "proximity observer error: " + throwable);
                        return null;
                    }
                })
                .withBalancedPowerMode()
                .build();

        final ProximityZone zone = new ProximityZoneBuilder().forTag("sector").inNearRange()
                .onContextChange(new Function1<Set<? extends ProximityZoneContext>, Unit>() {
                        @Override
                    public Unit invoke(Set<? extends ProximityZoneContext> contexts) {
                            if(isScanning) {
                                List<Integer> sectors = new ArrayList<>();
                                for (ProximityZoneContext context : contexts) {
                                     sectors.add(Integer.parseInt(context.getAttachments().get("sector")));
                                }
                                //sort list for bool check in sectorCombo method
                                Collections.sort(sectors);
                                Log.d("app", "In range of sectors: " + sectors);
                                SensorNumber.setText(getResources().getString(R.string.SectorNumber, sectors.toString()));
                                //this be the meat
                                sectorCombo(sectors);

//                            //database Stuff
                                //Uncomment to send to database.


                            P1Ref.setValue(sectors);
                            }
                                return null;
                    }
                }).build();

        RequirementsWizardFactory
                .createEstimoteRequirementsWizard()
                .fulfillRequirements(this,
                        // onRequirementsFulfilled
                        new Function0<Unit>() {
                            @Override
                            public Unit invoke() {
                                Log.d("app", "requirements fulfilled");
                                proximityObserver.startObserving(zone);
                                return null;
                            }
                        },
                        // onRequirementsMissing
                        new Function1<List<? extends Requirement>, Unit>() {
                            @Override
                            public Unit invoke(List<? extends Requirement> requirements) {
                                Log.e("app", "requirements missing: " + requirements);
                                return null;
                            }
                        },
                        // onError
                        new Function1<Throwable, Unit>() {
                            @Override
                            public Unit invoke(Throwable throwable) {
                                Log.e("app", "requirements error: " + throwable);
                                return null;
                            }
                        });

    }



    @Override
    protected void onResume() {
        super.onResume();
        isScanning = false;
    }

    public void buttonClick(View view){
        switch(view.getId()){
            case R.id.toggleScanning:
                toggleScanning();
                break;
            case R.id.toggleP2:
                togglePlayer2();
                break;
            case R.id.pieceCountdown:
                toggleScanning();

                //
                //this'd be where you grab player 2 notes from database and pass it into method to excecute it.
                //play it during the time that startPiece() is on
//for now just checking if task list is empty, if not, player 2 goes for it.

                    togglePlayer2();

                //begin the countdown for the piece.
                startPiece();
                break;
        }
    }

    //button methods


    private void togglePlayer2() {
        if(!player2isPlaying){
            if(!taskList.isEmpty()){
                p2countdown.start();
            }
            player2isPlaying = true;
            new Player2update().execute();
            toggleP2.setText(R.string.stopP2);
        } else {
            player2isPlaying = false;
            toggleP2.setText(R.string.startP2);
            pieceCountDown.setEnabled(true);
            p2countdown.cancel();
        }
    }

    private void toggleScanning () {
        if (!isScanning) {
            isScanning = true;
            scanningButton.setText(getResources().getString(R.string.stopScanning));
            System.out.println("on");
            startTime = System.currentTimeMillis();
        } else {
            isScanning = false;
            scanningButton.setText(getResources().getString(R.string.startScanning));
            System.out.println("off");
        }
    }

    private void startPiece() {
        if(!pieceUnderway) {
            pieceUnderway = true;
            pieceCountDown.setText(R.string.pieceInSession);
            pieceCountDown.setEnabled(false);
            startTime = System.currentTimeMillis();
            //make this its own method, and pass things to it?
            new CountDownTimer(20000, 1000) {
                int counter;
                @Override
                public void onTick(long l) {
                    countDownDisplay.setText(String.valueOf(counter));
                    counter++;
                }
                @Override
                public void onFinish() {
                    countDownDisplay.setText("Please Leave the room.");
                    pieceCountDown.setText(R.string.beginPiece);
                    pieceUnderway = false;
                    toggleScanning();
                    if(taskList.isEmpty()){
                        pieceCountDown.setEnabled(true);
                    }
                }
            }.start();
        }
    }

    //
    //piece logic
    //
    private void sectorCombo(List<Integer> sectors) {
        String sectorArea;
        int notes = 0; //gets resID for svg.

        boolean[] s = {sectors.contains(1), sectors.contains(2), sectors.contains(3), sectors.contains(4)};
//convert to switch statement, set a local var to string of sector and drawable, extract delay times from here.
        if(s[0] && !s[1] && !s[2] && !s[3]){
            sectorArea = "a";
            notes = R.drawable.sc_1;
        } else if(s[0] && s[1] && !s[2] && !s[3]){
            sectorArea = "b";
            notes = R.drawable.sc_2;
        } else if(!s[0] && s[1] && !s[2] && !s[3]){
            sectorArea = "c";
            notes = R.drawable.sc_3;
        }  else if(!s[0] && s[1] && s[2] && !s[3]) {
            sectorArea = "d";
            notes = R.drawable.sc_4;
        }  else if(!s[0] && !s[1] && s[2] && !s[3]) {
            sectorArea = "e";
            notes = R.drawable.sc_5;
        }  else if(!s[0] && !s[1] && s[2] && s[3]){
            sectorArea = "f";
            notes = R.drawable.sc_6;
        } else if(!s[0] && !s[1] && !s[2] && s[3]) {
            sectorArea = "g";
            notes = R.drawable.sc_7;
        } else if(!s[0] && s[1] && s[2] && s[3]) {
            sectorArea = "h";
            notes = R.drawable.sc_8;
        } else if(s[0] && !s[1] && s[2] && s[3]) {
            sectorArea = "i";
            notes = R.drawable.sc_9;
        } else if(s[0] && s[1] && !s[2] && s[3]) {
            sectorArea = "j";
            notes = R.drawable.sc_10;
        } else if(s[0] && s[1] && s[2] && !s[3]){
            sectorArea = "l";
            notes = R.drawable.sc_11;
        } else if(s[0] && !s[1] && s[2] && !s[3]) {
            sectorArea = "m";
            notes = 0;
            //notes = R.drawable.
        } else if(s[0] && !s[1] && !s[2] && s[3]) {
            sectorArea = "n";
            notes = 0;
        } else if(!s[0] && s[1] && !s[2] && s[3]) {
            sectorArea = "o";
            notes = 0;
        } else if(!s[0] && !s[1] && !s[2] && !s[3]) {
            sectorArea = "all off";
            notes = 0;
        } else if(s[0] && s[1] && s[2] && s[3]){
            sectorArea = "all on";
            notes = 0;
        } else {
            sectorArea = "undefined";
            notes = 0;
        }

        //Player 2 timer playback things
        delay = System.currentTimeMillis() - startTime;
        startTime = System.currentTimeMillis();

        //update Player 1
        sectorLetter.setText(sectorArea);
        player1.setImageResource(notes);
        //
        taskList.add(new NoteObject(notes, delay));

    }


    private void player2Play() {
        Log.d("app", "p2play trigger");
        if(!taskList.isEmpty()) {
            Log.d("app", "tasklist check true");
//            player2isPlaying = false;
            //timer event off task list, event ends with setting player2On to true, so next event can be timered
            DelayTasks p2NextNote = new DelayTasks(taskList.get(0).getSvg());
            //p2NextNote.setScoreObj(taskList.get(0).getSvg());
            //
            p2Timer.schedule(p2NextNote, taskList.get(0).getDelay());
            taskList.remove(0);
        } else{
            Log.d("app", "no scores");
            togglePlayer2();
        }
    }

    private class Player2update extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d("app","thread running");
            if(getPlayer2isPlaying()) {
                player2Play();
                Log.d("app" , " " + this.isCancelled());
            }
            if(taskList.isEmpty()){
                p2countdown.onFinish();
            }
            return null;
        }
        protected Void onPostExcecute() {
            Log.d("app", "post execute player2Play");
            return null;
        }
    }


    /////////////
// delayTask class to run with timer instance for the sector code.
//To be used along with delay time, calculated in sectorCombo method to repeat previous events in player2's image view
    class DelayTasks extends TimerTask {
        int scoreObj;

        public DelayTasks(int scoreObj) {
            setScoreObj(scoreObj);
        }

        public void setScoreObj(int task) {
            this.scoreObj = task;
        }

        public int getScoreObj() {
            return scoreObj;
        }

        @Override
        public void run() {
            //this is where the sector event gets passed to the timer object for player2 imageView, after a delay
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    player2.setImageResource(scoreObj);
                }
            });
            Log.d("app", "P2 End:");
            if (!taskList.isEmpty()) {
                //recursive call to method until end of taskList is found
                player2Play();
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        togglePlayer2();
                    }
                });
            }
        }
    }

    private class P2CountDown extends CountDownTimer{
        int counter;

        public P2CountDown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }



        public P2CountDown() {
            super(60000,1000);
            counter =0;
        }

        @Override
        public void onTick(long l) {
            if (taskList.isEmpty()) {
                this.onFinish();
            }
            p2CountdownDisplay.setText(String.valueOf(counter));
            counter++;
        }

        @Override
        public void onFinish() {
            p2CountdownDisplay.setText("End");
        }
    }


    //end of main
}

//////
//note Object, contains svg note int and delay time
//created to have an array containing both int note object and long delay time for DelayTasks
//////
class NoteObject {
    int svg;
    long delay;

    public NoteObject (int svg, long delay){
        this.svg = svg;
        this.delay = delay;
        Log.d("app", svg + " " + delay);
    }

    public void setSvg(int svg) {
        this.svg = svg;
    }

    public int getSvg() {
        return svg;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public long getDelay() {
        return delay;
    }

    public String toString(){
        return "svg: " + svg + "; delay" + delay;
    }
}





//////Backup code


/* on enter and exit code, could be useful for triggering events, on estimote builder

                .onEnter(new Function1<ProximityZoneContext, Unit>() {

                    @Override
            public Unit invoke(ProximityZoneContext context) {
                String sector = context.getAttachments().get("sector");
                Log.d("app", "This is sector number " + sector);
                SensorNumber.setText(getResources().getString(R.string.SectorNumber, sector));
                return null;
            }
        })
                .onExit(new Function1<ProximityZoneContext, Unit>() {
                            @Override
                            public Unit invoke(ProximityZoneContext proximityZoneContext) {
                                Log.d("app", "left sector.");
                                SensorNumber.setText("none");

                                return null;
                            }
                        }
                )
                .build();
*/