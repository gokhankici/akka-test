package mapreduce;

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import scala.concurrent.{ Future, Await }
import concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy
import java.util.Calendar

class Worker extends Actor with ActorLogging {
  val master: ActorSelection = context.actorSelection("../..")
  val wq: ActorSelection     = context.actorSelection("..")

  override def preStart(): Unit = {
    wq ! JobRequest
    log.info(s"[WORKER] Sent WQ a job request")
  }

  def receive = {
    case Work(n) => {
      log.info(s"[WORKER] Got job number ${n}")
      master ! Work(n)
      wq ! JobRequest
    }
    case Terminate => {
      log.info(s"[WORKER] Going to sleep now ...")
      // context.stop(self)
    }
  }
}
