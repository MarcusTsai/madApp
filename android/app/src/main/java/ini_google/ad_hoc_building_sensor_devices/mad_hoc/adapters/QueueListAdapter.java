package ini_google.ad_hoc_building_sensor_devices.mad_hoc.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

import ini_google.ad_hoc_building_sensor_devices.R;
import ini_google.ad_hoc_building_sensor_devices.mad_hoc.ui.QueueDisplayActivity;

public class QueueListAdapter extends BaseExpandableListAdapter{
    private Context _context;
    private List<String> _listDataHeader; // header titles
    // child data in format of header title, child title
    private HashMap<String, List<QueueDisplayActivity.Queue>> _listDataChild;
    private HashMap<String, Integer> colorMap = new HashMap<>();

    public QueueListAdapter(Context context, List<String> listDataHeader,
                             HashMap<String, List<QueueDisplayActivity.Queue>> listChildData) {
        this._context = context;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;
        //this._listIDSet = new HashSet<>();
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        String childText = (String) ((QueueDisplayActivity.Queue)getChild(groupPosition, childPosition)).getQname();
        String childVal = (String) ((QueueDisplayActivity.Queue)getChild(groupPosition, childPosition)).getQcount();
        int childColor = (int) ((QueueDisplayActivity.Queue)getChild(groupPosition, childPosition)).getQcolor();
        //Boolean fixed = (Boolean)((Parameter)getChild(groupPosition, childPosition)).getFixed();

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.queue_item, null);
        }

        TextView txtListChildItem = (TextView) convertView
                .findViewById(R.id.queue);

        TextView txtListChildVal = (TextView) convertView
                .findViewById(R.id.faceCount);

        if(childText.isEmpty())
        {
            txtListChildItem.setVisibility(View.INVISIBLE);
        }
        else {
        txtListChildItem.setText(childText);}
        txtListChildVal.setText(childVal);
        txtListChildVal.setBackgroundColor(childColor);

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .size();

    }

    @Override
    public Object getGroup(int groupPosition) {
        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this._listDataHeader.size();
    }



    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);

        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setTextColor(Color.BLACK);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

}
