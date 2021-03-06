/*
 * Licensed to Tuplejump Software Pvt. Ltd. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Tuplejump Software Pvt. Ltd. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

package com.tuplejump.calliope.stargate

import org.codehaus.jackson.annotate.{JsonCreator, JsonProperty}
import org.codehaus.jackson.map.ObjectMapper

import scala.util.parsing.json.{JSON, JSONArray, JSONObject}

object JsonMapping {

  trait Condition {
    def toJson: JSONObject
  }

  case class BooleanCondition(@JsonProperty("boost") boost: Float = 1.0f,
                              @JsonProperty("must") must: List[Condition],
                              @JsonProperty("should") should: List[Condition],
                              @JsonProperty("not") not: List[Condition]) extends Condition {
    override def toJson: JSONObject = {
      val baseMap: Map[String, Any] = Map("boost" -> boost, "type" -> "boolean")

      val mustMap: Map[String, Any] = if(must.isEmpty)Map.empty[String, Any] else Map("must" -> JSONArray(must.map(_.toJson)))

      val shouldMap: Map[String, Any] = if(should.isEmpty)Map.empty[String, Any] else Map("should" -> JSONArray(should.map(_.toJson)))

      val notMap: Map[String, Any] = if(not.isEmpty)Map.empty[String, Any] else Map("not" -> JSONArray(not.map(_.toJson)))

      JSONObject(baseMap ++ mustMap ++ shouldMap ++ notMap)
    }
  }

  case class FuzzyCondition(@JsonProperty("boost") boost: Float = 1.0f,
                            @JsonProperty("field") field: String,
                            @JsonProperty("value") value: String,
                            @JsonProperty("maxEdits") maxEdits: Integer = 2,
                            @JsonProperty("prefixLength") prefixLength: Integer = 0,
                            @JsonProperty("maxExpansions") maxExpansions: Integer = 50,
                            @JsonProperty("transpositions") transpositions: Boolean = true) extends Condition {
    override def toJson: JSONObject = JSONObject(Map(
      "boost" -> boost,
      "type" -> "fuzzy",
      "field" -> field,
      "value" -> value,
      "maxEdits" -> maxEdits,
      "prefixLength" -> prefixLength,
      "maxExpansions" -> maxExpansions,
      "transpositions" -> transpositions
    ))
  }

  case class LuceneCondition(@JsonProperty("boost") boost: Float = 1.0f,
                             @JsonProperty("field") field: String,
                             @JsonProperty("value") query: String) extends Condition {
    override def toJson: JSONObject = JSONObject(Map(
      "boost" -> boost,
      "type" -> "lucene",
      "field" -> field,
      "value" -> query
    ))
  }

  case class MatchCondition(@JsonProperty("boost") boost: Float = 1.0f,
                            @JsonProperty("field") field: String,
                            @JsonProperty("value") value: Any) extends Condition {
    override def toJson: JSONObject = JSONObject(Map(
      "boost" -> boost,
      "type" -> "match",
      "field" -> field,
      "value" -> value
    ))
  }

  case class RangeCondition(@JsonProperty("boost") boost: Float = 1.0f,
                            @JsonProperty("field") field: String,
                            @JsonProperty("lower") lower: Option[Any],
                            @JsonProperty("upper") upper: Option[Any],
                            @JsonProperty("include_lower") includeLower: Boolean = false,
                            @JsonProperty("include_upper") includeUpper: Boolean = false) extends Condition {
    require(lower.isDefined || upper.isDefined, "Either 'lower' or 'upper' or both ends of the range must be set.")

    override def toJson: JSONObject = JSONObject(Map(
      "boost" -> boost,
      "type" -> "range",
      "field" -> field,
      "include_lower" -> includeLower,
      "include_upper" -> includeUpper
    ) ++ ((upper, lower) match {
      case (Some(u), Some(l)) => Map("upper" -> u, "lower" -> l)
      case (None, Some(l)) => Map("lower" -> l)
      case (Some(u), None) => Map("upper" -> u)
      case (None, None) => {
        //This should never happen aas we are checking for either one to be defined
        Map.empty[String, Any]
      }
    }))
  }

  case class PhraseCondition(@JsonProperty("boost") boost: Float = 1.0f,
                             @JsonProperty("field") field: String,
                             @JsonProperty("values") values: List[String],
                             @JsonProperty("slop") slop: Integer = 0) extends Condition {
    override def toJson: JSONObject = JSONObject(Map(
      "boost" -> boost,
      "type" -> "phrase",
      "field" -> field,
      "values" -> values,
      "slop" -> slop
    ))
  }

  case class PrefixCondition(@JsonProperty("boost") boost: Float = 1.0f,
                             @JsonProperty("field") field: String,
                             @JsonProperty("value") value: String) extends Condition {
    override def toJson: JSONObject = JSONObject(Map(
      "boost" -> boost,
      "type" -> "prefix",
      "field" -> field,
      "value" -> value
    ))
  }

  case class RegexpCondition(@JsonProperty("boost") boost: Float = 1.0f,
                             @JsonProperty("field") field: String,
                             @JsonProperty("value") value: String) extends Condition {
    override def toJson: JSONObject = JSONObject(Map(
      "boost" -> boost,
      "type" -> "regexp",
      "field" -> field,
      "value" -> value
    ))
  }

  case class WildcardCondition(@JsonProperty("boost") boost: Float = 1.0f,
                               @JsonProperty("field") field: String,
                               @JsonProperty("value") value: String) extends Condition {
    override def toJson: JSONObject = JSONObject(Map(
      "boost" -> boost,
      "type" -> "wildcard",
      "field" -> field,
      "value" -> value
    ))
  }

  case class SortField(@JsonProperty("field") field: String, @JsonProperty("reverse") reverse: Boolean)

  case class Sort(@JsonProperty("fields") sortFields: List[SortField])

  case class Search(@JsonProperty("query") queryCondition: Condition,
                    @JsonProperty("filter") filterCondition: Condition,
                    @JsonProperty("sort") sort: Sort)

  /* def parseStargateQuery(query: String) = {
    val parsedQuery = JSON.parseFull(query)
    val (filter, function) = parsedQuery map {
      case Some(j) =>
        println(j)
        (JSONObject())
    }
  } */
}
