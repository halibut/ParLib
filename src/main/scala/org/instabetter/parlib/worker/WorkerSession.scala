package org.instabetter.parlib
package worker

import job.TaskInfo
import Messages._

import akka.actor._

abstract class WorkerSession(val sessionId:SessionId, val workManagerActor:ActorRef) extends Actor {

    private val _inWorkTasks = Map[TaskId,TaskInfo]()
    
//    override def receive = {
////        case GetInstruction(sessionId) => self.reply(getNextAvailableTask(sessionId));
////        case CompletedTask(sessionId,taskId,taskResult) => taskCompleted(sessionId, taskId, taskResult);
////        case UnregisterClient(sessionId) => unregister(sessionId)
//    }
    
}

