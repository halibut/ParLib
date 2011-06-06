package org.instabetter.parlib
package worker

import job.Job
import Messages._

import akka.actor._
import akka.event.EventHandler
import akka.actor.Actor._

import scala.collection.mutable.{Map}

class RemoteWorker() extends Serializable{

    private def handleTask(jobType:Class[_], task:Any):Any = {
        val job = getClientJobInstance(jobType)
        
        job.clientCode(task)
    }
    
    private val _jobTypes:Map[Class[_],Job[Any,Any]] = Map()
    
    private def getClientJobInstance(jobType:Class[_]):Job[Any,Any] = {
        var jobOpt = _jobTypes.get(jobType)
        
        if(jobOpt.isDefined){
            jobOpt.get
        }else{
            val jobInst = createJobInstanceForClient(jobType)
            _jobTypes += jobType -> jobInst
            jobInst
        }
    }
    
    private def createJobInstanceForClient(jobType:Class[_]):Job[Any,Any] = {
        println("Creating new Job: " +jobType)
        
        val constructors = jobType.getConstructors
        val typeParams = jobType.getTypeParameters
        var constructor = constructors(0)
        
        for(const <- constructors){
            if(const.getParameterTypes.size < constructor.getParameterTypes.size)
                constructor = const
        }
        
        if(constructor.getTypeParameters.size > 0){
            throw new RuntimeException("Requires a no-arg constructor.")
        }
        
        val job = constructor.newInstance()
        job.asInstanceOf[Job[Any,Any]]
    }
    
    private def getNullRefForType[T](clazz:Class[T]):T = {
        null.asInstanceOf[T]
    }
    
    def run(remoteHost:String, remotePort:Int, remoteService:String){
        var running = true;
        
        //Get the server
        val _server = remote.actorFor(remoteService, remoteHost, remotePort)
        val registerMsg = _server !! RegisterClient()

        if(registerMsg.isEmpty){
            return;
        }
        
        val sessionId = registerMsg.get.asInstanceOf[SessionId]
        
        while(running){
            val instOpt = _server !! GetInstruction(sessionId)
            
            if(instOpt.isEmpty){
                running = false;
            }
            else{
                val inst = instOpt.get
                inst match{
                    case Disconnect() => running = false;
                    case NoTasksAvailable() => Thread.sleep(1000);
                    case StartWorkerTask(jobType,taskId,task) => {
                        val result = handleTask(jobType, task)
                        _server ! CompletedTask(sessionId,taskId,result);
                    }
                    case msg => println("Received unknown message: "+msg); running = false;
                }
            }
        }
        
    }

}
