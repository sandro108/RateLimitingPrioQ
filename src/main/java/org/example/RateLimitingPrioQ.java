package org.example;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.json.*;
import com.google.common.util.concurrent.RateLimiter;

public class RateLimitingPrioQ /*implements Comparator<User>*/ {

    /******************************************************************
     *
     * TODO 1: implement a strict upper limit for the number of requests from one user
     * TODO 1: send this user a 429 (?) "Too many requests" right away and drop further requests
     * => but what to do to recover the user from being banned??
     *
     * TODO 2: Check if starvation is possible (with the current implementation) for a user
     * whose requests have been downgraded in prio,
     * if his request are only followed by other user's requests with high priority?
     * Idea: Set a timer. After timer expires. iterate through PrioQ (or only the last 100 elements)
     * and if they have prio==20, upgrade them to prio=100. (not possible with current java.util.PrioQ...though!)
     * Implement your own modifiable minHeap? See:
     * https://drops.dagstuhl.de/storage/00lipics/lipics-vol046-opodis2015/LIPIcs.OPODIS.2015.15/LIPIcs.OPODIS.2015.15.pdf
     */

    private static final Logger logger = Logger.getLogger(RateLimitingPrioQ.class.getName());

    RateLimiter rateLimiter;
    private final Map<Integer, Integer> requestCounterMap; // Map<user.UID, RequestCounter>
    private final Map<Integer, Long> lastArrivalTimeMap; // Map<user.UID, lastArrivalTime>
    private final PriorityBlockingQueue <User> prioQ;
    private static int REQUEST_COUNT_LIMIT;
    private static final int HIGH_PRIO = 100;
    private static final int LOW_PRIO = 20;
    String FILE_OUT = "./log.txt";

    public RateLimitingPrioQ(int permitsPerTimeUnit, int requestCountLimit) {
        this.rateLimiter = RateLimiter.create(permitsPerTimeUnit);
        this.requestCounterMap = new ConcurrentHashMap<>();
        this.lastArrivalTimeMap = new ConcurrentHashMap<>();
        this.prioQ = new PriorityBlockingQueue<>(100/*, this*/);
        REQUEST_COUNT_LIMIT = requestCountLimit;
    }
   /* @Override
    public int compare(User user1, User user2) {
        if (user1.getPriority() == user2.getPriority()){
            return user1.getArrivalTime().compareTo(user2.getArrivalTime());
        } else if (user1.getPriority() < user2.getPriority()) {
            return 1;
        } else if (user1.getPriority() > user2.getPriority()) {
            return -1;
        } else {
                throw new IllegalArgumentException("PrioQ: Comparison impossible.");
            }

    }*/
    private boolean isArrTimeDiffAboveThresh(User user, Long userPrev, Long userCurrent) {
        long arrTimeDiff = userCurrent - userPrev;
        System.out.println("UID: " + user.getUID() + " cnt: " + user.getCnt() + " CAT: " + userCurrent + ", PAT: " + userPrev + ", ATD: " + arrTimeDiff);
//        logger.info("Arrival time difference: " + arrTimeDiff);
        return (arrTimeDiff > 800_000L);  //nanos! //TODO: magic number!
    }



    public boolean enQuserRequest(User user) {
        if (user != null) {
            Integer uid = user.getUID();
            long currArrTime = user.getArrivalTime();

            if (!this.requestCounterMap.containsKey(uid)) { // new user
                this.requestCounterMap.put(uid, 1);
                this.lastArrivalTimeMap.put(uid, currArrTime);
                this.prioQ.put(user);
            } else {
                Integer newCount = requestCounterMap.get(uid); // existing user
                Long prevArrTime = this.lastArrivalTimeMap.get(uid);
                this.lastArrivalTimeMap.replace(uid, currArrTime); // update last arrival time
                ++newCount;
                boolean isATDaboveThresh = isArrTimeDiffAboveThresh(user, prevArrTime, currArrTime);
                if (newCount > REQUEST_COUNT_LIMIT && !isATDaboveThresh) {

                    writeToFile("User: " + uid + ", request: " + user.getCnt() + " has been downgraded to " + user.getPriority() + ".\n");
                }
                this.prioQ.put(user); // add new request of known user or block 'till space to put is available again
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
    public User deQuserRequest() throws InterruptedException {
        User user = this.prioQ.take(); // retrieve user who's first in Q or block if Q's empty
        Integer uid = user.getUID();
        if (this.requestCounterMap.containsKey(uid)) { // if user is known, decrement request counter
            Integer newCount = requestCounterMap.get(uid);
            if ((newCount - 1) <= 0) { //
                this.requestCounterMap.remove(uid);
            } else {
                this.requestCounterMap.replace(uid, --newCount);
            }
        }
        return user;
    }
    public synchronized JSONObject convMapToJSONObj(Map<Integer,Integer> map) {
        return new JSONObject(map);
    }

    public synchronized void writeToFile(Object any) {
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(new File(FILE_OUT),true))) {
            pw.append(any.toString());
        }
         catch (IOException e) {
            e.printStackTrace();
             throw new RuntimeException(e);
         }
    }
    public synchronized Map<Integer, Integer> getRequestCounterMap() {
        return requestCounterMap;
    }

    public /*synchronized*/ Map<Integer, Long> getLastArrivalTimeMap() {
        return lastArrivalTimeMap;
    }

    public synchronized String getFILE_OUT() {
        return FILE_OUT;
    }

    public synchronized void setFILE_OUT(String FILE_OUT) {
        this.FILE_OUT = FILE_OUT;
    }

    public /*synchronized*/ static int getRequestCountLimit() {
        return REQUEST_COUNT_LIMIT;
    }
    public /*synchronized*/ static void setRequestCountLimit(int requestCountLimit) {
        REQUEST_COUNT_LIMIT = requestCountLimit;
    }
    public synchronized Queue<User> getPrioQ() {
        return prioQ;
    }
}
