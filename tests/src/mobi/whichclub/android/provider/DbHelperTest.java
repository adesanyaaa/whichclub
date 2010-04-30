/**
 * 
 */
package mobi.whichclub.android.provider;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import mobi.whichclub.android.data.Ball;
import mobi.whichclub.android.data.Club;
import mobi.whichclub.android.data.Course;
import mobi.whichclub.android.data.Hole;
import mobi.whichclub.android.data.Player;
import mobi.whichclub.android.data.Round;
import mobi.whichclub.android.data.Shot;
import mobi.whichclub.android.provider.DbHelper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

/**
 * @author cdale
 *
 */
public class DbHelperTest extends AndroidTestCase {
    
    private static final String TAG = "DbHelperTest";
    private DbHelper helper;
    private SQLiteDatabase db;
    
    public void deleteDatabase() {
        File databaseFile = DbHelper.getDatabaseFile(getContext());
        Log.d(TAG, "Attempting to delete database: " + databaseFile);
        if (databaseFile.exists()) {
            if (databaseFile.delete()) {
                Log.i(TAG, "Deleted the database: " + databaseFile);
            } else {
                Log.w(TAG, "Failed to delete the database: " + databaseFile);
            }
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteDatabase();
        helper = new DbHelper(getContext());
        db = helper.getWritableDatabase();
    }

    @Override
    protected void tearDown() throws Exception {
        Log.w(TAG, "tearDown");
        if (db != null) {
            db.close();
            db = null;
        }
        if (helper != null) {
            helper.close();
            helper = null;
        }
        deleteDatabase();
        super.tearDown();
    }

    @SmallTest
    public void testOpenDatabase() {
        Log.w(TAG, "Opening the database");
    }

    @SmallTest
    public void testPerformance() {
        long startTime = System.nanoTime();
        Random rand = new Random();

        Cursor cursor = db.query(Ball.TABLE_NAME, new String[] {Ball._ID}, null, null, null, null, null);
        cursor.moveToFirst();
        long ballId = cursor.getLong(0);
        cursor.close();
        
        cursor = db.query(Player.TABLE_NAME, new String[] {Player._ID}, null, null, null, null, null);
        cursor.moveToFirst();
        long playerId = cursor.getLong(0);
        cursor.close();
        
        cursor = db.query(Course.TABLE_NAME, new String[] {Course._ID}, null, null, null, null, null);
        cursor.moveToFirst();
        long courseId = cursor.getLong(0);
        cursor.close();
        
        cursor = db.query(Club.TABLE_NAME, new String[] {Club._ID}, null, null, null, null, null);
        List<Long> clubIds = new ArrayList<Long>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            clubIds.add(cursor.getLong(0));
            cursor.moveToNext();
        }
        cursor.close();
        
        cursor = db.query(Hole.TABLE_NAME, new String[] {Hole._ID}, null, null, null, null, null);
        List<Long> holeIds = new ArrayList<Long>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            holeIds.add(cursor.getLong(0));
            cursor.moveToNext();
        }
        cursor.close();

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(Round.COURSE, courseId);
            values.put(Round.PLAYER, playerId);
            values.put(Round.DATE, new Date().getTime());
            long roundId = db.insert(Round.TABLE_NAME, null, values);
    
            for (int i = 1; i < 1000; i++) {
                values.clear();
                values.put(Shot.DISTANCE, rand.nextDouble()*400);
                values.put(Shot.BALL, ballId);
                values.put(Shot.CLUB, clubIds.get(rand.nextInt(clubIds.size())));
                values.put(Shot.END_LATITUDE, (rand.nextDouble() - 0.5)*180);
                values.put(Shot.END_LONGITUDE, rand.nextDouble()*360);
                values.put(Shot.HOLE, holeIds.get(rand.nextInt(holeIds.size())));
                values.put(Shot.LATERAL, (rand.nextDouble() - 0.5)*100);
                values.put(Shot.NUMBER, (i % 7) + 1);
                values.put(Shot.ROUND, roundId);
                values.put(Shot.START_LATITUDE, (rand.nextDouble() - 0.5)*180);
                values.put(Shot.START_LONGITUDE, rand.nextDouble()*360);
                values.put(Shot.WIND_DIR, rand.nextDouble()*360);
                values.put(Shot.WIND_SPEED, rand.nextDouble()*50);
                db.insert(Shot.TABLE_NAME, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        
        db.close();
        
        Log.d(TAG, "Initializing took: " + (System.nanoTime() - startTime));
        startTime = System.nanoTime();

        db = helper.getWritableDatabase();
        cursor = db.rawQuery("SELECT description, count(" + Shot.DISTANCE + "), sum(" + Shot.DISTANCE + ") FROM "
                + Shot.TABLE_NAME + " s LEFT JOIN " + Club.TABLE_NAME + " c ON s." + Shot.CLUB + " = c." + Club._ID + " GROUP BY c." + Club._ID, null);
        cursor.moveToFirst();
        Log.d(TAG, "Number of rows returned: " + cursor.getCount());
        while (!cursor.isAfterLast()) {
            clubIds.add(cursor.getLong(0));
            Log.d(TAG, "Club: " + cursor.getString(0) + "  Shots: " + cursor.getLong(1) + "  Avg Distance: " + cursor.getDouble(2));
            cursor.moveToNext();
        }
        Log.d(TAG, "Calculating took: " + (System.nanoTime() - startTime));
        cursor.close();
    }

}