package org.georchestra.commons.task;


public interface ExecutionTaskInterface extends Runnable, Comparable<ExecutionTaskInterface>{

    public ExecutionMetadata getMetadata();

    public ExecutionTaskInterface clone();


}
