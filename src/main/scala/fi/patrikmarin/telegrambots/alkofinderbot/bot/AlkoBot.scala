package fi.patrikmarin.telegrambots.alkofinderbot.bot

import org.scala_tools.time.Imports._
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
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._
import org.scala_tools.time.Imports._
import fi.patrikmarin.telegrambots.app.Keys
import fi.patrikmarin.telegrambots.alkofinderbot.dummy.Alko
import fi.patrikmarin.telegrambots.alkofinderbot.service.AlkoUpdater
import fi.patrikmarin.telegrambots.alkofinderbot.dummy.Location
import fi.patrikmarin.telegrambots.alkofinderbot.service.AlkoUpdater
import fi.patrikmarin.telegrambots.app.App

/**
 * The bot itself, contains methods for answering
 * queries send to the bot.
 */
class AlkoBot extends TelegramLongPollingBot  {
  
  // Time in seconds to save inline query results
  // TODO: parametrize?
  private final val CACHETIME: Int = 60;
  
  /**
   * Checks if the Alko list needs to be updated and
   * calls the update method if it needs updating.
   * 
   * @param msg the message that the user sent
   * @return true if the list is correct
   */
  private def updateList(msg: Either[Message, InlineQuery]): Boolean = {    
    if(Logic.daysUntil(AlkoUpdater.last_update, LocalDateTime.now) == 0) {
      return true
    } else {
      if (msg.isLeft) {
        // Send status message for user to inform
        // that the program is updating
        val sendMessageRequest = new SendMessage()
        sendMessageRequest.setChatId(msg.left.get.getChatId().toString())
        sendMessageRequest.setText("Päivitetään...\nOdota hetki.")
        try {
          sendMessage(sendMessageRequest)
        } catch {
          case e: Throwable => e.printStackTrace()
        }
      } else {
        val inlineQuery = msg.right.get
        try {
          answerInlineQuery(getUpdateInlineQueryResult(inlineQuery));
        } catch {
          case e: Throwable => e.printStackTrace()
        }
      }
      
      // Update the list
      return AlkoUpdater.forceUpdateAlkos();
    }
  }
  
  /**
   * The help message content which can be accessed
   * with comman /help.
   */
  private def helpMsg(msg: Message): Unit = {
    val sendMessageRequest = new SendMessage();
    sendMessageRequest.enableMarkdown(true);
    sendMessageRequest.setChatId(msg.getChatId().toString());
    sendMessageRequest.setText("Osaan kolme asiaa:\n\n" + 
        "1. Lähetä minulle *sijainti* niin kerron kolme lähintä Alkoa.\n" + 
        "2. *Inline-queryn* avulla voit hakea Alkoja paikkakunnan mukaan.\n" + 
        "3. Komennolla */hakuteksti* voit myös hakea Alkoja nimen tai osoitteen mukaan.\n\n" + 
        "Löydettyjen alkojen määrä: *" + Logic.alkos.size + "*\n" + 
        "Viimeksi päivitetty: *" + AppParameters.DATETIME_FORMATTER.print(AlkoUpdater.last_update) + "*\n" +
        "Ohjelma päivittyy itsestään kun tarve vaatii.")
      
    try {
      sendMessage(sendMessageRequest)
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
  
  /**
   * Generates a venue from given message and Alko store.
   * 
   * @return the generated venue
   */
  private def generateAlkoVenue(msg: Message)(alko: Alko): SendVenue = {
    val sendVenueRequest = new SendVenue();
    
    sendVenueRequest.setChatId(msg.getChatId().toString())
    
    sendVenueRequest.setLatitude(alko.location.lat.toFloat)
    sendVenueRequest.setLongitude(alko.location.lon.toFloat)
    
    sendVenueRequest.setAddress(alko.address)
    
    sendVenueRequest.setTitle(alko.title + " (" + alko.hours + ")")
    
    return sendVenueRequest;
  }
  
  /**
   * Gives user list of Alko stores that were found by user custom
   * search term, using command /[searchtext].
   */
  private def alkosByText(msg: Message) = {
    // Check update
    if(updateList(Left(msg))) {
      // The found Alko stores by the search parameter
      val closestAlkos: Array[Alko] = Logic.getAlkosByString(msg.getText().substring(1))
      // The list which to loop
      var filteredAlkos = closestAlkos
      
      var overTen = false
      //TODO: Parametrize?
      val maxCount = 5
      
      // If the result set is too large, take only some from
      // the beginning
      if (closestAlkos.size > maxCount) {
        filteredAlkos = closestAlkos.take(maxCount)
        overTen = true
      }
      
      // Initialize possible status message
      val sendMessageRequest = new SendMessage();
      sendMessageRequest.setChatId(msg.getChatId().toString())
      sendMessageRequest.enableMarkdown(true)
      
      // Set status message text
      if (closestAlkos.size == 0) {
        // Inform user if no stores were found
        sendMessageRequest.setText("Haulla *" + msg.getText().substring(1) + "* ei valitettavasti löytynyt yhtään tulosta.");
      } else if (overTen) {
        // Inform user that not all results are displayed
        sendMessageRequest.setText("Näytetään *" + maxCount + " / " + closestAlkos.size + "* tulosta.\n" + 
          "Täsmennä hakua saadaksesi tarkempia tuloksia.")
      }
  
      // Loop the modified array
      for(alko: Alko <- filteredAlkos) {
        
        // Generate alko venue to be sent
        val sendVenueRequest = generateAlkoVenue(msg)(alko)
        
        try {
          sendVenue(sendVenueRequest)
        } catch {
          case e: Throwable => e.printStackTrace()
        }
      }
      
      // Finally if the bot needs to send the status
      // message, do so
      try {
        if (overTen|| closestAlkos.size == 0) {
            sendMessage(sendMessageRequest)
        }
      } catch {
        case e: Throwable => e.printStackTrace()
      }
    }
  }
  
  /** 
   *  Finds and messages user the closest n Alko stores
   *  based on user location.
   */
  private def closestAlkos(msg: Message): Unit = {
    // Check update
    if(updateList(Left(msg))) {
      if ((msg.getForwardFrom != null && msg.getForwardFrom.getId == this.getMe.getId) || msg.getFrom.getId == this.getMe.getId) {
        // If the message was sent by the bot or forwarded from the bot, don't do anything.
        println("Skip response to own message.")
      } else if(msg.getVenue != null) {
         // If the message was a venue, don't do anything (so the bot doesn't reply its own inlineQuery)
        println("Skip response to venues.")
      } else {
        // Find the closest Alko stores
        val closestAlkos: Array[Alko] = Logic.getClosestAlkos(new Location(msg.getLocation.getLatitude, msg.getLocation.getLongitude))
        
        // Loop through the found stores
        for (alko: Alko <- closestAlkos) {
          // Generate venue for a store
          val sendVenueRequest = generateAlkoVenue(msg)(alko)
          
          // Send the message to user
          try {
            sendVenue(sendVenueRequest)
          } catch {
            case e: Throwable => e.printStackTrace()
          }
        }
      }
      
    }
  }
  
  /**
   * Answers user inline query by searching stores based on
   * the querys content.
   */
  private def getAlkosForInline(inlineQuery: InlineQuery): Unit = {
    // Check updates
    if(updateList(Right(inlineQuery))) {
      val query: String = inlineQuery.getQuery()
      // Log the query
      println("Searching: " + query)
      
      try {
          // While to query is too short, don't do anything.
          if (query.size > 3) {
             
            // Find data by the query text and answer
            val alkos = Logic.getAlkosByCity(query)
            answerInlineQuery(convertResultsToResponse(inlineQuery, alkos));
          } else {
            answerInlineQuery(convertResultsToResponse(inlineQuery, Array()));
          }
      } catch {
        case e: Throwable => e.printStackTrace()
      }
      
    }
  }
  
  /**
   * Answers to an inline query if the program is updating.
   */
  private def getUpdateInlineQueryResult(inlineQuery: InlineQuery): AnswerInlineQuery = {
    val answerInlineQuery = new AnswerInlineQuery()
    
    answerInlineQuery.setInlineQueryId(inlineQuery.getId())
    answerInlineQuery.setCacheTime(CACHETIME);
    
    val results = new ArrayBuffer[InlineQueryResult]()
    
    val result = new InlineQueryResultArticle()
    result.setId("UpdateID" + Math.random() * 100)
    result.setTitle("Päivitetään...")
    val message = new InputTextMessageContent()
    message.enableMarkdown(true)
    message.setMessageText("Yritä myöhemmin uudelleen.")
    result.setInputMessageContent(message)
    results += result
    
    answerInlineQuery.setResults(results.asJava)
    
    return answerInlineQuery;
  }
  
  /** 
   *  Converts list of alko stores and query to a query answer.
   *  
   *  @return the generated inline query answer
   */
  private def convertResultsToResponse(inlineQuery: InlineQuery, alkos: Array[Alko]): AnswerInlineQuery = {
    // Initialize the answer
    val answerInlineQuery = new AnswerInlineQuery();
    
    answerInlineQuery.setInlineQueryId(inlineQuery.getId());
    answerInlineQuery.setCacheTime(CACHETIME);
    
    // Populates the results
    answerInlineQuery.setResults(convertResults(alkos)(inlineQuery.getId));
    
    return answerInlineQuery;
  }
  
  /**
   * Converts list of Alko stores to a list of query results 
   * to be displayed in the answer.
   * 
   * @return Java list of generated query results
   */
  private def convertResults(alkos: Array[Alko])(id: String): java.util.List[InlineQueryResult] = {
    val results = new ArrayBuffer[InlineQueryResult]()
    
    // Loop through given stores
    for (i <- 0 until alkos.length) {
      val alko: Alko = alkos(i)
      
      // Generate new Venue
      val venue = new InlineQueryResultVenue()
      
      venue.setId(id + i)
      venue.setLatitude(alko.location.lat.toFloat)
      venue.setLongitude(alko.location.lon.toFloat)
      venue.setAddress(alko.address)
      venue.setTitle(alko.title + " (" + alko.hours + ")")
      
      results += venue
    }
    
    // Convert Scala Array to Java list
    val realResult: java.util.List[InlineQueryResult] = results.asJava
    
    return realResult
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
        } else if (msg.getText().startsWith("/")) {
          // Display Alkos found by free search
          alkosByText(msg)
        }
      } else if (msg.hasLocation()) {
        // If the message is location
        // find Alko stores by location
        closestAlkos(msg)
      }
    }
    
    // If the message is an inline query
    if (update.hasInlineQuery()) {
      // Read the query
      val query = update.getInlineQuery()
      
      // Send query results
      getAlkosForInline(query)
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
    return Keys.ALKOBOT_TELEGRAM_BOT_TOKEN
  }
}