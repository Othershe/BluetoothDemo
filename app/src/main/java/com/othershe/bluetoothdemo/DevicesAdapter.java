package com.othershe.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.othershe.baseadapter.ViewHolder;
import com.othershe.baseadapter.base.CommonBaseAdapter;

import java.util.List;

public class DevicesAdapter extends CommonBaseAdapter<BluetoothDevice> {
    public DevicesAdapter(Context context, List<BluetoothDevice> datas, boolean isOpenLoadMore) {
        super(context, datas, isOpenLoadMore);
    }

    @Override
    protected void convert(ViewHolder holder, BluetoothDevice data, int position) {
        holder.setText(R.id.address, data.getAddress());
        holder.setText(R.id.name, data.getName());
//        if ()
    }

    @Override
    protected int getItemLayoutId() {
        return R.layout.item_device_layout;
    }
}
