import akka.actor._
import scala.concurrent.duration._
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._

import Customer._

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
