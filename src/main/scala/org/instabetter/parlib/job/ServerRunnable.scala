package org.instabetter.parlib.job

/**
 * Trait for running per-Task code on the server
 * @tparam T type of Task to be run by the client
 * @tparam R type of Result computed by the client for each Task T
 */
trait ServerRunnable[T,R] {

    /**
     * Executes for each task after it is computed by the client
     * @param task the Task T
     * @param result the result of the computation performed by the client
     */
    def postComputeTask(task:T,result:R):Unit
    
    /**
     * Whether or not to perform the postComputeTask sequentially (defaults to true, but
     * can be overridden in implementing classes.
     */
    val synchronizePostComputeTask:Boolean = true
}


/**
 * No Op implementation of Server runnable code that does nothing
 * on each task.
 */
class NoOpServerRunnable[T,R] extends ServerRunnable[T,R]{
    override def postComputeTask(task:T,result:R):Unit = {}
    
    override val synchronizePostComputeTask:Boolean = false
}