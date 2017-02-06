package mapreduce;

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import scala.concurrent.{ Future, Await }
import concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy
import java.util.Calendar

object WQStrategy {
  val defaultDecider: Decider = {
    case _: Exception => Restart
  }
}

class WQStrategy(
  intensity: Int = 10,
  interval: Duration = 1.second,
  decider: Decider = WQStrategy.defaultDecider,
  wq: WQ = null
) extends OneForOneStrategy(intensity, interval)(decider)
{
  override def processFailure(
    context: ActorContext,
    restart: Boolean,
    child: ActorRef,
    cause: Throwable,
    stats: ChildRestartStats,
    children: Iterable[ChildRestartStats]): Unit =
  {
    super.processFailure(context, restart, child, cause, stats, children)
  }
}

class WQ(workerCount: Int, jobCount: Int) extends Actor with ActorLogging {
  var noOfJobsSent: Int = 0
  var noOfTerminateSent: Int = 0

  override val supervisorStrategy = new WQStrategy(wq = this)

  override def preStart(): Unit = {
    for (i <- 1 to workerCount) {
      context.actorOf(Props[Worker], s"Worker${i}")
    }
    log.info("[WQ] Started the workers")
  }

  def receive = {
    case JobRequest => {
      if (noOfJobsSent < jobCount) {
        noOfJobsSent += 1
        sender ! Work(noOfJobsSent)
        log.info(s"[WQ] Sent job number ${noOfJobsSent}")
      } else if (noOfTerminateSent < workerCount){
        noOfTerminateSent += 1
        sender ! Terminate
        log.info(s"[WQ] Sent terminate number ${noOfTerminateSent}")
        if (noOfTerminateSent == workerCount) {
          log.info("[WQ] Sent terminate messages to every worker ...")
        }
      }
    }
  }
}
