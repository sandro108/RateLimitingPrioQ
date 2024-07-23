package org.example;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.util.*;
import java.util.logging.Logger;

import org.json.*;
import com.google.common.util.concurrent.RateLimiter;

public class RateLimitingPrioQ implements Comparator<User> {

    private static final Logger logger = Logger.getLogger(RateLimitingPrioQ.class.getName());

    RateLimiter rateLimiter;
    private Map<Integer, Integer> requestCounterMap; // Map<user.UID, RequestCounter>
    private Map<Integer, Long> lastArrivalTimeMap; // Map<user.UID, lastArrivalTime>
    private Queue<User> prioQ;
    private static int REQUEST_COUNT_LIMIT;
    private final static int HIGH_PRIO = 100;
    private final static int LOW_PRIO = 20;
    String FILE_OUT = "./log.txt";

    public RateLimitingPrioQ(int permitsPerTimeUnit, int requestCountLimit) {
        this.rateLimiter = RateLimiter.create(permitsPerTimeUnit);
        this.requestCounterMap = new HashMap<>();
        this.lastArrivalTimeMap = new HashMap<>();
        this.prioQ = new PriorityQueue<>(this);
        REQUEST_COUNT_LIMIT = requestCountLimit;

    }


    @Override
    public int compare(User user1, User user2) { //TODO: revisit and complete the logic
        if (user1.getPriority() == user2.getPriority()){
            return user1.getArrivalTime().compareTo(user2.getArrivalTime());
        } else if (user1.getPriority() < user2.getPriority()) {
            return 1;
        } else if (user1.getPriority() > user2.getPriority()) {
            return -1;
        } else {
                throw new IllegalArgumentException("PrioQ: Comparison impossible.");
            }

    }
    private boolean isArrTimeDiffAboveThresh(User user, Long userPrev, Long userCurrent) {
        long arrTimeDiff = userCurrent - userPrev;  //TODO: maybe use unix timestamp instead
        System.out.println("UID: " + user.getUID() + " cnt: " + user.getCnt() + " CAT: " + userCurrent + ", PAT: " + userPrev + ", ATD: " + arrTimeDiff);
//        logger.info("Arrival time difference: " + arrTimeDiff);
        if (arrTimeDiff > 500_000_000L) { //TODO: millis or nanos?? Nanos now!
            return true;
        }
        return false;
    }



    // find a way how to upgrade the priority of a user

    public synchronized boolean enQuserRequest(User user) {
        if (user != null) {
            Integer uid = user.getUID();
            long currArrTime = user.getArrivalTime();
            if (!this.requestCounterMap.containsKey(uid)) { // new user
                this.requestCounterMap.put(uid, 1);
                this.lastArrivalTimeMap.put(uid, currArrTime);
                this.prioQ.add(user);
            } else {
                Integer newCount = requestCounterMap.get(uid); // existing user
                Long prevArrTime = this.lastArrivalTimeMap.get(uid);
                this.lastArrivalTimeMap.replace(uid, currArrTime); // update last arrival time
                ++newCount;
                if (newCount > REQUEST_COUNT_LIMIT && user.getPriority() != LOW_PRIO) {
                    user.setPriority(LOW_PRIO);
                }
                if (isArrTimeDiffAboveThresh(user, prevArrTime, currArrTime) && user.getPriority() != HIGH_PRIO) {
                    writeToFile("User: " + uid + " has been upgraded to HIGH_PRIO.\n");
                    user.setPriority(HIGH_PRIO);
                    newCount = 1;
                }
                if (newCount <= REQUEST_COUNT_LIMIT && user.getPriority() != HIGH_PRIO) {
                    user.setPriority(HIGH_PRIO);
                }
                this.prioQ.add(user); // add new request of known user to prioQ
                this.requestCounterMap.replace(uid, newCount); // update request counter
            }
            return true;
        }
        return false;
    }

    /**
     *
     * @return if not null: the dequeued user (request) , null otherwise
     */
    public User deQuserRequest() {
        User user = this.prioQ.poll(); // retrieve user who's first in Q
        if (user == null) {
            return null;
        }
        Integer uid = user.getUID();
        if (this.requestCounterMap.containsKey(uid)) { // if user is known, decrement request counter
            Integer newCount = requestCounterMap.get(uid);
            this.requestCounterMap.replace(uid, --newCount);
        }
        return user;
    }

    public void applyRateLimiting(User user) {
        if (user != null) {
            if (user.getPriority() == HIGH_PRIO) {
                rateLimiter.acquire();
            }
        }
    }

    public JSONObject convMapToJSONObj(Map<Integer,Integer> map) {
        return new JSONObject(map);
    }

    public void writeJSON2File(JSONObject jsonObject) {
//        String[] jsonString = new StringBuilder(jsonObject);
        try (PrintWriter pw = new PrintWriter(new File(FILE_OUT))) {
            pw.append(jsonObject.toString()).append('\n');
//            pw.close();
        }
         catch (IOException e) {
            e.printStackTrace();
             throw new RuntimeException(e);
         }
    }
    public void writeToFile(Object any) {
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(new File(FILE_OUT),true))) {
            pw.append(any.toString());//.append('\n');
//            pw.close();
        }
         catch (IOException e) {
            e.printStackTrace();
             throw new RuntimeException(e);
         }
    }

    public Map<Integer, Integer> getRequestCounterMap() {
        return requestCounterMap;
    }

    public Map<Integer, Long> getLastArrivalTimeMap() {
        return lastArrivalTimeMap;
    }

    public String getFILE_OUT() {
        return FILE_OUT;
    }

    public void setFILE_OUT(String FILE_OUT) {
        this.FILE_OUT = FILE_OUT;
    }

    public static int getRequestCountLimit() {
        return REQUEST_COUNT_LIMIT;
    }

    public static void setRequestCountLimit(int requestCountLimit) {
        REQUEST_COUNT_LIMIT = requestCountLimit;
    }

    public synchronized Queue<User> getPrioQ() {
        return prioQ;
    }
}
