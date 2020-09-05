package com.fibbometrix.logparser.actor

import java.io.FileNotFoundException
import java.net.MalformedURLException
import java.nio.file.Paths
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.fibbometrix.logparser.util.StreamUtility
// import akka.event.{LogMarker, MarkerLoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Framing}
import akka.util.ByteString

import scala.util.{Failure, Success, Try}


object Worker{
  def props(master: ActorRef, streamUtility: StreamUtility): Props = Props(new Worker(master, streamUtility))
}

class Worker(master: ActorRef, streamUtility: StreamUtility) extends Actor with ActorLogging {

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import Master._
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