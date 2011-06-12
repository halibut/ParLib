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

import job.{TaskInfo, Job}
import util.ResourceQueue
import Messages._

import akka.actor._
import akka.actor.Actor._
import scala.collection.mutable.{Map,Set,HashSet,ArrayBuffer}
import java.util.Date

class WorkManager(host:String, port:Int, service:String){
    
    private val _manager = actorOf(new Actor{
        private var _nextSessionLow = 0L
        private var _nextSessionHigh = 0L
        private var _nextTaskLow = 0L
        private var _nextTaskHigh = 0L
        
        private val _registeredClients:Map[SessionId,ArrayBuffer[TaskId]] = Map()
        private val _inWorkTasks:Map[TaskId,TaskInfo] = Map()
        
	    private val _jobQueue:ResourceQueue[String] = new ResourceQueue
	    private val _jobs:Map[String,Job[Any,Any]] = Map()

        
        override def receive = {
            case RegisterClient() => self.reply(register());
            case GetInstruction(sessionId) => self.reply(getNextAvailableTask(sessionId));
            case CompletedTask(sessionId,taskId,taskResult) => taskCompleted(sessionId, taskId, taskResult);
            case UnregisterClient(sessionId) => unregister(sessionId)
            case AddJob(name,job) => addJob(name,job); 
            case RemoveJob(name) => removeJob(name)
        }
        
        def register():SessionId = {
            val session = getNextSessionId
            println("Registered client: " + session)
            _registeredClients += session -> ArrayBuffer[TaskId]()
            session
        }
        
        def getNextAvailableTask(sessionId:SessionId):Any = {
            val tasksForSessionOpt = _registeredClients.get(sessionId)
            
            //The client is not registered, so we can't do anything
            if(tasksForSessionOpt.isEmpty){
                return Disconnect()
            }
            
            val tasksForSession = tasksForSessionOpt.get
            
            for(jobName <- _jobQueue.removeNext){
                val job = _jobs(jobName)
                for(task <- job.takeNextTask){
                    
                	val taskId = getNextTaskId
                    val taskInfo = TaskInfo(jobName, job.onClient.getClass, sessionId, taskId, task, new Date())
                    tasksForSession
                    _inWorkTasks += taskId -> taskInfo
                    _jobQueue.add(jobName)
                    return StartWorkerTask(job.onClient.getClass, taskId, task)
                }
                
                if(_jobQueue.getCount > 0){
                    val nextTask = getNextAvailableTask(sessionId)
                    _jobQueue.add(jobName)
                    return nextTask
                }
                else{
                    _jobQueue.add(jobName)
                }
            }
            return NoTasksAvailable()
        }
        
        def taskCompleted(sessionId:SessionId, taskId:TaskId, result:Any) = {
            for(inWorkTasks <- _registeredClients.get(sessionId);
            	taskInfo <- _inWorkTasks.remove(taskId)){
                
                val jobName = taskInfo.jobName;
	            val task = taskInfo.task
	            val job = _jobs(jobName)
	            
	            job.onTaskComplete(task,result)
            }
        }
        
        def unregister(sessionId:SessionId){
            //If the client is registered, then remove it
            val curTasksOpt = _registeredClients.remove(sessionId)
            
            curTasksOpt.foreach{curTasks =>
                for(taskId <- curTasks){
                    val taskOpt = _inWorkTasks.remove(taskId)
                    taskOpt.foreach{task =>
                        val jobName = task.jobName
                        _jobs(jobName).addTask(task.task)
                    }
                }
            }
        }
        
        def addJob(name:String, job:Job[Any,Any]){
            if(_jobs.contains(name)){
                removeJob(name)
            }
            _jobs += name -> job
            _jobQueue.add(name)
        }
        
        def removeJob(name:String){
            val affectedTasks = _inWorkTasks.values.filter(name == _.jobName)
            val affectedsessions = affectedTasks.map(_.sessionId)
            
            for(task <- affectedTasks){
            	val taskInfo = _inWorkTasks.remove(task.taskId)
            	
            	for(sessionId <- affectedsessions;
	            	taskList <- _registeredClients.get(sessionId)
	            	if(taskList.contains(task.taskId))){
	                	taskList -= task.taskId 
            	}
            }
            
            _jobs -= name
            _jobQueue.remove(name)
        }
        
        def getNextSessionId:SessionId = {
            val sessionLow = _nextSessionLow
            val sessionHigh = _nextSessionHigh
            if(sessionLow == Long.MaxValue){
                _nextSessionLow = 0
                _nextSessionHigh += 1
            }
            else{
                _nextSessionLow += 1
            }
            
            SessionId(sessionLow, sessionHigh)
        }
        def getNextTaskId:TaskId = {
            val taskLow = _nextTaskLow
            val taskHigh = _nextTaskHigh
            if(taskLow == Long.MaxValue){
                _nextTaskLow = 0
                _nextTaskHigh += 1
            }
            else{
                _nextTaskLow += 1
            }
            
            TaskId(taskLow, taskHigh)
        }
    }).start
    
    remote.start(host, port)
	remote.register(service, _manager)
	
	def addJob[T,R](name:String, job:Job[T,R]){
        _manager ! AddJob(name, job.asInstanceOf[Job[Any,Any]])
    }
    
    def removeJob(name:String){
        _manager ! RemoveJob(name)
    }
	
	private case class AddJob(name:String, job:Job[Any,Any])
	private case class RemoveJob(name:String)
}
