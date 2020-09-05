package com.fibbo.logparser.actor


import java.io.FileNotFoundException
import java.net.MalformedURLException
import java.nio.file.Paths
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
// import akka.event.{LogMarker, MarkerLoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Framing}
import akka.util.ByteString
import com.fibbo.logparser.util.StreamUtility

import scala.util.{Failure, Success, Try}


object Worker{
  def props(master: ActorRef, streamUtility: StreamUtility): Props = Props(new Worker(master, streamUtility))
}

class Worker(master: ActorRef, streamUtility: StreamUtility) extends Actor with ActorLogging {

  implicit val materializer: ActorMaterializer = ActorMaterializer()

//  private val marker = LogMarker(name = self.path.name)
//  implicit val log: MarkerLoggingAdapter = akka.event.Logging.withMarker(context.system, this.getClass)


  import Master._
  // when the actor starts, we register with the master. Also, we
  // tell it we are ready for work, in case a workload is in progress
  override def preStart() {
    log.info(s"Worker started: ${self.path.name}")
    master ! RegisterWorker
    master ! ReadyForOneWorkItem
  }
  override def postStop { log.info("Worker stoped") }

  override def receive: Receive = {
    case WorkIsAvailable => // master says a workload is available
      log.info("Work available")
      sender ! ReadyForOneWorkItem
    case AssignOneWorkItem(fileIndex, filename) => {
      log.info(s"Performing Work (${self.path.name}) - $filename")
//      Try(processFile(filename)) match {
//        case Success(_) => log.info("Done")
//        case Failure(ex) => log.error(ex, s"Problem rendering URL content: ${ex.getMessage}")
//      }

//      try {
//        processFile(filename)
//      } catch {
//          case RuntimeException(msg) => msg
//      }

      processFile(filename) match {
        case Success(_) =>
        case Failure(e) => log.error("Exception caught: {}", e)
      }

      sender ! ReadyForOneWorkItem
    }
  }

  private def processFile(filename: String) = {
    Try {
      streamUtility.fileProcessorGraph(filename, "mg-request").run
    }
  }
}