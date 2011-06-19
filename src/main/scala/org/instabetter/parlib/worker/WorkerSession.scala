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

import job.{Job,TaskInfo}
import Messages._

import scala.collection.mutable.{Map,Set}

import akka.actor._

import java.util.UUID
import java.util.Date

class WorkerSession(val sessionId:SessionId) extends Actor {

    private val _inWorkTasks:Map[TaskId,TaskInfo] = Map()
    private val _jobs:Map[JobId,Job[Any,Any]] = Map()
    
    override def receive = {
        case AssignJob(job) => 
            val taskOpt = job.takeNextTask
            taskOpt match{
                case None => self.reply(NoTasksAvailable())
                case Some(task) =>
                    if(!_jobs.contains(job.jobId)){
                        _jobs += job.jobId -> job
                    }
                    val taskId = WorkerSession.createTaskId
                    val clientCodeClass = job.onClient.getClass
                    val taskInfo = TaskInfo(job.jobId, clientCodeClass, sessionId, taskId, task, new Date())
                    _inWorkTasks += taskId -> taskInfo
                    self.reply(StartWorkerTask(clientCodeClass,taskId,task))
            }
        case CompletedTask(sessionId,taskId,taskResult) =>
            for(taskInfo <- _inWorkTasks.remove(taskId)){
                val job = _jobs(taskInfo.jobId)
                val task = taskInfo.task
                job.handleTaskComplete(task, taskResult)
            }
        case UnregisterClient(sessionId) =>
            //Add all in work tasks back to the jobs they came from
            for(taskEntry <- _inWorkTasks){
                val (taskId, taskInfo) = taskEntry
                val jobId = taskInfo.jobId
                val job = _jobs(jobId)
                job.addTask(taskInfo.task)
            }
            //Send a message to the WorkManager that this session has cleaned up
            //and it is okay to stop and remove it
            self.reply(KillSession(sessionId))
    }
    
}

object WorkerSession{
    def createId():SessionId = {
        val uuid = UUID.randomUUID()
        SessionId(uuid)
    }
    
    private def createTaskId():TaskId = {
        val uuid = UUID.randomUUID()
        TaskId(uuid)
    }
}
