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

package org.instabetter

import parlib.job.{Job,InMemoryTaskProvider,RunOnClientFunc,ServerRunnable,ClientRunnable};
import parlib.WorkManager;

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.{ArrayBuffer, Map}

package object parlib {
    
    class CollectionJobPimp[T,CT<:Traversable[T]](col:CT){
        
        def distribMap[R,CR](mapFunc:(T)=>R)(implicit manifest : Manifest[R], bf: CanBuildFrom[CT, R, CR]): CR = {
            
            var received = 0
            
            val tmpResults:Array[R] = new Array(col.size)
            val tmpOrdering:Map[T,Int] = Map[T,Int]()
            val job = new Job[T,R](
	            new InMemoryTaskProvider[T],
	            new RunOnClientFunc[T,R](mapFunc),
	            new ServerRunnable[T,R](){
	                def postComputeTask(task:T,result:R):Unit ={
	                    val ind = tmpOrdering(task)
	                    tmpResults(ind) = result
	                    received += 1
	                }
	            },
	            1)
	        
	        var index = 0
	        for(task <- col){
	            tmpOrdering += (task -> index)
	            job.addTask(task)
	            index+=1
	        }
            Config.defaultWorkerManager.addJob(job)
            
            while(received < col.size){
                Thread.sleep(100);
            }
	            
	        val builder = bf()
	        for(result <- tmpResults){
	        	builder += result
	        }
	        builder.result()
        }
    }
    
    implicit def convertToCollectionJobPimp[T,C[T] <: Traversable[T]](col:C[T]):CollectionJobPimp[T,C[T]] = {
        new CollectionJobPimp(col)
    }
    
    object Config{
        var workerManagerPort = 8888;
        var workerManagerHost = "localhost";
        var workerManagerName = "service";
        
        lazy val defaultWorkerManager = {
            new WorkManager(workerManagerHost,
                    workerManagerPort,
                    workerManagerName)
        }
    }
}