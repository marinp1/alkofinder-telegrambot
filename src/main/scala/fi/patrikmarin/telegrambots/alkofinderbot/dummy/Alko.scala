package fi.patrikmarin.telegrambots.alkofinderbot.dummy

/**
 * Represents an Alko store.
 * Currently only normal stores are fetched.
 * 
 * TODO: Save also phone number and type of store?
 */
class Alko(val title: String, val hours: String, val address: String, val location: Location) {
  
}