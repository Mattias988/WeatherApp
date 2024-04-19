package org.example;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class WeatherApp {
    public static JSONObject getWeatherData(String locationName){

        //get location coordinates by geolocation API
        JSONArray locationData = getLocationData(locationName);

        //extract latitude and longitude data
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        //build api request with location
        String urlString = "https://api.open-meteo.com/v1/forecast?" + "latitude=" + latitude + "&longitude=" + longitude + "&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=auto";

        try{
            //call api and get response
            HttpsURLConnection conn = fetchApiResponse(urlString);

            //checkt for repsonse status
            if(conn.getResponseCode() != 200){
                System.out.println("ERROR: Could not connect to API");
                return null;
            }

            //store resulting data
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while(scanner.hasNext()){
                resultJson.append(scanner.nextLine());
            }

            //close scanner
            scanner.close();

            //close url connection
            conn.disconnect();

            //parse JSON
            JSONParser parser = new JSONParser();
            JSONObject result = (JSONObject) parser.parse(String.valueOf(resultJson));

            //retrieve hourly data
            JSONObject hourly = (JSONObject) result.get("hourly");

            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);

            //get temperature
            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            //get weather code
            JSONArray weathercode = (JSONArray) hourly.get("weathercode");
            System.out.println(weathercode);
            String weatherCondition = convertWeatherCode((long) weathercode.get(index));

            //get humidity
            JSONArray relativeHumidity = (JSONArray) hourly.get("relativehumidity_2m");
            long humidity = (Long) relativeHumidity.get(index);

            //get windspeed
            JSONArray windspeedData = (JSONArray) hourly.get("windspeed_10m");
            double windspeed = (double) windspeedData.get(index);

            //build the weather json data object
            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);
            System.out.println(weatherData);
            return weatherData;
        }catch (Exception e ){
            e.printStackTrace();
        }

        return null;
    }

    //retrieves geographic cordinates
    private static JSONArray getLocationData(String locationName){
        //replace any whitespace in location name to+to adhere to API's request format
        locationName = locationName.replaceAll(" ", "+");

        //build API url with location parameter
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" + locationName + "&count=10&language=en&format=json";

        try{
            //call api and get response
            HttpsURLConnection conn = fetchApiResponse(urlString);

            //check response status
            if(conn.getResponseCode() != 200){
                System.out.println("ERROR: Could not connect to API");
                return null;
            }else {
                //store the api result
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(conn.getInputStream());
                while(scanner.hasNext()){
                    resultJson.append(scanner.nextLine());
                }

                //close scanner
                scanner.close();

                //close URL connection
                conn.disconnect();

                //parse JSON string into a Json object
                JSONParser parser = new JSONParser();
                JSONObject result = (JSONObject) parser.parse(String.valueOf(resultJson));

                //get list of the location data the api generated
                JSONArray locationData = (JSONArray) result.get("results");
                //TODO: FIX BUG - location data is null
                return locationData;

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static HttpsURLConnection fetchApiResponse(String urlString){
        try{
            //attempt to create connection
            URL url = new URL(urlString);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            //set request method to get
            conn.setRequestMethod("GET");

            //connect to our API
            conn.connect();
            return conn;
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList){
        String currentTime = getCurrentTime();


        //iterate through the time list
        for (int i = 0; i < timeList.size(); i++){
            String time = (String) timeList.get(i);
            if(time.equalsIgnoreCase(currentTime)){
                //return the index
                return i;
            }
        }
        return 0;
    }

    private static String getCurrentTime(){
        //get current data and time
        LocalDateTime currentDateTime = LocalDateTime.now();

        //format date to be 2024-04-06T00:00
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");

        //format and print the currnet date and time
        String formattedDateTime = currentDateTime.format(formatter);

        return formattedDateTime;
    }

    private static String convertWeatherCode(long weathercode){
        String weatherCondition = "";
        if(weathercode == 0L){
            //clear
            weatherCondition = "Clear";
        }else if(weathercode <= 3L && weathercode > 0L){
            //cloudy
            weatherCondition = "Cloudy";
        }else if((weathercode >= 51L && weathercode <= 67L) || (weathercode >=80L && weathercode <= 99L)){
            //rain
            weatherCondition = "Rain";
        }else if(weathercode >= 71L && weathercode <= 77L){
            //snow
            weatherCondition = "Snow";
        }
            return weatherCondition;
    }
}
