package mapreduce;

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import scala.concurrent.{ Future, Await }
import concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy
import java.util.Calendar

case class JobRequest()

sealed trait JobReply
case class Work(n: Int) extends JobReply
case class Terminate()  extends JobReply

object MasterStrategy {
  val defaultDecider: Decider = {
    case _: Exception => Restart
  }
}

class MasterStrategy(
  intensity: Int = 10,
  interval: Duration = 1.second,
  decider: Decider = MasterStrategy.defaultDecider,
  master: Master = null
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

class Master(workerCount: Int, jobCount: Int) extends Actor with ActorLogging {
  var noOfReqsReceived: Int = 0
  override val supervisorStrategy = new MasterStrategy(master = this)

  override def preStart(): Unit = {
    context.actorOf(Props(classOf[WQ], workerCount, jobCount), "WQ")
    log.info("[MASTER] Started WQ")
  }

  def receive = {
    case Work(n) => {
      log.info(s"[MASTER] Got message number ${n}")
      noOfReqsReceived += 1

      if (noOfReqsReceived == jobCount) {
        log.info("[MASTER] Got all the messages, sleeping now ...")
        // context.stop(self)
      }
    }
  }
}
