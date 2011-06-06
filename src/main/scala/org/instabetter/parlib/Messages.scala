package org.instabetter.parlib


object Messages{
	//Messages the client sends to the Server
	case class RegisterClient();
	case class UnregisterClient(sessionId:SessionId);
	case class GetInstruction(sessionId:SessionId);
	case class CompletedTask(sessionId:SessionId,taskId:TaskId,taskResult:Any);
	
	//Messages the server sends to the client
	case class SessionId(sessionLow:Long, sessionHigh:Long);
	case class TaskId(taskLow:Long, taskHigh:Long);
	case class StartWorkerTask(jobType:Class[_],taskId:TaskId,task:Any);
	case class NoTasksAvailable();
	case class Disconnect();

}

