package concdb

import akka.actor._
import concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy

object ConcDBStrategy {
  val intensity: Int = 10
  val interval: Duration = 1.second
  val defaultDecider: Decider = {
    case _: Exception => Restart
  }
}

class ConcDBStrategy extends SupervisorStrategy {
  val decider = ConcDBStrategy.defaultDecider

  def handleChildTerminated(
    context: ActorContext,
    child: ActorRef,
    children: Iterable[ActorRef]
  ): Unit = ()

  def processFailure(
    context: ActorContext,
    restart: Boolean,
    child: ActorRef,
    cause: Throwable,
    stats: ChildRestartStats,
    children: Iterable[ChildRestartStats]
  ): Unit = {

    context.stop(child)

    if (restart) {
      context.actorOf(Props[DB])
    }
  }
}

class ConcDB extends Actor with ActorLogging {
  override val supervisorStrategy = new ConcDBStrategy()

  override def preStart(): Unit = {
    context.actorOf(Props[DB])
  }

  def receive = {
    case _: Any => ()
  }
}

object ConcDBApp extends App {
  val system = ActorSystem("concdb")
  val mr = system.actorOf(Props[ConcDB])
}
