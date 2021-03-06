package com.huangkun.duskweather;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.huangkun.duskweather.gson.Forecast;
import com.huangkun.duskweather.gson.Weather;
import com.huangkun.duskweather.service.AutoUpdateService;
import com.huangkun.duskweather.util.HttpUtil;
import com.huangkun.duskweather.util.Utility;


import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;
    public SwipeRefreshLayout swipeRefresh;

    public DrawerLayout drawerLayout;
    private Button navButton;
    private String mweatherId;
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
        setContentView(R.layout.activity_weather);
        //初始化各个控件
        weatherLayout=(ScrollView)findViewById(R.id.weather_layout);
        titleCity=(TextView)findViewById(R.id.title_city);
        titleUpdateTime=(TextView)findViewById(R.id.title_update_time);
        degreeText=(TextView)findViewById(R.id.degree_text);
        weatherInfoText=(TextView)findViewById(R.id.weather_info_text);
        forecastLayout=(LinearLayout)findViewById(R.id.forecast_layout);
        aqiText=(TextView)findViewById(R.id.aqi_text);
        pm25Text=(TextView)findViewById(R.id.pm25_text);
        comfortText=(TextView)findViewById(R.id.comfort_text);
        carWashText=(TextView)findViewById(R.id.car_wash_text);
        sportText=(TextView)findViewById(R.id.sport_text);
        bingPicImg=(ImageView)findViewById(R.id.bing_pic_img);

        swipeRefresh=(SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);

        drawerLayout=(DrawerLayout)findViewById(R.id.drawer_layout);
        navButton=(Button)findViewById(R.id.nav_button);

        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString=prefs.getString("weather",null);


        if(weatherString!=null){
            //有缓存时直接解析天气数据
            Weather weather= Utility.handleWeatherResponse(weatherString);
            mweatherId=weather.basic.weatherId;
            showWeatherInfo(weather);
        }else{
            //无缓存时去服务器查询天气
            mweatherId=getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mweatherId);
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mweatherId);
            }
        });
        //尝试从SharedPreferences中读取缓存的背景
        String bingPic=prefs.getString("bing_pic",null);
        if(bingPic!=null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else{
            //如果没有就使用loadBingPic()方法加载图片
            loadBingPic();
        }
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }
    //加载必应每日一图
    private void loadBingPic(){

        String requestBingPic="http://guolin.tech/api/bing_pic";
        //HttpUtil.sendOkHttpRequest()方法必应取到必应背景图的连接
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                final String bingPic=response.body().string();
                //将这个连接缓存到SharedPreferences当中
                SharedPreferences.Editor editor=PreferenceManager.
                        getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                //切换到主线程
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //使用Glide加载这张图片
                        Glide.with(WeatherActivity.this).load(bingPic)
                                .into(bingPicImg);
                    }
                });
            }
        });
    }

    //根据天气id请求城市天气信息

    public void requestWeather(final String weatherId){
        String weatherUrl="http://guolin.tech/api/weather?cityid="+weatherId
                +"&key=e297f8ce4f3a472fb24ee10ce8a046e1";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText=response.body().string();
                final Weather weather=Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            mweatherId=weather.basic.weatherId;
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
        //每次请求天气信息是刷新背景图片
        loadBingPic();
    }

    //处理并展示Weather实体类中的数据
    private void showWeatherInfo(Weather weather){
        if(weather!=null&&"ok".equals(weather.status)){
            String cityName=weather.basic.cityName;
            String updateTime=weather.basic.update.updateTime.split(" ")[1];
            String degree=weather.now.temperature+"°C";
            String weatherInfo=weather.now.more.info;
            titleCity.setText(cityName);
            titleUpdateTime.setText(updateTime);
            degreeText.setText(degree);
            weatherInfoText.setText(weatherInfo);
            forecastLayout.removeAllViews();
            for (Forecast forecast:weather.forecastList){
                View view= LayoutInflater.from(this).inflate(R.layout.forecast_item,
                        forecastLayout,false);
                TextView dateText=(TextView)view.findViewById(R.id.date_text);
                TextView infoText=(TextView)view.findViewById(R.id.info_text);
                TextView maxText=(TextView)view.findViewById(R.id.max_text);
                TextView minText=(TextView)view.findViewById(R.id.min_text);
                dateText.setText(forecast.date);
                infoText.setText(forecast.more.info);
                maxText.setText(forecast.temperature.max+"°C");
                minText.setText(forecast.temperature.min+"°C");
                forecastLayout.addView(view);
            }
            if(weather.aqi!=null){
                aqiText.setText(weather.aqi.city.aqi);
                pm25Text.setText(weather.aqi.city.pm25);
            }
            String comfort="舒适度："+weather.suggestion.comfort.info;
            String carWash="洗车指数："+weather.suggestion.carWash.info;
            String sport="运动建议："+weather.suggestion.sport.info;
            comfortText.setText(comfort);
            carWashText.setText(carWash);
            sportText.setText(sport);
            weatherLayout.setVisibility(View.VISIBLE);
            Intent intent=new Intent(this, AutoUpdateService.class);
            startService(intent);

            Intent intent1=new Intent(this,WeatherActivity.class);
            PendingIntent pi=PendingIntent.getActivity(this,0,intent1,0);
            NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            Notification notification=new Notification.Builder(this)
                    .setContentTitle(cityName)
                    .setContentText(degree+"  "+weatherInfo)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.mipmap.ic_app)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.mipmap.ic_app))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();
            manager.notify(1,notification);
        }else{
            Toast.makeText(this, "获取填写信息失败", Toast.LENGTH_SHORT).show();
        }

    }
}
