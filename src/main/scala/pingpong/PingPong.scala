package pingpong;

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import scala.concurrent.{ Future, Await }
import concurrent.duration._


sealed trait Message
case object Start       extends Message
case object PingMessage extends Message
case object PongMessage extends Message

object Constants {
  final val timeout = 5.seconds
}

class Ping extends Actor with ActorLogging {
  override def preStart() {
    log.info("starting...")
  }
  val workerCount = 10
  var responseCount = 0

  def receive = {
    case Start => {
      for (i <- 1 to workerCount) {
        context.actorOf(Props(classOf[Pong], i), s"Worker${i}")
      }
      for (i <- 1 to workerCount) {
        val worker = context.actorSelection(s"Worker${i}")
        worker ! PingMessage
      }
    }
    case PongMessage => {
      log.info("got " + PongMessage + " from " + sender().path)
      responseCount += 1
      if (responseCount == workerCount)
        log.info("done")
    }
  }
}

class Pong(id: Int) extends Actor with ActorLogging {
  def receive = {
    case PingMessage => {
      log.info("got " + PingMessage)
      sender() ! PongMessage
    }
  }
}

object PingPongApp extends App {
  implicit val timeout = Timeout(Constants.timeout)

  val system = ActorSystem("pingpong")
  val master = system.actorOf(Props[Ping], "Ping")

  master ! Start
}
