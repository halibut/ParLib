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

package org.instabetter.parlib.job

class JobBuilder[T,R]() {

    private var _taskProvider:Option[TaskProvider[T]] = None;
    private var _onTaskCompleteFunc:Option[(T, R)=>Unit] = None;
    private var _onClientFunc:Option[(T)=>R] = None;
    
    def clientCode(clientCodeFunc:(T)=>R):JobBuilder[T,R] = {
        _onClientFunc = Option(clientCodeFunc); 
        this;
    }
    def andClientCode(clientCodeFunc:(T)=>R):JobBuilder[T,R] = {
        clientCode(clientCodeFunc)
    }
    
    def taskPr0vider(taskProviderFunc:TaskProvider[T]):JobBuilder[T,R] = {
        _taskProvider = Option(taskProviderFunc)
        this
    }
    def andTaskProvider(taskProviderFunc:TaskProvider[T]):JobBuilder[T,R] = {
        taskPr0vider(taskProviderFunc)
    }
    
    def taskComplete(taskCompleteFunc:(T,R)=>Unit):JobBuilder[T,R] = {
        _onTaskCompleteFunc = Option(taskCompleteFunc)
        this
    }
    def andTaskComplete(taskCompleteFunc:(T,R)=>Unit):JobBuilder[T,R] = {
        taskComplete(taskCompleteFunc)
    }
    
    def build():Job[T,R] = {
        require(_onClientFunc.isDefined)
        
        val taskProvider = _taskProvider.getOrElse(new InMemoryTaskProvider[T]())
        val onTaskComplete = _onTaskCompleteFunc.getOrElse(null)
        val onClient = _onClientFunc.get
        
        new Job(taskProvider, onTaskComplete, onClient)
    }
}