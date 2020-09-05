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

package com.fibbometrix.logparser.di.module

import java.io.File

import akka.actor.{ActorRef, ActorSystem, DeadLetter, Props}
import com.fibbometrix.logparser.actor.DeadLetterMonitorActor
import com.fibbometrix.logparser.actor.Master.Workload
import com.fibbometrix.logparser.util.StreamUtility
import com.google.inject.AbstractModule
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import javax.inject.{Inject, Named}

import scala.concurrent.ExecutionContext


class StartupModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Startup]).to(classOf[StartupRunner]).asEagerSingleton()
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

  val directory = new File("source_logs")
  //val files = directory.listFiles.filter(_.isFile).map(_.getAbsolutePath).filter(_.endsWith(".json")).to[List]
  val files = directory.listFiles.filter(_.isFile).map(_.getAbsolutePath).filter(!_.endsWith("DS_Store")).to[List]
  masterActor ! Workload(files)

  //streamUtility.consumeKafka("mg-request")
}