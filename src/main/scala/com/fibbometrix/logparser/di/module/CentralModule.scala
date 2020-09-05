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

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.fibbometrix.logparser.actor.Master
import com.fibbometrix.logparser.di.impl.KafkaImpl
import com.fibbometrix.logparser.di.intf.Kafka
import com.fibbometrix.logparser.util.StreamUtility
import com.google.inject.{AbstractModule, Provides}
import com.typesafe.config.Config
import javax.inject.Named
import net.codingwell.scalaguice.ScalaModule

import scala.concurrent.ExecutionContext

class CentralModule(actorSystem: ActorSystem, materializer: ActorMaterializer, config: Config) extends AbstractModule with ScalaModule {
  override def configure() {

    bind[Config].toInstance(config)
    bind[ActorSystem].toInstance(actorSystem)
    bind[ExecutionContext].toInstance(actorSystem.dispatcher)
    bind[ActorMaterializer].toInstance(materializer)
    bind[Kafka].to[KafkaImpl]
  }

  @Provides
  @Named("masterActor")
  def getMasterActor(actorSystem: ActorSystem, streamUtility: StreamUtility, config: Config): ActorRef = {
    actorSystem.actorOf(Master.props(streamUtility, config.getInt("workerSize")), "master")
  }
}
