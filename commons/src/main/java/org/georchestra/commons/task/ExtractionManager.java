package org.georchestra.commons.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.georchestra.commons.configuration.GeorchestraConfiguration;
import org.springframework.beans.factory.annotation.Autowired;


public class ExtractionManager {
	
    private static final Log LOG = LogFactory.getLog(ExtractionManager.class.getPackage().getName());
    
    private PriorityThreadPoolExecutor executor;
    private int maxExtractions;
    private int minThreads;

    // ThreadPoolExecutor API says that the internal queue should not be
    // accessed except for debugging so this
    // queue is here so that the non-running tasks can be accessed
    
    /** maintains the tasks ready to execute */
    /** comparison by priorities */

    private final static int INITIAL_CAPACITY = 20;
    private Queue<ExecutionTaskInterface> readyTaskQueue = new PriorityBlockingQueue<ExecutionTaskInterface>(INITIAL_CAPACITY);
    
    private Collection<ExecutionTaskInterface> cancelledTaskQueue = new PriorityBlockingQueue<ExecutionTaskInterface>();
    
    /** maintains the paused tasks. They can be selected by the user in random way */
    private Map<String, ExecutionTaskInterface> pausedTasks = Collections.synchronizedMap(new HashMap<String, ExecutionTaskInterface>());
    
    @Autowired
    private GeorchestraConfiguration georConfig ;

    public synchronized void init() {
        if ((georConfig != null) && (georConfig.activated())) {
            maxExtractions = Integer.parseInt(georConfig.getProperty("maxExtractions"));
            minThreads = Integer.parseInt(georConfig.getProperty("minThreads"));
        }

        BlockingQueue<Runnable> workQueue = new PriorityBlockingQueue<Runnable>();
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("Extractorapp-thread"
                        + System.currentTimeMillis());
                thread.setDaemon(true);
                return thread;
            }
        };
        executor = new PriorityThreadPoolExecutor(minThreads, maxExtractions, 5,
                TimeUnit.SECONDS, workQueue, threadFactory);
    }

    public void setMaxExtractions(int maxExtractions) {
        this.maxExtractions = maxExtractions;
    }

    public void setMinThreads(int minThreads) {
        this.minThreads = minThreads;
    }


    /**
     * Submits the task taking into account the task priorities.
     * 
     * @param task
     */
	public synchronized void submit(ExecutionTaskInterface task) {

		// creates the waiting task queue ordered by priority task
		this.readyTaskQueue.offer(task);

		Future<?> future = executor.submit(task);
		task.getMetadata().setFuture(future);
	}


    /**
     * Updates the priority if the task is in waiting status.
     * 
     * @param id	identification of required id		
     * @param newPriority
     */
    public synchronized void updatePriority(final String id, final ExecutionPriority newPriority) {
    	
        // search in the waiting tasks and updates the priority of the task
        ExecutionTaskInterface foundTask = null;
		for (ExecutionTaskInterface task : this.readyTaskQueue) {
			if(task.getMetadata().getState().equals(ExecutionState.WAITING) ){
				Future<?> future = task.getMetadata().getFuture();
				if(!future.isCancelled() && !future.isDone()){					
		            if (task.getMetadata().getUuid().equals(id)) {
		            	foundTask = task;
		            	break;
		            }
				}
			}
        }
		if(foundTask != null){
            // sets the new priority and reinsert the task
//			this.readyTaskQueue.remove(foundTask);
//
//			foundTask.executionMetadata.setPriority(newPriority);
//			
//			this.readyTaskQueue.offer(foundTask);
			
// XXX the commented lines where replaced by de following, looks like the ClassCastException was solved. Requires more test.
// the following code throws a ClassCastException: java.util.concurrent.FutureTask cannot be cast to java.lang.Comparable			
			foundTask.getMetadata().setPriority(newPriority);
            ExecutionTaskInterface taskCloned = foundTask.clone();
		
			foundTask.getMetadata().getFuture().cancel(true);
			this.executor.remove(foundTask);
			this.executor.purge();
			this.readyTaskQueue.remove(foundTask);
			
			submit(taskCloned);
			
			
        } else {
        	// searches if the task is in the paused queue
            ExecutionTaskInterface pausedTask = this.pausedTasks.get(id);
        	if(pausedTask != null){
            	pausedTask.getMetadata().setPriority(newPriority);
        	}
        }
    }

    

	/**
     * Will set priorities of all tasks to MEDIUM and re-add all waiting tasks back to the queue in the order of the uuids in newOrder.  
     * If a uuid is not the newOrder it will be deleted from the queue. 
     * 
     * @param newOrder a list of the task's uuids
     */
    public synchronized void updateAllPriorities(final List<String> newOrder) {
        executor.purge();
        Collection<ExecutionTaskInterface> newWaitingTasks = new TreeSet<ExecutionTaskInterface>(new Comparator<ExecutionTaskInterface>(){

            @Override
            public int compare(ExecutionTaskInterface task1, ExecutionTaskInterface task2) {
                return newOrder.indexOf(task1.getMetadata().getUuid()) - newOrder.indexOf(task2.getMetadata().getUuid());
            }
            
        });
        for (ExecutionTaskInterface task : this.readyTaskQueue) {
            if(task.getMetadata().isWaiting()) {
                readyTaskQueue.remove(task);
                if(newOrder.contains(task.getMetadata().getUuid())) {
                    newWaitingTasks.add(task);
                } else {
                    task.getMetadata().cancel();
                    cancelledTaskQueue.add(task);
                }
            }
        }
        
        for (ExecutionTaskInterface task : newWaitingTasks) {
            task.getMetadata().setPriority(ExecutionPriority.MEDIUM);
            submit(task);
        }
    }
    
    /**
     * Remove the task if it has got the waiting status
     * @param uuid
     */
    public synchronized void removeTask(String uuid) {
    	
        for (ExecutionTaskInterface task : this.readyTaskQueue) {
        	if(task.getMetadata().getPriority().equals(ExecutionState.WAITING) ){
                if (task.getMetadata().getUuid().equals(uuid)) {
                    task.getMetadata().cancel();
                    this.executor.remove(task);
                    // move from ready to canceled list
                    readyTaskQueue.remove(task);
                    this.cancelledTaskQueue.add(task);
                    break;
                }
        	}
        }
    }

    /**
     * Gets a deep copy of task queue metadata. The metadata objects are only copies 
     * (defensive copy) so no changes will be reflected on the actual tasks
     */
    public synchronized List<ExecutionMetadata> getTaskQueue() {
        List<ExecutionMetadata> queue = new ArrayList<ExecutionMetadata>();
        for (ExecutionTaskInterface task : this.readyTaskQueue) {
            queue.add(new ExecutionMetadata(task.getMetadata()));
        }
        for (ExecutionTaskInterface task : this.pausedTasks.values()) {
            queue.add(new ExecutionMetadata(task.getMetadata()));
        }
        for (ExecutionTaskInterface task : this.cancelledTaskQueue) {
            queue.add(new ExecutionMetadata(task.getMetadata()));
        }
        return queue;
    }

    /**
     * Search the task with the indeed uuid in the ready and paused tasks.
     * The search is done between the waiting, paused tasks
     * 
     * @param uuid	identifier of task to find
     * @return the {@link ExecutionTaskInterface} it exists, null in other case.
     */
    public synchronized ExecutionTaskInterface findTask(final String uuid) {
    	
    	List<ExecutionTaskInterface> tasks = queryRunableTask();
        for (ExecutionTaskInterface task: tasks) {
            if (task.getMetadata().getUuid().equals(uuid)) {
            	return task;
            }
        }
        return this.pausedTasks.get(uuid);
    }
    
    /**
     * @return Returns the waiting, paused tasks. That is all runnable tasks.
     */
    private List<ExecutionTaskInterface> queryRunableTask(){
    	
        List<ExecutionTaskInterface> queue = new LinkedList<ExecutionTaskInterface>();
        for (ExecutionTaskInterface task : this.readyTaskQueue) {
        	ExecutionState st = task.getMetadata().getState();
        	if(st.equals(ExecutionState.WAITING)){
                queue.add(task);
        	}
        }
        for (ExecutionTaskInterface pausedTask : this.pausedTasks.values()) {
            queue.add(pausedTask);
        }
        return queue;

    }
    /**
     * Changes the task's status
     * 
     * @param id	Task's identifier
     * @param newStatus the new status
     */
	public synchronized void  updateStatus(final String id, final ExecutionState newStatus) {
		
		switch (newStatus) {
		case COMPLETED:
		case RUNNING:
			break; // nothing to do
		case CANCELLED:
			cancelTask(id);
			break;
		case PAUSED:
			pauseTask(id);
			break;
		case WAITING:
			resumeTask(id);
			break;
		default:
			assert false : "illegal task state";
		}
	}

	/**
	 * If the task is in Waiting or Paused State it can be canceled
	 * @param id task's identifier
	 */
    private void cancelTask(final String id) {

        ExecutionTaskInterface foundTask = findTask(id);

		if(foundTask == null){
			return;
		}
		
		if (foundTask.getMetadata().isWaiting() ){

		    this.readyTaskQueue.remove(foundTask);
		    this.cancelledTaskQueue.add(foundTask);
		    cancelProcess(foundTask);
			
		} else if( foundTask.getMetadata().isPaused() ) {

        	this.pausedTasks.remove(id);
        	this.cancelledTaskQueue.add(foundTask);
			cancelProcess(foundTask);
		}
	}
    
    /**
     * Cancel the process 
     * @param task
     */
    private synchronized boolean cancelProcess(final ExecutionTaskInterface task) {
    	
    	task.getMetadata().cancel();
    	boolean wasCanceled = task.getMetadata().getFuture().cancel(true);
    	this.executor.remove(task); // purge cancelled task
    	return wasCanceled;
    }
    
    
	/**
     * Moves the task to the paused queue if it is in waiting status.
     * @param id task's identifier
     */
    private synchronized void pauseTask(final String id){

        ExecutionTaskInterface foundTask = null;
        for (ExecutionTaskInterface task: this.readyTaskQueue) {
        	if(task.getMetadata().getState().equals(ExecutionState.WAITING)){
        		if(!task.getMetadata().getFuture().isCancelled() && !task.getMetadata().getFuture().isDone()){
                    if (task.getMetadata().getUuid().equals(id)) {
                    	foundTask = task;
                    	break;
                    }
        		}
        	}
        }
		if(foundTask == null){
			return;
		}
		// moves the found task to the paused task queue
//		foundTask.executionMetadata.getFuture().cancel(true);
//		this.executor.remove(foundTask);
//        this.readyTaskQueue.remove(foundTask);
//        
//        foundTask.executionMetadata.setPaused();
//        this.pausedTasks.put(id, foundTask);
//        
//XXX Replaced by the following sentences:        
        foundTask.getMetadata().setPaused();
        ExecutionTaskInterface taskCloned = foundTask.clone();
	
		foundTask.getMetadata().getFuture().cancel(true);
		this.executor.remove(foundTask);
		this.executor.purge();
		this.readyTaskQueue.remove(foundTask);
		
        this.pausedTasks.put(id, taskCloned);
        
    }
    
    /**
     * Moves a paused task to the ready task queue
     * @param id
     */
    private synchronized void resumeTask(final String id){

        ExecutionTaskInterface foundTask = this.pausedTasks.get(id);
    	if( foundTask != null){
    		this.pausedTasks.remove(id);
    		foundTask.getMetadata().setWaiting();
    		submit(foundTask);
    	}
    }
	
    public synchronized void cleanExpiredTasks(long expiry) {
        ArrayList<ExecutionTaskInterface> toRemove = new ArrayList<ExecutionTaskInterface>();
        for (ExecutionTaskInterface task : readyTaskQueue) {
            ExecutionMetadata metadata = task.getMetadata();
            if (metadata.isCompleted() && (metadata.getStateChangeTime().getTime() + expiry) > System.currentTimeMillis()) {
                toRemove.add(task);
            }
        }
        readyTaskQueue.removeAll(toRemove);
        toRemove.clear();
        for (ExecutionTaskInterface task : cancelledTaskQueue) {
            ExecutionMetadata metadata = task.getMetadata();
            if ((metadata.getStateChangeTime().getTime() + expiry) > System.currentTimeMillis()) {
                toRemove.add(task);
            }
        }
        cancelledTaskQueue.removeAll(toRemove);
    }


}
