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

import scala.collection.mutable.Map

import Messages.AddJob
import Messages.RemoveJob
import Messages.SessionId
import akka.actor.Actor.actorOf
import akka.actor.Actor.remote
import akka.actor.actorRef2Scala
import akka.actor.Actor
import akka.actor.ActorRef
import akka.remoteinterface.RemoteClientConnected
import akka.remoteinterface.RemoteClientDisconnected
import akka.remoteinterface.RemoteClientError
import akka.remoteinterface.RemoteClientShutdown
import akka.remoteinterface.RemoteClientStarted
import akka.remoteinterface.RemoteClientWriteFailed
import job.Job
import job.JobManager
import util.Logging
import worker.WorkerSession

class WorkManager(host:String, port:Int, service:String) extends Logging{
    
    private val _jobManager = new JobManager();
    
    private val _manager = actorOf(new Actor() {

        
        private val _sessions:Map[SessionId,ActorRef] = Map()
        
	    override def receive = {
            case msg:AddJob => _jobManager.managerActor.forward(msg); 
            case msg:RemoveJob => _jobManager.managerActor.forward(msg);
            case msg => warn("Unhandled Message: {}",msg)
        }
        
        
    }).start
    
    val _listener = actorOf(new Actor {
	  def receive = {
	    case msg:RemoteClientError => warn("An error occured: {}",msg)
	    case msg:RemoteClientWriteFailed => warn("A write failed: {}",msg)
	    case msg => debug("Received RemoteClient message: {}", msg)
	  }
	}).start()

	
	remote.start(host, port)
	remote.registerPerSession(service, actorOf(new WorkerSession(_manager, _jobManager.managerActor)))
	remote.addListener(_listener)
	
	def addJob[T,R](job:Job[T,R], name:String = "Job"){
        _manager ! AddJob(name, job.asInstanceOf[Job[Any,Any]])
    }
    
    def removeJob(job:Job[Any,Any]){
        _manager ! RemoveJob(job)
    }
	
}
