package net.idothehax.invissues;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.util.ArrayList;
import java.util.List;

/**
 * Proper tick-based task scheduler for delayed execution.
 */
public class TaskScheduler {
    private static final List<ScheduledTask> tasks = new ArrayList<>();
    private static final List<ScheduledTask> tasksToRemove = new ArrayList<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            Invissues.LOGGER.warn("TaskScheduler already initialized!");
            return;
        }

        // Register a tick event to process scheduled tasks
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tasksToRemove.clear();

            // Decrement all tasks and collect those that are ready to run
            for (ScheduledTask task : new ArrayList<>(tasks)) {
                task.ticksRemaining--;

                if (task.ticksRemaining <= 0) {
                    try {
                        task.runnable.run();
                    } catch (Exception e) {
                        Invissues.LOGGER.error("Error executing scheduled task", e);
                    }
                    tasksToRemove.add(task);
                }
            }

            // Remove completed tasks AFTER iteration
            tasks.removeAll(tasksToRemove);
        });

        initialized = true;
        Invissues.LOGGER.info("TaskScheduler initialized successfully!");
    }

    /**
     * Schedule a task to run after a certain number of ticks.
     * @param delayTicks Number of ticks to wait before executing
     * @param task The task to run
     */
    public static void schedule(int delayTicks, Runnable task) {
        if (!initialized) {
            Invissues.LOGGER.error("TaskScheduler not initialized! Call TaskScheduler.init() first!");
            return;
        }
        tasks.add(new ScheduledTask(delayTicks, task));
        Invissues.LOGGER.info("Scheduled task with {} tick delay. Total tasks: {}", delayTicks, tasks.size());
    }

    private static class ScheduledTask {
        int ticksRemaining;
        Runnable runnable;

        ScheduledTask(int ticks, Runnable runnable) {
            this.ticksRemaining = ticks;
            this.runnable = runnable;
        }
    }
}