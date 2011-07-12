package org.instabetter.parlib.job

/**
 * Code to be executed by a client for each Task
 * @tparam T the type of Task T; the data to perform the computation on
 * @tparam R the result of the computation
 */
trait ClientRunnable[-T,+R] {

    /**
     * The code to run on the client for each task.
     * @param task the data to perform the computation on
     * @return the result of the computation
     */
    def runOnClient(task:T):R;
    
    val clientClassName = this.getClass().getName()
}


final class RunOnClientFunc[-T,+R](val func:(T)=>R) extends ClientRunnable[T,R]{
    
    def runOnClient(task:T):R = {
        func(task)
    }
    
    override val clientClassName = func.getClass().getName()
}