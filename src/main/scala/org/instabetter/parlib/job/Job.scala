/*
 * Copyright (C) 2011 instaBetter Software <http://insta-better.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.instabetter.parlib
package job

import util.ResourceQueue

import akka.actor._
import akka.actor.Actor._
import scala.collection.mutable.Queue


final class Job[T,R](private val taskProvider:TaskProvider[T], 
        onTaskCompleteFunc:(T, R)=>Unit,
        onClientFunc:(T)=>R) extends Serializable{
    
    def addTasks(tasks:Iterable[T]) { taskProvider.addTasks(tasks) }
    def addTask(task:T){ taskProvider.addTask(task) }
    def numTasks():Int = { taskProvider.numTasks }
    def takeNextTask():Option[T] = { taskProvider.takeNextTask }
    def removeTask(task:T){ taskProvider.removeTask(task) }
    
    /**
     * Executes on the server whenever the client has completed a task
     */
    val onTaskComplete = onTaskCompleteFunc;

    /**
     * Executes asynchronously on the client
     */
    val onClient = onClientFunc;

}

