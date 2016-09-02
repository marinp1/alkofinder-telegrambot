package fi.patrikmarin.telegrambots.alkofinderbot.bot

import org.scala_tools.time.Imports._

import fi.patrikmarin.telegrambots.alkofinderbot.service._
import fi.patrikmarin.telegrambots.alkofinderbot.dummy._
import org.telegram.telegrambots.TelegramBotsApi
import org.telegram.telegrambots.logging.BotLogger
import fi.patrikmarin.telegrambots.alkofinderbot.dummy.Alko
import fi.patrikmarin.telegrambots.alkofinderbot.service.JsonReader
import fi.patrikmarin.telegrambots.alkofinderbot.dummy.Location
import fi.patrikmarin.telegrambots.alkofinderbot.service.LocationManager

/**
 * Object containing miscellaneous helper functions.
 */
object Logic {
  
  private var telegramBotsApi: TelegramBotsApi = null;
  
  var alkos: Array[Alko] = Array()
  
  /** 
   *  Registers the bot and tries to read the store data
   *  from stored JSON file.
   */
  def registerBot(): Unit = {
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
   * Calucates the number of days that are
   * between given datetimes.
   */
  def daysUntil(source: LocalDateTime, target: LocalDateTime): Int = {
    // Copy the source date
    var newDate = source
    
    var daysUntil = 0;
    
    // Increment newDate by one day until it matches target
    while (!newDate.toLocalDate().isEqual(target.toLocalDate())) {
      newDate = newDate.plusDays(1)
      daysUntil += 1
    }
    
    // Return the result
    return daysUntil
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
}