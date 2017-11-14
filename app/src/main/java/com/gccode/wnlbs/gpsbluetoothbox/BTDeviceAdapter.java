package com.gccode.wnlbs.gpsbluetoothbox;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class BTDeviceAdapter extends BaseAdapter{


    private List<BTItem> mListItem=new ArrayList<BTItem>();;
    private Context mcontext=null;
    private LayoutInflater mInflater=null;

    public BTDeviceAdapter(Context context){
        this.mcontext=context;
        //  this.mListItem=mListItem;
        this.mInflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    void clearData(){
        mListItem.clear();
    }

    void addDataModel(List<BTItem> itemList){
        if(itemList==null || itemList.size()==0)
            return;
        mListItem.addAll(itemList);
        notifyDataSetChanged();
    }
    void addDataModel(BTItem item){
        mListItem.add(item);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {

        return mListItem.size();
    }

    @Override
    public Object getItem(int position) {

        return mListItem.get(position);
    }

    @Override
    public long getItemId(int position) {

        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder=null;

        if(convertView==null){
            holder=new ViewHolder();
            convertView = mInflater.inflate(R.layout.device_item_row, null);
            holder.tv=(TextView)convertView.findViewById(R.id.itemText);
            holder.ts=(TextView)convertView.findViewById(R.id.itemStatus);
            holder.td=(TextView)convertView.findViewById(R.id.itemAddress);
            convertView.setTag(holder);
        }else{
            holder=(ViewHolder)convertView.getTag();
        }
        holder.tv.setText(mListItem.get(position).getBuletoothName());
        holder.td.setText(mListItem.get(position).getBluetoothAddress());
        String status = "未绑定";
        switch (mListItem.get(position).getBluetoothType()){
            case BluetoothDevice.BOND_BONDED:
                status = "已绑定";
                break;
            case BluetoothDevice.BOND_BONDING:
                status = "正在配对......";
                break;
            case BluetoothDevice.BOND_NONE:
            default:
                break;
        }
        holder.ts.setText(status);
        return convertView;
    }


    class ViewHolder{
        TextView tv;
        TextView ts;
        TextView td;
    }

}
