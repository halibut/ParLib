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

import worker.RemoteWorker

object ClientRunner {
    private val HELP_ARG = "-help"
    
    def main(args:Array[String]):Unit = {
        if(args.size == 0){
            printUsage();
            return;
        }
        
        val validArgs:Seq[Option[(String,Int,String)]] = args.map{arg =>
	   		arg match {
	   		    case HELP_ARG => printUsage(); None;
	   		    case _ => Some(validateArg(arg))
	   		}
	    }
	    
        for(validArg <- validArgs;
        	connParams <- validArg){
            startClient(connParams._1, connParams._2, connParams._3)
        }
    }
    
    private def startClient(host:String,port:Int,service:String):RemoteWorker = {
        val worker = new RemoteWorker()
        worker.run(host,port,service)
        worker
    }
    
    private def validateArg(arg:String):(String,Int,String) = {
        require(arg.matches("{^:}+:\\d+:{^:}+"),"Invalid argument. Should be in format: host:port:service")
        
        val parts = arg.split(":")
        
        (parts(0), Integer.valueOf(parts(1)), parts(2))
    }
    
    private def printUsage(){
        println("Usage: runclient host1:port1:service1 ... hostN:portN:serviceN")
        println("Examples:")
        println("    runclient localhost:8888:testService")
        println("    runclient someHost:7788:service1 someHost:7788:service2 someOtherHost:9901:service")
    }
}