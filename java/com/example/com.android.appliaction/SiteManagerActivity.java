package com.example.s3818520_assignment2;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SiteManagerActivity extends AppCompatActivity {

    private DatabaseManager dbManager;
    private TextView leaderText;
    private TextView latText;
    private TextView longText;
    private EditText titleEdit;
    private EditText testedNum;
    private TextView headerText;

    private String leaderId;
    private String leaderName;
    private String latitude;
    private String longitude;
    private String siteId;
    private String originalTitle;
    private String originalTestedNum;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_manager);

        leaderText = (TextView) findViewById(R.id.textViewLeader);
        latText = (TextView) findViewById(R.id.textViewLat);
        longText = (TextView) findViewById(R.id.textViewLong);

        testedNum = (EditText) findViewById(R.id.resTested);
        headerText = (TextView) findViewById(R.id.textViewTitle);
        titleEdit = (EditText) findViewById(R.id.resTitle);

        dbManager = new DatabaseManager(this);
        dbManager.open();

        Intent intentGet = getIntent();

        //Change the layout of the activity depending on whether the intent is from add or update method
        if (intentGet.getExtras().get("mode") != null) {
            headerText.setText("Update Site");
            titleEdit.setText(""+intentGet.getExtras().get("site_title"));
            testedNum.setText(""+intentGet.getExtras().get("tested_people"));
            siteId = intentGet.getExtras().get("site_id").toString();
            originalTitle = ""+intentGet.getExtras().get("site_title");
            originalTestedNum = ""+intentGet.getExtras().get("tested_people");
            setVisible(R.id.resTested, true);
            setVisible(R.id.textViewTested, true);
            setVisible(R.id.buttonIncrease, true);
            setVisible(R.id.buttonDecrease, true);
            setVisible(R.id.buttonConfirmUpdate, true);
            setVisible(R.id.buttonConfirm, false);
        } else {
            headerText.setText("Add New Site");
            setVisible(R.id.resTested, false);
            setVisible(R.id.textViewTested, false);
            setVisible(R.id.buttonIncrease, false);
            setVisible(R.id.buttonDecrease, false);
            setVisible(R.id.buttonConfirmUpdate, false);
            setVisible(R.id.buttonConfirm, true);
        }

        leaderText.setText(""+intentGet.getExtras().get("leader_name"));
        latText.setText(""+intentGet.getExtras().get("latitude"));
        longText.setText(""+intentGet.getExtras().get("longitude"));

        leaderId = ""+intentGet.getExtras().get("leader_id");
        leaderName = ""+intentGet.getExtras().get("leader_name");
        latitude = ""+intentGet.getExtras().get("latitude");
        longitude = ""+intentGet.getExtras().get("longitude");

    }

    public void increaseNumber(View view) {
        int num = Integer.parseInt(testedNum.getText()+"")+1;

        testedNum.setText(String.valueOf(num));
    }

    public void decreaseNumber(View view) {
        int num = Integer.parseInt(testedNum.getText()+"");

        if (num > 0) {
            num = num -1;
            testedNum.setText(String.valueOf(num));
        }
    }

    public void onAddLocation(View view) {

        if ((titleEdit.getText()+"").equals("")) {
            Toast.makeText(SiteManagerActivity.this, "Field must not be empty!", Toast.LENGTH_SHORT).show();
        } else {
            int addedId = dbManager.addSite(titleEdit.getText()+"",leaderId , longText.getText()+"", latText.getText()+"", "0");


            Intent intent = new Intent(SiteManagerActivity.this, MapsActivity.class);
            intent.putExtra("site_id", addedId);
            intent.putExtra("leader_id", leaderId);
            intent.putExtra("title", titleEdit.getText()+"");
            intent.putExtra("leader_name", leaderName);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            setResult(RESULT_OK, intent);
            finish();
        }


    }

    public void onUpdateLocation(View view) {

        if ((titleEdit.getText()+"").equals("")) {
            Toast.makeText(SiteManagerActivity.this, "Field must not be empty!", Toast.LENGTH_SHORT).show();
        } else {
            int updatedId = dbManager.updateSite(Integer.parseInt(siteId),titleEdit.getText()+"",leaderId , latText.getText()+"", longText.getText().toString(), testedNum.getText()+"");

            Cursor cursor = dbManager.fetchVolunteers(siteId);

            //If this site has any volunteers, store updated info with their id in the notificaiton db
            if (cursor.getCount() > 0) {
                String content = "";

                if (!(""+titleEdit.getText()).equals(originalTitle) && !(""+testedNum.getText()).equals(originalTestedNum)) {
                    content = originalTitle+" site : Title has been changed to "+titleEdit.getText()+" and details has been updated";
                } else if (!(""+titleEdit.getText()).equals(originalTitle)) {
                    content = originalTitle+" site : Title has been changed to "+titleEdit.getText();
                } else if (!(""+testedNum.getText()).equals(originalTestedNum)) {
                    content = originalTitle+" site : Details of this site has been updated";
                } else {
                    content = "";
                }

                cursor.moveToPosition(-1);

                if (!content.equals("")) {
                    try {
                        while(cursor.moveToNext()) {
                            String volunteerId = cursor.getString(0);

                            dbManager.addNotification(volunteerId, content);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }



            Intent intent = new Intent(SiteManagerActivity.this, MapsActivity.class);
            intent.putExtra("site_id", updatedId);
            intent.putExtra("leader_id", leaderId);
            intent.putExtra("title", titleEdit.getText()+"");
            intent.putExtra("leader_name", leaderName);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.putExtra("tested_people", testedNum.getText()+"");
            intent.putExtra("mode", "update");
            setResult(RESULT_OK, intent);
            finish();
        }



    }

    private void setVisible(int id, boolean isVisible) {
        View aView = findViewById(id);
        if (isVisible) {
            aView.setVisibility(View.VISIBLE);
        } else {
            aView.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }


}