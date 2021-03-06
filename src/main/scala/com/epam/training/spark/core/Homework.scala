package com.epam.training.spark.core

import java.time.LocalDate

import com.epam.training.spark.core.domain.Climate
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object Homework {
  val DELIMITER = ";"
  val RAW_BUDAPEST_DATA = "data/budapest_daily_1901-2010.csv"
  val OUTPUT_DUR = "output"

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .setAppName("EPAM BigData training Spark Core homework")
      .setIfMissing("spark.master", "local[2]")
      .setIfMissing("spark.sql.shuffle.partitions", "10")
    val sc = new SparkContext(sparkConf)

    processData(sc)

    sc.stop()

  }

  def processData(sc: SparkContext): Unit = {

    /**
      * Task 1
      * Read raw data from provided file, remove header, split rows by delimiter
      */
    val rawData: RDD[List[String]] = getRawDataWithoutHeader(sc, Homework.RAW_BUDAPEST_DATA)

    /**
      * Task 2
      * Find errors or missing values in the data
      */
    val errors: List[Int] = findErrors(rawData)
    println(errors)

    /**
      * Task 3
      * Map raw data to Climate type
      */
    val climateRdd: RDD[Climate] = mapToClimate(rawData)

    /**
      * Task 4
      * List average temperature for a given day in every year
      */
    val averageTemeperatureRdd: RDD[Double] = averageTemperature(climateRdd, 1, 2)

    /**
      * Task 5
      * Predict temperature based on mean temperature for every year including 1 day before and after
      * For the given month 1 and day 2 (2nd January) include days 1st January and 3rd January in the calculation
      */
    val predictedTemperature: Double = predictTemperature(climateRdd, 1, 2)
    println(s"Predicted temperature: $predictedTemperature")

  }

  def getRawDataWithoutHeader(sc: SparkContext, rawDataPath: String): RDD[List[String]] = {
    sc.textFile(rawDataPath)
      .flatMap(s => s.split("""\r?\n"""))
      .filter(s => !s.startsWith("#"))
      .map(s => s.split(";", -1).toList)
  }

  def findErrors(rawData: RDD[List[String]]): List[Int] = {
    rawData
      .map(l => l.map(s => if(s.nonEmpty) 0 else 1 ))
      .reduce((l1, l2) => l1.zip(l2).map(l => l._1 + l._2))
  }

  def mapToClimate(rawData: RDD[List[String]]): RDD[Climate] = {
    rawData.map(l => Climate(l(0), l(1), l(2), l(3), l(4), l(5), l(6)))
  }

  def averageTemperature(climateData: RDD[Climate], month: Int, dayOfMonth: Int): RDD[Double] = {
    climateData
      .filter(c => c.observationDate.getMonthValue() == month && c.observationDate.getDayOfMonth() == dayOfMonth)
      .map(c => c.meanTemperature.value)
  }

  def dateToTuple(date: LocalDate): (Int, Int) = (date.getMonthValue, date.getDayOfMonth)

  def datesInRange(month: Int, dayOfMonth: Int): List[(Int, Int)] = {
    val date = LocalDate.of(2017, month, dayOfMonth)
    List(dateToTuple(date.minusDays(1)), dateToTuple(date), dateToTuple(date.plusDays(1)))
  }

  def predictTemperature(climateData: RDD[Climate], month: Int, dayOfMonth: Int): Double = {
    val dates = datesInRange(month, dayOfMonth)
    val pair = climateData
      .filter(c => dates.contains(c.observationDate.getMonthValue(), c.observationDate.getDayOfMonth()))
      .map(c => (c.meanTemperature.value, 1))
      .reduce((x,y) => (x._1 + y._1, x._2 + y._2))
    pair._1/pair._2
  }
}


