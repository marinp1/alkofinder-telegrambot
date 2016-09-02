package fi.patrikmarin.telegrambots.otasubibot.bot

import org.telegram.telegrambots.api.methods.AnswerInlineQuery
import org.telegram.telegrambots.api.methods.send.SendMessage
import org.telegram.telegrambots.api.methods.send.SendVenue
import org.telegram.telegrambots.api.objects.Update
import org.telegram.telegrambots.api.objects.Message
import org.telegram.telegrambots.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent
import org.telegram.telegrambots.api.objects.inlinequery.InlineQuery
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResult
import org.telegram.telegrambots.api.objects.inlinequery.result.InlineQueryResultVenue
import org.telegram.telegrambots.api.objects.inlinequery.result._

import org.telegram.telegrambots.bots.TelegramLongPollingBot

import fi.patrikmarin.telegrambots.app.Keys
import org.scala_tools.time.Imports._

import scala.collection.mutable.Map

class OtasubiBot extends TelegramLongPollingBot  {
  
  
  var sublist = Map("1"   -> "Kana Fajita",
                    "2"   -> "Tonnikala",
                    "3"   -> "Kananrinta",
                    "4"   -> "Italian B.M.T.",
                    "5"   -> "Vegepihvi",
                    "6"   -> "American Steakhouse Melt",
                    "7"   -> "Kinkku");
  
  val daymap =  Map("ma"   -> "1",
                    "ti"   -> "2",
                    "ke"   -> "3",
                    "to"   -> "4",
                    "pe"   -> "5",
                    "la"   -> "6",
                    "su"   -> "7");
  
  
  /**
   * The help message content which can be accessed
   * with command /help.
   */
  private def helpMsg(msg: Message): Unit = {
    val sendMessageRequest = new SendMessage();
    sendMessageRequest.enableMarkdown(true);
    sendMessageRequest.setChatId(msg.getChatId().toString());
    sendMessageRequest.setText("Kertoo päivän subin")
      
    try {
      sendMessage(sendMessageRequest)
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
  
  /**
   * Answers user request with today's daily sub.
   */
  private def getDailySub(msg: Message): Unit = {
    val sendMessageRequest = new SendMessage();
    sendMessageRequest.enableMarkdown(true);
    sendMessageRequest.setChatId(msg.getChatId().toString());
    
    val weekday = LocalDateTime.now.dayOfWeek().get()
    
    val subi = sublist.get(weekday.toString())
    
    sendMessageRequest.setText(subi.getOrElse("Hupsista, ilmota Patulle."))
      
    try {
      sendMessage(sendMessageRequest)
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
  
  private def updateSubList(msg: Message): Unit = {
    
    val sendMessageRequest = new SendMessage();
    sendMessageRequest.enableMarkdown(true);
    sendMessageRequest.setChatId(msg.getChatId().toString());
    
    if (msg.getFrom().getId == 35726235) {
      
      val text = msg.getText().split("::");
      
      if (text.length == 3 && daymap.contains(text(1))) {
        val newSub = text(2)
        
        val dayNum = daymap(text(1).toLowerCase())
        
        sublist(dayNum) = newSub.capitalize
        
        sendMessageRequest.setText("Subi päivitetty onnistuneesti.")
        
      } else {
        sendMessageRequest.setText("Päivitysesimerkki: /update::ma::kananrinta")
      }
      
    } else {
      sendMessageRequest.setText("Äläs ny")
    }
      
    try {
      sendMessage(sendMessageRequest)
    } catch {
      case e: Throwable => e.printStackTrace()
    }
    
    
  }
  
  /** 
   *  Handles received user messages.
   */
  def onUpdateReceived(update: Update): Unit = {
    // Check that there is new message
    if (update.hasMessage()) {
      // Read the new message
      val msg = update.getMessage()
      
      if (msg.hasText()) {
        // If the message is text
        if (msg.getText() == "/help") {
          // Display bot help
          helpMsg(msg)
        } else if (msg.getText() == "/get_subi" || msg.getText() == "/get_subi@otasubi_bot") {
          getDailySub(msg)
        } else if (msg.getText().startsWith("/update")) {
          updateSubList(msg)
        }
      }
    }
  }
  
  /**
   * Gets the bot's user name.
   * 
   * @return bot's username
   */
  def getBotUsername() : String = {
      AppParameters.TELEGRAM_BOT_NAME
  }

  /**
   * Gets bot token.
   * 
   * @return the token of the bot
   */
  def getBotToken(): String = {
    return Keys.OTASUBI_TELEGRAM_BOT_TOKEN
  }
}