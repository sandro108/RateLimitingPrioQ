package org.example;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;


import org.json.*;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    static int cnt = 0;

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        ExecutorService executorService2 = Executors.newFixedThreadPool(1);
        RateLimitingPrioQ prioQ = new RateLimitingPrioQ(1, 5);
        Random random = new Random(42L);
        boolean isEnqueued = false;
        int n = 0;


//        while(n < 10) {
//            int m = random.nextInt( 10);
        int m = 1;
        while(m < 10) {
            logger.info("Round-Nr: " + m);
            StringBuilder userInData = new StringBuilder("IN "+m+":\n");
            StringBuilder userOutData = new StringBuilder("OUT "+m+":\n");
            m++;
            executorService.execute(new Runnable() {
                public void run() {
                    logger.info("threadpool1 started:" + Thread.currentThread().getId());
//                    double delay = random.nextDouble();
//                    if (delay > 0.5) {
//                        try {
//                            Thread.sleep(0, 25);
//                            logger.info("Thread.Nr:"+ Thread.currentThread().getId() + " has slept.");
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
                    synchronized (this) {
                        User user = new User(random.nextInt(3), ++cnt);
                        prioQ.enQuserRequest(user);
                        logger.info("Enqueued user:" + user.cnt);
                        userInData.append(user.toString()).append("\n");
                        JSONObject jsonMapBeforeDeQ = prioQ.convMapToJSONObj(prioQ.getRequestCounterMap());
//                        cnt++;
                        prioQ.writeToFile(
                                userInData + "\n"
                                        + jsonMapBeforeDeQ.toString() + "\n");
                    }
                }
            });
            //                User user = new User(random.nextInt(3), cnt);
            //                isEnqueued = prioQ.enQuserRequest(user);
            //                cnt++;
            //                userInData.append(user.toString()).append("\n");
            //                if (!isEnqueued) {continue;}
            //            }
            //            while(prioQ.getPrioQ().size() > 10) {

            executorService2.execute(new Runnable() {

                public void run() {

                    logger.info("threadpool2 started:" + Thread.currentThread().getId());
                    synchronized (this) {

                        if (!prioQ.getPrioQ().isEmpty()) {
                            User userRequest = prioQ.deQuserRequest();
                            logger.info("dequeued userRequest:" + userRequest.cnt);
                            userOutData.append(userRequest.toString()).append("\n");

                        }
                        JSONObject jsonMapAfterDeQ = prioQ.convMapToJSONObj(prioQ.getRequestCounterMap());
                        prioQ.writeToFile(
//                                    userInData + "\n"
//                                            + jsonMapBeforeDeQ.toString() + "\n"
                                        userOutData + "\n"
                                        + jsonMapAfterDeQ.toString() + "\n"
                        );
                    }
                }

                //            n++;
                //            Thread.sleep(1000);
            });
        }
        executorService.shutdown();
        if (executorService.isTerminated()) {
            logger.info("execSVC1 shutdown");
        }
        executorService2.shutdown();

        executorService.awaitTermination(10_000L, TimeUnit.MILLISECONDS );
        if (executorService.isTerminated()) {
            logger.info("execSVC1 shutdown");
        }
        executorService2.awaitTermination(10_000L, TimeUnit.MILLISECONDS );
        if (executorService2.isTerminated()) {
            logger.info("execSVC2 shutdown");
        }
        System.out.println("Job is done! Bye!");
    }
}