package com.fibbo.logparser.actor

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, Terminated}
import akka.event.{LogMarker, MarkerLoggingAdapter}
import com.fibbo.logparser.util.StreamUtility

import scala.collection.mutable
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer

object Master {
  def props(streamUtility: StreamUtility, workerSize: Int): Props = Props(new Master(streamUtility, workerSize))
  case class RegisterWorker() // worker announces itself to master
  case class WorkIsAvailable() // master announces to workers there is work
  case class ReadyForOneWorkItem() // worker asks master for a work item
  case class AssignOneWorkItem(itemIndex: Int, item: String) // master sends worker one work item
  case class Workload(s: List[String]) // master gets a workload from somewhere
}

class Master(streamUtility: StreamUtility, workerSize: Int) extends Actor with ActorLogging {
  import Master._

//  private val marker = LogMarker(name = self.path.name)
//  implicit val log: MarkerLoggingAdapter = akka.event.Logging.withMarker(context.system, this.getClass)

  //var workitemptr = 0
  //var workers = new ListBuffer[ActorRef]
  val workers = mutable.Set.empty[ActorRef]

  var workitems: List[String] = List[String]() // a buffer to hold the work items
  private val workitemptr = new AtomicInteger()


  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case e: MatchError => Restart
      case _: Exception  => Escalate
//      case _: NullPointerException     ⇒ Restart
//      case _: IllegalArgumentException ⇒ Stop
    }

  override def preStart {

    log.info("Master preStart: Starting worker actor")
    for(instanceId <- 1 to workerSize) {
      context.actorOf(Worker.props(self, streamUtility), s"worker-$instanceId")
    }
  }

  override def postStop { log.info("Master LifeCycleActor: postStop") }

//  override def preRestart(reason: Throwable, message: Option[Any]) {
//    log.info("LifeCycleActor: preRestart")
//    log.info(s"LifeCycleActor reason: ${reason.getMessage}")
//    log.info(s"LifeCycleActor message: ${message.getOrElse("")}")
//    super.preRestart(reason, message)
//  }
//  override def postRestart(reason: Throwable) {
//    log.info("LifeCycleActor: postRestart")
//    log.info(s"LifeCycleActor reason: ${reason.getMessage}")
//    super.postRestart(reason)
//  }

  override def receive: Receive = {
    case Workload(items: List[String]) => // a new workload has arrived
      workitems = items
      log.info(s"Work Size: ${workitems.size}")
      workers.foreach { p => p ! WorkIsAvailable }
    case RegisterWorker => // a worker says he's available for new workloads
      log.info(s"worker $sender() registered")
      workers += sender()
    case ReadyForOneWorkItem => // a worker wants work from current workload
      //log.info(s"${sender().path.name} is Ready to receive work :: WorkItemPtr: ${workitemptr}")
      if (workitemptr.get() < workitems.size) {
        val index = workitemptr.incrementAndGet();
        sender ! AssignOneWorkItem(index, workitems(index - 1))
        log.info(s"Sent Work to ${sender().path.name} :: WorkItemPtr: ${index}")
      }

    case "kill" => log.info("Kill here")
    case Terminated(worker) => log.info("Terminated")

  }
}
