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

package maritime

/**
  * Created by nkatz at 28/1/20
  */

import java.io.File
import java.io.PrintWriter

import akka.actor.{ActorSystem, Props}
import com.typesafe.scalalogging.LazyLogging
import oled.app.runutils.CMDArgs
import oled.app.runutils.InputHandling.{MongoDataOptions, getMongoData}
//import maritime.InputHandling.getMongoData
import oled.datahandling.Example
import oled.learning.LocalCoordinator
import oled.learning.Types.RunSingleCore
import IntervalHandler.readInputFromFile
import ParseMaritime.ParseFileDataOptions
import ParseMaritime.ParseMaritime.writeDataToMongo

object Runner extends LazyLogging {

  def main(args: Array[String]) = {

    /* Because we want some HLEs to get handled as LLEs */
    /*
    val all_LLEs = List("gap_end", "coord", "velocity", "change_in_heading", "entersArea",
                        "stop_start", "change_in_speed_start", "gap_start", "change_in_speed_end",
                        "stop_end", "leavesArea", "slow_motion_end", "slow_motion_start")

    val all_HLEs = List("withinArea","tuggingSpeed","stopped","highSpeedNC","movingSpeed"
                        ,"underWay","proximity","anchoredOrMoored","changingSpeed","gap"
                        ,"lowSpeed","trawlingMovement","trawlSpeed","drifting","loitering"
                        ,"sarMovement","sarSpeed","rendezVous","pilotBoarding","trawling","sar"
                        ,"tugging")
    */
    // LLEs to happensAt, HLEs to holdsAt

    // Missing: "trawlSpeed", "sarSpeed", "withinArea", "highSpeedNC",
    //"movingSpeed", "underWay", "changingSpeed", , "gap"

    val allLLEs = List("gap_end", /*"coord", "velocity",*/
      "change_in_heading", "entersArea","stop_start", "change_in_speed_start",
      "gap_start", "change_in_speed_end","stop_end", "leavesArea",
      "slow_motion_end", "slow_motion_start", "stopped","proximity", "lowSpeed")

    val allHLEs = List("rendezVous")

    val argsok = CMDArgs.argsOk(args)

    if (argsok._1) {
      val runningOptions = CMDArgs.getOLEDInputArgs(args) // returns RunningOptions object // THIS SHOULD GET PATH FOR HLE DIR AND LLE FILE
      // OR IT WILL CHANGED IN THE CODE
      /*
      val bias_list = runningOptions.globals.MODES

      // I should find a better way
      val bias_fluents = bias_list.map(clause => clause.split('(')(2)).toSet.toList ++ bias_list.map(clause => clause.split('(')(3)).toSet.toList
      */

      val evalOneTestSet = false

      if (!evalOneTestSet) {
        /*
        /* Single-pass run on the entire dataset */
        val trainingDataOptions = new MongoDataOptions(dbNames       = train1, chunkSize = runningOptions.chunkSize,
                                                       targetConcept = runningOptions.targetHLE, sortDbByField = "time", what = "training")

        val testingDataOptions = trainingDataOptions

        val trainingDataFunction: MongoDataOptions => Iterator[Example] = getMongoData
        val testingDataFunction: MongoDataOptions => Iterator[Example] = getMongoData
        */

        // SOME PARAMETERS SHOULD BE CONFIGURED
        // --train = /home/manosl/Desktop/BSc_Thesis/Datasets/IntervalHandlerInputDatasets
        val HLE_Dir_Path = runningOptions.train + "/HLEs"
        val LLEs_File = runningOptions.train + "/LLEs.csv"

        // val HLE_lang_bias = allHLEs  //bias_fluents.filter(x => all_HLEs.contains(x))
        // val LLE_lang_bias = allLLEs  //bias_fluents.filter(x => all_LLEs.contains(x))

        /*
        /*Getting the coords of each vessel */
        val coordLines = Source.fromFile(LLEs_File).getLines().filter(x => x.split("\\|")(0) == "coord").toIterator
        val vesselCoordinatesMap = new mutable.HashMap[(String,String),Coordinate]() // Will be (MMSI, Time) -> (Long,Lat)

        while(coordLines.hasNext) {
          val coordSplit = coordLines.next.split("\\|")

          val currMMSI = coordSplit(3).toString
          val currTime = coordSplit(1).toString
          val currLong = coordSplit(4).toDouble
          val currLat = coordSplit(5).toDouble

          vesselCoordinatesMap += ((currMMSI, currTime) -> new Coordinate(currLong, currLat))
        }
        */

        val fileOpts = new FileDataOptions(HLE_Files_Dir = HLE_Dir_Path, LLE_File = LLEs_File,
          allHLEs = allHLEs, allLLEs = allLLEs, runOpts = runningOptions, false)


        /*val trainingDataOptions = new MongoDataOptions(dbNames       = Vector(runningOptions.train), chunkSize = runningOptions.chunkSize,
          targetConcept = runningOptions.targetHLE, sortDbByField = "time", what = "training")*/


        val trainingDataFunction: MongoDataOptions => Iterator[Example] = getMongoData
        val testingDataFunction: MongoDataOptions => Iterator[Example] = getMongoData




        val system = ActorSystem("LocalLearningSystem")
        val startMsg = new RunSingleCore

        // Actor and Props are for parallel processing
        //val coordinator = system.actorOf(Props(new LocalCoordinator(runningOptions, trainingDataOptions,
        //                                                            testingDataOptions, trainingDataFunction, testingDataFunction)), name = "LocalCoordinator")

        val coordinator = system.actorOf(Props(new LocalCoordinator(runningOptions, trainingDataOptions,
                                                trainingDataOptions, trainingDataFunction, testingDataFunction)), name = "LocalCoordinator")

        coordinator ! startMsg

      } else {

        /*val caviarNum = args.find(x => x.startsWith("caviar-num")).get.split("=")(1)

      val trainSet = Map(1 -> MeetingTrainTestSets.meeting1, 2 -> MeetingTrainTestSets.meeting2, 3 -> MeetingTrainTestSets.meeting3,
        4 -> MeetingTrainTestSets.meeting4, 5 -> MeetingTrainTestSets.meeting5, 6 -> MeetingTrainTestSets.meeting6,
        7 -> MeetingTrainTestSets.meeting7, 8 -> MeetingTrainTestSets.meeting8, 9 -> MeetingTrainTestSets.meeting9,
        10 -> MeetingTrainTestSets.meeting10)

      val dataset = trainSet(caviarNum.toInt)

      val trainingDataOptions =
        new MongoDataOptions(dbNames = dataset._1,//trainShuffled, //
          chunkSize = runningOptions.chunkSize, targetConcept = runningOptions.targetHLE, sortDbByField = "time", what = "training")

      val testingDataOptions =
        new MongoDataOptions(dbNames = dataset._2,
          chunkSize = runningOptions.chunkSize, targetConcept = runningOptions.targetHLE, sortDbByField = "time", what = "testing")

      val trainingDataFunction: MongoDataOptions => Iterator[Example] = FullDatasetHoldOut.getMongoData
      val testingDataFunction: MongoDataOptions => Iterator[Example] = FullDatasetHoldOut.getMongoData

      val system = ActorSystem("HoeffdingLearningSystem")
      val startMsg = "start"

      system.actorOf(Props(new Dispatcher(runningOptions, trainingDataOptions, testingDataOptions,
        trainingDataFunction, testingDataFunction) ), name = "Learner") ! startMsg*/

      }

      /*----------------------------*/
      /*Eval on test set in the end:*/
      /*----------------------------*/
      /*val caviarNum = args.find(x => x.startsWith("caviar-num")).get.split("=")(1)

      val trainSet = Map(1 -> MeetingTrainTestSets.meeting1, 2 -> MeetingTrainTestSets.meeting2, 3 -> MeetingTrainTestSets.meeting3,
        4 -> MeetingTrainTestSets.meeting4, 5 -> MeetingTrainTestSets.meeting5, 6 -> MeetingTrainTestSets.meeting6,
        7 -> MeetingTrainTestSets.meeting7, 8 -> MeetingTrainTestSets.meeting8, 9 -> MeetingTrainTestSets.meeting9,
        10 -> MeetingTrainTestSets.meeting10)

      val dataset = trainSet(caviarNum.toInt)

      val trainingDataOptions =
        new MongoDataOptions(dbNames = dataset._1,//trainShuffled, //
          chunkSize = runningOptions.chunkSize, targetConcept = runningOptions.targetHLE, sortDbByField = "time", what = "training")

      val testingDataOptions =
        new MongoDataOptions(dbNames = dataset._2,
          chunkSize = runningOptions.chunkSize, targetConcept = runningOptions.targetHLE, sortDbByField = "time", what = "testing")

      val trainingDataFunction: MongoDataOptions => Iterator[Example] = FullDatasetHoldOut.getMongoData
      val testingDataFunction: MongoDataOptions => Iterator[Example] = FullDatasetHoldOut.getMongoData

      val system = ActorSystem("HoeffdingLearningSystem")
      val startMsg = "start"

      system.actorOf(Props(new Dispatcher(runningOptions, trainingDataOptions, testingDataOptions,
        trainingDataFunction, testingDataFunction) ), name = "Learner") ! startMsg*/

    } else {
      logger.error(argsok._2)
      System.exit(-1)
    }
  }

}
