/*
 * Copyright (c) 2019 Fibbometrix Technologies
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.fibbo.logparser.di.module

import java.io.File
import java.lang.Exception
import java.time.{LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

import akka.actor.{ActorRef, ActorSystem, DeadLetter, Props}
import com.fibbo.logparser.actor._
import com.google.inject.AbstractModule
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import javax.inject.{Inject, Named}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import akka.pattern.ask
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.fibbo.logparser.actor.Master.Workload
import com.fibbo.logparser.di.intf.Kafka
import com.fibbo.logparser.util.StreamUtility

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import java.util.regex.Matcher
import java.util.regex.Pattern

import scala.util.matching.Regex

class StartupModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Startup]).to(classOf[StartupRunner]).asEagerSingleton()
    //bind(classOf[RouteHandler]).asEagerSingleton()
  }
}

trait Startup {
  //val zero = 0;
}

class StartupRunner @Inject()(config: Config, @Named("masterActor") masterActor: ActorRef, streamUtility: StreamUtility)(implicit system: ActorSystem,
                                                                                                                         ec: ExecutionContext) extends Startup with StrictLogging {
  logger.info("Startup Service - Started")

  val deadLetterMonitorActor = system.actorOf(Props[DeadLetterMonitorActor],
      name = "deadlettermonitoractor")
  system.eventStream.subscribe(deadLetterMonitorActor, classOf[DeadLetter])

  //  implicit val timeout = Timeout(5.seconds)
//  val result = (shapeActor ? Rectangle(3.0, 4.0)).mapTo[Double]
//  val value = Await.result(result, Duration.Inf)
//  logger.info(s"Result: ${value.toString}")


  val directory = new File("mg")
  //val files = directory.listFiles.filter(_.isFile).map(_.getAbsolutePath).filter(_.endsWith(".json")).to[List]
  val files = directory.listFiles.filter(_.isFile).map(_.getAbsolutePath).filter(!_.endsWith("DS_Store")).to[List]
  masterActor ! Workload(files)

  //streamUtility.consumeKafka("mg-request")
}