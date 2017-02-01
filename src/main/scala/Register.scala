import akka.actor._
import scala.concurrent.duration._
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._

object Register {
  sealed trait Article
  case object Espresso extends Article
  case object Cappuccino extends Article
  case class Transaction(article: Article)
}

class Register extends Actor with ActorLogging {
  import akka.pattern.ask
  import akka.pattern.pipe
  import akka.util.Timeout
  import context.dispatcher
  import Register._
  import Barista._
  import ReceiptPrinter._

  implicit val timeout = Timeout(4.seconds)
  var revenue = 0
  val prices = Map[Article, Int](Espresso -> 150, Cappuccino -> 250)
  val printer = context.actorOf(Props[ReceiptPrinter], "Printer")

  override def postRestart(reason: Throwable) {
    super.postRestart(reason)
    log.info(s"Restarted, and revenue is $revenue cents")
  }

  def receive = {
    case Transaction(article) =>
      val price = prices(article)
      val requester = sender
      (printer ? PrintJob(price)).map((requester, _)).pipeTo(self)
    case (requester: ActorRef, receipt: Receipt) =>
      revenue += receipt.amount
      log.info(s"revenue is $revenue cents")
      requester ! receipt
  }

  val decider: PartialFunction[Throwable, Directive] = {
    // handle only paper jam exception ...
    case _: PaperJamException => {
      log.info(s"got exception, now revenue is $revenue cents")
      Resume
    }
  }

  override def supervisorStrategy: SupervisorStrategy = {
    // ... leave the rest for the default strategy
    OneForOneStrategy()(decider.orElse(SupervisorStrategy.defaultStrategy.decider))
  }
}
