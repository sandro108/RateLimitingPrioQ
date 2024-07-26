package org.example;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;


import org.json.*;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    static int cnt = 0;
    private static final int MAX_REQUESTS = 50;
    private static int req_cnt_dq = 1;
    private static int req_cnt_eq = 1;

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);//.newSingleThreadScheduledExecutor();   //.newFixedThreadPool(1);
        ExecutorService executorService2 = Executors.newSingleThreadScheduledExecutor(); // .newFixedThreadPool(1);
        RateLimitingPrioQ prioQ = new RateLimitingPrioQ(1, 5);
        Random random = new Random(42L);
        final Object lock = new Object();
        final Object sleepLock = new Object();

        prioQ.writeToFile("\nStart DateTime: " + LocalDateTime.now() + "\n");
        prioQ.writeToFile("//#########################Job started!########################\n");



            executorService.execute(new Runnable() {
                public void run() {
                    logger.info("Enqueueing done by Thread: " + Thread.currentThread().getId());

                    while(req_cnt_eq <= MAX_REQUESTS) {

                        StringBuilder userInData = new StringBuilder("IN " + req_cnt_eq + ":");
                        req_cnt_eq++;
                        User user = null;
                        // create User 2 requests more frequent:
                        double rNum = random.nextDouble();
                        if (rNum > 0.3) {
                            user = new User(2, ++cnt);
                        } else {
                            user = new User(random.nextInt(3), ++cnt);
                        }
                        // or let the dice decide the distribution
//                           User user = new User(random.nextInt(3), ++cnt);
                        prioQ.enQuserRequest(user);
                        logger.info("Enqueued user w UID: " + user.getUID() + " cnt: " + user.getCnt());
                        synchronized (lock) {
                            userInData.append(user.toString());

                            JSONObject jsonMapBeforeDeQ = prioQ.convMapToJSONObj(prioQ.getRequestCounterMap());
                            prioQ.writeToFile(userInData + " - " + jsonMapBeforeDeQ.toString() + "\n");
                        }
                    }
                }
            });

            executorService2.execute(new Runnable() {
                public void run() {
                    logger.info("Dequeueing done by Thread: " + Thread.currentThread().getId());

                    while(req_cnt_dq <= MAX_REQUESTS ) {

                        synchronized (sleepLock) {
                            try {
                                sleepLock.wait(0, 500_000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        StringBuilder userOutData = new StringBuilder("OUT " + req_cnt_dq + ":");
                        req_cnt_dq++;

                        User userRequest = null;
                        try {
                            userRequest = prioQ.deQuserRequest();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        logger.info("dequeued userRequest:" + userRequest.getCnt());
                        userOutData.append(userRequest.toString());

                        JSONObject jsonMapAfterDeQ = prioQ.convMapToJSONObj(prioQ.getRequestCounterMap());
                        prioQ.writeToFile(userOutData + " - " + jsonMapAfterDeQ.toString() + "\n");

                    }
                }
            });

        // shutdown both executor services:
        executorService.shutdown();
        executorService.awaitTermination(1000L, TimeUnit.MILLISECONDS );
        if (executorService.isTerminated()) {
            logger.info("execSVC1 shutdown");
        }

        executorService2.shutdown();
        executorService2.awaitTermination(1000L, TimeUnit.MILLISECONDS );
        if (executorService2.isTerminated()) {
            logger.info("execSVC2 has shutdown.");
        }

       /* prioQ.writeToFile("//#########################Job is done!########################\n");
        prioQ.writeToFile("Job finished DateTime: " + LocalDateTime.now() + "\n");
        prioQ.writeToFile("--------------------------------------------------------------------------------------------------\n");
        prioQ.writeToFile("\n");*/
        System.out.println("Job is done! Bye!");
    }
}