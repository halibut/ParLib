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
import event._
import worker.WorkerSession

import Messages._

import akka.actor._
import akka.actor.Actor._
import scala.collection.mutable.Queue
import akka.dispatch.Future

import java.util.UUID


/**
 * This class defines how work is split, computed on remote worker clients, and
 * then put back together again on the server.
 * @tparam T type of Task the be processed by this job
 * @tparam R result type to be returned by the client when it is done processing a task T
 * @param taskProvider The container that this job uses to manage tasks.
 * @param onClientFunc The code that will be executed on the client.
 * @param onTaskCompleteFunc The code that is executed on the server once a task has been completed by the client.
 * @param synchronizeTaskCompletion If true, the job will process the result of a completed task one at a time.
 * Otherwise, completed tasks may be processed in parallel. If no onTaskCompleteFunc is defined, this has no effect.  
 * @param batchSize number of tasks that should be processed by a single client at a time. Most be greater than zero.
 * This is a hint to the WorkManager. The actual number processed by a client may be determined by batchSize, the
 * physical size of a task:T, the number of processors on the client, etc...
 */
final class Job[T,R](private val taskProvider:TaskProvider[T], 
        onTaskCompleteFunc:(T, R)=>Unit,
        onClientFunc:(T)=>R,
        synchronizeTaskCompletion:Boolean = false,
        batchSize:Int = 1) 
        extends Serializable with CollectionEventProvider with CollectionListener{

    require(taskProvider != null, "You must provide a taskProvider.")
    require(onClientFunc != null, "You must provide a onClient function.")
    require(batchSize >= 0, "batchSize must be greater than zero. Found " + batchSize)
    
    val jobId = Job.createId()
    
    taskProvider.addCollectionListener(this)
    
    
    val _taskCompleteActor:Option[ActorRef] = 
    	if(onTaskComplete != null){
	        val actor = actorOf(new Actor(){
		        override def receive = {
		            case Job.TaskComplete(task,result) =>
		                if(synchronizeTaskCompletion)
				          	onTaskComplete(task.asInstanceOf[T],result.asInstanceOf[R])
				        else{
				            Future{
				                onTaskComplete(task.asInstanceOf[T],result.asInstanceOf[R])
				            }
				        }
		        }
		    })
		    actor.start()
		    Some(actor)
    	}
    	else{
    	    None
    	}

    /**
     * Adds tasks to be completed.
     * @param tasks the tasks to add to the Job
     */
    def addTasks(tasks:Iterable[T]) { taskProvider.addTasks(tasks) }
    
    /**
     * Add a single task to be completed
     * @param task the task to add to the Job
     */
    def addTask(task:T){ taskProvider.addTask(task) }
    
    /**
     * Get the current number of tasks waiting to be processed 
     * (does not include tasks that are in work, but not completed yet)
     */
    def numTasks():Int = { taskProvider.numTasksRemaining }
    
    /**
     * Used by the WorkManager to send to a client to do work.
     */
    def takeNextTask():Option[T] = { taskProvider.takeNextTask }
    
    /**
     * Remove a task from the Job without processing it by a client.
     */
    def removeTask(task:T){ taskProvider.removeTask(task) }
    
    def handleTaskComplete(task:T,result:R){
        for(actor <- _taskCompleteActor){
            actor ! Job.TaskComplete(task,result)
        }
    }
    
    /**
     * Executes on the server whenever the client has completed a task
     */
    val onTaskComplete = onTaskCompleteFunc;

    /**
     * Executes asynchronously on the client
     */
    val onClient = onClientFunc;
    
    
    def handleCollectionEvent(event:Event[CollectionEventMessage]){
        triggerEvent(event.eventMessage)
    }
    
}

object Job{
    private def createId():JobId = {
        val uuid = UUID.randomUUID()
        JobId(uuid)
    }
    
    private case class TaskComplete[T,R](task:T,result:R)
}
