package concdb

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import scala.concurrent.{ Future, Await }
import concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy
import java.util.Calendar
import scala.concurrent.TimeoutException

object DBStrategy {
  val intensity: Int = 10
  val interval: Duration = 1.second
  val defaultDecider: Decider = {
    case _: Exception => Restart
  }
}

case class Lookup(key: String)
case class Value(value: Int)

case class Allocate(key: String)
case class Allocated()
case class Free()
case class Set(value: Int)

class DBStrategy extends SupervisorStrategy {
  val decider = DBStrategy.defaultDecider

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
      context.actorOf(Props[Worker])
    }
  }
}

class DB extends Actor with ActorLogging {
  override val supervisorStrategy = new DBStrategy()
  val workerCount = 7

  var answerCount = 0

  import util.Random
  var failed: Boolean = false

  override def preStart(): Unit = {
    for (i <- 1 to workerCount) {
      context.actorOf(Props[Worker])
    }
    log.info("[DB] Started workers")
  }

  override def postStop(): Unit = {
    log.info("[DB] Time to sleep ...")
  }

  def msgHandler: Receive = {
    case Lookup(key) => {
      if (Random.nextBoolean()) failed = true
      if (failed) throw new Exception("[DB] OMG, not again!")

      log.info(s"[DB] Looking up key ${key} ...")
      sender ! Value(0)

      answerCount += 1
    }

    case Allocate(key) => {
      if (Random.nextBoolean()) failed = true
      if (failed) throw new Exception("[DB] OMG, not again!")

      log.info(s"[DB] Looking up key ${key} ...")
      if (Random.nextBoolean()) {
        sender ! Allocated
        log.info(s"[DB] Sorry, ${key} is already allocated")
      } else {
        // (1) this is one way to "ask" another actor
        implicit val timeout = Timeout(1.seconds)
        val future = sender ? Free
        try {
          Await.result(future, timeout.duration).asInstanceOf[Set] match {
            case Set(value) => log.info(s"[DB] Setting ${key} to ${value}")
          }
        } catch {
            case e: TimeoutException => log.info(s"[DB] No response from ${sender}")
        }
      }

      answerCount += 1
    }
  }

  def receive = {
    case msg: Any => {
      if (answerCount == workerCount) {
        log.info(s"[DB] !!! Got message from everybody !!! ")
        context.stop(self)
      } else {
        msgHandler.apply(msg)
      }
    }
  }
}
