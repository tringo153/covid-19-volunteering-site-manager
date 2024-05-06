package com.example.s3818520_assignment2;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class SignInActivity extends AppCompatActivity {

    private DatabaseManager dbManager;

    private EditText tName;
    private EditText tUsername;
    private EditText tPassword;
    private EditText tRePassword;

    private String superUser = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        dbManager = new DatabaseManager(this);
        dbManager.open();


        tName = (EditText) findViewById(R.id.signName);
        tUsername = (EditText) findViewById(R.id.signUsername);
        tPassword = (EditText) findViewById(R.id.signPassword);
        tRePassword = (EditText) findViewById(R.id.signRePassword);
    }

    private void setVisible(int id, boolean isVisible) {
        View aView = findViewById(id);
        if (isVisible) {
            aView.setVisibility(View.VISIBLE);
        } else {
            aView.setVisibility(View.INVISIBLE);
        }

    }

    @SuppressLint("SetTextI18n")
    public void signIn(View view) {


        String password = tPassword.getText()+"";
        String rePassword = tRePassword.getText()+"";
        String name = tName.getText()+"";
        String username = tUsername.getText()+"";

        TextView nameWarning = (TextView) findViewById(R.id.textNameSignWarning);
        TextView usernameWarning = (TextView) findViewById(R.id.textUsernameSignWarning);
        TextView passwordWarning = (TextView) findViewById(R.id.textPassSignWarning);
        TextView rePassWarning = (TextView) findViewById(R.id.textRePassSignWarning);

        int errorCounter = 0;

        //Check if all the fields are empty
        if (name.equals("")) {
            nameWarning.setText("* Empty");
            errorCounter++;
        } else {
            nameWarning.setText("");
        }

        if (username.equals("")) {
            usernameWarning.setText("* Empty");
            errorCounter++;
        } else {
            if (dbManager.checkExistingUsername(username)) {
                usernameWarning.setText("* Username already taken");
                errorCounter++;
            } else {
                usernameWarning.setText("");
            }
        }

        if (password.equals("")) {
            passwordWarning.setText("* Empty");
            errorCounter++;
        } else {
            passwordWarning.setText("");
        }

        if (rePassword.equals("")) {
            rePassWarning.setText("* Empty");
            errorCounter++;
        } else {
            rePassWarning.setText("");
        }

        //Check if the password and confirm password are the same
        if (!password.equals("") && !rePassword.equals("")) {
            if (!password.equals(rePassword)) {
                passwordWarning.setText("* Unmatched");
                rePassWarning.setText("* Unmatched");
                errorCounter++;
            } else {
                passwordWarning.setText("");
                rePassWarning.setText("");
            }
        }

        if (errorCounter == 0) {


            dbManager.addUser(tName.getText()+"", tUsername.getText()+"", tPassword.getText()+"", superUser);


            Intent intent = new Intent(SignInActivity.this, LoginActivity.class);
            intent.putExtra("username", tUsername.getText()+"");
            intent.putExtra("password", tPassword.getText()+"");
            setResult(RESULT_OK, intent);
            finish();

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }

//    public void onSuperBoxClicked(View view) {
//
//        CheckBox checkBox = (CheckBox) view;
//
//        if (view.getId() == R.id.checkBoxSuper) {
//            if (checkBox.isChecked()) {
//                superUser = "1";
//                Toast.makeText(SignInActivity.this, "This user is super", Toast.LENGTH_SHORT).show();
//            } else {
//                superUser = "0";
//                Toast.makeText(SignInActivity.this, "This user is not super", Toast.LENGTH_SHORT).show();
//            }
//        }
//
//    }
}