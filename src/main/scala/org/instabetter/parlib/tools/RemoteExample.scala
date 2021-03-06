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

import job.{Job,InMemoryTaskProvider,ClientRunnable}
import worker.client.RemoteWorker
import util.Logging


object RemoteExampleServer {

    import org.instabetter.parlib._
    
    def main(args:Array[String]):Unit = {
    	Config.workerManagerPort = 8888;
    	Config.workerManagerHost = "localhost"
    	Config.workerManagerName = "service"
    	
    	val strings = List("Hello", "Goodby", "Blow up the outside", "Remote control", "Test the")
    	val mappedStrings = strings.distribMap{task =>
            println("Got: " + task)
	        Thread.sleep(5000);
	        task + " world."
    	}
    	
    	for(str <- mappedStrings){
    	    println(str)
    	}
    }
    
}

object RemoteExampleClient {
    def main(args:Array[String]):Unit = {
    	val worker = new RemoteWorker("localhost",8888,"service")
    	worker.run()
    }
}