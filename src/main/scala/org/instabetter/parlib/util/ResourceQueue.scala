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
package util

import akka.actor._
import akka.actor.Actor._
import scala.collection.mutable.{Queue}

class ResourceQueue[T]() extends Serializable{
    import ResourceQueue._
    
    private val _poolMgr:ActorRef = actorOf(new Actor{
        val _resources:Queue[T] = Queue()
        
        def receive = {
            case Enqueue(res) => {_resources.enqueue(res.asInstanceOf[T])}
            case Remove(res) => {_resources.dequeueFirst(_ == res)}
			case Dequeue() => {
			    if(_resources.isEmpty){
			        self.reply(None)
			    }
			    else{
				    self.reply(Some(_resources.dequeue))
			    }
			}
			case Size() => {self.reply(_resources.size)}
		}
    }).start
     
    def getCount():Int = {
        (_poolMgr !! Size()).foreach{value =>
            return value.asInstanceOf[Int];
        }
        throw new RuntimeException("Task Manager stopped responding.")
    }
    
    def remove(resource:T){
        _poolMgr ! Remove(resource)
    }
    
    def removeNext():Option[T] = {
        val response = _poolMgr !! Dequeue()
        response.foreach{removed =>
        	return removed.asInstanceOf[Option[T]].map(_.asInstanceOf[T])
        }
        throw new RuntimeException("Task Manager stopped responding.")
    }

    def add(resource:T){
        _poolMgr ! Enqueue(resource)
    }
}

object ResourceQueue{
    case class Enqueue(resource:Any)
    case class Remove(resource:Any)
    case class Dequeue()
    case class Size()
    
    abstract class PoolEvent()
    case class EmptyPool() extends PoolEvent
    case class PoolHasResources() extends PoolEvent
}