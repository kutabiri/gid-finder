package ai.qed.gridlocator;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 9/25/15.
 */
public class GridAdapter<String> extends BaseAdapter {
    private Context context;
    private List<String> cells = new ArrayList<>();
    private int zoomLevel;
    private String currentGID;

    public GridAdapter(Context context, List<String> cells, int zoomLevel, String currentGID) {
        this.context = context;
        this.cells = cells;
        this.zoomLevel = zoomLevel;
        this.currentGID = currentGID;
    }

    @Override
    public int getCount() {
        return cells.size();
    }

    @Override
    public String getItem(int position) {
        if (getCount() > position) {
            return cells.get(position);
        }

        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            if (zoomLevel == 1) {
                view = View.inflate(context, R.layout.small_grid_item, null);
            }
            else if (zoomLevel == 2) {
                view = View.inflate(context, R.layout.medium_grid_item, null);
            }
            else {
                view = View.inflate(context, R.layout.large_grid_item, null);
            }
        }

        String cell = getItem(position);
        TextView text = (TextView) view.findViewById(R.id.cell_number);
        text.setText((CharSequence) cell);

        if (cell.equals(currentGID)) {
            text.setBackgroundResource(R.color.faded_green);
        }
        else {
            text.setBackground(null);
        }

        return view;
    }
}
