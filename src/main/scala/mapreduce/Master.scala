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
  val intensity: Int = 10
  val interval: Duration = 1.second
  val defaultDecider: Decider = {
    case _: Exception => Restart
  }
}

class MasterStrategy(master: Master) extends SupervisorStrategy {
  val decider = MasterStrategy.defaultDecider

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
      val newJobCount = master.jobCount - master.noOfReqsReceived
      println(s"[MASTER] Restarting WQ with ${newJobCount} jobs ...")
      context.actorOf(Props(classOf[WQ], master.workerCount, newJobCount))
    }
  }
}

class Master(val workerCount: Int, val jobCount: Int) extends Actor with ActorLogging {
  var noOfReqsReceived: Int = 0
  override val supervisorStrategy = new MasterStrategy(master = this)

  import util.Random
  var failed: Boolean = false

  override def preStart(): Unit = {
    context.actorOf(Props(classOf[WQ], workerCount, jobCount))
    log.info("[MASTER] Started WQ")
  }

  override def postStop(): Unit = {
    log.info("[MASTER] Time to sleep ...")
  }

  def receive = {
    case Work(n) => {
      if (Random.nextBoolean()) failed = true
      if (failed) throw new Exception("[MASTER] OMG, not again!")

      log.info(s"[MASTER] Got message number ${n}")
      noOfReqsReceived += 1

      if (noOfReqsReceived == jobCount) {
        log.info("[MASTER] Got all the messages, sleeping now ...")
        // context.stop(self)
      }
    }
  }
}
