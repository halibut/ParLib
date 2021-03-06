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

import event._
import akka.actor._
import akka.actor.Actor._
import scala.collection.mutable.{Queue,ArrayBuffer}

class ResourceQueue[T]() extends CollectionEventProvider with Serializable{
    import ResourceQueue._
    
    private val _queueMgr:ActorRef = actorOf(new Actor{
        private val _resources:Queue[T] = Queue()
        private var _inQueue:Int = 0
        
        def receive = {
            case Enqueue(res) => 
                _resources.enqueue(res.asInstanceOf[T])
                _inQueue += 1
                if(_inQueue == 1)
                    triggerEvent(CollectionHasElement)
            case Remove(res) =>
                val removed = _resources.dequeueFirst(_ == res)
                if(removed.isDefined){
                    _inQueue -= 1
                    checkForEmptyCollection()
                }
			case Dequeue(numElements) => 
			    var count = numElements
		        var elements = ArrayBuffer[T]()
		        while(_inQueue > 0 && count > 0){
		            elements += _resources.dequeue
		            count -= 1
		            _inQueue -= 1
			    }
			    checkForEmptyCollection()
		        self.reply(elements)
			case Size() => 
			    self.reply(_resources.size)
		}
        
        private def checkForEmptyCollection(){
            if(_inQueue == 0)
                triggerEvent(EmptyCollection)
        }
    }).start
     
    def getCount():Int = {
        (_queueMgr !! Size()).foreach{value =>
            return value.asInstanceOf[Int];
        }
        throw new RuntimeException("Task Manager stopped responding.")
    }
    
    def remove(resource:T){
        _queueMgr ! Remove(resource)
    }
    
    def removeNext():Option[T] = {
        val removed = removeNext(1)
        if(removed.isEmpty)
            None
        else
            Some(removed.head)
    }
    
    def removeNext(numElements:Int):Iterable[T] = {
        val response = _queueMgr !! Dequeue(numElements)
        response.foreach{removed =>
        	val elements = removed.asInstanceOf[Iterable[T]]
        	return elements
        }
        ArrayBuffer()
    }

    def add(resource:T){
        _queueMgr ! Enqueue(resource)
    }
    
    
   
}

object ResourceQueue{
    case class Enqueue(resource:Any)
    case class Remove(resource:Any)
    case class Dequeue(numElements:Int)
    case class Size()
    
    abstract class PoolEvent()
    case class EmptyPool() extends PoolEvent
    case class PoolHasResources() extends PoolEvent
}