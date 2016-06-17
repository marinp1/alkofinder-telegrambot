# alkofinder-telegrambot
Telegram bot for finding Alko stores made with Scala.

Uses Telegram Bot API Java implementation found in https://github.com/rubenlagus/TelegramBots.

The bot fetches data of Alko stores once a day from Alko's website and
uses Google's Geocoding API to get coordinates for addresses.

### Instructions
Create a file called `Keys.scala` in package `fi.patrikmarin.alkofinderbot.app`.

File content:
```java
object Keys {
  val GOOGLE_API_KEY: String = ???
  val TELEGRAM_BOT_TOKEN: String = ???
}
```

### Current features
* Command **/help** - information of the bot
* Command **/[searchtext]** - finds list of stores where searchtext is part of store's name or address
* Sent **location** - finds three closest stroes from given location
* **Inline query** - allows user to find stores by location
