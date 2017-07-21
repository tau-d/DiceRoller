package tau.david.mydiceroller;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

// TODO: constraint layout
// TODO: shake to roll setting
// TODO: custom dice sizes?
// TODO: use @styles
// TODO: conditional rolls (ex. normally roll 1d6+2 damage, but on 19-20 crit range, roll 2d6+2 damage)

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";

    private static final String DICE_ROLL_LIST_KEY = "DiceRollListKey";

    private LayoutInflater mInflater;
    private MyDiceRollerAdapter mDiceRollerAdapter;
    private DatabaseHelper mDatabaseOpenHelper;

    private void testDb() {
        Log.d("test", "TESTING");

        DatabaseHelper.deleteDatabase(this);

        SQLiteDatabase db = mDatabaseOpenHelper.getReadableDatabase();

        DatabaseHelper.getDiceSets(db);
        Cursor rolls = DatabaseHelper.getAllDiceRollsInSet(db);

        mDiceRollerAdapter.loadDiceSet(rolls);

        db.close();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TypedArray themeArray = MainActivity.this.getTheme().obtainStyledAttributes(new int[] {android.R.attr.editTextColor});
        int defaultTextColour = themeArray.getColor(0, 0);
        themeArray.recycle();

        DiceRoll.setDefaultColor(defaultTextColour);

        if (savedInstanceState != null) {
            mDiceRollerAdapter = new MyDiceRollerAdapter(this,
                    savedInstanceState.<DiceRoll>getParcelableArrayList(DICE_ROLL_LIST_KEY));
        } else {
            mDiceRollerAdapter = new MyDiceRollerAdapter(this);
        }

        mDatabaseOpenHelper = new DatabaseHelper(this);
        mInflater = getLayoutInflater();

        final ListView listView = (ListView) findViewById(R.id.diceRollListView);
        listView.setAdapter(mDiceRollerAdapter);

        final Button addButton = (Button) findViewById(R.id.addDiceButton);
        final Button rollButton = (Button) findViewById(R.id.rollButton);
        final Button resetButton = (Button) findViewById(R.id.resetDiceButton);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDiceRollerAdapter.add(new DiceRoll());
            }
        });

        rollButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int numItems = mDiceRollerAdapter.getCount();
                for (int i = 0; i < numItems; ++i) {
                    mDiceRollerAdapter.getItem(i).rollDice();
                }
                mDiceRollerAdapter.notifyDataSetChanged();
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDiceRollerAdapter.reset();
            }
        });
    }

    private class MyDiceRollerAdapter extends BaseAdapter {

        private ArrayList<DiceRoll> items;
        private ArrayAdapter<Integer> mSpinnerAdapter;


        public MyDiceRollerAdapter(Context context) {
            initSpinnerAdapter(context);
            items = new ArrayList<>();
            items.add(new DiceRoll());
        }

        public MyDiceRollerAdapter(Context context, ArrayList<DiceRoll> items) {
            initSpinnerAdapter(context);
            if (items == null) {
                items = new ArrayList<>();
                items.add(new DiceRoll());
            } else {
                this.items = items;
            }
        }

        private void initSpinnerAdapter(Context context) {
            int[] intDiceSizeArray = getResources().getIntArray(R.array.dice_size_array);
            mSpinnerAdapter = new ArrayAdapter<>(context, R.layout.spinner_item, integerArrayFromIntArray(intDiceSizeArray));
        }

        public void add(DiceRoll diceRoll) {
            items.add(diceRoll);
            notifyDataSetChanged();
        }

        public void remove(int position) {
            items.remove(position);
            notifyDataSetChanged();
        }

        private void reset() {
            items.clear();
            items.add(new DiceRoll());
            notifyDataSetChanged();
        }

        public void loadDiceSet(Cursor rolls) {
            items.clear();
            if (rolls.getCount() == 0) return;
            else {
                rolls.moveToFirst();
                do {
                    items.add(DatabaseHelper.cursorToDiceRoll(rolls));
                } while (rolls.moveToNext());
            }
            notifyDataSetChanged();
        }

        public ArrayList<DiceRoll> getList() {
            return items;
        }

        private class DiceRollViewHolder {
            private int currPosition;

            private EditText numDiceEditText;
            private Spinner diceSizeSpinner;
            private TextView plusTextView;
            private EditText modifierEditText;
            private TextView totalTextView;
            private TextView rollResultsTextView;

            private DiceRollViewHolder(View convertView) {
                numDiceEditText = (EditText) convertView.findViewById(R.id.numDiceEditText);
                diceSizeSpinner = (Spinner) convertView.findViewById(R.id.diceSizeSpinner);
                plusTextView = (TextView) convertView.findViewById(R.id.plusTextView);
                modifierEditText = (EditText) convertView.findViewById(R.id.modifierEditText);
                totalTextView = (TextView) convertView.findViewById(R.id.totalTextView);
                rollResultsTextView = (TextView) convertView.findViewById(R.id.rollResultsTextView);

                diceSizeSpinner.setAdapter(mSpinnerAdapter);
                diceSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int spinnerPosition, long id) {
                        Log.d(LOG_TAG, currPosition + ": Spinner item selected");
                        DiceRoll item = getCurrItem();
                        if (item == null) return;

                        int diceSize = mSpinnerAdapter.getItem(spinnerPosition);
                        if (item.getDiceSize() != diceSize) {
                            item.setDiceSize(diceSize);
                            item.resetRollsAndTotal();
                            mDiceRollerAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

                numDiceEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            Log.d(LOG_TAG, currPosition + ": Num dice lost focus");
                            DiceRoll item = getCurrItem();
                            if (item == null) return;

                            String text = ((EditText) v).getText().toString();
                            if (item.getNumDice().equals(text)) return;

                            try {
                                item.setNumDice(Integer.parseInt(text));
                            } catch (NumberFormatException e) {
                                item.setToDefaultNumDice();
                            }
                            item.resetRollsAndTotal();
                            mDiceRollerAdapter.notifyDataSetChanged();
                        }
                    }
                });

                modifierEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            Log.d(LOG_TAG, currPosition + ": Modifier lost focus");
                            DiceRoll item = getCurrItem();
                            if (item == null) return;

                            String text = ((EditText) v).getText().toString();
                            if (item.getModifier().equals(text)) return;

                            try {
                                item.setModifier(Integer.parseInt(text));
                            } catch (NumberFormatException e) {
                                item.setToDefaultModifier();
                            }
                            item.resetRollsAndTotal();
                            mDiceRollerAdapter.notifyDataSetChanged();
                        }
                    }
                });

                View.OnLongClickListener optionsOnLongClickListener = new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        openDiceRollOptionsDialog();
                        return true;
                    }
                };

                numDiceEditText.setOnLongClickListener(optionsOnLongClickListener);
                diceSizeSpinner.setOnLongClickListener(optionsOnLongClickListener);
                plusTextView.setOnLongClickListener(optionsOnLongClickListener);
                modifierEditText.setOnLongClickListener(optionsOnLongClickListener);
                totalTextView.setOnLongClickListener(optionsOnLongClickListener);
                rollResultsTextView.setOnLongClickListener(optionsOnLongClickListener);
            }

            private void openDiceRollOptionsDialog() {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.diceRollOptionsDialogTitle);

                View v = mInflater.inflate(R.layout.dice_roll_options_dialog, null);
                builder.setView(v);

                final CheckBox rerollOnesCheckBox = (CheckBox) v.findViewById(R.id.rerollOnesCheckBox);
                final CheckBox dropLowestCheckBox = (CheckBox) v.findViewById(R.id.dropLowestCheckBox);
                final CheckBox highlightMinMaxCheckBox = (CheckBox) v.findViewById(R.id.highlightMinMaxCheckBox);
                final CheckBox sortRollsCheckBox = (CheckBox) v.findViewById(R.id.sortRollsCheckBox);
                final GridView colorGridView = (GridView) v.findViewById(R.id.colorGridView);
                final Button deleteButton = (Button) v.findViewById(R.id.deleteDialogOptionButton);

                int[] textColors = MainActivity.this.getResources().getIntArray(R.array.textColors);
                Integer[] colorArray = integerArrayFromIntArray(textColors);
                colorArray[0] = DiceRoll.DEFAULT_COLOR;

                final ColorSelectorAdapter colorAdapter = new ColorSelectorAdapter(MainActivity.this, -1, colorArray);
                colorGridView.setAdapter(colorAdapter);

                DiceRoll item = getCurrItem();
                rerollOnesCheckBox.setChecked(item.rerollOnesOption());
                dropLowestCheckBox.setChecked(item.dropLowestOption());
                highlightMinMaxCheckBox.setChecked(item.highlightMinMaxOption());
                sortRollsCheckBox.setChecked(item.sortRollsOption());
                colorAdapter.setSelectedColor(item.getColor());

                builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DiceRoll item = getCurrItem();
                        long options = (rerollOnesCheckBox.isChecked() ? DiceRoll.REROLL_ONES : 0) |
                                (dropLowestCheckBox.isChecked() ? DiceRoll.DROP_LOWEST : 0) |
                                (highlightMinMaxCheckBox.isChecked() ? DiceRoll.HIGHLIGHT_MIN_MAX: 0) |
                                (sortRollsCheckBox.isChecked() ? DiceRoll.SORT_ROLLS : 0);
                        int color = colorAdapter.getSelectedColor();

                        if (item.setOptions(options) || item.setColor(color)) {
                            mDiceRollerAdapter.notifyDataSetChanged();
                        }
                    }
                });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                final AlertDialog optionsDialog = builder.create();

                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDiceRollerAdapter.remove(getCurrPos());
                        optionsDialog.dismiss();
                    }
                });

                optionsDialog.show();
            }

            @Nullable
            private DiceRoll getCurrItem() {
                // prevent strange interaction with reset button and focus changing
                if (currPosition >= mDiceRollerAdapter.getCount()) return null;
                else return mDiceRollerAdapter.getItem(currPosition);
            }

            private int getCurrPos() {
                return currPosition;
            }
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public DiceRoll getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int listItemPosition, View convertView, ViewGroup parent) {
            DiceRollViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.diceroll_list_entry, parent, false);
                holder = new DiceRollViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (DiceRollViewHolder) convertView.getTag();
            }
            holder.currPosition = listItemPosition; // must update list position in holder

            DiceRoll diceRoll = getItem(listItemPosition);

            holder.numDiceEditText.setText(diceRoll.getNumDice());
            holder.diceSizeSpinner.setSelection(mSpinnerAdapter.getPosition(diceRoll.getDiceSize()));
            holder.modifierEditText.setText(diceRoll.getModifier());
            holder.totalTextView.setText(diceRoll.getTotal());
            holder.totalTextView.setTextColor(diceRoll.getColor());
            holder.rollResultsTextView.setText(diceRoll.getRollsSpannable(), TextView.BufferType.SPANNABLE);

            return convertView;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.loadMenuItem:
                openLoadDiceSetDialog();
                return true;
            case R.id.saveMenuItem:
                openSaveDiceSetDialog();
                return true;
            case R.id.deleteMenuItem:
                openDeleteDialog();
                return true;
            case R.id.helpMenuItem:
                showHelp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openLoadDiceSetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.loadDiceSetDialogTitle);

        final SQLiteDatabase db = mDatabaseOpenHelper.getReadableDatabase();
        final Cursor diceSets = DatabaseHelper.getDiceSets(db);

        final CursorAdapter cursorAdapter = new CursorAdapter(this, diceSets, false) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return LayoutInflater.from(context).inflate(android.R.layout.select_dialog_singlechoice, parent, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                CheckedTextView item = (CheckedTextView) view;
                item.setText(DatabaseHelper.cursorToSetName(cursor));
            }
        };

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setAdapter(cursorAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Cursor diceSetCursor = (Cursor) cursorAdapter.getItem(which);

                SQLiteDatabase db = mDatabaseOpenHelper.getReadableDatabase();
                Cursor diceRolls = DatabaseHelper.getAllDiceRollsInSet(db, diceSetCursor);

                mDiceRollerAdapter.loadDiceSet(diceRolls);
            }
        });

        builder.show();
        db.close();
    }

    private void openSaveDiceSetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.saveDiceSetDialogTitle);

        final EditText saveSetNameEditText = (EditText) mInflater.inflate(R.layout.save_dice_set_dialog, null);
        builder.setView(saveSetNameEditText);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String setName = saveSetNameEditText.getText().toString();
                if (setName.isEmpty()) {
                    String emptyNameError = MainActivity.this.getResources().getString(R.string.saveDiceSetEmptyNameError);
                    Toast toast = Toast.makeText(MainActivity.this, emptyNameError, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    SQLiteDatabase db = mDatabaseOpenHelper.getReadableDatabase();
                    boolean isDuplicateName = DatabaseHelper.isDiceSetNameUsed(db, setName);
                    db.close();

                    if (isDuplicateName) {
                        // TODO: Ask to overwrite
                        // TODO: Autocomplete existing set names?
                        String duplicateNameError = MainActivity.this.getResources().getString(R.string.saveDiceSetDuplicateNameError);
                        Toast toast = Toast.makeText(MainActivity.this, duplicateNameError, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    } else {
                        saveDiceSet(setName);
                    }
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void saveDiceSet(String setName) {
        SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();

        List<DiceRoll> diceRolls = new ArrayList<>();
        for (int i = 0; i < mDiceRollerAdapter.getCount(); ++i) {
            diceRolls.add(mDiceRollerAdapter.getItem(i));
        }

        DatabaseHelper.saveDiceSet(db, setName, diceRolls);
        db.close();
    }

    private void openDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.deleteDiceSetDialogTitle);

        final SQLiteDatabase db = mDatabaseOpenHelper.getReadableDatabase();
        final Cursor diceSets = DatabaseHelper.getDiceSets(db);

        final CursorAdapter cursorAdapter = new CursorAdapter(this, diceSets, false) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return LayoutInflater.from(context).inflate(android.R.layout.select_dialog_singlechoice, parent, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                CheckedTextView item = (CheckedTextView) view;
                item.setText(DatabaseHelper.cursorToSetName(cursor));
            }
        };

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setAdapter(cursorAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
                final Cursor diceSetCursor = (Cursor) cursorAdapter.getItem(which);
                DatabaseHelper.deleteDiceSet(db, diceSetCursor);
                db.close();
            }
        });

        builder.show();
        db.close();
    }

    private void showHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.helpDialogTitle);
        builder.setMessage(R.string.helpDialogMessage);
        builder.show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(DICE_ROLL_LIST_KEY, mDiceRollerAdapter.getList());
    }

    private static Integer[] integerArrayFromIntArray(int[] array) {
        Integer[] integerArray = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            integerArray[i] = Integer.valueOf(array[i]);
        }
        return integerArray;
    }
}