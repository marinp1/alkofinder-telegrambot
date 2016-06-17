package fi.patrikmarin.alkofinderbot.app

import org.scala_tools.time.Imports._

/** 
 *  Parameters for the bot.
 */
object AppParameters {
    // The name of the bot
    val TELEGRAM_BOT_NAME: String = "Alko locator"
    // The Chrome driver location, required for Selenium
    val CHROME_DRIVER_LOCATION: String = "src/main/resources/chromedriver"
    
    val DATETIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    
    val ALKO_DATA_LOCATION: String = "src/main/resources/data.json"
}