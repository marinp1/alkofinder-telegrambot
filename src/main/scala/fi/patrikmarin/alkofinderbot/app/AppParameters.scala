package fi.patrikmarin.alkofinderbot.app

import org.scala_tools.time.Imports._

/** 
 *  Parameters for the bot.
 */
object AppParameters {
    // The name of the bot
    val TELEGRAM_BOT_NAME: String = "Alkofinder-bot"
    
    // The path to the phantomjs binary
    val PHANTOM_LOCATION: String = "src/main/resources/phantomjs/phantomjs"
    
    // The datetime format for data reading
    val DATETIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    
    // The file where to store the fetched data
    val ALKO_DATA_LOCATION: String = "src/main/resources/data.json"
}