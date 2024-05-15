package org.example;

import java.util.Random;
import org.json.*;

public class Main {



    public static void main(String[] args) throws InterruptedException {

        RateLimitingPrioQ prioQ = new RateLimitingPrioQ(1, 5);
        Random random = new Random(42L);
        boolean isEnqueued = false;
        int n = 0;
        int cnt = 1;

        while(n < 10) {
            StringBuilder userInData = new StringBuilder("IN "+n+":\n");
            StringBuilder userOutData = new StringBuilder("OUT "+n+":\n");
            int m = random.nextInt( 10);
            while(m < 15) {
                m++;
                User user = new User(random.nextInt(3), cnt);
                isEnqueued = prioQ.enQuserRequest(user);
                cnt++;
                userInData.append(user.toString()).append("\n");
                if (!isEnqueued) {continue;}
            }
            JSONObject jsonMapBeforeDeQ = prioQ.convMapToJSONObj(prioQ.getRequestCounterMap());
            while(prioQ.getPrioQ().size() > 10) {
                User userRequest = prioQ.deQuserRequest();
                userOutData.append(userRequest.toString()).append("\n");
            }
            JSONObject jsonMapAfterDeQ = prioQ.convMapToJSONObj(prioQ.getRequestCounterMap());
            prioQ.writeToFile(
                    userInData + "\n"
                    + jsonMapBeforeDeQ.toString() + "\n"
                    + userOutData + "\n"
                    + jsonMapAfterDeQ.toString() + "\n"
            );
            n++;
//            Thread.sleep(1000);
        }

        System.out.println("Job is done! Bye!");
    }





}