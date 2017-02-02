package coffee {
  import akka.actor._

  object Barista {
    case object EspressoRequest
    case object ClosingTime
    case class EspressoCup(state: EspressoCup.State)
    object EspressoCup {
      sealed trait State
      case object Clean extends State
      case object Filled extends State
      case object Dirty extends State
    }
    case class Receipt(amount: Int)
    case object ComebackLater
  }

  class Barista extends Actor {
    import coffee.Barista._
    import coffee.Barista.EspressoCup._
    import coffee.Register._
    import context.dispatcher
    import akka.util.Timeout
    import akka.pattern.ask
    import akka.pattern.pipe
    import akka.pattern.AskTimeoutException
    import concurrent.duration._

    implicit val timeout = Timeout(4.seconds)
    val register = context.actorOf(Props[Register], "Register")

    def receive = {
      case EspressoRequest =>
          val receipt = register ? Transaction(Espresso)
          receipt.map((EspressoCup(Filled), _)).recover {
            case _: AskTimeoutException => ComebackLater
          } pipeTo(sender)
      case ClosingTime => context.system.terminate()
    }
  }
}
