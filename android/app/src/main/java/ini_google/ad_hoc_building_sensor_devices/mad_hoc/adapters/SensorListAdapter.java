package ini_google.ad_hoc_building_sensor_devices.mad_hoc.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import ini_google.ad_hoc_building_sensor_devices.R;
import ini_google.ad_hoc_building_sensor_devices.mad_hoc.ui.ListActivity.Parameter;

public class SensorListAdapter extends BaseExpandableListAdapter{

        private Context _context;
        private String _DataHeader; // header titles
        // child data in format of header title, child title
        private HashMap<String, List<Parameter>> _listDataChild;
        private HashSet<ID> _listIDSet;

        public SensorListAdapter(Context context, String DataHeader,
                                     HashMap<String, List<Parameter>> listChildData) {
            this._context = context;
            this._DataHeader = DataHeader;
            this._listDataChild = listChildData;
            this._listIDSet = new HashSet<>();
        }

        @Override
        public Object getChild(int groupPosition, int childPosititon) {
            System.out.println("childPosition:" + childPosititon);
            return this._listDataChild.get(this._DataHeader)
                    .get(childPosititon);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getChildView(int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {

            String childText = (String) ((Parameter)getChild(groupPosition, childPosition)).getItem();
            String childVal = (String) ((Parameter)getChild(groupPosition, childPosition)).getVal();

            if (convertView == null) {
                LayoutInflater infalInflater = (LayoutInflater) this._context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.list_item, null);
            }

            TextView txtListChildItem = (TextView) convertView
                    .findViewById(R.id.lblListItem);

            EditText txtListChildVal = (EditText) convertView
                    .findViewById(R.id.lblListItemVal);

            txtListChildVal.setFocusable(true);
            System.out.println("childText:" + childText);
            txtListChildItem.setText(childText);

            //System.out.println("childVal: " +childVal);
            //System.out.println("real childVal:" + txtListChildVal.getText());

            if(txtListChildVal.getText().toString().equals("null")) {
                //System.out.println("childVal: " +childVal);
                txtListChildVal.setText(childVal);
            }

            // store position
            ID curID = new ID(groupPosition, childPosition, convertView);
            if(!_listIDSet.contains(curID)) {
               _listIDSet.add(curID);
            }
            //String in =  txtListChildVal.getText().toString();
            //((Parameter)getChild(groupPosition, childPosition)).setVal(in);

            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return this._listDataChild.get(this._DataHeader)
                        .size();

        }

        @Override
        public Object getGroup(int groupPosition) {
            return this._DataHeader;
        }

        @Override
        public int getGroupCount() {
            return 1;
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

        // update editText

        public void updateVal() {

            for(ID id: _listIDSet) {
                int groupPosition = (int) id.groupPosition;
                int childPosition = (int) id.childPosition;
                View convertView = (View) id.convertView;

                EditText txtListChildVal = (EditText) convertView
                        .findViewById(R.id.lblListItemVal);

                String in = txtListChildVal.getText().toString();
                System.out.println("before in:" + in);
                // Should check if the input value is valid
                ((Parameter) getChild(groupPosition, childPosition)).setVal(in);
                System.out.println("after in:" + ((Parameter) getChild(groupPosition, childPosition)).getVal());

            }
        }

//        public void updateList(HashMap<String, List<Parameter>> listChildData) {
//            //listDataHeader = this._listDataHeader;
//            listChildData = this._listDataChild;
//        }

        public class ID {
            int groupPosition;
            int childPosition;
            View convertView;

            public ID(int groupPosition, int childPosition, View convertView) {
                this.groupPosition = groupPosition;
                this.childPosition = childPosition;
                this.convertView = convertView;
            }

            // input object o for compare
            @Override
            public boolean equals(Object o) {
                if(this.groupPosition == ((ID)o).groupPosition &&
                       this.childPosition == ((ID)o).childPosition &&
                       this.convertView == ((ID)o).convertView
                            ) {
                      return true;
                    } else {
                      return false;
                    }
            }
        }

}
