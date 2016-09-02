package fi.patrikmarin.telegrambots.otasubibot.bot

import org.telegram.telegrambots.TelegramBotsApi
import org.telegram.telegrambots.logging.BotLogger

object Logic {
  
  private var telegramBotsApi: TelegramBotsApi = null;
  
  def registerBot(): Unit = {
    telegramBotsApi = new TelegramBotsApi()
    
    try {
      telegramBotsApi.registerBot(new OtasubiBot())
    } catch {
      case e: Throwable => BotLogger.error("MAIN", e)
    }
  }
}