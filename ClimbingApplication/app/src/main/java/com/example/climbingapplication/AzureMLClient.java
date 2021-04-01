package com.example.climbingapplication;

import java.net.*;
import java.io.*;
import java.util.*;

public class AzureMLClient
{

    private static String endPointURL; //Azure ML Endpoint


    public AzureMLClient(String endPointURL)
    {
        this.endPointURL= endPointURL;
    }
    /*
     Takes an Azure ML Request Body then Returns the Response String Which Contains Scored Lables etc
    */
    public static String requestResponse( String requestBody )
    {
        System.out.println("GOT TO REQUEST RESPONSE");
        URL u = null;
        try {
            u = new URL(endPointURL);
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage() + "___1");
            e.printStackTrace();
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) u.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage() + "___2");
        }

        conn.setRequestProperty("Content-Type","application/json");
        try {
            conn.setRequestMethod("POST");
        } catch (ProtocolException e) {
            e.printStackTrace();
            System.out.println(e.getMessage() + "___3");
        }
        String body= new String(requestBody);

        conn.setDoOutput(true);
        try {
            conn.connect();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage() + "___4");
        }
        int code = 0;
        try {
            code = conn.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage() + "___5");
        }
        System.out.println(code);
        OutputStreamWriter wr= null;
        try {
            wr = new OutputStreamWriter(conn.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage() + "___6");
        }

        try {
            wr.write(body);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage() + "___7");
        }
        try {
            wr.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage() + "___8");
        }

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage() + "___9");
        }

        String decodedString;
        String responseString="";

        while (true)
        {
            try {
                if (!((decodedString = in.readLine()) != null)) break;
                responseString+=decodedString;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(e.getMessage() + "___10");
            }
        }
        System.out.println(responseString);
        return responseString;
    }

}
