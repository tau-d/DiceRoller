package tau.david.mydiceroller;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

// TODO: options
// TODO: reset total and roll results after changing a field
// TODO: Shadowrun style rollsSpannable

public class DiceRoll implements Parcelable {

    private static final String LOG_TAG = "DiceRoll";

    public static final long REROLL_ONES = 1;
    public static final long DROP_LOWEST = 1 << 1;
    public static final long HIGHLIGHT_MIN_MAX = 1 << 2;
    public static final long SORT_ROLLS = 1 << 3;

    private static final int MIN_COLOR = Color.RED;
    private static final int MAX_COLOR = Color.GREEN;

    private static final int DEFAULT_NUM_DICE = 1;
    private static final int DEFAULT_DICE_SIZE = 4;
    private static final int DEFAULT_MODIFIER = 0;

    public static final Creator<DiceRoll> CREATOR = new Creator<DiceRoll>() {
        @Override
        public DiceRoll createFromParcel(Parcel in) {
            return new DiceRoll(in);
        }

        @Override
        public DiceRoll[] newArray(int size) {
            return new DiceRoll[size];
        }
    };

    private static final Random RAND = new Random();

    private int numDice;
    private int diceSize;
    private int modifier;
    private ArrayList<Integer> rollList;
    // String to support empty TextViews before first roll
    private SpannableStringBuilder rollsSpannable;
    private String total;
    private long options;


    public DiceRoll(int numDice, int diceSize, int modifier, long options) {
        this.numDice = numDice;
        this.diceSize = diceSize;
        this.modifier = modifier;
        this.options = options;

        rollList = new ArrayList<>();
        total = "";
        rollsSpannable = new SpannableStringBuilder();
    }

    public DiceRoll() {
        this(DEFAULT_NUM_DICE, DEFAULT_DICE_SIZE, DEFAULT_MODIFIER, 0L);
    }

    private DiceRoll(Parcel in) {
        numDice = in.readInt();
        diceSize = in.readInt();
        modifier = in.readInt();
        total = in.readString();
        options = in.readLong();
        rollList = in.readArrayList(Integer.class.getClassLoader());
        rollsSpannable = new SpannableStringBuilder(TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in));
        Log.d(LOG_TAG, rollsSpannable.getSpans(0, rollsSpannable.length(), Object.class).length + "");
    }


    public void resetRollsAndTotal() {
        total = "";
        rollList.clear();
        rollsSpannable.clear();
        rollsSpannable.clearSpans();
    }

    public void rollDice() {
        resetRollsAndTotal();

        // initial rollsSpannable
        for (int i = 0; i < numDice; ++i) {
            rollList.add(getOneRoll());
        }

        if (rerollOnesOption()) rerollOnes();
        if (sortRollsOption()) Collections.sort(rollList);

        // calculate roll total and make roll results string
        int currTotal = modifier;
        int currLowest = Integer.MAX_VALUE;
        int lowestStart = -1;
        int lowestEnd = -1;
        for (int i : rollList) {
            currTotal += i;

            String rollString = Integer.toString(i);
            rollsSpannable.append(rollString);

            int start = rollsSpannable.length() - rollString.length();
            int end = rollsSpannable.length();
            if (highlightMinMaxOption()) {
                if (i == 1) { // min
                    rollsSpannable.setSpan(new ForegroundColorSpan(MIN_COLOR), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else if (i == diceSize) { // max
                    rollsSpannable.setSpan(new ForegroundColorSpan(MAX_COLOR), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            if (dropLowestOption() && i < currLowest) {
                currLowest = i;
                lowestStart = start;
                lowestEnd = end;
            }

            rollsSpannable.append(' ');
        }

        if (dropLowestOption()) {
            currTotal -= currLowest;
            rollsSpannable.setSpan(new StrikethroughSpan(), lowestStart, lowestEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        total = "" + currTotal;
    }

    public void rerollOnes() {
        for (int i = 0; i < rollList.size(); ++i) {
            while (rollList.get(i) == 1) {
                rollList.set(i, getOneRoll());
            }
        }
    }

    private int getOneRoll() {
        return RAND.nextInt(diceSize) + 1;
    }

    @Override
    public String toString() {
        return numDice + "d" + diceSize + "+" + modifier;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(numDice);
        dest.writeInt(diceSize);
        dest.writeInt(modifier);
        dest.writeString(total);
        dest.writeLong(options);
        dest.writeList(rollList);
        /*dest.writeString(rollsSpannable.toString());
        dest.writeArray(rollsSpannable.getSpans(0, rollsSpannable.length(), Object.class));*/
        TextUtils.writeToParcel(rollsSpannable, dest, flags);
    }

    public void setNumDice(int numDice) {
        this.numDice = numDice;
    }

    public void setDiceSize(int diceSize) {
        this.diceSize = diceSize;
    }

    public void setModifier(int modifier) {
        this.modifier = modifier;
    }

    public void setOptions(long options) {
        this.options = options;
    }

    public void setDefaultNumDice() {
        this.numDice = DEFAULT_NUM_DICE;
    }

    public void setDefaultModifier() {
        this.modifier = DEFAULT_MODIFIER;
    }

    public String getNumDice() {
        return "" + numDice;
    }

    public int getDiceSize() {
        return diceSize;
    }

    public String getModifier() {
        return "" + modifier;
    }

    public long getOptions() {
        return options;
    }

    public String getTotal() {
        return total;
    }

    public SpannableStringBuilder getRollsSpannable() {
        return rollsSpannable;
    }

    public boolean rerollOnesOption() {
        return (options & REROLL_ONES) != 0;
    }

    public boolean dropLowestOption() {
        return (options & DROP_LOWEST) != 0;
    }

    public boolean highlightMinMaxOption() {
        return (options & HIGHLIGHT_MIN_MAX) != 0;
    }

    public boolean sortRollsOption() {
        return (options & SORT_ROLLS) != 0;
    }
}
