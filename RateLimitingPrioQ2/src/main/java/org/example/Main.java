package org.example;

import java.util.PriorityQueue;
import java.util.Random;
import org.json.*;

public class Main {



    public static void main(String[] args) throws InterruptedException {

        RateLimitingPrioQ prioQ = new RateLimitingPrioQ(1, 5);
        Random random = new Random(42L);
        boolean isEnqueued = false;
        int n = 0;

        while(n < 10) {
            StringBuilder userInData = new StringBuilder("IN "+n+":\n");
            StringBuilder userOutData = new StringBuilder("OUT "+n+":\n");
            int m = random.nextInt( 10);
            while(m < 20) {
                m++;
                User user = new User(random.nextInt(3));
                isEnqueued = prioQ.enqueueUserRequest(user);
                userInData.append(user.toString()).append("\n");
                if (!isEnqueued) {continue;}
            }
            JSONObject jsonMapBefore = prioQ.convMapToJSONObj(prioQ.getRequestCounterMap());
            while(prioQ.getPrioQ().size() > 10) {
                User userRequest = prioQ.dequeueUserRequest();
                userOutData.append(userRequest.toString()).append("\n");
            }
            JSONObject jsonMapAfter = prioQ.convMapToJSONObj(prioQ.getRequestCounterMap());
            prioQ.writeToFile(
                    userInData + "\n"
                    + jsonMapBefore.toString() + "\n"
                    + userOutData + "\n"
                    + jsonMapAfter.toString() + "\n"
            );
            n++;
//            Thread.sleep(1000);
        }

        System.out.println("Job is done! Bye!");
    }





}