package tau.david.mydiceroller;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

class DatabaseHelper {

    private static final String LOG_TAG = "DatabaseHelper";

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "my_dice_roller_database";

    private static final String TABLE_DICE_SET = "dice_set";
    private static final String COL_SET_ID = "_id"; // REFERENCED FOREIGN KEY
    private static final String COL_SET_NAME = "set_name";
    // private static final String COL_DICE_SET_DESC = "dice_set_desc";

    private static final String TABLE_DICE_ROLL = "dice_roll";
    private static final String COL_ROLL_ID = "_id";
    private static final String COL_ROLL_SET = "set_id"; // FOREIGN KEY
    private static final String COL_ROLL_NUM_DICE = "num_dice";
    private static final String COL_ROLL_DICE_SIZE = "dice_size";
    private static final String COL_ROLL_MODIFIER = "modifier";
    private static final String COL_ROLL_OPTIONS = "options";
    private static final String COL_ROLL_COLOR = "color";

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
                    COL_ROLL_COLOR + " INT NOT NULL, " +
                    "FOREIGN KEY (" + COL_ROLL_SET + ") REFERENCES " + TABLE_DICE_SET + "(" + COL_SET_ID + ")" +
                ");";


    private MyOpenHelper openHelper;

    DatabaseHelper(Context context) {
        openHelper = new MyOpenHelper(context);
    }


    // Returns set_id with given set_name or -1 if there is no such set
    long isDiceSetNameUsed(String setName) {
        SQLiteDatabase db = openHelper.getReadableDatabase();

        String selection = COL_SET_NAME + " = ?";
        String[] selectionArgs = { setName };

        Cursor c = db.query(TABLE_DICE_SET, null, selection, selectionArgs, null, null, null);

        long id = -1;
        if (c.getCount() > 0) {
            c.moveToNext();
            id = c.getLong(c.getColumnIndex(COL_SET_ID));
        }

        c.close();
        db.close();

        return id;
    }

    List<DiceRoll> getAllDiceRollsInSet(DiceSet diceSet) {
        SQLiteDatabase db = openHelper.getReadableDatabase();

        String[] cols = { COL_ROLL_NUM_DICE, COL_ROLL_DICE_SIZE, COL_ROLL_MODIFIER, COL_ROLL_OPTIONS, COL_ROLL_COLOR};
        String selection = COL_ROLL_SET + " = ?";
        String[] selectionArgs = { Long.toString(diceSet.id) };

        Cursor c = db.query(TABLE_DICE_ROLL, cols, selection, selectionArgs, null, null, null);

        List<DiceRoll> list = new ArrayList<>(c.getCount());
        while (c.moveToNext()) {
            list.add(cursorToDiceRoll(c));
        }

        c.close();
        db.close();

        return list;
    }

    List<DiceSet> getDiceSets() {
        SQLiteDatabase db = openHelper.getReadableDatabase();

        String[] cols = { COL_SET_ID, COL_SET_NAME };
        String orderBy = COL_SET_NAME + " COLLATE NOCASE ASC";

        Cursor c = db.query(TABLE_DICE_SET, cols, null, null, null, null, orderBy);

        List<DiceSet> list = new ArrayList<>(c.getCount());
        while (c.moveToNext()) {
            list.add(cursorToDiceSet(c));
        }

        c.close();
        db.close();

        return list;
    }

    void saveDiceSet(String setName, List<DiceRoll> diceRolls) {
        SQLiteDatabase db = openHelper.getWritableDatabase();

        db.beginTransaction();

        ContentValues diceSetValues = new ContentValues();
        diceSetValues.put(COL_SET_NAME, setName);
        long setId = db.insert(TABLE_DICE_SET, null, diceSetValues);

        if (setId == -1) {
            Log.d(LOG_TAG, "Save dice set transaction failed: error inserting the dice set");
            db.endTransaction();
            return;
        }

        if (!insertDiceRolls(db, setId, diceRolls)) {
            Log.d(LOG_TAG, "Save dice set transaction failed");
            db.endTransaction();
            return;
        }

        db.setTransactionSuccessful();
        db.endTransaction();

        db.close();
    }

    void overwriteDiceSet(long duplicateSetId, List<DiceRoll> diceRolls) {
        SQLiteDatabase db = openHelper.getWritableDatabase();

        db.beginTransaction();

        deleteDiceRolls(db, duplicateSetId);

        if (!insertDiceRolls(db, duplicateSetId, diceRolls)) {
            db.endTransaction();
            Log.d(LOG_TAG, "Dice set overwrite transaction aborted");
            return;
        }

        db.setTransactionSuccessful();
        db.endTransaction();

        db.close();
    }

    void deleteDiceSet(DiceSet diceSet) {
        SQLiteDatabase db = openHelper.getWritableDatabase();

        db.beginTransaction();

        deleteDiceRolls(db, diceSet.id);

        String whereClause = COL_SET_ID + " = ?";
        String[] whereArgs = { Long.toString(diceSet.id) };
        db.delete(TABLE_DICE_SET, whereClause, whereArgs);

        db.setTransactionSuccessful();
        db.endTransaction();

        db.close();
    }

    // Returns true if there were no errors, returns false if there was an error
    private static boolean insertDiceRolls(SQLiteDatabase db, long setId, List<DiceRoll> diceRolls) {
        for (DiceRoll roll : diceRolls) {
            ContentValues rollValues = new ContentValues();
            rollValues.put(COL_ROLL_SET, setId);
            rollValues.put(COL_ROLL_NUM_DICE, roll.getNumDice());
            rollValues.put(COL_ROLL_DICE_SIZE, roll.getDiceSize());
            rollValues.put(COL_ROLL_MODIFIER, roll.getModifier());
            rollValues.put(COL_ROLL_OPTIONS, roll.getOptions());
            rollValues.put(COL_ROLL_COLOR, roll.getColor());

            long rowId = db.insert(TABLE_DICE_ROLL, null, rollValues);
            if (rowId == -1) {
                Log.d(LOG_TAG, "Error inserting dice roll:\n" + rollValues);
                return false;
            }
        }
        return true;
    }

    private static void deleteDiceRolls(SQLiteDatabase db, long setId) {
        String whereClause = COL_ROLL_SET + " = ?";
        String[] whereArgs = { Long.toString(setId) };
        db.delete(TABLE_DICE_ROLL, whereClause, whereArgs);
    }

    static void deleteDatabase(Context context) {
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
    }

    static DiceRoll cursorToDiceRoll(Cursor c) {
        return new DiceRoll(c.getInt(c.getColumnIndex(COL_ROLL_NUM_DICE)),
                c.getInt(c.getColumnIndex(COL_ROLL_DICE_SIZE)),
                c.getInt(c.getColumnIndex(COL_ROLL_MODIFIER)),
                c.getLong(c.getColumnIndex(COL_ROLL_OPTIONS)),
                c.getInt(c.getColumnIndex(COL_ROLL_COLOR))
            );
    }

    static DiceSet cursorToDiceSet(Cursor c) {
        return new DiceSet(c.getString(c.getColumnIndex(COL_SET_NAME)),
                c.getLong(c.getColumnIndex(COL_SET_ID))
            );
    }


    // Testing methods
    List<DiceRoll> getAllDiceRolls() {
        SQLiteDatabase db = openHelper.getReadableDatabase();

        Cursor c = db.query(TABLE_DICE_ROLL, null, null, null, null, null, null);

        List<DiceRoll> list = new ArrayList<>(c.getCount());
        while (c.moveToNext()) {
            Log.d(LOG_TAG, "" + c.getPosition());
            Log.d(LOG_TAG, c.toString());
            list.add(cursorToDiceRoll(c));
        }

        c.close();
        db.close();

        return list;
    }

    private void onCreateTest() {
        SQLiteDatabase db = openHelper.getWritableDatabase();

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

        db.close();
    }


    private class MyOpenHelper extends SQLiteOpenHelper {

        public MyOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
    }
}
