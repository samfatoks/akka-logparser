package com.fibbometrix.logparser
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.Guice
import com.typesafe.config.{Config, ConfigFactory}
import com.fibbometrix.logparser.di.module.{CentralModule, StartupModule}


object Boot extends App {

  def startApp(): Unit = {
    implicit val system = ActorSystem("akka-logparser")
    implicit val materializer = ActorMaterializer()
    //implicit val executionContext = system.dispatcher
    implicit lazy val executionContext = system.dispatchers.lookup("akka-http-routes-dispatcher")
    val config: Config = ConfigFactory.load()
    Guice.createInjector(new CentralModule(system, materializer, config), new StartupModule)
  }

  startApp()
}
