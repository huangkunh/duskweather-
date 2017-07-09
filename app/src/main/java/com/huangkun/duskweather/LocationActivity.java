package com.huangkun.duskweather;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class LocationActivity extends AppCompatActivity {
    //创建了一个LocationClient实例
    public LocationClient mLocationClient;
    private TextView positionText;
    private MapView mapView;
    private BaiduMap baiduMap;
    private Button break_button;
    private TextView city_text;
    private boolean isFirstLocate=true;
    private int Callback;
    private long exitTime = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //进行系统版本号的判断，5.0版本后才能实现
        if(Build.VERSION.SDK_INT>=21){
            //通过getWindow().getDecorView()方法活动DecorView
            View decorView=getWindow().getDecorView();
            //调用它的setSystemUiVisibility()方法来改变UI，
            // 这里View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN,
            // View.SYSTEM_UI_FLAG_LAYOUT_STABLE表示将布局显示在状态栏上
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            //setStatusBarColor()方法将状态栏设置为透明色
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        Callback=0;
        //接收一个Context参数
        mLocationClient=new LocationClient(getApplicationContext());
        //利用LocationClient的registerLocationListener()方法注册一个监听器
        mLocationClient.registerLocationListener(new MyLocationListener());
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_location);
        mapView=(MapView)findViewById(R.id.bmapView);
        break_button=(Button)findViewById(R.id.back_button);
        //break_button=(Button)findViewById(R.id.break_button);
        city_text=(TextView)findViewById(R.id.title_city);

        baiduMap=mapView.getMap();
        positionText=(TextView)findViewById(R.id.position_text_view);
        baiduMap.setMyLocationEnabled(true);


        //使用List集合存放没有申请的权限
        List<String> permissionList=new ArrayList<>();
        if (ContextCompat.checkSelfPermission(LocationActivity.this, Manifest
                .permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){

            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(LocationActivity.this,Manifest.
                permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(ContextCompat.checkSelfPermission(LocationActivity.this,Manifest.
                permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(!permissionList.isEmpty()){
            //最后将List转换成数组，调用ActivityCompat.requestPermissions一次性申请权限
            String[]permissions=permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(LocationActivity.this,permissions,1);

        }else{
            requestLocation();
        }

        /*break_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(this,MainActivity.class);
            }
        });*/
        break_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentbreak=new Intent(LocationActivity.this,WeatherActivity.class);
                startActivity(intentbreak);
                finish();
            }
        });
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    //
    private void navigateTo(BDLocation location){
        if(isFirstLocate){
            //先获取经纬度并传入LatLng中
            LatLng ll=new LatLng(location.getLatitude(),
                    location.getLongitude());
            //调用MapStatusUpdateFactory的newLatLng()方法并将LatLng对象传入
            MapStatusUpdate update= MapStatusUpdateFactory.newLatLng(ll);
            //把MapStatusUpdate对象传入BaiduMap的animateMapStatus()方法当中
            baiduMap.animateMapStatus(update);
            //设置缩放级别为16
            update=MapStatusUpdateFactory.zoomTo(16f);
            baiduMap.animateMapStatus(update);
            //这里为了防止多次调用animateMapStatus()方法，因为只需调用一次定位就够了
            //isFirstLocate=false;
        }
        //将Location中报到的经纬度分别封装到MyLocationData.Builder当中
        MyLocationData.Builder locationBuilder=new MyLocationData.Builder();
        locationBuilder.latitude(location.getLatitude());
        locationBuilder.longitude(location.getLongitude());
        MyLocationData locationData=locationBuilder.build();
        baiduMap.setMyLocationData(locationData);
        if(Callback==1){
            Toast.makeText(this, "位置：" + location.getAddrStr(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    //调用LocationClient的start()方法就能开始定位了，
    // 结果会回调到前面注册的监听器里MyLocationListener
    private void requestLocation(){
        initLocation();
        mLocationClient.start();
    }

    private void initLocation(){
        //表明要查询详细地址信息
        LocationClientOption option=new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setScanSpan(1000);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case 1:
                if(grantResults.length>0){
                    //对申请权限进行判断，假如有一个拒绝则关闭应用
                    for(int result:grantResults){
                        if(result!=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this, "必须同意所有权限才能使用本程序", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    //当所有权限用户都同意时才调用requestLocation()开始定位
                    requestLocation();
                }else{
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            final StringBuilder currentPosition=new StringBuilder();
            //通过getLatitude()获取纬度
            /*currentPosition.append("纬度：").append(bdLocation.getLatitude()).
                    append("\n");
            //通过getLongitude()获取经度
            currentPosition.append("经度：").append(bdLocation.getLongitude()).
                    append("\n");
            currentPosition.append("国家：").append(bdLocation.getCountry()).
                    append("\n");
            currentPosition.append("省：").append(bdLocation.getProvince()).
                    append("\n");
            currentPosition.append("市：").append(bdLocation.getCity()).
                    append("\n");*/
            currentPosition.append("区：").append(bdLocation.getDistrict()).
                    append("\n");
            /*currentPosition.append("街道：").append(bdLocation.getStreet()).
                    append("\n");*/


            currentPosition.append("定位方式：");
            //通过getLocType()方法获取当前定位的方式
            if(bdLocation.getLocType()==BDLocation.TypeGpsLocation){
                currentPosition.append("GPS");
            }else if(bdLocation.getLocType()==BDLocation.TypeNetWorkLocation){
                currentPosition.append("网络");
            }
            Log.d("MainActivity", "onReceiveLocation: "+currentPosition);
            //切换到主线程，更新UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    positionText.setText(currentPosition.toString());
                    //city_text.setText(currentPosition.toString());
                }
            });

            if(Callback<2){
                if (bdLocation.getLocType()==BDLocation.TypeGpsLocation
                        ||bdLocation.getLocType()==BDLocation.TypeNetWorkLocation){
                    navigateTo(bdLocation);
                }
            }

            //当设备获得当前的位置时，直接把BDLocation对象传给navigateTo()方法
            Callback++;
        }


        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }
    }
}
