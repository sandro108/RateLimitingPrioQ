package org.example;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;


import org.json.*;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    static int cnt = 0;
    private static int MAX_REQUESTS = 50;
    private static int req_cnt = 1;

    public static void main(String[] args) throws InterruptedException {
        //ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();   //.newFixedThreadPool(1);
        //ExecutorService executorService2 = Executors.newSingleThreadScheduledExecutor(); // .newFixedThreadPool(1);
        RateLimitingPrioQ prioQ = new RateLimitingPrioQ(1, 5);
        Random random = new Random(42L);
        boolean isEnqueued = false;

        prioQ.writeToFile("\nStart DateTime: " + LocalDateTime.now() + "\n");
        prioQ.writeToFile("//#########################Job started!########################\n");



//            executorService.execute(new Runnable() {
//                public void run() {
                    logger.info("Enqueueing done by Thread: " + Thread.currentThread().getId());
                    while(req_cnt <= MAX_REQUESTS) {
//                      logger.info("Round-Nr: " + req_cnt);
                        StringBuilder userInData = new StringBuilder("IN " + req_cnt + ":");
                        req_cnt++;
                        double delay = random.nextDouble();
                        if (delay > 0.5) {
                            try {
                                Thread.sleep(800, 0);
                                logger.info("Thread.Nr:"+ Thread.currentThread().getId() + " has slept, delay: " + delay);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
//                        synchronized (this) {
                            User user = new User(random.nextInt(3), ++cnt);
                            prioQ.enQuserRequest(user);
                            logger.info("Enqueued user w UID: " + user.getUID() + " cnt: " + user.getCnt());
                            userInData.append(user.toString());//.append("\n");
                            JSONObject jsonMapBeforeDeQ = prioQ.convMapToJSONObj(prioQ.getRequestCounterMap());
    //                        cnt++;
                            prioQ.writeToFile(userInData + " - " + jsonMapBeforeDeQ.toString() + "\n");
//                        }
                    }
//                }
//            });
       /* executorService.shutdown();
        if (executorService.isTerminated()) {
            logger.info("execSVC1 shutdown");
        }
        executorService.awaitTermination(10_000L, TimeUnit.MILLISECONDS );
        if (executorService.isTerminated()) {
            logger.info("execSVC1 shutdown");
        }*/
        prioQ.writeToFile("----------PrioQ filled-------------\n");
        req_cnt = 1;

//            executorService.execute(new Runnable() {
//
//
//                public void run() {
                    while(req_cnt <= MAX_REQUESTS ) {

                        StringBuilder userOutData = new StringBuilder("OUT " + req_cnt + ":");
                        req_cnt++;

                        logger.info("Dequeueing done by Thread: " + Thread.currentThread().getId());
//                        synchronized (this) {

                            if (!prioQ.getPrioQ().isEmpty()) {
                                User userRequest = prioQ.deQuserRequest();
                                logger.info("dequeued userRequest:" + userRequest.getCnt());
                                userOutData.append(userRequest.toString());//.append("\n");

                            }
                            JSONObject jsonMapAfterDeQ = prioQ.convMapToJSONObj(prioQ.getRequestCounterMap());
                            prioQ.writeToFile(userOutData + " - " + jsonMapAfterDeQ.toString() + "\n");
//                        }
                    }
//                }

                //            n++;
                //            Thread.sleep(1000);
//            });


        /*executorService.shutdown();


        executorService.awaitTermination(1000L, TimeUnit.MILLISECONDS );
        if (executorService.isTerminated()) {
            logger.info("execSVC has shutdown.");
        }*/
        prioQ.writeToFile("//#########################Job is done!########################\n");
        prioQ.writeToFile("Job finished DateTime: " + LocalDateTime.now() + "\n");
        prioQ.writeToFile("--------------------------------------------------------------------------------------------------\n");
        prioQ.writeToFile("\n");
        System.out.println("Job is done! Bye!");
    }
}