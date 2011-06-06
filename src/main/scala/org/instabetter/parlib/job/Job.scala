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


abstract class Job[T,R]() extends Serializable{
    
    private lazy val _tasks:ResourceQueue[T] = { 
        val taskPool = new ResourceQueue[T]()
        taskPool
    }
    
    def addTasks(tasks:Iterable[T]) { tasks.foreach(_tasks.add(_)) }
    def addTask(task:T){ _tasks.add(task) }
    def numTasks():Int = { _tasks.getCount() }
    def takeNextTask():Option[T] = { _tasks.removeNext }
    def removeTask(task:T){ _tasks.remove(task) }
    
    /**
     * Executes on the server whenever the client has completed a task
     */
    def taskCompleted(task:T, results:R);

    /**
     * Executes on the client whenever a remote client connects and 
     * can handle the task.
     */
    def clientCode(task:T):R;

}

object TestJobs{
    val test = new Job[String,String](){
        def taskCompleted(task:String, results:String){}
        def clientCode(task:String):String = { task + "World" }
    }
    
    test.addTasks(List("Hello", "Goodby"))
    
}