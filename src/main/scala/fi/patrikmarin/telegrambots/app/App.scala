package fi.patrikmarin.telegrambots.app

import org.scala_tools.time.Imports._
import fi.patrikmarin.telegrambots._

/** 
 *  The main App for the program.
 *  Contains main functions for the bot and filtering
 *  methods for Alko stores.
 */
object App extends App {
  
  // On startup register bots
  override def main(args: Array[String]) {
      alkofinderbot.bot.Logic.registerBot()
      otasubibot.bot.Logic.registerBot()
  }
}