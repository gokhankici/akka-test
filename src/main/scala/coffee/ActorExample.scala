package coffee {
  import akka.actor._
  import coffee.Customer._

  object ActorExample extends App {
    val system = ActorSystem("Coffeehouse")

    val barista = system.actorOf(Props[Barista], "Barista")

    val customerJohnny = system.actorOf(Props(classOf[Customer], barista), "Johnny")
    val customerAlina = system.actorOf(Props(classOf[Customer], barista), "Alina")

    customerJohnny ! CaffeineWithdrawalWarning
    customerAlina ! CaffeineWithdrawalWarning
    customerJohnny ! CaffeineWithdrawalWarning
    customerAlina ! CaffeineWithdrawalWarning
    customerJohnny ! CaffeineWithdrawalWarning
    customerAlina ! CaffeineWithdrawalWarning
  }
}
