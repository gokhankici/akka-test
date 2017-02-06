package mapreduce;

import akka.actor._
import concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy

class MapReduce extends Actor with ActorLogging {
  val workerCount = 10
  val jobCount = 15

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.second) {
      case _: Exception => Restart
    }

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
