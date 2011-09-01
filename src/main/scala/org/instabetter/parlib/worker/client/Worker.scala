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
package worker.client

import job.{ClientRunnable,RunOnClientFunc}
import Messages._
import util.Logging

import akka.actor._
import akka.actor.Actor._
import scala.collection.mutable.{Map}

trait Worker extends Logging{
    
    protected val _serverActor:ActorRef; 

    private def handleTasks(onClient:ClientRunnable[Any,Any], tasks:Iterable[(TaskId,Any)]):Iterable[(TaskId,Any)] = {
        //parallelize the Iterable and perform the computation for each task
        tasks.par.map{case (taskId, task) =>
            val result = onClient.runOnClient(task)
            (taskId, result)
        }.seq	//convert back to a sequential Iterable
    }
    
    private val _jobTypes:Map[String,ClientRunnable[Any,Any]] = Map()
    
    private def getClientCodeInstance(clientClassName:String):ClientRunnable[Any,Any] = {
        var jobOpt = _jobTypes.get(clientClassName)
        
        if(jobOpt.isDefined){
            jobOpt.get
        }else{
            val clientCode = createClientJobInstance(clientClassName)
            _jobTypes += clientClassName -> clientCode
            clientCode
        }
    }
    
    private def createClientJobInstance(clientClassName:String):ClientRunnable[Any,Any] = {
        println("Creating new job client: " + clientClassName)
        
        val clientClass = getClass().getClassLoader().loadClass(clientClassName)
        
        val constructors = clientClass.getConstructors
        var constructor = constructors(0)
        
        for(const <- constructors){
            if(const.getParameterTypes.size < constructor.getParameterTypes.size)
                constructor = const
        }

        val paramTypes = constructor.getParameterTypes
        val params:Array[AnyRef] = paramTypes.map{(clazz)=>
            getDefaultRefForType(clazz)
        }
        
        val clientCode = constructor.newInstance(params:_*)
        
        //If the clientCode is a Function1, then it needs to be wrapped in a RunOnClientFunc class
        //otherwise, it's an implementation of ClientRunnable and can directly be returned
        clientCode match{
            case func:Function1[_,_] => 
                new RunOnClientFunc[Any,Any](func.asInstanceOf[Function1[Any,Any]])
            case code:ClientRunnable[_,_] => 
                code.asInstanceOf[ClientRunnable[Any,Any]]
            case unknownCode =>
                error("Found unknown client code type: {}", unknownCode)
                throw new UnsupportedOperationException("Cannot instantiate object of type " + unknownCode)
        }
    }
    
    private def getDefaultRefForType[T](clazz:Class[T]):AnyRef = {
        clazz.getSimpleName match{
            case "byte" => Byte.box(0)
            case "short" => Short.box(0)
            case "int" => Int.box(0)
            case "long" => Long.box(0)
            case "char" => Char.box(0)
            case "float" => Float.box(0f)
            case "double" => Double.box(0)
            case "boolean" => Boolean.box(false)
            case _ => null
        }
    }
    
    def run(){
        var running = true;
        
        //Register with the server
        val registerMsg = _serverActor !! RegisterClient()

        if(registerMsg.isEmpty){
            return;
        }
        
        val sessionId = registerMsg.get.asInstanceOf[SessionId]
        
        while(running){
            val instOpt = _serverActor !! GetInstruction(sessionId)
            
            
            if(instOpt.isEmpty){
                warn("Did not receive a response from server. Exiting."); 
                running = false;
            }
            else{
                val inst = instOpt.get
                inst match{
                    case Disconnect() => running = false;
                    case NoTasksAvailable() => Thread.sleep(1000);
                    case StartWorkerTask(clientFuncClassName,tasks) => {
                        val job = getClientCodeInstance(clientFuncClassName)
                        val results = handleTasks(job, tasks)
                        _serverActor ! CompletedTask(sessionId,results);
                    }
                    case msg => 
                        warn("Received unknown message: {}. Exiting.",msg); 
                        running = false;
                }
            }
        }
    }

}
