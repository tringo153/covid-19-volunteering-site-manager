package com.example.s3818520_assignment2;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private DatabaseManager dbManager;
    private EditText tUsername;
    private EditText tPassword;
    private TextView usernameWarning;
    private TextView passwordWarning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbManager = new DatabaseManager(this);
        dbManager.open();
        usernameWarning = (TextView) findViewById(R.id.textUsernameLogWarn);
        passwordWarning = (TextView) findViewById(R.id.textPassLogWarn);

        tUsername = (EditText) findViewById(R.id.signUsername);
        tPassword = (EditText) findViewById(R.id.signPassword);

        usernameWarning.setText("");
        passwordWarning.setText("");
    }

    @SuppressLint("SetTextI18n")
    public void logIn(View v) {
        String password = tPassword.getText()+"";
        String username = tUsername.getText()+"";



        int errorCounter = 0;

        if (username.equals("")) {
            usernameWarning.setText("* Empty");
            Toast.makeText(LoginActivity.this, "Please fill in the input fields!", Toast.LENGTH_SHORT).show();
            errorCounter++;
        } else {
            usernameWarning.setText("");
        }

        if (password.equals("")) {
            passwordWarning.setText("* Empty");
            Toast.makeText(LoginActivity.this, "Please fill in the input fields!", Toast.LENGTH_SHORT).show();

            errorCounter++;
        } else {
            passwordWarning.setText("");
        }

        if (errorCounter == 0) {
            usernameWarning.setText("");
            passwordWarning.setText("");
            String isSignedUp = dbManager.checkUser(tUsername.getText()+"", tPassword.getText()+"");

            if (isSignedUp != null) {
                Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                String[] data = isSignedUp.split(",");
                Intent intent = new Intent(LoginActivity.this, MapsActivity.class);
                intent.putExtra("name", ""+data[1]);
                intent.putExtra("user_id", data[0]);
                intent.putExtra("super_user", data[2]);
                startActivity(intent);
            } else {

                Toast.makeText(LoginActivity.this, "Incorrect username or password!", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void signInActivityTravel(View view) {
        Intent intent = new Intent(LoginActivity.this, SignInActivity.class);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                if(data.getExtras().get("username") != null) {
                    String username = data.getExtras().get("username").toString();
                    tUsername.setText(username, TextView.BufferType.EDITABLE);
                    Toast.makeText(LoginActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

}