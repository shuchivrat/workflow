package com.nirmata.workflow.details;

import com.google.common.base.Preconditions;
import com.nirmata.workflow.details.internalmodels.CompletedTaskModel;
import com.nirmata.workflow.details.internalmodels.ExecutableTaskModel;
import com.nirmata.workflow.spi.TaskExecution;
import com.nirmata.workflow.spi.TaskExecutionResult;
import com.nirmata.workflow.spi.TaskExecutor;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.nirmata.workflow.details.InternalJsonSerializer.addCompletedTask;
import static com.nirmata.workflow.spi.JsonSerializer.newNode;
import static com.nirmata.workflow.spi.JsonSerializer.nodeToString;

public class ExecutableTaskRunner
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final TaskExecutor taskExecutor;
    private final CuratorFramework curator;

    public ExecutableTaskRunner(TaskExecutor taskExecutor, CuratorFramework curator)
    {
        this.taskExecutor = Preconditions.checkNotNull(taskExecutor, "taskExecutor cannot be null");
        this.curator = Preconditions.checkNotNull(curator, "curator cannot be null");
    }

    public void executeTask(ExecutableTaskModel executableTask)
    {
        TaskExecution taskExecution = taskExecutor.newTaskExecution(executableTask.getTask());

        TaskExecutionResult result = taskExecution.execute();
        CompletedTaskModel completedTask = new CompletedTaskModel(true, result.getResultData());
        String json = nodeToString(addCompletedTask(newNode(), completedTask));
        try
        {
            String path = ZooKeeperConstants.getCompletedTaskPath(executableTask.getScheduleId(), executableTask.getTask().getTaskId());
            curator.setData().forPath(path, json.getBytes());
        }
        catch ( Exception e )
        {
            log.error("Could not set completed data for executable task: " + executableTask, e);
            throw new RuntimeException(e);
        }
    }
}
