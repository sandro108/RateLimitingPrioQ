package org.example;

import com.google.common.util.concurrent.RateLimiter;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

public class RateLimitingPrioQv3 {

    private static final Logger logger = Logger.getLogger(RateLimitingPrioQv3.class.getName());

    RateLimiter rateLimiter;
    //TODO: (maybe add 4th map to differentiate between
    // request count in general (for statistical purposes rather) and request count
    // that gets reset after arrivalTimeThreshold was exceeded.
    private Map<Integer, Integer> requestCounterMap; // Map<user.UID, RequestCounter>
    private Map<Integer, Long> lastArrivalTimeMap; // Map<user.UID, lastArrivalTime>
    private Map<Integer, Integer> userPrioMap; // Map<user.UID, last granted priority>

    private BlockingQueue<User> fastQ;

    private BlockingQueue<User> slowQ;
    private static int REQUEST_COUNT_LIMIT;
    private static int PENALTY_LIMIT;
    private final static int HIGH_PRIO = 100;
    private final static int LOW_PRIO = 20;
    private final static int REJECTED = 0;

    /* after dequeueing swapQ times, swap from fastQ to slowQ once */
    private final static int swapQ = 5;

    /* after RESET_CNT dequeues, reset the dQcnt to avoid overflows*/
    private final static int RESET_CNT = 20;
    private int dQcnt = 0;
    String FILE_OUT = "./log." + LocalDateTime.now() + ".csv";


    //TODO: Remove everything related to RateLimiter class
    public RateLimitingPrioQv3(int permitsPerTimeUnit, int requestCountLimit) {
        this.rateLimiter = RateLimiter.create(permitsPerTimeUnit);
        this.requestCounterMap = new ConcurrentHashMap<>();
        this.lastArrivalTimeMap = new ConcurrentHashMap<>();
        this.userPrioMap = new ConcurrentHashMap<>();
        this.fastQ = new LinkedBlockingQueue<>();
        this.slowQ = new LinkedBlockingQueue<>();
        REQUEST_COUNT_LIMIT = requestCountLimit;
        PENALTY_LIMIT = REQUEST_COUNT_LIMIT + 10;

    }

    //TODO: remove unused argument: User user
    private boolean areReqComingInTooFast(User user, Long userPrev, Long userCurrent) {
        long arrTimeDiff = userCurrent - userPrev;
//        System.out.println("UID: " + user.getUID() + " cnt: " + user.getCnt() + " CAT: " + userCurrent + ", PAT: " + userPrev + ", ATD: " + arrTimeDiff);
//        logger.info("Arrival time difference: " + arrTimeDiff);
       /* if (arrTimeDiff > 800_000L) {  //nanos! //TODO: magic number!
            return false;
        }*/
        return arrTimeDiff < 10_000_000L; //nanos! //TODO: magic number!
    }

    /**
     * @throws InterruptedException
     * @return true if user request was enqueued , false otherwise
     */
    public synchronized boolean enQuserRequest(User user) throws InterruptedException {
        if (user != null) {
            Integer uid = user.getUID();
            long currArrTime = user.getArrivalTime();
            if (!this.requestCounterMap.containsKey(uid)) { // new user
                this.requestCounterMap.put(uid, 1);
                this.lastArrivalTimeMap.put(uid, currArrTime);
                this.userPrioMap.put(uid, HIGH_PRIO);
                this.fastQ.put(user);
                logger.info("Enqueued new user to fastQ w UID: " + user.getUID() + " cnt: " + user.getCnt());
            } else {
                Integer reqCount = requestCounterMap.get(uid); // existing user
                Long prevArrTime = this.lastArrivalTimeMap.get(uid);
                this.lastArrivalTimeMap.replace(uid, currArrTime); // update last arrival time
                ++reqCount;
                boolean comingInTooFast = areReqComingInTooFast(user, prevArrTime, currArrTime);
                int prio = userPrioMap.get(uid);

                if (prio == HIGH_PRIO && reqCount > REQUEST_COUNT_LIMIT && comingInTooFast) { //TODO: set back to != LOW_PRIO
                    this.userPrioMap.replace(uid, LOW_PRIO);
                    reqCount = 1;
                }
                if (prio == LOW_PRIO && /*reqCount > PENALTY_LIMIT &&*/ !comingInTooFast) { //TODO: set back to != HIGH_PRIO later
                    userPrioMap.replace(uid, HIGH_PRIO);
                }
                // TODO: How can a rejected user be redeemed??
                if (prio == LOW_PRIO && (reqCount > 2 * PENALTY_LIMIT) && comingInTooFast) { //TODO: set back to != HIGH_PRIO later
                    userPrioMap.replace(uid, REJECTED);
                }
                /* now start enqueue (or reject) according to priority */
                if (userPrioMap.get(uid) == REJECTED) {
                    user.setPriority("REJECTED!!!"); // TODO: only for dev, remove later (replace with HTTP-Response:<<Too many request>>)
                }
                else if (userPrioMap.get(uid) == HIGH_PRIO) {
                    this.fastQ.put(user);
                    user.setPriority("F"); // TODO: only for dev, remove later
                    logger.info("Enqueued user to fastQ - UID: " + user.getUID() + " cnt: " + user.getCnt());
                } else {
                    this.slowQ.put(user);
                    user.setPriority("S"); // TODO: only for dev, remove later
                    logger.info("Enqueued user to slowQ - UID: " + user.getUID() + " cnt: " + user.getCnt());
                }
                this.requestCounterMap.replace(uid, reqCount); // update request counter
            }
            return true;
        }
        return false;
    }

    /**
     * @throws InterruptedException
     * @return if not null: the dequeued user (request) , null otherwise
     */
    public User deQuserRequest() throws InterruptedException {
        User user = null;
//        dQcnt++;
        if (this.slowQ.isEmpty() && this.fastQ.isEmpty()) {
            //TODO: block here and wait on a conditional variable
        }
        else if ((!this.slowQ.isEmpty() && dQcnt % swapQ == 0) || (!this.slowQ.isEmpty() && dQcnt % swapQ != 0 && this.fastQ.isEmpty())) {
            user = this.slowQ.poll(); // retrieve or wait/block for user who's first in slowQ
            logger.info("dequeued userRequest on slowQ:" + user.getCnt());
            dQcnt++;
            if (dQcnt == RESET_CNT) {
                dQcnt = 0;
            }
        } else if ((!this.fastQ.isEmpty() && dQcnt % swapQ != 0) || (!this.fastQ.isEmpty() && dQcnt % swapQ == 0 && this.slowQ.isEmpty())) {
            user = this.fastQ.poll(); // retrieve or wait/block for user who's first in fastQ
            dQcnt++;
            logger.info("dequeued userRequest on fastQ:" + user.getCnt());
        }
        if (user == null) {
            return null;
        }
        /*Integer uid = user.getUID();
        if (this.requestCounterMap.containsKey(uid)) { // if user is known, decrement request counter
            Integer newCount = requestCounterMap.get(uid);
            if ((newCount - 1) <= 0) { //
                this.requestCounterMap.remove(uid);
            } else {
                this.requestCounterMap.replace(uid, --newCount);
            }
        }*/
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
    public Map<Integer, Integer> getUserPrioMap() {
        return userPrioMap;
    }

    public BlockingQueue<User> getFastQ() {
        return fastQ;
    }

    public BlockingQueue<User> getSlowQ() {
        return slowQ;
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

    public int getdQcnt() {
        return dQcnt;
    }
}
