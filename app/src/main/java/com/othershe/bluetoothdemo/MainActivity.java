package com.othershe.bluetoothdemo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.othershe.baseadapter.ViewHolder;
import com.othershe.baseadapter.interfaces.OnItemClickListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private final String TAG = "Bluetooth Info";

    private Context mContext;

    private BluetoothAdapter bluetoothAdapter;
    private Map<String, BluetoothDevice> devicesMap = new HashMap<>();
    private List<BluetoothDevice> devices = new ArrayList<>();

    private Button mReadBtn, mWriteBtn, mDisconnect;
    private RecyclerView mDevicesList;
    private DevicesAdapter mDevicesAdapter;

    private String mCurrentAddress;
    private BluetoothGatt mCurrentGatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        initView();

        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = manager.getAdapter();

        requestLocationPermission();
    }

    private void initView() {
        mReadBtn = (Button) findViewById(R.id.read_data);
        mWriteBtn = (Button) findViewById(R.id.write_data);
        mDisconnect = (Button) findViewById(R.id.disconnect);
        mReadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //读取数据
                BluetoothGattService service = mCurrentGatt.getService(UUID.fromString("SERVICE_UUID"));
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("CHARACTER_UUID"));
                //通知系统去读取数据
                mCurrentGatt.readCharacteristic(characteristic);
            }
        });
        mWriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //往蓝牙数据通道的写入数据
                BluetoothGattService service = mCurrentGatt.getService(UUID.fromString("SERVICE_UUID"));
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("CHARACTER_UUID"));
                characteristic.setValue(new byte[]{1, 2, 3});
                //通知数据已经完成写入
                mCurrentGatt.writeCharacteristic(characteristic);
            }
        });
        mDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentGatt.disconnect();
            }
        });

        mDevicesList = (RecyclerView) findViewById(R.id.devices_list);
        mDevicesAdapter = new DevicesAdapter(mContext, devices, false);
        mDevicesAdapter.setOnItemClickListener(new OnItemClickListener<BluetoothDevice>() {
            @Override
            public void onItemClick(ViewHolder viewHolder, BluetoothDevice device, int position) {
                //连接蓝牙设备
                device.connectGatt(mContext, true, gattCallback);
                mCurrentAddress = device.getAddress();
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mDevicesList.setLayoutManager(layoutManager);
        mDevicesList.setAdapter(mDevicesAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 2000) {
            return;
        }

        scanDevices();
    }

    private void scanDevices() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //延时20秒后停止扫描
                bluetoothAdapter.stopLeScan(scanCallback);
            }
        }, 20 * 1000);
        //开始扫描
        bluetoothAdapter.startLeScan(scanCallback);
    }

    //扫描回调
    private BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            //过滤重复设备
            if (!devicesMap.containsKey(device.getAddress())) {
                devicesMap.put(device.getAddress(), device);
                devices.add(device);
                mDevicesAdapter.notifyDataSetChanged();
                Log.e(TAG, device.getAddress() + "#" + device.getName());
            }
        }
    };

    //设备连接回调
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        /**
         * @param gatt 服务连接类
         * @param status 代表是否成功执行了连接操作
         * @param newState 代表当前设备的连接状态
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_SUCCESS) {

            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {//设备已经连接, 开始发现服务
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close();
            }
        }

        /**
         * 发现服务成功，接下来可进行蓝牙通信操作：数据读写
         * @param gatt
         * @param status
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            mCurrentGatt = gatt;
            Toast.makeText(mContext, "设备连接成功", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            //如果读取到了蓝牙设备发送过来的数据。则调用该方法
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "read data:" + characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (characteristic.getValue() != (new byte[]{1, 2, 3})) {
                // 执行重发策略
                gatt.writeCharacteristic(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    @AfterPermissionGranted(1000)
    private void requestLocationPermission() {
        String[] perms = {Manifest.permission.ACCESS_COARSE_LOCATION};
        if (EasyPermissions.hasPermissions(this, perms)) {
            //如果蓝牙未开启，则尝试开启
            if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, 1000);
            } else {
                scanDevices();
            }
        } else {
            EasyPermissions.requestPermissions(this, "请打开定位权限", 1000, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }
}
