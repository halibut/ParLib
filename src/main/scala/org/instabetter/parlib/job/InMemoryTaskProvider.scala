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

import event._
import util.ResourceQueue
import scala.collection.Iterable


/**
 * TaskProvider implementation that keeps all tasks in memory.
 */
class InMemoryTaskProvider[T] extends TaskProvider[T] with CollectionListener{
    
    private val _tasks:ResourceQueue[T] = { 
        val taskPool = new ResourceQueue[T]()
        taskPool
    }
    _tasks.addCollectionListener(this)
    
    def addTasks(tasks:Iterable[T]) { tasks.foreach(_tasks.add(_)) }
    def addTask(task:T){ _tasks.add(task) }
    def numTasksRemaining():Int = { _tasks.getCount() }
    def takeNextTask():Option[T] = { _tasks.removeNext }
    def removeTask(task:T){ _tasks.remove(task) }


    def handleCollectionEvent(event:Event[CollectionEventMessage]){
        triggerEvent(event.eventMessage)
    }
}