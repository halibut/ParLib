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


import Messages._
import event._

import akka.dispatch._
import akka.actor.Actor
import akka.actor.Actor._

import java.util.UUID

import scala.collection.mutable.{Map, Queue, Set}

class JobManager {
    import JobManager._

    val managerActor = actorOf(new Actor() {
        //Set up a priority dispatcher so that some messages get handled before others
        self.dispatcher = new PriorityExecutorBasedEventDrivenDispatcher("Job Manager Dispatcher", priorityGenerator)
        
        private val _jobListener = new CollectionListener(){
            override def handleCollectionEvent(event:Event[CollectionEventMessage]){
                val job = event.sender.asInstanceOf[Job[Any,Any]]
                event.eventMessage match{
                    case EmptyCollection      => self ! OutOfTasks(job)
                    case CollectionHasElement => self ! ContainsTasks(job)
                }
            }
        }
        
        /**
         * Contains a map of id -> Job for the jobs that currently have tasks remaining
         */
        private val _idJobMap:Map[JobId,Job[Any,Any]] = Map()	
    	
        /**
         * Contains a map of Job -> id for all registered jobs, regardless of whether or not they have tasks
         */
        private val _jobIdMap:Map[Job[Any,Any],JobId] = Map()	 
    	
        /**
         * Contains a set of JobId for registered jobs that do not have any tasks
         */
    	private val _outOfTaskJobs:Map[JobId,Job[Any,Any]] = Map()
    	
    	/**
    	 * Queue of jobIds for jobs that have tasks
    	 */
    	private val _jobQueue:Queue[JobId] = Queue()
    	
    	override def receive = {
    	   case AddJob(name,job) => addJob(name,job); 
           case RemoveJob(job) => removeJob(job);
           case GetJob(jobId) => self.reply(getJob(jobId))
           case QueueJob(jobId) => queueJob(jobId);
           case NextJob => self.reply(nextJob());
           case OutOfTasks(job) => outOfTasks(job);
           case ContainsTasks(job) => containsTasks(job);
    	}
    	
    	private def addJob(name:String, job:Job[Any,Any]):JobId = {
    	    if(_jobIdMap.contains(job)){
    	        _jobIdMap(job)
    	    }
    	    else{
	    	    val id = job.jobId
	    	    _idJobMap += id -> job
	    	    _jobIdMap += job -> id
	    	    job.addCollectionListener(_jobListener)
	    	    if(job.numTasks > 0){
	    	    	self ! QueueJob(id)
	    	    }
	    	    else{
	    	        self ! OutOfTasks(job)
	    	    }
	    	    id
    	    }
    	}
    	
    	private def removeJob(job:Job[Any,Any]){
    	    for(id <- _jobIdMap.remove(job)){
    	        _jobIdMap.remove(job)
    	        job.removeCollectionListener(_jobListener)
    	        _jobQueue.dequeueAll(_ == job)
    	    }
    	}
    	
    	private def getJob(id:JobId):Option[Job[Any,Any]] = {
    	    for(job <- _idJobMap.get(id)){
    	        return Some(job)
    	    }
    	    for(job <- _outOfTaskJobs.get(id)){
    	        return Some(job)
    	    }
    	    return None
    	}
    	
    	private def queueJob(jobId:JobId){
    	    for(job <- _idJobMap.get(jobId)){
    	        _jobQueue.enqueue(jobId)
    	    }
    	}
    	
    	private def nextJob():Option[Job[Any,Any]] = {
    	    if(_jobQueue.isEmpty)
    	        return None
    	        
    	    val jobId = _jobQueue.dequeue
    	    val job = _idJobMap(jobId)
	        if(job.numTasks > 0){
	            self ! QueueJob(jobId)	//Queue the job again
	            Some(job)
	        }
	        else{
	            nextJob()
	        }
    	}
    	
    	private def outOfTasks(job:Job[Any,Any]) = {
    	    for(jobId <- _jobIdMap.get(job)){
    	    	_jobQueue.dequeueAll(_ == jobId)
    	    	_idJobMap.remove(jobId)
    	    	_outOfTaskJobs += jobId -> job
    	    }
    	}
    	
    	private def containsTasks(job:Job[Any,Any]) = {
    	    for(jobId <- _jobIdMap.get(job);
    	    	if(_outOfTaskJobs.contains(jobId))){
    	    	_outOfTaskJobs.remove(jobId)
    	    	_idJobMap += jobId -> job
    	    	self ! QueueJob(jobId)
    	    }
    	}
    	
    }).start()
    
}

object JobManager{
    
    private val priorityGenerator = PriorityGenerator { // Create a new PriorityGenerator, lower priority means more important
	    case OutOfTasks(job)    => 0    // OutOfTasks is most important because it means we should skip it when looking for tasks
	    case ContainsTasks(job) => 50   // ContainsTasks is less important than OutOfTasks, but more important than other messages
	    case _                  => 100  // All other messages should be treated last
	}
    
    private case class OutOfTasks(job:Job[Any,Any])
    private case class ContainsTasks(job:Job[Any,Any])
    private case class QueueJob(jobId:JobId)
}