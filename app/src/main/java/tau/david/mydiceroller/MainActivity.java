package tau.david.mydiceroller;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.TypedArray;
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
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

// TODO: constraint layout
// TODO: shake to roll setting
// TODO: use @styles?
// TODO: conditional rolls (ex. normally roll 1d6+2 damage, but on 19-20 crit range, roll 2d6+2 damage)
// TODO: improve lifecycle methods

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";

    private static final String DICE_ROLL_LIST_KEY = "DiceRollListKey";

    private LayoutInflater mInflater;
    private MyDiceRollerAdapter mDiceRollerAdapter;
    private DatabaseHelper mDatabaseHelper;

    private void testDb() {
        Log.d(LOG_TAG, "DB test");

        DatabaseHelper.deleteDatabase(this);

        mDatabaseHelper.getDiceSets();
        List<DiceRoll> rolls = mDatabaseHelper.getAllDiceRolls();

        mDiceRollerAdapter.loadDiceSet(rolls);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabaseHelper = new DatabaseHelper(this);
        mInflater = getLayoutInflater();

        TypedArray themeArray = MainActivity.this.getTheme().obtainStyledAttributes(new int[] {android.R.attr.editTextColor});
        int defaultTextColour = themeArray.getColor(0, 0);
        themeArray.recycle();
        DiceRoll.setDefaultColor(defaultTextColour);

        if (savedInstanceState != null) {
            mDiceRollerAdapter =
                    new MyDiceRollerAdapter(savedInstanceState.<DiceRoll>getParcelableArrayList(DICE_ROLL_LIST_KEY));
        } else {
            mDiceRollerAdapter = new MyDiceRollerAdapter();
        }

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


        MyDiceRollerAdapter() {
            items = new ArrayList<>();
            items.add(new DiceRoll());
        }

        MyDiceRollerAdapter(ArrayList<DiceRoll> items) {
            if (items == null) {
                items = new ArrayList<>();
                items.add(new DiceRoll());
            } else {
                this.items = items;
            }
        }

        void add(DiceRoll diceRoll) {
            items.add(diceRoll);
            notifyDataSetChanged();
        }

        void remove(int position) {
            items.remove(position);
            notifyDataSetChanged();
        }

        private void reset() {
            items.clear();
            items.add(new DiceRoll());
            notifyDataSetChanged();
        }

        void loadDiceSet(List<DiceRoll> rolls) {
            items.clear();
            if (rolls.isEmpty()) return;
            else {
                items.addAll(rolls);
                notifyDataSetChanged();
            }
        }

        ArrayList<DiceRoll> getList() {
            return items;
        }

        private class DiceRollViewHolder {
            private int currPosition;

            private TextView diceRollTextView;
            private TextView totalTextView;
            private TextView rollResultsTextView;

            private DiceRollViewHolder(View convertView) {
                diceRollTextView = (TextView) convertView.findViewById(R.id.diceRollTextView);
                totalTextView = (TextView) convertView.findViewById(R.id.totalTextView);
                rollResultsTextView = (TextView) convertView.findViewById(R.id.rollResultsTextView);

                View.OnClickListener basicOptionsOnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openBasicDiceOptionsDialog();
                    }
                };

                diceRollTextView.setOnClickListener(basicOptionsOnClickListener);
                totalTextView.setOnClickListener(basicOptionsOnClickListener);
                rollResultsTextView.setOnClickListener(basicOptionsOnClickListener);

                View.OnLongClickListener advancedOptionsOnLongClickListener = new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        openAdvancedDiceRollOptionsDialog();
                        return true;
                    }
                };

                diceRollTextView.setOnLongClickListener(advancedOptionsOnLongClickListener);
                totalTextView.setOnLongClickListener(advancedOptionsOnLongClickListener);
                rollResultsTextView.setOnLongClickListener(advancedOptionsOnLongClickListener);
            }

            private void openBasicDiceOptionsDialog() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.basicDiceRollOptionsDialogTitle);

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                final View v = mInflater.inflate(R.layout.basic_dice_options_dialog, null);
                final EditText numDiceEditTest = (EditText) v.findViewById(R.id.numDiceEditText);
                final EditText diceSizeEditTest = (EditText) v.findViewById(R.id.diceSizeEditText);
                final EditText modifierEditTest = (EditText) v.findViewById(R.id.modifierEditText);
                builder.setView(v);

                final DiceRoll item = getCurrItem();

                numDiceEditTest.setText(item.getNumDiceStr());
                diceSizeEditTest.setText(item.getDiceSizeStr());
                modifierEditTest.setText(item.getModifierStr());

                builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Integer numDice = myStringToInteger(numDiceEditTest.getText().toString());
                        Integer diceSize = myStringToInteger(diceSizeEditTest.getText().toString());
                        Integer mod = myStringToInteger(modifierEditTest.getText().toString());

                        String error = "";

                        if (numDice != null) {
                             if (!item.setNumDice(numDice)) {
                                 String numDiceError = MainActivity.this.getResources().getString(R.string.numDiceToastError);
                                 error += numDiceError + '\n';
                             }
                        } else item.setToDefaultNumDice();

                        if (diceSize != null) {
                            if (!item.setDiceSize(diceSize)) {
                                String diceSizeError = MainActivity.this.getResources().getString(R.string.diceSizeToastError);
                                error += diceSizeError + '\n';
                            }
                        } else item.setToDefaultDiceSize();

                        if (!error.isEmpty()) showToast(error.trim());

                        if (mod != null) item.setModifier(mod);
                        else item.setToDefaultModifier();

                        item.resetRollsAndTotal();
                        mDiceRollerAdapter.notifyDataSetChanged();
                    }
                });

                v.requestFocus();
                builder.show();
            }

            private void openAdvancedDiceRollOptionsDialog() {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.advancedDiceRollOptionsDialogTitle);

                View v = mInflater.inflate(R.layout.advanced_dice_roll_options_dialog, null);
                builder.setView(v);

                final CheckBox rerollOnesCheckBox = (CheckBox) v.findViewById(R.id.rerollOnesCheckBox);
                final CheckBox dropLowestCheckBox = (CheckBox) v.findViewById(R.id.dropLowestCheckBox);
                final CheckBox highlightMinMaxCheckBox = (CheckBox) v.findViewById(R.id.highlightMinMaxCheckBox);
                final CheckBox sortRollsCheckBox = (CheckBox) v.findViewById(R.id.sortRollsCheckBox);
                final GridView colorGridView = (GridView) v.findViewById(R.id.colorGridView);
                final Button deleteButton = (Button) v.findViewById(R.id.deleteDialogOptionButton);

                int[] textColors = MainActivity.this.getResources().getIntArray(R.array.textColors);
                Integer[] colorArray = integerArrayFromIntArray(textColors);
                colorArray[0] = DiceRoll.getDefaultColor();

                final ColorSelectorAdapter colorAdapter = new ColorSelectorAdapter(MainActivity.this, -1, colorArray);
                colorGridView.setAdapter(colorAdapter);

                final DiceRoll item = getCurrItem();

                rerollOnesCheckBox.setChecked(item.rerollOnesOption());
                dropLowestCheckBox.setChecked(item.dropLowestOption());
                highlightMinMaxCheckBox.setChecked(item.highlightMinMaxOption());
                sortRollsCheckBox.setChecked(item.sortRollsOption());
                colorAdapter.setSelectedColor(item.getColor());

                builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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

            holder.diceRollTextView.setText(diceRoll.toString());
            holder.totalTextView.setText(diceRoll.getTotal());
            holder.totalTextView.setTextColor(diceRoll.getColor());
            holder.rollResultsTextView.setText(diceRoll.getRollsSpannable(), TextView.BufferType.SPANNABLE);

            return convertView;
        }
    }

    private static Integer myStringToInteger(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
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

        final List<DiceSet> diceSets = mDatabaseHelper.getDiceSets();

        final ArrayAdapter<DiceSet> adapter = new ArrayAdapter<>(this,
                android.R.layout.select_dialog_singlechoice,
                diceSets);

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DiceSet diceSet = adapter.getItem(which);

                List<DiceRoll> diceRolls = mDatabaseHelper.getAllDiceRollsInSet(diceSet);

                mDiceRollerAdapter.loadDiceSet(diceRolls);
            }
        });

        builder.show();
    }

    private void openSaveDiceSetDialog() {
        final AutoCompleteTextView saveSetNameEditText = (AutoCompleteTextView) mInflater.inflate(R.layout.save_dice_set_dialog, null);
        final ArrayAdapter<DiceSet> autoCompleteAdapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line,
                        mDatabaseHelper.getDiceSets()
                );
        saveSetNameEditText.setAdapter(autoCompleteAdapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.saveDiceSetDialogTitle)
                .setView(saveSetNameEditText)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        final AlertDialog saveDialog = builder.create();
        saveDialog.show();

        saveDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String setName = saveSetNameEditText.getText().toString();
                if (setName.isEmpty()) {
                    String emptyNameError = MainActivity.this.getResources().getString(R.string.saveDiceSetEmptyNameError);
                    showToast(emptyNameError);
                } else {
                    final long duplicateSetId = mDatabaseHelper.isDiceSetNameUsed(setName);

                    if (duplicateSetId == -1) { // no duplicate set name
                        mDatabaseHelper.saveDiceSet(setName, mDiceRollerAdapter.getList());
                        saveDialog.dismiss();
                    } else {
                        // TODO: Autocomplete existing set names?
                        AlertDialog.Builder overwriteBuilder = new AlertDialog.Builder(MainActivity.this);

                        overwriteBuilder.setMessage(R.string.overwriteDiceSetMessage)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface overwriteDialog, int which) {
                                        mDatabaseHelper.overwriteDiceSet(duplicateSetId, mDiceRollerAdapter.getList());
                                        overwriteDialog.dismiss();
                                        saveDialog.dismiss();
                                    }
                                })
                                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface overwriteDialog, int which) {
                                        overwriteDialog.dismiss();
                                    }
                                });

                        overwriteBuilder.show();
                    }
                }
            }
        });
    }

    private void openDeleteDialog() {
        final ArrayAdapter<DiceSet> adapter = new ArrayAdapter<>(this,
                android.R.layout.select_dialog_singlechoice,
                mDatabaseHelper.getDiceSets());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.deleteDiceSetDialogTitle)
                .setAdapter(adapter, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        final AlertDialog deleteSetDialog = builder.create();
        deleteSetDialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String msg = MainActivity.this.getResources().getString(R.string.confirmDeleteDialogMessage);
                final DiceSet item = adapter.getItem(position);

                final AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(MainActivity.this);
                confirmBuilder.setTitle(R.string.confirmDeleteDialogTitle)
                        .setMessage(msg + '\n' + item.name)
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface confirmDialog, int which) {
                                confirmDialog.dismiss();
                            }
                        })
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface confirmDialog, int which) {
                                mDatabaseHelper.deleteDiceSet(item);
                                confirmDialog.dismiss();
                                deleteSetDialog.dismiss();
                            }
                        });

                confirmBuilder.show();
            }
        });

        deleteSetDialog.show();
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

    private void showToast(String message) {
        Toast toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private static Integer[] integerArrayFromIntArray(int[] array) {
        Integer[] integerArray = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            integerArray[i] = array[i];
        }
        return integerArray;
    }
}