package com.huangkun.duskweather.gson;

/**
 * Created by Administrator on 2017/7/6.
 */

public class AQI {
    public AQICity city;
    public class AQICity{
        public String aqi;
        public String pm25;
    }
}
