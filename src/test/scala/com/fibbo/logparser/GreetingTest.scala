package com.fibbo.logparser

import com.fibbo.logparser.domain.{Greeting, GreetingEmotion}
import org.scalatest.{FlatSpec, FunSuite, Matchers}

import scala.collection.mutable.Stack

class GreetingTest extends FlatSpec with Matchers {
  it should "produce emotion of JOY" in {
    val happyGreeting = Greeting(greetingEmotion = GreetingEmotion.JOY, greetingDetail = "Top of the morning to you!")
    println(s"I have a feeling of ${happyGreeting.greetingEmotion.toString} so I say '${happyGreeting.greetingDetail}'")
    happyGreeting.greetingEmotion shouldBe GreetingEmotion.JOY
  }
}
