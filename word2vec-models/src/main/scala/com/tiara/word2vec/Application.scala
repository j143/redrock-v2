package com.tiara.word2vec

import akka.actor.ActorSystem
import com.typesafe.config.{ConfigFactory, Config}

/**
 * Created by barbaragomes on 4/15/16.
 */
object Application extends App{
  implicit val system = ActorSystem("Word2Vec-Actor")
  val updateWord2VecDaily = system.actorOf(W2vScheduler.props)
  updateWord2VecDaily ! W2vScheduler.StartW2VModelGeneration
}

object Config {
  // Global Application configuration
  val appConf: Config = ConfigFactory.load("tiara-app").getConfig("tiara")
  val word2vecConf = appConf.getConfig("word-2-vec")
}
