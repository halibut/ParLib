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
package worker

import util.Logging
import job.{Job,TaskInfo}
import Messages._

import scala.collection.mutable.{Map,Set}

import akka.actor._
import akka.dispatch.Future

import java.util.UUID
import java.util.Date

import WorkerSession._

class WorkerSession(private val workManager:ActorRef, private val jobManager:ActorRef) extends Actor with Logging {
    //Make sure that each time this actor is created, 
    //it gets a unique Uuid
	self.id = newUuid.toString
	
	private val _inWorkTasks:Map[TaskId,TaskInfo] = Map()
    private val _jobs:Map[JobId,Job[Any,Any]] = Map()
    
    private var _sessionId:Option[SessionId] = None
    
    override def receive = {
	    case RegisterClient() =>
	        if(_sessionId.isEmpty)
	            _sessionId = Some(createId());
	        else
	            debug("Client was already registered. Returning existing session ID.");
	        self.reply(_sessionId.get)
	    case msg:ClientMessage if(_sessionId.isEmpty) =>
	        warn("Client attempted to send a message without registering first.")
	        self.reply(NotRegistered("Client must be be registered to send messages."))
	    case msg:ClientMessage if(_sessionId.isDefined && _sessionId.get != msg.sessionId) =>
	        warn("Client attempted to send a message with the wrong session ID.")
	        self.reply(NotRegistered("Client must be send messages with the same Session ID as the one that was registered."))
	    case GetInstruction(sessionId) =>
	        val jobOpt = jobManager !! NextJob
        	jobOpt match {
            	case None => self.reply(NoTasksAvailable())			    //If we don't get a response, then reply with a NoTaskAvailable message
                case Some(None) => self.reply(NoTasksAvailable())     	    //If there is no job, then reply with a NoTaskAvailable message
                case Some(Some(jobAny)) =>
                	val job = jobAny.asInstanceOf[Job[Any,Any]]		
                    val tasks = job.takeNextBatch
		            if(tasks.isEmpty){
		                self.reply(NoTasksAvailable())
		            }
		            else{
		                if(!_jobs.contains(job.jobId)){
		                    _jobs += job.jobId -> job
		                }
		                val tasksIterable = tasks.map{task =>
		                    val taskId = WorkerSession.createTaskId
		                    val taskInfo = TaskInfo(job.jobId, sessionId, taskId, task, new Date())
		                    _inWorkTasks += taskId -> taskInfo
		                    (taskId, task)
		                }
		                
		                val jobClass = job.getClass
		                self.reply(StartWorkerTask(jobClass,tasksIterable))
		            }
            }
		case CompletedTask(sessionId,taskResults) =>
            for(idAndResult <- taskResults){
                val (taskId, taskResult) = idAndResult
	            for(taskInfo <- _inWorkTasks.remove(taskId)){
	                val job = _jobs(taskInfo.jobId)
	                val task = taskInfo.task
	                job.handleTaskComplete(task, taskResult)
	            }
            }
        case UnregisterClient(sessionId) =>
            //Add all in work tasks back to the jobs they came from
            returnUnfinishedTasks()
	    case msg =>
	        warn("Server received unrecognized message {}.", msg)
    }
    
	override def postStop{
	    debug("The remote client was disconnected.")
	    returnUnfinishedTasks()
	}
	
	private def returnUnfinishedTasks(){
	    for(taskEntry <- _inWorkTasks){
            val (taskId, taskInfo) = taskEntry
            val jobId = taskInfo.jobId
            val job = _jobs(jobId)
            job.addTask(taskInfo.task)
        }
	}
}

object WorkerSession{
    private def createId():SessionId = {
        val uuid = UUID.randomUUID()
        SessionId(uuid)
    }
    
    private def createTaskId():TaskId = {
        val uuid = UUID.randomUUID()
        TaskId(uuid)
    }
}
