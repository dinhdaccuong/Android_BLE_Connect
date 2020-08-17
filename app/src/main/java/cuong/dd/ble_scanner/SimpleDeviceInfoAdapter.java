package cuong.dd.ble_scanner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class SimpleDeviceInfoAdapter extends BaseAdapter {
    private Context context;
    private int layout;
    private List<SimpleDeviceInfo> listDevicesInfo;

    public SimpleDeviceInfoAdapter(Context context, int layout, List<SimpleDeviceInfo> listDevicesInfo) {
        this.context = context;
        this.layout = layout;
        this.listDevicesInfo = listDevicesInfo;
    }

    @Override
    public int getCount() {
        return listDevicesInfo.size();
    }

    @Override
    public Object getItem(int i) {
        if(i >= listDevicesInfo.size())
            return null;
        return listDevicesInfo.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    private class ViewHolder{
        TextView tvDeviceName;
        TextView tvMacAddress;
        TextView tvRSSI;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder hodler;
        if(view == null){
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(layout, null);
            hodler = new ViewHolder();
            hodler.tvDeviceName = (TextView) view.findViewById(R.id.textViewName);
            hodler.tvMacAddress = (TextView) view.findViewById(R.id.textViewMacAdd);
            hodler.tvRSSI = (TextView) view.findViewById(R.id.textViewRSSI);
            view.setTag(hodler);
        }
        else{
            hodler = (ViewHolder)view.getTag();
        }

        SimpleDeviceInfo dvInfo = listDevicesInfo.get(i);
        hodler.tvDeviceName.setText(dvInfo.getDeivceName());
        hodler.tvMacAddress.setText(dvInfo.getDeviceMacAddress());
        hodler.tvRSSI.setText(dvInfo.getRssi());

        return view;
    }
}
