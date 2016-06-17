package fi.patrikmarin.alkofinderbot.service

import scala.xml._
import fi.patrikmarin.alkofinderbot.app.Keys
import fi.patrikmarin.alkofinderbot.dummy.Location

/**
 * Service class for finding geolocation
 * for address using Google's geocoding API.
 */
object LocationManager extends {
  
  // The base URL for the API
  private val BASE_URL = "https://maps.googleapis.com/maps/api/geocode/xml?address="
  // The key part of the API
  private val KEY_PART = "&key=" + Keys.GOOGLE_API_KEY
  
  // Trigger for checking if the quota was exceeded
  var overQuota = false
  
  /**
   * Corrects address for Google API.
   * In the address
   * 	commas should be removed and
   * 	spaces should be rplaced as plus signs.
   * 
   * Also "finland" is prepended to the address for
   * increasing search reliability.
   * 
   * @return the corrected address
   */
  private def correctedAddress(address: String) : String = {
    var fixedAddress = "finland+" + address.toLowerCase().replace(",","").replace(" ", "+")
    return fixedAddress
  }
  
  /**
   * Fetches API response for given address.
   * 
   * @return the geocoded location for the address if it was found
   */
  def getLocation(address: String): Option[Location] = {
    // Generate the URL for the API
    val URL = BASE_URL + correctedAddress(address) + KEY_PART
    println(URL)
    // Read web page content to a string
    val response: String = scala.io.Source.fromURL(URL).mkString
    
    // Fetch coordinates from the website content
    val coords = extractLatLon(response)
    
    if (coords.isDefined) {
      Some(new Location(coords.get._1, coords.get._2))
    } else {
      None
    }
  }
  
  /**
   * Extracts the location coordinates from the API result
   * with XPath
   * 
   * @return a tuple of found coordinates
   */
  private def extractLatLon(xmlString: String): Option[(Double, Double)] = {
    var location: Option[(Double, Double)] = None
    // Remove all possible control characters that mess with parsing
    val finalString = xmlString.trim().replaceAll("\\p{C}", "")
    
    // Parse given string as XML
    val xml = XML.loadString(finalString)
    
    // Find location node with XPath
    val xmlNodes = xml \\ "geometry" \ "location"
    
    // If result was found, read it
    if (xmlNodes.size > 0) {
      val xmlNode = xmlNodes(0)
      try {
        
        // Fetch latitude for the location
        val lat = (xmlNode \\ "lat") text
        // Fetch longitude for the location
        val lon = (xmlNode \\ "lng") text
        
        location = Option(lat.trim().toDouble, lon.trim().toDouble)
        
      } catch {
        case e: Throwable => e.printStackTrace()
      }
    }
    
    return location
  }
  
  /**
   * Gets a direct distance of two locations, using
   * Pythagora's theorem.
   * 
   * @return the distance of given locations
   */
  def getDistance(l1: Location, l2: Location): Double = {
    val xDiff = Math.abs(l1.lat - l2.lat)
    val yDiff = Math.abs(l1.lon - l2.lon)
    
    return Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2))
  }
}