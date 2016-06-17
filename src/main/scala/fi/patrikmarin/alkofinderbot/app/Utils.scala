package fi.patrikmarin.alkofinderbot.app

import org.scala_tools.time.Imports._

/**
 * Object containing miscellaneous helper functions.
 */
object Utils {
  
  /**
   * Calucates the number of days that are
   * between given datetimes.
   */
  def daysUntil(source: LocalDateTime, target: LocalDateTime): Int = {
    // Copy the source date
    var newDate = LocalDateTime.fromDateFields(source.toDate())
    
    var daysUntil = 0;
    
    // Increment newDate by one day until it matches target
    while (!newDate.toLocalDate().isEqual(target.toLocalDate())) {
      newDate = newDate.plusDays(1)
      daysUntil += 1
    }
    
    // Return the result
    return daysUntil
  }
}