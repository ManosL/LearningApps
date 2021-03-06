/*
 * Copyright (C) 2016  Nikos Katzouris
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package openssh

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._

import scala.io.Source

/**
  * Created by nkatz at 28/1/20
  */

object DataHandling extends App {

  val lines = Source.fromFile("/home/nkatz/dev/ADL-datasets/openshs-classification/d1_1m_10tm.csv").getLines

  // used for test for now
  //val lines = Source.fromFile("/home/nkatz/dev/ADL-datasets/openshs-classification/d3_2m_10tm.csv").getLines

  val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def time(t: String) = format.parse(t).getTime / 1000

  //Set(eat, sleep, personal, leisure, other)
  //Set(bathroomLight, hallwayLight, kitchenLight, bedroomCarp, oven, tv, kitchenDoor, bedroomLight, livingCarp, bed, kitchenCarp, couch, mainDoorLock, wardrobe, mainDoor, bathroomCarp, fridge, bedTableLamp, bathroomDoor, bedroomDoor)

  val header = lines.take(1).next().split(",")

  println(header)

  var activities = Set[String]()
  var events = Set[String]()

  val mongoClient = MongoClient()
  val collection = mongoClient("openssh")("examples")

  // used for test for now
  //val collection = mongoClient("openssh-test")("examples")
  collection.drop()

  lines.drop(1) foreach { x =>
    val split = x.split(",")
    val z = (header zip split).filter(y => y._2 != "0").reverse
    val first = z.take(2)
    val last = z.drop(2)
    val timeStamp = time(first.head._2)
    val activity = first.tail.head._2
    //if (!activities.contains(activity)) activities = activities + activity
    val lles_happens = last.map(x => s"happensAt(${x._1},$timeStamp)")
    val hle = s"holdsAt($activity,$timeStamp)"

    val entry = MongoDBObject("time" -> timeStamp.toInt) ++ ("annotation" -> List(hle)) ++ ("narrative" -> lles_happens.toList)
    println(entry)
    collection.insert(entry)
    /*
    val lles = last.map(x => x._1)
    lles foreach { lle =>
      if (!events.contains(lle)) events = events + lle
    }
    */
    //println(timeStamp, activity, lles)

  }
  //println(activities)
  //println(events)

}
