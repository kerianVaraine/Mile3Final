package com.kerian.mile3;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.estimote.mustard.rx_goodness.rx_requirements_wizard.Requirement;
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory;
import com.estimote.proximity_sdk.api.EstimoteCloudCredentials;
import com.estimote.proximity_sdk.api.ProximityObserver;
import com.estimote.proximity_sdk.api.ProximityObserverBuilder;
import com.estimote.proximity_sdk.api.ProximityZone;
import com.estimote.proximity_sdk.api.ProximityZoneBuilder;
import com.estimote.proximity_sdk.api.ProximityZoneContext;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

public class MainActivity extends AppCompatActivity {

    private ProximityObserver proximityObserver;

    private TextView SensorNumber;
    private Button scanningButton;
    private boolean isScanning = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //layout stuff
        SensorNumber = findViewById(R.id.SectorNumber);

        scanningButton = findViewById(R.id.toggleScanning);


        //

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

                        //
                        //IF SCANNING ON!!!!!
                        //
                                List<String> sectors = new ArrayList<>();
                            for (ProximityZoneContext context : contexts) {
                                sectors.add(context.getAttachments().get("sector"));
                            }

                            Log.d("app", "In range of sectors: " + sectors);
                            SensorNumber.setText(getResources().getString(R.string.SectorNumber, sectors.toString()));
//
//                            //database Stuff
                            //Uncomment to send to database.

//                            FirebaseDatabase database = FirebaseDatabase.getInstance();
//                            DatabaseReference myRef = database.getReference("P1");
//                            myRef.setValue(sectors);
                            
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

    }

    public void buttonClick(View view){
        switch(view.getId()){
            case R.id.toggleScanning:
                if(!isScanning){
                    isScanning = true;
                    scanningButton.setText(getResources().getString(R.string.stopScanning));
                    System.out.println("on");

                    break;
                }else {
                    isScanning = false;
                    scanningButton.setText(getResources().getString(R.string.startScanning));
                    System.out.println("off");
                    break;
                }
        }
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

/*
                                //Converts from digit to room; my house.
                                String s = context.getAttachments().get("sector");
                                String room;
                                switch(s) {
                                    case ("1"):
                                        room = "K's Computer";
                                        break;
                                    case("2"):
                                        room = "Hallway";
                                        break;
                                    case("3"):
                                        room = "K's Room";
                                        break;
                                    case("4"):
                                        room = "Tui's Room";
                                        break;
                                    default:
                                        room = "none";
                                }
                                sectors.add(room);
*/