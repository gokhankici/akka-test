package mapreduce;

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import scala.concurrent.{ Future, Await }
import concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy
import java.util.Calendar

import scala.collection.mutable.HashMap
import scala.collection.mutable.Queue

object WQStrategy {
  val intensity: Int = 10
  val interval: Duration = 1.second
  val defaultDecider: Decider = {
    case _: Exception => Restart
  }
}

class WQStrategy(wq: WQ) extends SupervisorStrategy {
  val decider = WQStrategy.defaultDecider

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
      wq.lastWorkMap.get(child) match {
        case Some(Work(n)) => {
          wq.jobs += n
          wq.lastWorkMap -= child
        }
        case None          => {}
      }

      context.actorOf(Props[Worker])
    }
  }
}

class WQ(val workerCount: Int, val jobCount: Int) extends Actor with ActorLogging {
  var noOfTerminateSent: Int = 0

  var jobs: Queue[Int] = new Queue()
  var lastWorkMap: HashMap[ActorRef, Work] = new HashMap()

  override val supervisorStrategy = new WQStrategy(wq = this)

  import util.Random
  var failed: Boolean = false

  override def preStart(): Unit = {
    if(jobCount <= 0) {
      context.stop(self)
      return
    }

    jobs ++= (1 to jobCount)

    for (i <- 1 to workerCount) {
      context.actorOf(Props[Worker])
    }

    log.info("[WQ] Started the workers")
  }

  def receive = {
    case JobRequest => {
      if (Random.nextBoolean()) failed = true
      if (failed) throw new Exception("[WQ] OMG, not again!")

      if (! jobs.isEmpty) {
        val j = jobs.dequeue()
        sender ! Work(j)
        log.info(s"[WQ] Sent job number ${j}")

        lastWorkMap += ((sender, Work(j)))
      } else if (noOfTerminateSent < workerCount){
        sender ! Terminate
        noOfTerminateSent + 1
        log.info(s"[WQ] Sent terminate number ${noOfTerminateSent}")
        if (noOfTerminateSent == workerCount) {
          log.info("[WQ] Sent terminate messages to every worker ...")
        }
      }
    }
  }

  override def postStop(): Unit = {
    log.info("[WQ] time to sleep ...")
  }
}
