/**
 * (C) Copyright IBM Corp. 2015, 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.tiara.restapi

import org.apache.spark.Logging
import play.api.libs.json._
import redis.clients.jedis.Jedis
import redis.clients.jedis.Tuple
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent._

/**
 * Created by barbaragomes on 5/2/16.
 */
object ExecuteMetricAnalysis extends Logging {

  val hashTerms = "hashtags"
  val handlesTerms = "handles"
  val redisCountKeySuffix = Config.restapi.getString("redis-key-entity")

  def getTopTerms(count: Int): Future[String] = {
    logInfo(s"Getting Top Terms for day ${InMemoryData.date}")
    val result = future {getResponseForTopTerms(count)}

    result.recover {
      case e: Exception =>
        logError("Could not execute request.", e); Json.stringify(buildResponse(false))
    }
  }

  private def getResponseForTopTerms(count: Int): String = {
    val jedis = ApplicationContext.jedisPool.getResource
    try {
      val hashtags = getTopHashtags(count, jedis)
      val handles = getTopHandles(count, jedis)
      Json.stringify(buildResponse(true, hashtags, handles))
    } catch {
      case e: Exception =>
        logError("Could not get top terms", e); Json.stringify(buildResponse(false))
    } finally {
      jedis.close()
    }
  }

  private def getTopHashtags(count: Int, jresource: Jedis): JsObject = {
    logInfo(s"Getting hashtags: ${InMemoryData.date}:${redisCountKeySuffix}#")
    try {
      val topHashes =
        jresource.zrevrangeWithScores(s"${InMemoryData.date}:${redisCountKeySuffix}#", 0, count)
      var hashes: Array[JsObject] = Array.empty
      val it = topHashes.iterator()
      while (it.hasNext) {
        val elem = it.next()
        hashes = hashes :+ Json.obj("term" -> elem.getElement, "score" -> elem.getScore)
      }
      Json.obj(hashTerms -> hashes)
    } catch {
      case e: Exception => logError("Could not get top hashtags", e); Json.obj(hashTerms -> JsNull)
    }
  }

  private def getTopHandles(count: Int, jresource: Jedis): JsObject = {
    logInfo(s"Getting handles: ${InMemoryData.date}:${redisCountKeySuffix}@")
    try {
      val topHandles =
        jresource.zrevrangeWithScores(s"${InMemoryData.date}:${redisCountKeySuffix}@", 0, count)
      var handles: Array[JsObject] = Array.empty
      val it = topHandles.iterator()
      while (it.hasNext) {
        val elem = it.next()
        handles = handles :+ Json.obj("term" -> elem.getElement, "score" -> elem.getScore)
      }
      Json.obj(handlesTerms -> handles)
    } catch {
      case e: Exception =>
        logError("Could not get top handles", e); Json.obj(handlesTerms -> JsNull)
    }
  }

  private def buildResponse(success: Boolean,
                            hashes: JsObject = null,
                            handles: JsObject = null): JsObject = {
    val response = Json.obj("success" -> success) ++ Json.obj("status" -> 0)
    if (success) {
      response ++ hashes ++ handles
    } else {
      response ++ Json.obj(hashTerms -> JsNull) ++ Json.obj(handlesTerms -> JsNull)
    }
  }
}
