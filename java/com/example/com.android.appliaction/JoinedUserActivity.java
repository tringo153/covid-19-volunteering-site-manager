package com.example.s3818520_assignment2;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class JoinedUserActivity extends AppCompatActivity {

    private String siteId;
    private String siteTitle;

    private DatabaseManager dbManager;
    private Cursor cursor;
    private String[] from = new String[] {
            DatabaseHelper.NAME,
            DatabaseHelper.FRIEND
    };
    private int[] to = new int[]{
            R.id.lUser,
            R.id.lFriend
    };

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joined_user);
        dbManager = new DatabaseManager(this);

        Intent intent = getIntent();
        siteId = intent.getExtras().get("site_id").toString();
        siteTitle = intent.getExtras().get("title").toString();

        dbManager.open();
        cursor = dbManager.fetchVolunteers(siteId);
        if (cursor.getCount() == 0){
            setVisible(R.id.noRecordTextSite, true);
            setVisible(R.id.listVol, false);
            setVisible(R.id.textView7, false);
            setVisible(R.id.textView8, false);
            setVisible(R.id.textView9, false);
            setVisible(R.id.buttonDownload, false);
        }
        else {
            setVisible(R.id.textView7, true);
            setVisible(R.id.textView8, true);
            setVisible(R.id.textView9, true);
            setVisible(R.id.buttonDownload, true);
            TextView tHeader = (TextView) findViewById(R.id.textView7);
            tHeader.setText("List of volunteers joining site "+siteTitle);
            ListView listView = (ListView) findViewById(R.id.listVol);
            listView.setVisibility(View.VISIBLE);
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                    this, R.layout.activity_view_joined_user, cursor, from, to, 0);
            adapter.notifyDataSetChanged();
            listView.setAdapter(adapter);

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

    public void onDownloadButtonClicked(View view) {

        String siteName = siteTitle.toLowerCase(Locale.ROOT)+"_volunteer_list";
        String file = siteName+".csv";

        StringBuilder fileContent = new StringBuilder();

        cursor.moveToPosition(-1);

        fileContent.append("Volunteer").append(",").append("Friend name").append("\n");
        try {
            while (cursor.moveToNext()) {


                fileContent.append(cursor.getString(1)).append(",").append(cursor.getString(2)).append("\n");

            }
        } finally {
            cursor.close();
        }
        String content = fileContent.toString();

        saveFile(file, content);

    }

    public void saveFile(String file, String content) {

        try {
            FileOutputStream fileOutputStream = openFileOutput(file, Context.MODE_PRIVATE);

            fileOutputStream.write(content.getBytes());
            fileOutputStream.close();
            Toast.makeText(JoinedUserActivity.this, "File has been saved to "+JoinedUserActivity.this.getFilesDir().getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}