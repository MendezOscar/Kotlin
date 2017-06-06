import java.util.Base64;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.URL;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.google.gson.*;

fun main(args: Array<String>) {
  println("Server Started");
  val server: HttpServer = HttpServer.create(InetSocketAddress(8080), 0);
  server.createContext("/ejercicio1", Ejercicio1());
  server.createContext("/ejercicio2", Ejercicio2());
  server.createContext("/ejercicio3", Ejercicio3());
  server.setExecutor(null);
  server.start();
}

class Ejercicio1 : HttpHandler {
  override fun handle(t: HttpExchange) {
    if (t.getRequestMethod() == "POST") {
      val os: OutputStream = t.getResponseBody();
      val response: ByteArray = t.getRequestBody().readBytes();
      var jo_input: JsonObject = JsonParser().parse(String(response)).getAsJsonObject();
      val origen: String = jo_input.get("origen").getAsString().replace(" ", "+");
      val destino: String = jo_input.get("destino").getAsString().replace(" ", "+");
      println("Origen: " + origen);
      println("Destino: " + destino);
      var url: String = "https://maps.googleapis.com/maps/api/directions/json?origin="+origen+"&destination="+destino+"&key=AIzaSyDm3w4oPw82M8upaATU0IY5Sm_-tNvaXp8";
      var response_to_get: String = sendGet(url);

      val jsonArray = JsonParser().parse(response_to_get).getAsJsonObject().get("routes").getAsJsonArray().get(0).getAsJsonObject().get("legs").getAsJsonArray().get(0).getAsJsonObject().get("steps").getAsJsonArray();
      var response_map = mutableMapOf<String, ArrayList<JsonObject>>();
      response_map.put("ruta", ArrayList<JsonObject>());
      response_map["ruta"]!!.add(jsonArray.get(0).getAsJsonObject().get("start_location").getAsJsonObject());
      for (dir in jsonArray)
        response_map["ruta"]!!.add(dir.getAsJsonObject().get("end_location").getAsJsonObject());

      var gson = GsonBuilder().create();
      var json = gson.toJson(response_map);
      println(json);

      t.getResponseHeaders().add("content-type", "json");
      t.sendResponseHeaders(200, json.toByteArray().size.toLong() );
      os.write(json.toByteArray());
      os.close();
    }
  }
}

class Ejercicio2 : HttpHandler {
  override fun handle(t: HttpExchange) {
    if (t.getRequestMethod() == "POST") {
      val os: OutputStream = t.getResponseBody();
      val response: ByteArray = t.getRequestBody().readBytes();
      val jo_input: JsonObject = JsonParser().parse(String(response)).getAsJsonObject();
      val origen: String = jo_input.get("origen").getAsString().replace(" ", "+");
      println("Origen: " + origen);
      var url: String = "https://maps.googleapis.com/maps/api/geocode/json?address="+origen+"&key=AIzaSyDXvf09KDyp0cTk5nroClhViAJBa2fwVRk";
      var response_to_get: String = sendGet(url);

      val jsonObj = JsonParser().parse(response_to_get).getAsJsonObject().get("results").getAsJsonArray().get(0).getAsJsonObject().get("geometry").getAsJsonObject().get("location").getAsJsonObject();
      val lat = jsonObj.get("lat").getAsString();
      val lng = jsonObj.get("lng").getAsString();
      println(lat + " |-| " + lng);

      url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="+lat+","+lng+"&radius=5000&type=restaurant&key=AIzaSyDXvf09KDyp0cTk5nroClhViAJBa2fwVRk";
      response_to_get = sendGet(url);
      val jsonArray = JsonParser().parse(response_to_get).getAsJsonObject().get("results").getAsJsonArray();

      var response_map = mutableMapOf<String, ArrayList<JsonObject>>();
      response_map.put("restaurantes", ArrayList<JsonObject>());
      var tmp: JsonObject;
      for(dir in jsonArray) {
        tmp = JsonObject();
        tmp.add("nombre", dir.getAsJsonObject().get("name"));
        tmp.add("lat", dir.getAsJsonObject().get("geometry").getAsJsonObject().get("location").getAsJsonObject().get("lat"));
        tmp.add("lon", dir.getAsJsonObject().get("geometry").getAsJsonObject().get("location").getAsJsonObject().get("lng"));
        response_map["restaurantes"]!!.add(tmp);
      }
      val gson = GsonBuilder().create();
      val json = gson.toJson(response_map);
      println(json);

      t.getResponseHeaders().add("content-type", "json");
      t.sendResponseHeaders(200, json.toByteArray().size.toLong() );
      os.write(json.toByteArray());
      os.close();
    }
  }
}

class Ejercicio3 : HttpHandler {
  override fun handle(t: HttpExchange) {
    if (t.getRequestMethod() == "POST") {
      val os: OutputStream = t.getResponseBody();
      val response: ByteArray = t.getRequestBody().readBytes();
      val jo_input: JsonObject = JsonParser().parse(String(response)).getAsJsonObject();
      val data = jo_input.get("data").getAsString();
      val decoded_data = Base64.getDecoder().decode(data);
      //println(String(decoded_data));
      val width = ByteBuffer.wrap(decoded_data.sliceArray(IntRange(18, 21)).reversedArray()).getInt();
      val height = ByteBuffer.wrap(decoded_data.sliceArray(IntRange(22, 25)).reversedArray()).getInt();
      val bpp = ByteBuffer.wrap(decoded_data.sliceArray(IntRange(28, 29)).reversedArray()).getShort();
      val starting_location = ByteBuffer.wrap(decoded_data.sliceArray(IntRange(10, 13)).reversedArray()).getInt();
      println("Width: " + width + " | Height: " + height + " | BPP: " + bpp + " | starting_location: " + starting_location);

      var response_arr = ByteArray(decoded_data.size);

      for (index in 0..starting_location)
        response_arr[index] = decoded_data[index];

      for (x in starting_location..decoded_data.size step 3) {
        if (bpp == 32.toShort()) {
          if (x + 3 < decoded_data.size) {
            val r = decoded_data[x];
            val g = decoded_data[x + 1];
            val b = decoded_data[x + 2];
            val alpha = decoded_data[x + 3];
            var grey: Byte = ((r * 0.21) + (g * 0.72) + (b * 0.07)).toByte();
            response_arr[x] = grey;
            response_arr[x + 1] = grey;
            response_arr[x + 2] = grey;
            response_arr[x + 3] = alpha;
          }
        } else if(bpp == 24.toShort()) {
          if (x + 2 < decoded_data.size) {
            var r = decoded_data[x];
            var g = decoded_data[x + 1];
            var b = decoded_data[x + 2];
            var grey: Byte = ((r * 0.21) + (g * 0.72) + (b * 0.07)).toByte();
            response_arr[x] = grey;
            response_arr[x + 1] = grey;
            response_arr[x + 2] = grey;
          }
        }
      }

      val encoded_data = Base64.getEncoder().encode(response_arr);

      var response_map = mutableMapOf<String, String>();
      response_map.put("data", String(encoded_data));

      val gson = GsonBuilder().create();
      val json = gson.toJson(response_map).toByteArray();

      t.getResponseHeaders().add("content-type", "json");
      t.sendResponseHeaders(200, json.size.toLong());
      os.write(json);
      os.close();
    }
  }
}

fun sendGet(url: String): String{

	val obj: URL = URL(url);
	val con: HttpURLConnection = obj.openConnection() as HttpURLConnection;

	// optional default is GET
	con.setRequestMethod("GET");

	//add request header
  //con.setRequestProperty("User-Agent",  "Mozilla/5.0");

	val responseCode: Int = con.getResponseCode();
  if (responseCode != 200)
    return "nil"

  return String(con.getInputStream().readBytes());
}
