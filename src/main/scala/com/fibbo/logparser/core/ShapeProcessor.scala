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

package com.fibbo.logparser.core

import com.fibbo.logparser.actor.{Circle, Rectangle, Shape}

//sealed trait Shape {
//  def area: Double
//}
//final case class Rectangle(width: Double, height: Double) extends Shape {
//  override def area: Double = width * height
//}
//final case class Circle(radius: Double) extends Shape {
//  override def area: Double = math.Pi * Math.pow(radius, 2)
//}

class ShapeProcessor {

//  sealed abstract class Maybe[+A] {
//    def isEmpty: Boolean
//    def get: A
//    def getOrElse[B >: A](default: B): B = {
//      if(isEmpty) default else get
//    }
//  }
//  final case class Just[A](value: A) extends Maybe[A] {
//    def isEmpty = false
//    def get = value
//  }
//  case object Nil extends Maybe[Nothing] {
//    def isEmpty = true
//    def get = throw new NoSuchElementException("Nil.get")
//  }
//
//  def position[A](list: List[A], value :A): Maybe[Int] = {
//    val index = list.indexOf(value)
//    if (index > -1) Just(index) else Nil
//  }
//
//  position(List("3445"), "234").getOrElse("sd")

//  val rect: Shape = Rectangle(3.0, 4.0)
//  val circ: Shape = Circle(1.0)

  def perimeter(shape: Shape): Double =
    shape match {
      case Rectangle(w, h) => 2 * (w + h)
      case Circle(r) => 2 * math.Pi * r
    }
}
//trait Thing
//trait Vehicle extends Thing
//class Car extends Vehicle
//class Jeep extends Car
//class Coupe extends Car
//class Motorcycle extends Vehicle
//class Vegetable
//class Parking[E](val place: E)
