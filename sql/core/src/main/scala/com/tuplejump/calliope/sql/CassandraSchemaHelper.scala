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

package com.tuplejump.calliope.sql

import com.datastax.driver.core._
import org.apache.spark.Logging

import scala.collection.GenMap
import scala.collection.mutable.{HashMap, SynchronizedMap, Map => MMap}
import scala.util.{Failure, Success, Try}

object CassandraSchemaHelper extends Logging {

  @transient
  val sessionMap: MMap[String, Session] = new HashMap[String, Session] with SynchronizedMap[String, Session]

  def getCassandraTableSchema(host: String,
                              port: String,
                              keyspace: String,
                              columnFamily: String,
                              cassandraUsername: Option[String],
                              cassandraPassword: Option[String]): TableMetadata = {

    require(keyspace != null, "Unable to read schema: keyspace is null")
    require(columnFamily != null, "Unable to read schema: columnFamily is null")

    val clusterMeta: Metadata = getCassandraMetadata(host, port, cassandraUsername, cassandraPassword)
    val keyspaceMeta: KeyspaceMetadata = clusterMeta.getKeyspace( s""""${keyspace}"""")
    val tableMeta = keyspaceMeta.getTable( s""""${columnFamily}"""")
    tableMeta
  }

  def getCassandraMetadata(host: String, nativePort: String, cassandraUsername: Option[String], cassandraPassword: Option[String]): Metadata = {

    val sessionKey: String = if (cassandraUsername.isDefined && cassandraPassword.isDefined) {
      s"$host:$nativePort:$cassandraUsername.$cassandraPassword"
    } else {
      s"$host:$nativePort"
    }

    val session: Session = sessionMap.get(sessionKey) match {
      case Some(s) => s
      case None =>
        val newSession = createSession(host, nativePort, cassandraUsername, cassandraPassword)
        if(newSession != null) {
          sessionMap.put(sessionKey, newSession)
        }
        newSession
    }
    if (session != null) session.getCluster.getMetadata else null
  }

  private def createSession(host: String, nativePort: String, cassandraUsername: Option[String], cassandraPassword: Option[String]): Session = {
    val builder = if (cassandraUsername.isDefined && cassandraPassword.isDefined) {
      new Cluster.Builder()
        .addContactPoint(host)
        .withPort(nativePort.toInt)
        .withCredentials(cassandraUsername.get, cassandraPassword.get)
    } else {
      new Cluster.Builder()
        .addContactPoint(host)
        .withPort(nativePort.toInt)
    }

    Try(builder.build().connect()) match {
      case Success(driver) =>
        driver
      case Failure(ex) =>
        logWarning(s"Cassandra server not available at $host:$nativePort")
        logWarning(s"Cassandra data will not be available")
        logError(s"Reason for not connecting:  ${ex.getMessage}")
        logDebug(s"Failed to connect to Cassandra", ex)
        null
    }
  }
}
