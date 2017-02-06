package mapreduce;

import akka.actor._
import concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy

object MRStrategy {
  val intensity: Int = 10
  val interval: Duration = 1.second
  val defaultDecider: Decider = {
    case _: Exception => Restart
  }
}

class MRStrategy() extends SupervisorStrategy {
  val decider = MRStrategy.defaultDecider

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
      context.actorOf(Props[MapReduce])
    }
  }
}

class MapReduce extends Actor with ActorLogging {
  val workerCount = 3
  val jobCount = 5

  override val supervisorStrategy = new MRStrategy()

  override def preStart(): Unit = {
    context.actorOf(Props(classOf[Master], workerCount, jobCount))
  }

  def receive = {
    case _: Any => ()
  }
}

object MapReduceApp extends App {
  val system = ActorSystem("mapreduce")
  val mr = system.actorOf(Props[MapReduce])
}
