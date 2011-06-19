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

import event._
import job.{TaskInfo, Job, JobManager}
import worker.{WorkerSessionManager,WorkerSession}
import util.ResourceQueue
import Messages._

import akka.actor._
import akka.actor.Actor._
import scala.collection.mutable.{Map,Set,HashSet,ArrayBuffer}
import java.util.Date

class WorkManager(host:String, port:Int, service:String){
    
    private val _manager = actorOf(new Actor() {

        private val _jobManager = new JobManager();
        private val _sessions:Map[SessionId,ActorRef] = Map()
        
	    override def receive = {
            case RegisterClient() => 
                val sessionId = WorkerSession.createId
                val session = actorOf(new WorkerSession(sessionId));
                session.start();
                _sessions += sessionId -> session
            case GetInstruction(sessionId) =>
                val sessionOpt = _sessions.get(sessionId)
                sessionOpt match {
                    case None => self.reply(NotRegistered())    //If the session isn't registered, then return a NotRegistered() message
                    case Some(session) => 
                        val jobOpt = (_jobManager.managerActor !! NextJob)
		                jobOpt match {
		                    case None => self.reply(NoTasksAvailable())			//If we don't get a response, then reply with a NoTaskAvailable message
		                    case Some(None) => self.reply(NoTasksAvailable())   //If there is no job, then reply with a NoTaskAvailable message
		                    case Some(Some(jobAny)) =>
		                        val job = jobAny.asInstanceOf[Job[Any,Any]]		
		                        val sessionOpt = _sessions.get(sessionId)
		                        self.reply(session !!! AssignJob(job))			//Reply with a future from the worker session
		                }
                }
            case CompletedTask(sessionId,taskId,taskResult) => 
                val sessionOpt = _sessions.get(sessionId)
                sessionOpt match {
                    case None => self.reply(NotRegistered())    //If the session isn't registered, then return a NotRegistered() message
                    case Some(session) =>
                        self.forward(session)					//Forward to the session responsible for the task
                }
            case UnregisterClient(sessionId) => 
                val sessionOpt = _sessions.get(sessionId)
                sessionOpt match {
                    case None => self.reply(NotRegistered())    //If the session isn't registered, then return a NotRegistered() message
                    case Some(session) =>
                        self.forward(session)					//Tell the session to clean up and prepare to be stopped
                }
            case KillSession(sessionId) =>
                //This message is sent by the Session, and it means
                //that any unfinished tasks have been added back to the appropriate
                //job. Now we can stop the Session and remove it from our map
                val sessionOpt = _sessions.remove(sessionId)
                sessionOpt.foreach{session =>
                    session.stop()
                }
            case AddJob(name,job) => self.forward(_jobManager.managerActor); 
            case RemoveJob(jobId) => self.forward(_jobManager.managerActor);
        }
        
        
    }).start
    
    remote.start(host, port)
	remote.register(service, _manager)
	
	def addJob[T,R](job:Job[T,R], name:String = "Job"){
        _manager ! AddJob(name, job.asInstanceOf[Job[Any,Any]])
    }
    
    def removeJob(job:Job[Any,Any]){
        _manager ! RemoveJob(job)
    }
	
}
