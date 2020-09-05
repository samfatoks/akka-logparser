package com.fibbo.logparser.actor

import akka.actor.Actor
import com.fibbo.logparser.core.ShapeProcessor

class MyActor (shapeProcessor: ShapeProcessor) extends Actor {
  def receive = {
    case shape: Shape => sender() ! shapeProcessor.perimeter(shape)
  }
}

sealed trait Shape
final case class Rectangle(width: Double, height: Double) extends Shape
final case class Circle(radius: Double) extends Shape