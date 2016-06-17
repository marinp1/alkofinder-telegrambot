package fi.patrikmarin.alkofinderbot.service

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.scala_tools.time.Imports._
import java.io._
import fi.patrikmarin.alkofinderbot.app.App
import fi.patrikmarin.alkofinderbot.app.AppParameters
import fi.patrikmarin.alkofinderbot.app.Utils
import fi.patrikmarin.alkofinderbot.dummy.Alko

/**
 * Reads and writes Alko stores from/to a JSON file.
 */
object JsonReader {
  
  // Try to init file
  initFile();
  
  /**
   * Creates data file if it doesn't exist.
   */
  private def initFile() = {
    
    val JSON_FILE = new File(AppParameters.ALKO_DATA_LOCATION)
    
    // If file doesn't exist
    if (!JSON_FILE.exists()) {
      // If parent folder path doesn't exist, create it
      if (!JSON_FILE.getParentFile().exists()) JSON_FILE.getParentFile().mkdirs()
      
      // Create empty settings file
      val pw = new PrintWriter(new File(AppParameters.ALKO_DATA_LOCATION))
      pw.write("{\n}")
      pw.close
    }
  }
  
  /** 
   *  Tries to read store data from file.
   *  If not successful or the data is outdated,
   *  fetch newest information.
   */
  def readAlkosFromFile() = {
    try {
      
      // Read file data to a string
      val source = scala.io.Source.fromFile(AppParameters.ALKO_DATA_LOCATION)
      val lines = try source.mkString finally source.close()
      
      // Initialize parser and parse the string as JSON
      val parser = new JsonParser();
      val o = parser.parse(lines).getAsJsonObject()
      
      // If data has updated-field, check content
      if (o.has("updated")) {
        // Parse updated field to datetime
        val dt = AppParameters.DATETIME_FORMATTER.parseDateTime(o.get("updated").getAsString).toLocalDateTime()
        // If the data was updated today, read content from file
        if (Utils.daysUntil(dt, LocalDateTime.now) == 0) {
          println("Reading data from existing file...")
          
          // Read stores field as array
          val arr: JsonArray = o.get("stores").getAsJsonArray
            
          // Generate Alko store objects with Gson
          val gson = new Gson();
          App.alkos = gson.fromJson(arr, classOf[Array[Alko]])
          
          // Updated last_updated field
          AlkoUpdater.last_update = dt
          
          println("Found " + App.alkos.length + " elements.")
        } else {
          // Otherwise update content
          AlkoUpdater.forceUpdateAlkos()
        }
      } else {
        // If no updated field exists, update content
        AlkoUpdater.forceUpdateAlkos()
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
  
  /**
   * Saves alko objects to a JSON file.
   */
  def saveAlkosToFile() = {
    // Create Gson object for converting data
    // to a JSON array
    val gson = new Gson();
    
    try {
      // Reads existing JSON file
      val source = scala.io.Source.fromFile(AppParameters.ALKO_DATA_LOCATION)
      val lines = try source.mkString finally source.close()
      
      // Parse file content as JSON
      val parser = new JsonParser();
      val o = parser.parse(lines).getAsJsonObject()
      
      // If the file has updates or stores fields,
      // remove them
      if (o.has("updated")) {
        o.remove("updated")
      }
      
      if (o.has("stores")) {
        o.remove("stores")
      }
      
      // Add updated field
      o.addProperty("updated",AppParameters.DATETIME_FORMATTER.print(LocalDateTime.now))
      
      // Add stores field by converting Alkos to JSOn using Gson
      o.add("stores", parser.parse(gson.toJson(App.alkos)).getAsJsonArray);
      
      // Create Gson pretty printer
		  val jsonBeautifier = new GsonBuilder().setPrettyPrinting().create()
			val prettyJsonString = jsonBeautifier.toJson(o)
      
      // Create file writer
			val pw = new PrintWriter(new File(AppParameters.ALKO_DATA_LOCATION))
			
		  // Write the beautified JSON to file
			pw.write(prettyJsonString)
			
			pw.close
			
			System.out.println("Alko data updated.");
      
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
}