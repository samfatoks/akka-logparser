package com.fibbo.logparser


import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.util.{ByteString, Timeout}
import com.fibbo.logparser.actor.{Circle, Rectangle, Shape}
import com.fibbo.logparser.di.module.{CentralModule, StartupModule}
import com.google.inject.Guice
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._
import com.fibbo.logparser.util.ActorUtil._
import akka.pattern.ask
import scala.concurrent.{Await, Future}


object Boot extends App {

  def startApp(): Unit = {

    implicit val system = ActorSystem("AkkaLogParser")
    implicit val materializer = ActorMaterializer()
    //implicit val executionContext = system.dispatcher
    implicit lazy val executionContext = system.dispatchers.lookup("akka-http-routes-dispatcher")

    // val lStart = System.currentTimeMillis()

    val config: Config = ConfigFactory.load()
//    val logger = Logging(system.eventStream, this.getClass)
    //val injector = Guice.createInjector(new CentralModule(system, materializer, config), new StartupModule)
    Guice.createInjector(new CentralModule(system, materializer, config), new StartupModule)

//    implicit val timeout = Timeout(5.seconds)
//    val shapeActor = injector.getActor("shapeActor")
//
//
//    val result = (shapeActor ? Rectangle(3.0, 4.0)).mapTo[Double]
//    val value = Await.result(result, Duration.Inf)
//    logger.info(value.toString)

    //Await.result(injector.getInstance(classOf[ActorSystem]).terminate, Duration.Inf)
  }

  startApp()

}
