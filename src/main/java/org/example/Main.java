package org.example;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import static org.example.RLPQ_Config.*;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;


import org.json.*;

//import static org.example.RLPQ_Config.MAX_REQUESTS;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    /*this is just for print outs of sequence */
    static int cnt = 0;

    private static int req_cnt_dq = 1;
    private static int req_cnt_eq = 1;

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();   //.newFixedThreadPool(1);
        ExecutorService executorService2 = Executors.newSingleThreadScheduledExecutor(); // .newFixedThreadPool(1);
        RateLimitingPrioQv4 prioQ = new RateLimitingPrioQv4(1, REQUEST_COUNT_LIMIT); //was 5
        Random random = new Random(RANDOM_SEED);
        final Object lock = new Object();
        final Object sleepLock = new Object();

//        prioQ.writeToFile("\nStart DateTime: " + LocalDateTime.now() + "\n");
        //prioQ.writeToFile("//#########################Job started!########################\n");



            executorService.execute(new Runnable() {
                public void run() {
                    logger.info("Enqueueing done by Thread: " + Thread.currentThread().getId());

                    while(req_cnt_eq <= MAX_REQUESTS) {

                        StringBuilder userInData = new StringBuilder("IN," + req_cnt_eq +  ",");
                        req_cnt_eq++;
                        User user = null;
                        // create User 'FAVORED_USER' requests more frequent:
                        double rNum = random.nextDouble();
                        if (rNum > REQ_DIST_PROBABILITY) {
                            user = new User(FAVORED_USER, ++cnt);
                        } else {
                            user = new User(random.nextInt(MAX_USER), ++cnt);
                        }
                        // or let the dice decide the distribution
//                           User user = new User(random.nextInt(MAX_USER), ++cnt);
                        try {
                            prioQ.enQuserRequest(user);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
//                        logger.info("Enqueued user w UID: " + user.getUID() + " cnt: " + user.getCnt());
                        synchronized (lock) {
                            userInData.append(user.toString());

                            JSONObject jsonMapBeforeDeQ = prioQ.convMapToJSONObj(prioQ.getRequestCounterMap());
                            prioQ.writeToFile(userInData + /*" - " + jsonMapBeforeDeQ.toString() + */"\n");
                        }
                    }
                }
            });

            executorService2.execute(new Runnable() {
                public void run() {
                    logger.info("Dequeueing done by Thread: " + Thread.currentThread().getId());
                  /*
                        try {
                            Thread.sleep(0, 500_000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                  */
                    while(req_cnt_dq <= MAX_REQUESTS + D_QUEUE_THREAD_EXTRA_ROUNDS) {
                  /*
                        try {
                            Thread.sleep(0, 1_000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                  */
                  /*
                        synchronized (sleepLock) {
                            try {
                                sleepLock.wait(0, 1_000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                   */
                        StringBuilder userOutData = new StringBuilder("OUT," + req_cnt_dq + ",");
                        req_cnt_dq++;

                        User userRequest = null;
                        try {
                            userRequest = prioQ.deQuserRequest();
                            if (userRequest == null) {
                                System.out.println("User dequeued is null.");
                                continue;}
                            userRequest.setdQTime(System.nanoTime());
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

//                        logger.info("dequeued userRequest:" + userRequest.getCnt());
                        userOutData.append(userRequest.toString());

                        JSONObject jsonMapAfterDeQ = prioQ.convMapToJSONObj(prioQ.getRequestCounterMap());
                        prioQ.writeToFile(userOutData /*+ " - " + jsonMapAfterDeQ.toString()*/ + "\n");

                    }
                    /*synchronized (sleepLock) {
                        try {
                            sleepLock.wait(2_000, 0);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }*/
                }
            });

            executorService2.shutdown();
            try {
                executorService2.awaitTermination(500L, TimeUnit.MILLISECONDS );
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (executorService2.isTerminated()) {
                logger.info("execSVC2 has shutdown.");
            }
            // shutdown both executor services:
            executorService.shutdown();
            try {
                executorService.awaitTermination(1000L, TimeUnit.MILLISECONDS );
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (executorService.isTerminated()) {
                logger.info("execSVC1 has shutdown");
            }


       /* prioQ.writeToFile("//#########################Job is done!########################\n");
        prioQ.writeToFile("Job finished DateTime: " + LocalDateTime.now() + "\n");
        prioQ.writeToFile("--------------------------------------------------------------------------------------------------\n");
        prioQ.writeToFile("\n");*/
        System.out.println("Job is done! Bye!");
    }
}