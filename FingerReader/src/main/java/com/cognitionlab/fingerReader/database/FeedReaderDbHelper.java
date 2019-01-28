package com.cognitionlab.fingerReader.database;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FeedReaderDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "mydb";
    public static final String ASSETS_PATH = "databases";
    private final String DB_PATH;
    private final Context myContext;
    private SQLiteDatabase myDataBase;

    public FeedReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.myContext = context;
        this.DB_PATH = "/data/data/com.cognitionlab.fingerReader/databases/";
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over

        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public synchronized void close() {

        if (myDataBase != null)
            myDataBase.close();

        super.close();

    }

    private void installDatabaseFromAssets(Context context) {
        System.out.println("Database Creation Started.");
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = context.getAssets().open(ASSETS_PATH + File.separator + DATABASE_NAME + ".sqlite3");
            File outputFile = context.getDatabasePath(DATABASE_NAME);
            outputStream = new FileOutputStream(outputFile);

            int c;
            int counter = 0;
            int val = 0;
            while ((c = inputStream.read()) != -1) {
                outputStream.write(c);

                counter++;
                if (counter == 1000000) {
                    System.out.println(val++);
                    counter = 0;
                }
            }

            inputStream.close();

            outputStream.flush();
            outputStream.close();
        } catch (Exception exception) {
            throw new RuntimeException("The DATABASE_NAME database couldn't be installed.", exception);
        } finally {


        }

        System.out.println("Database Creation completed.");
    }

    public void createDataBase() throws IOException {

        boolean dbExist = checkDataBase();

        if (dbExist ) {
            //do nothing - database already exist
        } else {

            //By calling this method and empty database will be created into the default system path
            //of your application so we are gonna be able to overwrite that database with our database.
            this.getReadableDatabase();

            try {

                installDatabaseFromAssets(myContext);

            } catch (Exception e) {

                throw new Error("Error copying database");

            }
        }

    }

    private boolean checkDataBase() {

        SQLiteDatabase checkDB = null;

        try {
            String myPath = DB_PATH + DATABASE_NAME;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);

        } catch (SQLiteException e) {

            //database does't exist yet.

        }

        if (checkDB != null) {

            checkDB.close();

        }

        return checkDB != null ? true : false;
    }

    public void openDataBase() throws SQLException {

        //Open the database
        String myPath = DB_PATH + DATABASE_NAME;
        myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);

    }

    public SQLiteDatabase getDatabase() {
        return myDataBase;
    }
}
