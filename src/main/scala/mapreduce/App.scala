package mapreduce;

import akka.actor._

object MapReduceApp extends App {
  val workerCount = 10
  val jobCount = 15

  val system = ActorSystem("mapreduce")

  val master = system.actorOf(Props(classOf[Master], workerCount, jobCount), "Master")
}
