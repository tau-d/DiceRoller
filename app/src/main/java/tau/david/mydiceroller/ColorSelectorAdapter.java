package tau.david.mydiceroller;


import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class ColorSelectorAdapter extends ArrayAdapter<Integer> {
    private int translucent;
    private int outerBorderColor;
    private int innerBorderColor;

    private LayoutInflater mInflater;
    private int selectedPosition = 0;

    public ColorSelectorAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull Integer[] objects) {
        super(context, resource, objects);
        Resources res = context.getResources();
        translucent = res.getColor(R.color.translucent);
        outerBorderColor = DiceRoll.DEFAULT_COLOR;
        innerBorderColor = res.getColor(R.color.black);
        mInflater = LayoutInflater.from(context);
    }

    public int getSelectedColor() {
        return getItem(selectedPosition);
    }

    public void setSelectedColor(int selectedColor) {
        selectedPosition = getPosition(selectedColor);
    }

    private class ColorSquareViewHolder {
        private int currPosition;

        View outerBorderSquare;
        View innerBorderSquare;
        View colorSquare;

        private ColorSquareViewHolder(View convertView) {
            outerBorderSquare = convertView.findViewById(R.id.colorSquareOuterBorder);
            innerBorderSquare = convertView.findViewById(R.id.colorSquareInnerBorder);
            colorSquare = convertView.findViewById(R.id.colorSquareInner);

            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedPosition = getCurrPosition();
                    notifyDataSetChanged();
                }
            };

            outerBorderSquare.setOnClickListener(onClickListener);
            innerBorderSquare.setOnClickListener(onClickListener);
            colorSquare.setOnClickListener(onClickListener);
        }

        private int getCurrPosition() {
            return currPosition;
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ColorSquareViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.empty_view, parent, false);
            holder = new ColorSquareViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ColorSquareViewHolder) convertView.getTag();
        }
        holder.currPosition = position;

        Integer item = getItem(position);
        holder.colorSquare.setBackgroundColor(item);
        holder.innerBorderSquare.setBackgroundColor(innerBorderColor);
        if (selectedPosition == position) {
            holder.outerBorderSquare.setBackgroundColor(outerBorderColor);
        } else {
            holder.outerBorderSquare.setBackgroundColor(translucent);
        }

        return convertView;
    }

}
