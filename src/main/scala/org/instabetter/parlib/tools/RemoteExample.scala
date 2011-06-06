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