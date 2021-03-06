package coffee {
  import akka.actor._
  import scala.concurrent.duration._

  object Customer {
    case object CaffeineWithdrawalWarning
  }

  class Customer(coffeeSource: ActorRef) extends Actor with ActorLogging {
    import coffee.Customer._
    import coffee.Barista._
    import coffee.Barista.EspressoCup._
    import context.dispatcher

    context.watch(coffeeSource)

    def receive = {
      case CaffeineWithdrawalWarning => coffeeSource ! EspressoRequest
      case (EspressoCup(Filled), Receipt(amount)) =>
        log.info(s"yay, caffeine for ${self}!")
      case ComebackLater =>
        log.info("grumble, grumble")
        context.system.scheduler.scheduleOnce(300.millis) {
          coffeeSource ! EspressoRequest
        }
      // case Terminated(barista) =>
        // log.info("Oh well, let's find another coffeehouse...")
    }
  }
}
