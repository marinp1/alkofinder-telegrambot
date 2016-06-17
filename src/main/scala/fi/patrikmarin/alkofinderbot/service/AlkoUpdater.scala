package fi.patrikmarin.alkofinderbot.service

import org.scala_tools.time.Imports._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Document
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import scala.collection.mutable.ArrayBuffer
import fi.patrikmarin.alkofinderbot.app.AppParameters
import fi.patrikmarin.alkofinderbot.dummy.Alko
import fi.patrikmarin.alkofinderbot.app.App

/**
 * Creates a web browser instance (Chrome) with Selenium and 
 * fetches all Alko stores found in Alko's site.
 */
object AlkoUpdater {
  // The URL of the stores
  private val ALKO_URL: String = "http://www.alko.fi/haku/myymalat/?tags=(6445)&page=30&sort=1"
  
  // Class names for finding the correct elements
  private val RES_DIV_CLASS: String     = "."  +  "search-result-details"
  private val TITLE_DIV_CLASS: String   = "."  +  "result-title"
  private val OPEN_HOURS_CLASS: String  = "."  +  "opening-hours-value"
  private val ADDRESS_CLASS: String     = "."  +  "address-value"
  
  // Timeout to wait for the page to load
  private val PAGE_TIMEOUT: Int = 15000
  
  // Last time the store data was fetched
  var last_update: LocalDateTime = null;
  
  /**
   * Creates a Chrome instance with Selenium and navigates
   * to correct URL.
   * 
   * @return the loaded website as a Document or None if error occurred.
   */
  private def getDocument(): Option[Document] = {
    
    try {
      // Initialise browser
      System.setProperty("webdriver.chrome.driver", AppParameters.CHROME_DRIVER_LOCATION);
      val options = new org.openqa.selenium.chrome.ChromeOptions()
      options.setBinary("/usr/bin/chromium-browser")
      val client = new org.openqa.selenium.chrome.ChromeDriver(options)
      
      
      // Navigate
      client.get(ALKO_URL)
      
      // Wait for content to load
      Thread.sleep(PAGE_TIMEOUT)
      
      // Create JsoupBrowser instance in order to parse the data
      val browser = new JsoupBrowser()
      val doc: Document = browser.parseString(client.getPageSource)
      
      // Close the browser
      client.quit()
      
      return Some(doc)
    } catch {
      case e: Throwable => e.printStackTrace()
      return None
    }
  }
  
  /**
   * Parses store data from website content and 
   * creates a dummy class for each store.
   * 
   * @return a list of generated Alko elements
   */
  private def getResults(doc: Document): ArrayBuffer[Alko] = {
    val stores: ArrayBuffer[Alko] = new ArrayBuffer()

    // Find list of elements that have class "RES_DIV_CLASS"
    val mainResult: List[Element] = doc >> elementList(RES_DIV_CLASS)
    
    println("Found " + mainResult.size + " store elements.")
    
    // Loop through the results
    for (elem <- mainResult) {
      // Find title element of the store
      val titles: List[Element] = (elem >> elementList(TITLE_DIV_CLASS))
      val title = titles.head >> text("h3")
      
      // Find open hours of the store
      val opens: List[Element] = (elem >> elementList(OPEN_HOURS_CLASS))
      val open = opens.head >> text("span")
      
      // Find the address of the store
      val locs: List[Element] = (elem >> elementList(ADDRESS_CLASS))
      val loc = locs.head >> text("span")
      
      // If quota was exceeded, stop fetching data.
      if (!LocationManager.overQuota) {
        // Try to geocode the location using Google's Geocoding API
        val geoloc = LocationManager.getLocation(loc)
        
        // Wait a while before continuing
        Thread.sleep(10)
        
        // If geoloc was found correctly, add store
        // to the list, otherwise quit and inform error.
        if (geoloc.isDefined) {
          System.out.println("Fetched data for " + title)
          stores += new Alko(title, open, loc, geoloc.get)
        } else {
          System.out.println("Failed to get data for " + title)
          System.out.println(loc)
          
          System.exit(1)
        }
      }

    }
    
    println("Returning results for " + stores.size + " stores.")
    
    // Return list of found stores
    return stores
  }
  
  /**
   * Calls the Selenium instance to read website content and
   * calls the parse method if content is available.
   * 
   * @return list of found elements
   */
  private def getAlkos(): Option[ArrayBuffer[Alko]] = {
    val doc = getDocument()
    var results: Option[ArrayBuffer[Alko]] = None
    
    if (doc.isDefined) {
      println("Trying to fetch results...")
      results = Some(getResults(doc.get))
    } else {
      println("Error with fetching...")
    }

    return results
  }

  /**
   * Calls methods for updating store information.
   * If fetch was not successful, close program.
   * 
   * @return true if the update was successful
   */
  def forceUpdateAlkos(): Boolean = {
    println("Fetching new list...")
    
    // Try to fetch new content
    val tryFetch = AlkoUpdater.getAlkos()
    
    // If successful, update the container and 
    // save the data to file
    if (tryFetch.isDefined) {
      App.alkos = tryFetch.get.toArray
      JsonReader.saveAlkosToFile()
      last_update = LocalDateTime.now;
      return true
    } else {
      println("Error, shutting down...")
      System.exit(0)
      return false
    }
  }
}