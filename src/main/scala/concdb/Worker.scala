package concdb

import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import scala.concurrent.{ Future, Await }
import concurrent.duration._
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy
import java.util.Calendar

class Worker extends Actor with ActorLogging {
  val db: ActorSelection     = context.actorSelection("..")
  var randomKey: String = ""

  import util.Random
  var failed: Boolean = false

  override def preStart(): Unit = {
    randomKey = randomAlpha(10)
    if (Random.nextBoolean()) {
      db ! Allocate(randomKey)
      log.info(s"[WORKER] Trying to allocate ${randomKey}")
    } else {
      db ! Lookup(randomKey)
      log.info(s"[WORKER] Trying to lookup ${randomKey}")
    }
  }

  override def postStop(): Unit = {
    log.info(s"[WORKER] Time to sleep ...")
  }

  def receive = {
    case Value(v) => {
      if (Random.nextBoolean()) failed = true
      if (failed) throw new Exception("[WORKER] OMG, not again!")

      log.info(s"[WORKER] Seems like ${randomKey} = ${v}")
      context.stop(self)
    }
    case Allocated => {
      if (Random.nextBoolean()) failed = true
      if (failed) throw new Exception("[WORKER] OMG, not again!")

      log.info(s"[WORKER] ${randomKey} exists in the database")
      context.stop(self)
    }
    case Free => {
      if (Random.nextBoolean()) failed = true
      if (failed) throw new Exception("[WORKER] OMG, not again!")

      val randomVal = randInt(10)
      sender ! Set(randomVal)
      log.info(s"[WORKER] Yuppi ! Setting ${randomKey} to ${randomVal}")
    }
  }

  def randomAlpha(length: Int): String = {
    val chars = ('a' to 'z') ++ ('A' to 'Z')
    randomStringFromCharList(length, chars)
  }

  def randomStringFromCharList(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    for (i <- 1 to length) {
      val randomNum = Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }

  def randInt(max: Int): Int = {
    val range = 1 to max
    range(Random.nextInt(range.length))
  }
}
