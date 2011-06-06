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
package tools

import job.Job
import worker.RemoteWorker

object RemoteExampleServer {

    def main(args:Array[String]):Unit = {
    	val workManager = new WorkManager("localhost",8888,"service")
    	
    	val strings = List("Hello", "Goodby", "Blow up the outside", "Remote control", "Test the")
    	val job = new Job[String,String](){
    	    
    	    def clientCode(task:String):String = {
    	        println("Got: " + task)
    	        Thread.sleep(5000);
    	        task + " world."
    	    }
    	    
    	    def taskCompleted(task:String, result:String) = {
    	        println(result)
    	    }
    	}
    	
    	job.addTasks(strings)
    	
    	workManager.addJob("testJob", job);
    }
}

object RemoteExampleClient {
    def main(args:Array[String]):Unit = {
    	val worker = new RemoteWorker
    	worker.run("localhost",8888,"service")
    }
}