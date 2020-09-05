package com.fibbo.logparser.util

import akka.actor.ActorRef
import com.google.inject.name.Names
import com.google.inject.{Injector, Key}


object ActorUtil {

  implicit class ActorInjector(injector: Injector) {
    def getActor(name: String): ActorRef = {
      injector.getInstance(Key.get(classOf[ActorRef], Names.named(name)))
    }
  }

}

