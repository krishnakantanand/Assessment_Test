/**
 * Copyright 2017 Attivio Inc., All rights reserved.
 */
package com.attivio.sa.satest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;	
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.ingest.IngestField;
import com.attivio.sdk.ingest.IngestFieldValue;
import com.attivio.sdk.schema.FieldNames;
import com.attivio.sdk.search.QueryLanguages;
import com.attivio.sdk.search.QueryRequest;
import com.attivio.sdk.search.fields.StoredField;
import com.attivio.sdk.server.annotation.ConfigurationOption;
import com.attivio.sdk.server.annotation.ConfigurationOptionInfo;
import com.attivio.sdk.server.component.ingest.DocumentModifyingTransformer;
import com.attivio.sdk.search.fields.GeoDistance;

/** An Example of the simplest document transformer possible. */
@ConfigurationOptionInfo(displayName = "Geocode Location", description = "Fetch lat long info from google API using location", groups = {
  @ConfigurationOptionInfo.Group(path = ConfigurationOptionInfo.PLATFORM_COMPONENT, propertyNames = { "field", "value" })
})
public class GeocodeLocation implements DocumentModifyingTransformer {

  private String field = FieldNames.TITLE;
  private String value = "My new title";

  @Override
  public boolean processDocument(IngestDocument doc) throws AttivioException {
	   
    //get location 
    IngestField  location = doc.getField("location");
    
    if(location != null) {  // if location found get first field
    	IngestFieldValue val =  location.getFirstValue();
    	
        // do lookup on value found   
    	String queryWord = val.toString();		// Gets the location 
    	
    	//lookup for lat long using google map API
    	 String latLong = getLatLong(queryWord);
    	 
    	 String[] vals = latLong.split(":::");
    	
    	doc.setField("latitude", vals[0]);
    	doc.setField("longitude", vals[1]);
    	
	
    }
    
    
    return true;
  }
  
  
  /**************************************************************************
   * This method uses google MAP API to retreive lat long of the location
   * @input location 
   * @return  lat long of location
 *  
   ******************************************************/

   String getLatLong(String query) {
	   
	   query= query.replace(" ", "+");  // 
	 

       InputStream inputStream = null;
       String json = "";

       try {           
           HttpClient client = new DefaultHttpClient();  
           query = "https://maps.googleapis.com/maps/api/geocode/json?address="+query;
           HttpPost post = new HttpPost(query);
           HttpResponse response = client.execute(post);
           HttpEntity entity = response.getEntity();
           inputStream = entity.getContent();
       } catch(Exception e) {
       }

       try {           
           BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,"utf-8"),8);
           StringBuilder sbuild = new StringBuilder();
           String line = null;
           while ((line = reader.readLine()) != null) {
               sbuild.append(line);
           }
           inputStream.close();
           json = sbuild.toString();               
       } catch(Exception e) {
       }


       //now parse
       JSONParser parser = new JSONParser();
       Object obj=null;
	try {
		obj = parser.parse(json);
	} catch (ParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
       JSONObject jb = (JSONObject) obj;

       //now read
       JSONArray jsonObject1 = (JSONArray) jb.get("results");
       JSONObject jsonObject2 = (JSONObject)jsonObject1.get(0);
       JSONObject jsonObject3 = (JSONObject)jsonObject2.get("geometry");
       JSONObject location = (JSONObject) jsonObject3.get("location");

       String result =  location.get("lat")+ ":::" +location.get("lng");
       
       return result;

	   
   }
  
  
  @ConfigurationOption(displayName = "Field to set", description = "Name of the new field to create")
  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

  @ConfigurationOption(displayName = "Field value", description = "Value for the new field")
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }


}
