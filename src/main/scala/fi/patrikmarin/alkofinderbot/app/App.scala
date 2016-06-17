package fi.patrikmarin.alkofinderbot.app

import org.scala_tools.time.Imports._
import org.telegram.telegrambots.TelegramBotsApi
import org.telegram.telegrambots.logging.BotLogger
import fi.patrikmarin.alkofinderbot.dummy.Alko
import fi.patrikmarin.alkofinderbot.bot.AlkoBot
import fi.patrikmarin.alkofinderbot.service.JsonReader
import fi.patrikmarin.alkofinderbot.dummy.Location
import fi.patrikmarin.alkofinderbot.service.LocationManager

/** 
 *  The main App for the program.
 *  Contains main functions for the bot and filtering
 *  methods for Alko stores.
 */
object App extends App {
  private var telegramBotsApi: TelegramBotsApi = null;
  // The container for generated Alko objects.
  var alkos: Array[Alko] = Array()
  
  /** 
   *  Registers the bot and tries toread the store data
   *  from stored JSON file.
   */
  private def registerBot(): Unit = {
    telegramBotsApi = new TelegramBotsApi()
    alkos = Array[Alko]()
    
    try {
      telegramBotsApi.registerBot(new AlkoBot())
      JsonReader.readAlkosFromFile()
    } catch {
      case e: Throwable => BotLogger.error("MAIN", e)
    }
  }
  
  /**
   * Finds three closest Alko stores based on given location.
   * 
   * @return three Alko stores that are closest to the given coordinates.
   */
  def getClosestAlkos(loc: Location): Array[Alko] = {
    alkos.sortBy((x: Alko) => LocationManager.getDistance(loc, x.location)).take(3)
  }
  
  /**
   * Finds Alko stores whose address contains the search parameter.
   * 
   * @return list of Alko stores that are in given city
   */
  def getAlkosByCity(city: String): Array[Alko] = {
    alkos.filter(x => x.address.toLowerCase().contains(city.toLowerCase()))
  }
  
  /**
   * Finds Alko stores by name or by address.
   * 
   * @return list of Alko stores that match the search parameter.
   */
  def getAlkosByString(text: String): Array[Alko] = {
    alkos.filter(x => x.address.toLowerCase().contains(text.toLowerCase()) || x.title.toLowerCase().contains(text.toLowerCase()))
  }
  
  // On startup register the bot
  override def main(args: Array[String]) {
      registerBot()
  }
}