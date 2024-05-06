package com.example.s3818520_assignment2;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class DatabaseManager {
    private DatabaseHelper dbHelper;

    private Context context;
    private SQLiteDatabase database;

    public DatabaseManager(Context c) {
        context = c;
    }

    public DatabaseManager open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close(){
        dbHelper.close();
    }

    @SuppressLint("Range")
    public String checkUser(String username, String password) {

        String userData;
        Cursor cursor = database.rawQuery("select * from "+DatabaseHelper.TABLE_USER+" WHERE "+DatabaseHelper.USERNAME+" = ? AND "+DatabaseHelper.PASSWORD+" = ?", new String[] {username, password}, null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            userData = cursor.getString(0);
            userData += ","+cursor.getString(1);
            userData += ","+cursor.getString(4);
            cursor.close();
            return userData;
        } else {
            return null;
        }

    }

    public boolean checkExistingUsername(String username) {
        Cursor cursor = database.rawQuery("select * from "+DatabaseHelper.TABLE_USER+" WHERE "+DatabaseHelper.USERNAME+" = ?", new String[] {username}, null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            cursor.close();
            return true;
        } else {
            return false;
        }
    }

    public void addUser(String newName, String newUsername, String newPassword, String superUser) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.NAME, newName);
        contentValues.put(DatabaseHelper.USERNAME, newUsername);
        contentValues.put(DatabaseHelper.PASSWORD, newPassword);
        contentValues.put(DatabaseHelper.SUPER, superUser);
        database.insert(DatabaseHelper.TABLE_USER,null,contentValues);
    }

    public int addSite(String newTitle, String leadId, String newLong, String newLat, String testedPeople) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.TITLE, newTitle);
        contentValues.put(DatabaseHelper.LEADER, leadId);
        contentValues.put(DatabaseHelper.LATITUDE, newLat);
        contentValues.put(DatabaseHelper.LONGITUDE, newLong);
        contentValues.put(DatabaseHelper.TESTED_PEOPLE, testedPeople);

        return (int) database.insert(DatabaseHelper.TABLE_SITE,null,contentValues);
    }

    public int updateSite(int id, String title, String leadId, String lat, String longitude, String testedPeople) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.TITLE, title);
        contentValues.put(DatabaseHelper.LEADER, leadId);
        contentValues.put(DatabaseHelper.LATITUDE, lat);
        contentValues.put(DatabaseHelper.LONGITUDE, longitude);
        contentValues.put(DatabaseHelper.TESTED_PEOPLE, testedPeople);

        return database.update(DatabaseHelper.TABLE_SITE,
                contentValues,
                DatabaseHelper.SITE_ID + " = " + id, null);
    }

    public void addVolunteer(String userId, String siteId, String friend) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.USER, userId);
        contentValues.put(DatabaseHelper.SITE, siteId);
        contentValues.put(DatabaseHelper.FRIEND, friend);

        database.insert(DatabaseHelper.TABLE_VOLUNTEER,null,contentValues);
    }

    public void addNotification(String ownerId, String message) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.OWNER, ownerId);
        contentValues.put(DatabaseHelper.CONTENT, message);

        database.insert(DatabaseHelper.TABLE_NOTIFICATION, null, contentValues);
    }

    public Cursor fetchNotification(String ownerId) {
        Cursor cursor = database.rawQuery("select * from "+DatabaseHelper.TABLE_NOTIFICATION+" WHERE "+DatabaseHelper.OWNER+" = ?",new String[] {ownerId}, null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            return cursor;
        } else {
            return null;
        }
    }

    public void deleteNotification(String id) {
        database.delete(DatabaseHelper.TABLE_NOTIFICATION, DatabaseHelper.NOTI_ID+" = "+id, null);

        database.delete("SQLITE_SEQUENCE", "NAME = ?", new String[] {DatabaseHelper.TABLE_NOTIFICATION});
    }

    public Cursor fetchAllUser() {
        String[] columns = new String[] {
                DatabaseHelper.USER_ID,
                DatabaseHelper.NAME,
                DatabaseHelper.USERNAME,
                DatabaseHelper.PASSWORD,
                DatabaseHelper.SUPER };

        Cursor cursor = database.query(DatabaseHelper.TABLE_USER, columns,
                null, null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor fetchOneUser(String id) {

        Cursor cursor = database.rawQuery("select * from "+DatabaseHelper.TABLE_USER+" WHERE "+DatabaseHelper.USER_ID+" = ?",new String[] {id}, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor fetchAllSite() {
        String[] columns = new String[] {
                DatabaseHelper.SITE_ID,
                DatabaseHelper.TITLE,
                DatabaseHelper.LEADER,
                DatabaseHelper.LATITUDE,
                DatabaseHelper.LONGITUDE,
                DatabaseHelper.TESTED_PEOPLE
        };

        Cursor cursor = database.query(DatabaseHelper.TABLE_SITE, columns,
                null,null,null,null,null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor fetchAllVolunteer() {
        String[] columns = new String[] {
                DatabaseHelper.VOLUNTEER_ID,
                DatabaseHelper.USER,
                DatabaseHelper.SITE,
                DatabaseHelper.FRIEND
        };

        Cursor cursor = database.query(DatabaseHelper.TABLE_VOLUNTEER, columns,
                null,null,null,null,null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public ArrayList<String> fetchSitesJoined(String id) {

        ArrayList<String> data = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from "+DatabaseHelper.TABLE_VOLUNTEER+" WHERE "+DatabaseHelper.USER+" = ?",new String[] {id}, null);

        if (cursor.getCount() > 0) {
            try {
                while (cursor.moveToNext()) {
                    data.add(cursor.getString(2));
                }
            } finally {
                cursor.close();

            };

        }
        return data;
    }

    public Cursor fetchVolunteers(String id) {

        Cursor cursor = database.rawQuery("select "+DatabaseHelper.TABLE_USER+"."+DatabaseHelper.USER_ID+", "+DatabaseHelper.NAME+", "+DatabaseHelper.FRIEND+" from "+DatabaseHelper.TABLE_VOLUNTEER+", "+DatabaseHelper.TABLE_USER+" " +
                "WHERE "+DatabaseHelper.SITE+" = ? "+" " +
                "AND "+DatabaseHelper.TABLE_USER+"."+DatabaseHelper.USER_ID+" = "+DatabaseHelper.TABLE_VOLUNTEER+"."+DatabaseHelper.USER+";",new String[] {id}, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor searchSiteResult(String input, String filter) {

        Cursor cursor;

        if (filter.equals("title")) {
            cursor = database.rawQuery("select "+DatabaseHelper.TABLE_SITE+"."+DatabaseHelper.SITE_ID+", "+DatabaseHelper.TITLE+", "+DatabaseHelper.LATITUDE+", "+DatabaseHelper.LONGITUDE+", "+DatabaseHelper.TABLE_USER+"."+DatabaseHelper.NAME+" from "+DatabaseHelper.TABLE_SITE+", "+DatabaseHelper.TABLE_USER+" " +
                    "WHERE "+DatabaseHelper.TITLE+" LIKE ? "+
                    "AND "+DatabaseHelper.TABLE_SITE+"."+DatabaseHelper.LEADER+" = "+DatabaseHelper.TABLE_USER+"."+DatabaseHelper.USER_ID+";", new String[] {"%"+input+"%"}, null);

        } else {
            cursor = database.rawQuery("select "+DatabaseHelper.TABLE_SITE+"."+DatabaseHelper.SITE_ID+", "+DatabaseHelper.TITLE+", "+DatabaseHelper.LATITUDE+", "+DatabaseHelper.LONGITUDE+", "+DatabaseHelper.TABLE_USER+"."+DatabaseHelper.NAME+" from "+DatabaseHelper.TABLE_SITE+", "+DatabaseHelper.TABLE_USER+" " +
                    "WHERE "+DatabaseHelper.NAME+" LIKE ? "+
                    "AND "+DatabaseHelper.TABLE_SITE+"."+DatabaseHelper.LEADER+" = "+DatabaseHelper.TABLE_USER+"."+DatabaseHelper.USER_ID+";", new String[] {"%"+input+"%"}, null);
        }

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            return cursor;
        } else {
            return null;
        }

    }

}
