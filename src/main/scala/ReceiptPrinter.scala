import akka.actor._

object ReceiptPrinter {
  case class PrintJob(amount: Int)
  class PaperJamException(msg: String) extends Exception(msg)
}

class ReceiptPrinter extends Actor with ActorLogging {
  import ReceiptPrinter._
  import Barista._
  import util.Random

  var paperJam = false
  override def postRestart(reason: Throwable) {
    super.postRestart(reason)
    log.info(s"Restarted, paper jam == $paperJam")
  }

  def receive = {
    case PrintJob(amount) => sender ! createReceipt(amount)
  }

  def createReceipt(price: Int): Receipt = {
    if (Random.nextBoolean()) paperJam = true
    if (paperJam) throw new PaperJamException("OMG, not again!")
    Receipt(price)
  }
}
