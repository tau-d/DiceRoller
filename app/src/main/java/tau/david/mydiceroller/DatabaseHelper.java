package tau.david.mydiceroller;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String LOG_TAG = "DatabaseHelper";

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "my_dice_roller_database";

    private static final String TABLE_DICE_SET = "dice_set";
    private static final String COL_SET_ID = "_id"; // REFERENCED FOREIGN KEY
    private static final String COL_SET_NAME = "set_name";
    //private static final String COL_DICE_SET_DESC = "dice_set_desc";

    private static final String TABLE_DICE_ROLL = "dice_roll";
    private static final String COL_ROLL_ID = "_id";
    private static final String COL_ROLL_SET = "set_id"; // FOREIGN KEY
    private static final String COL_ROLL_NUM_DICE = "num_dice";
    private static final String COL_ROLL_DICE_SIZE = "dice_size";
    private static final String COL_ROLL_MODIFIER = "modifier";
    private static final String COL_ROLL_OPTIONS = "options";

    private static final String TABLE_CREATE_DICE_SET =
            "CREATE TABLE " + TABLE_DICE_SET + " (" +
                    COL_SET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_SET_NAME + " TEXT UNIQUE NOT NULL" +
                ");";

    private static final String TABLE_CREATE_DICE_ROLL =
            "CREATE TABLE " + TABLE_DICE_ROLL + " (" +
                    COL_ROLL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_ROLL_SET + " INT NOT NULL, " +
                    COL_ROLL_NUM_DICE + " INT NOT NULL, " +
                    COL_ROLL_DICE_SIZE + " INT NOT NULL, " +
                    COL_ROLL_MODIFIER + " INT NOT NULL, " +
                    COL_ROLL_OPTIONS + " INT NOT NULL, " +
                    "FOREIGN KEY (" + COL_ROLL_SET + ") REFERENCES " + TABLE_DICE_SET + "(" + COL_SET_ID + ")" +
                ");";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    public static Cursor getAllDiceRollsInSet(SQLiteDatabase db, Cursor diceSetCursor) {
        long setId = diceSetCursor.getLong(diceSetCursor.getColumnIndex(COL_SET_ID));

        String[] cols = { COL_ROLL_NUM_DICE, COL_ROLL_DICE_SIZE, COL_ROLL_MODIFIER, COL_ROLL_OPTIONS };
        String selection = COL_ROLL_SET + " = ?";
        String[] selectionArgs = { Long.toString(setId) };

        return db.query(TABLE_DICE_ROLL, cols, selection, selectionArgs, null, null, null);
    }

    public static Cursor getDiceSets(SQLiteDatabase db) {
        String[] cols = { COL_SET_ID, COL_SET_NAME };
        return db.query(TABLE_DICE_SET, cols, null, null, null, null, null);
    }

    public static void saveDiceSet(SQLiteDatabase db, String setName, List<DiceRoll> diceRolls) {
        db.beginTransaction();

        ContentValues diceSetValues = new ContentValues();
        diceSetValues.put(COL_SET_NAME, setName);
        long setId = db.insert(TABLE_DICE_SET, null, diceSetValues);

        if (setId == -1) {
            Log.d(LOG_TAG, "Transaction failed: error inserting the dice set");
            db.endTransaction();
            return;
        }

        for (DiceRoll roll : diceRolls) {
            ContentValues rollValues = new ContentValues();
            rollValues.put(COL_ROLL_SET, setId);
            rollValues.put(COL_ROLL_NUM_DICE, roll.getNumDice());
            rollValues.put(COL_ROLL_DICE_SIZE, roll.getDiceSize());
            rollValues.put(COL_ROLL_MODIFIER, roll.getModifier());
            rollValues.put(COL_ROLL_OPTIONS, roll.getOptions());
            long rowId = db.insert(TABLE_DICE_ROLL, null, rollValues);
            if (rowId == -1) {
                Log.d(LOG_TAG, "Transaction failed: error inserting a dice roll\n" + rollValues);
                db.endTransaction();
                return;
            }
        }

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public static void deleteDiceSet(SQLiteDatabase db, Cursor diceSetCursor) {
        long setId = diceSetCursor.getLong(diceSetCursor.getColumnIndex(COL_SET_ID));
        db.beginTransaction();

        String whereClause = COL_ROLL_SET + " = ?";
        String[] whereArgs = { Long.toString(setId) };
        db.delete(TABLE_DICE_ROLL, whereClause, whereArgs);

        whereClause = COL_SET_ID + " = ?";
        db.delete(TABLE_DICE_SET, whereClause, whereArgs);

        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public static void deleteDatabase(Context context) {
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
    }

    public static DiceRoll cursorToDiceRoll(Cursor c) {
        return new DiceRoll(c.getInt(c.getColumnIndex(COL_ROLL_NUM_DICE)),
                c.getInt(c.getColumnIndex(COL_ROLL_DICE_SIZE)),
                c.getInt(c.getColumnIndex(COL_ROLL_MODIFIER)),
                c.getLong(c.getColumnIndex(COL_ROLL_OPTIONS))
            );
    }

    public static String cursorToSetName(Cursor c) {
        return c.getString(c.getColumnIndex(COL_SET_NAME));
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_DICE_SET);
        db.execSQL(TABLE_CREATE_DICE_ROLL);
        //onCreateTest(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "Upgrading database");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DICE_ROLL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DICE_SET);
        onCreate(db);
    }

    // Testing methods
    public static Cursor getAllDiceRollsInSet(SQLiteDatabase db) {
        Cursor c = db.query(TABLE_DICE_ROLL, null, null, null, null, null, null);
        while (c.moveToNext()) {
            Log.d(LOG_TAG, "" + c.getPosition());
            Log.d(LOG_TAG, c.toString());
        }
        return c;
    }

    private void onCreateTest(SQLiteDatabase db) {
        Log.d(LOG_TAG, TABLE_CREATE_DICE_SET);
        Log.d(LOG_TAG, TABLE_CREATE_DICE_ROLL);

        ContentValues testDiceSet = new ContentValues();
        testDiceSet.put(COL_SET_NAME, "Test Dice Set");
        long rowId = db.insert(TABLE_DICE_SET, null, testDiceSet);

        Log.d(LOG_TAG, "row id: " + rowId);

        ContentValues testRoll1 = new ContentValues();
        testRoll1.put(COL_ROLL_SET, rowId);
        testRoll1.put(COL_ROLL_NUM_DICE, 2);
        testRoll1.put(COL_ROLL_DICE_SIZE, 6);
        testRoll1.put(COL_ROLL_MODIFIER, -1);
        rowId = db.insert(TABLE_DICE_ROLL, null, testRoll1);

        Log.d(LOG_TAG, "row id: " + rowId);

        ContentValues testRoll2 = new ContentValues();
        testRoll2.put(COL_ROLL_SET, rowId);
        testRoll2.put(COL_ROLL_NUM_DICE, 3);
        testRoll2.put(COL_ROLL_DICE_SIZE, 4);
        testRoll2.put(COL_ROLL_MODIFIER, 9);
        rowId = db.insert(TABLE_DICE_ROLL, null, testRoll2);

        Log.d(LOG_TAG, "row id: " + rowId);
    }
}
