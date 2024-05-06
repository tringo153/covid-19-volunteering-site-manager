package com.example.s3818520_assignment2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "s3818520.DB";
    private static final int DB_VERSION = 1;

    //Table name
    public static final String TABLE_USER = "User";
    public static final String TABLE_SITE = "Site";
    public static final String TABLE_VOLUNTEER = "Volunteer";
    public static final String TABLE_NOTIFICATION = "Notification";

    //Columns for user table
    public static final String USER_ID = "_id";
    public static final String NAME = "name";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String SUPER = "super";

    //Columns for site table
    public static final String SITE_ID = "_id";
    public static final String TITLE = "title";
    public static final String LEADER = "leader_id";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String TESTED_PEOPLE = "tested_people";

    //Columns for volunteer table (Many to many relationship between user and site)
    public static final String VOLUNTEER_ID = "_id";
    public static final String USER = "user_id";
    public static final String SITE = "site_id";
    public static final String FRIEND = "friend";

    //Columns for site notification
    public static final String NOTI_ID = "_id";
    public static final String OWNER = "owner_id";
    public static final String CONTENT = "content";

    //Query for creating table
    private static final String CREATE_USER_TABLE =
            "create table "+ TABLE_USER +"("+
                    USER_ID +" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    NAME +" TEXT NOT NULL," +
                    USERNAME +" TEXT NOT NULL," +
                    PASSWORD +" TEXT NOT NULL," +
                    SUPER +" INTEGER NOT NULL "+");";

    private static final String CREATE_SITE_TABLE =
            "create table "+ TABLE_SITE +"("+
                    SITE_ID +" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    TITLE +" TEXT NOT NULL," +
                    LEADER +" INTEGER NOT NULL," +
                    LATITUDE +" DOUBLE," +
                    LONGITUDE +" DOUBLE," +
                    TESTED_PEOPLE +" INTEGER," +
                    "FOREIGN KEY("+LEADER+") REFERENCES "+TABLE_USER+"("+USER_ID+") );";

    private static final String CREATE_VOLUNTEER_TABLE =
            "create table "+ TABLE_VOLUNTEER +"("+
                    VOLUNTEER_ID +" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    USER +" INTEGER NOT NULL," +
                    SITE +" INTEGER NOT NULL," +
                    FRIEND +" TEXT," +
                    "FOREIGN KEY("+USER+") REFERENCES "+TABLE_USER+"("+USER_ID+")," +
                    "FOREIGN KEY("+SITE+") REFERENCES "+TABLE_SITE+"("+SITE_ID+") );";

    private static final String CREATE_NOTIFICATION_TABLE =
            "create table "+ TABLE_NOTIFICATION +"("+
                    NOTI_ID +" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    OWNER +" INTEGER NOT NULL," +
                    CONTENT +" TEXT," +
                    "FOREIGN KEY("+OWNER+") REFERENCES "+TABLE_USER+"("+USER_ID+") );";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_SITE_TABLE);
        db.execSQL(CREATE_VOLUNTEER_TABLE);
        db.execSQL(CREATE_NOTIFICATION_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_USER);
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_SITE);
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_VOLUNTEER);
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_NOTIFICATION);

        onCreate(db);
    }
}
