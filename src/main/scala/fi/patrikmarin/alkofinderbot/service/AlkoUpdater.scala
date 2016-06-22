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

import java.util.concurrent.TimeUnit

import org.openqa.selenium.phantomjs.PhantomJSDriver
import org.openqa.selenium.remote.DesiredCapabilities;

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
  
  // Last time the store data was fetched
  var last_update: LocalDateTime = null
  var failCount = 0
  
  /**
   * Creates a Chrome instance with Selenium and navigates
   * to correct URL.
   * 
   * @return the loaded website as a Document or None if error occurred.
   */
  private def getDocument(): Option[Document] = {
    
    try {
      
      // Set capabilities for the driver
      val capabilities = new DesiredCapabilities()
      capabilities.setJavascriptEnabled(true)
      capabilities.setCapability("takesScreenshot", false)
      
      // Set system property for the phantomjs binary
      System.setProperty("phantomjs.binary.path", AppParameters.PHANTOM_LOCATION)
      
      // Create WebDriver as PhantomJSDriver with capabilities
      val driver = new PhantomJSDriver(capabilities)
      val js = "var page = this; page.clearCookies(); page.clearMemoryCache(); page.close(); return 'DONE';";
      
      // Wait 5 seconds so the PhantomJS doesn't fail on random occasions
      Thread.sleep(5000)
      
      driver.get(ALKO_URL)
      // Wait maximum of 45 seconds for the page to load
      driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS)
      
      // Create JsoupBrowser instance in order to parse the data
      val browser = new JsoupBrowser()
      val doc: Document = browser.parseString(driver.getPageSource())
      
      // Close the browser
      driver.quit()
      
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
    println("Fetching new list... Attempt " + failCount + " / " + 20)
    
    // Try to fetch new content
    val tryFetch = AlkoUpdater.getAlkos()
    
    // If successful, update the container and 
    // save the data to file
    if (tryFetch.isDefined) {
      
      if (tryFetch.get.nonEmpty) {
        App.alkos = tryFetch.get.toArray
        JsonReader.saveAlkosToFile()
        last_update = LocalDateTime.now;
        println("[DONE] Returning results for " + tryFetch.get.size + " stores.")
        return true
      } else if (failCount <= 20) {
        Thread.sleep(2000)
        println("[ERROR] Failed to get data.")
        failCount += 1
        return forceUpdateAlkos()
      } else {
        println("Couldn't update list, shutting down...")
        System.exit(0)
        return false
      }
    } else {
      println("Error, shutting down...")
      System.exit(0)
      return false
    }
  }
}