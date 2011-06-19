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
package event

import akka.dispatch.Future

trait CollectionEventProvider {

    private var _collectionListeners:Set[CollectionListener] = Set()
    
    def addCollectionListener(listener:CollectionListener){
        _collectionListeners = _collectionListeners + listener
    }
    
    def removeCollectionListener(listener:CollectionListener){
        _collectionListeners = _collectionListeners - listener
    }
    
    protected def triggerEvent(eventMessage:CollectionEventMessage){
        for(listener <- _collectionListeners){
            Future{
            	listener.handleCollectionEvent(Event(this, eventMessage))
            }
        }
    }
}