package org.instabetter.parlib
package job

import Messages._
import java.util.Date
import akka.actor._

case class TaskInfo(jobName:String, jobType:Class[_], sessionId:SessionId, taskId:TaskId, task:Any, startTime:Date){
    private val starTimeMillis = startTime.getTime
    
    def elapsedTime:Long = {
        new Date().getTime() - starTimeMillis
    }
}