package pingpong;

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import scala.concurrent.{ Future, Await }
import concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy
import java.util.Calendar

case class StartMessage()
case class RunMessage()
case class PingMessage()
case class PongMessage()

object Constants {
  final val timeout = 1.seconds
}

object MyStrategy {
  val defaultDecider: Decider = {
    case _: Exception => Restart
  }
}

class MyStrategy(
  intensity: Int = 10,
  interval: Duration = 1.second,
  decider: Decider = MyStrategy.defaultDecider,
  master: Ping = null
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
    println(s"Supervisor: I'm ${context.self.path}")
    val today = Calendar.getInstance().getTime()
    println(s"Supervisor: time = ${today}")
    println(s"Supervisor: wc = ${master.workerCount}, rc = ${master.responseCount}")
    println(s"Supervisor: Oh, no! ${child} has died ...")
    super.processFailure(context, restart, child, cause, stats, children)
  }
}

class Ping extends Actor with ActorLogging {
  import scala.concurrent.TimeoutException

  implicit val timeout = Timeout(Constants.timeout)
  val workerCount = 10
  var responseCount = 0

  override val supervisorStrategy = new MyStrategy(master = this)

  override def preStart() {
    log.info("starting ping ...")
  }

  def receive = {
    case StartMessage => {
      for (i <- 1 to workerCount) {
        context.actorOf(Props(classOf[Pong], i), s"Worker${i}")
      }
      sender() ! "OK"
    }
    case RunMessage => {
      for (i <- 1 to workerCount) {
        val worker = context.actorSelection(s"Worker${i}")
        val f : Future[PongMessage] = (worker ? PingMessage()).mapTo[PongMessage]
        try {
          val result = Await.result(f, Constants.timeout)
          log.info(result.toString())
          responseCount += 1
        } catch {
            case e: TimeoutException => log.info(s"no response from Worker${i}")
        }
      }
      log.info(s"DONE. Got reply from ${responseCount} workers")
    }
  }
}

class Pong(id: Int) extends Actor with ActorLogging {
  import util.Random

  val mypath = context.self.path
  var failed = false

  override def postRestart(reason: Throwable) {
    log.info(s"restarted, failed = ${failed}, reason = ${reason}")
    preStart()
  }

  def receive = {
    case PingMessage() => {
      if (Random.nextBoolean()) failed = true
      if (failed) throw new Exception("OMG, not again!")

      log.info("got " + PingMessage)
      sender() ! PongMessage()
    }
  }
}

object PingPongApp extends App {
  implicit val timeout = Timeout(Constants.timeout)

  val system = ActorSystem("pingpong")
  val master = system.actorOf(Props[Ping], "Ping")

  val f: Future[String] = (master ? StartMessage).mapTo[String]
  Await.result(f, Constants.timeout)

  master ! RunMessage
}
