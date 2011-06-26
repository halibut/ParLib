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

import job.Job
import worker.WorkerSession
import java.util.UUID
import akka.actor._

object Messages{
	//Messages the client sends to the Server
	case class RegisterClient();
	case class UnregisterClient(sessionId:SessionId);
	case class GetInstruction(sessionId:SessionId);
	case class CompletedTask(sessionId:SessionId,taskResults:Iterable[(TaskId,Any)]);
	
	//Messages the server sends to the client
	case class SessionId(uuid:UUID);
	case class TaskId(uuid:UUID);
	case class StartWorkerTask(clientCode:Class[_],tasks:Iterable[(TaskId,Any)]);
	case class NoTasksAvailable();
	case class NotRegistered();
	case class Disconnect();
	case class AssignJob(job:Job[Any,Any])
	case class KillSession(sessionid:SessionId)
	
	//Messages to manage jobs on the server
	case class AddJob(name:String, job:Job[Any,Any])
	case class RemoveJob(job:Job[Any,Any])
	case class GetJob(jobId:JobId)
	case class JobId(uuid:UUID);
	case object NextJob
}


