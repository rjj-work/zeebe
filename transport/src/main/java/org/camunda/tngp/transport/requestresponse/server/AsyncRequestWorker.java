package org.camunda.tngp.transport.requestresponse.server;

import org.camunda.tngp.dispatcher.FragmentHandler;
import org.camunda.tngp.dispatcher.Subscription;
import org.camunda.tngp.util.actor.Actor;

/**
 * <p>
 * Base class for implementing asynchronous request/response server workers processing requests which need to wait on async i/o.
 * </p>
 *
 * <p>
 * Setup:
 * </p>
 * <ul>
 * <li>A request buffer to which incoming requests are submitted</li>
 * <li>A response write buffer to which responses are written</li>
 * <li>An async work dispatcher to which asynchronous work is submitted and operates in pipeline mode</li>
 * </ul>
 *
 * <p>
 * Requirement: response must be deferred until asynchronous work completes.
 *  Completion is tracked by acting as the last consumer on the async work pipeline.
 * </p>
 *
 * <p>
 * Workflow:
 * </p>
 * <ul>
 * <li>requests are taken from the request buffer.</li>
 * <li>the request header is decoded and the corresponding {@link AsyncRequestHandler} is determined</li>
 * <li>the request is passed to the {@link AsyncRequestHandler} which processes it.</li>
 * <li>if the request requires a response, the response is allocated from a {@link DeferredResponsePool}.</li>
 * <li>this involves claiming the required space on the response send buffer</li>
 * <li>this needs to be done *before* the request submits asynchronous work</li>
 * <li>the response is then deferred until the async work completes</li>
 * <li>after that the response is committed</li>
 * </ul>
 */
public class AsyncRequestWorker implements Actor
{
    protected final String name;

    protected final FragmentHandler fragmentHandler;
    protected final Subscription requestSubscription;
    protected final DeferredResponsePool responsePool;

    protected final WorkerTask[] workerTasks;

    protected final AsyncRequestWorkerContext context;

    public AsyncRequestWorker(String name, AsyncRequestWorkerContext context)
    {
        this.name = name;
        this.context = context;
        this.fragmentHandler = new RequestFragmentHandler(context);
        this.requestSubscription = context.getRequestBufferSubscription();
        this.responsePool = context.getResponsePool();
        this.workerTasks = context.getWorkerTasks();
    }

    @Override
    public int doWork() throws Exception
    {
        int workCount = 0;

        final int pooledResponseCount = responsePool.getPooledCount();

        if (pooledResponseCount > 0)
        {
            // poll for as many incoming requests as pooled responses are available
            workCount += requestSubscription.peekAndConsume(fragmentHandler, pooledResponseCount);
        }

        // run additional tasks
        if (workerTasks != null)
        {
            for (int i = 0; i < workerTasks.length; i++)
            {
                workCount += workerTasks[i].execute(context);
            }
        }

        return workCount;
    }

    @Override
    public String name()
    {
        return name;
    }

}
