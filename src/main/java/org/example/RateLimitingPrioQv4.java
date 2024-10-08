package org.example;

import com.google.common.util.concurrent.RateLimiter;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;


import static org.example.RLPQ_Config.*;

public class RateLimitingPrioQv4 {

    private static final Logger logger = Logger.getLogger(RateLimitingPrioQv4.class.getName());

    RateLimiter rateLimiter;
    private final Lock  lock = new ReentrantLock();
    private final Condition justEnqd = lock.newCondition();


    //TODO: (maybe add 4th map to differentiate between
    // request count in general (for statistical purposes rather) and request count
    // that gets reset after arrivalTimeThreshold was exceeded.

    private Map<Integer, Integer> requestCounterMap; // Map<user.UID, RequestCounter>
    private Map<Integer, Long> lastArrivalTimeMap; // Map<user.UID, lastArrivalTime>
    private Map<Integer, Integer> userPrioMap; // Map<user.UID, last granted priority>

    private BlockingQueue<User> fastQ;

    private BlockingQueue<User> slowQ;

    private int dQcnt = 0;


    //TODO: Remove everything related to RateLimiter class
    public RateLimitingPrioQv4(int permitsPerTimeUnit, int requestCountLimit) {
     //   this.rateLimiter = RateLimiter.create(permitsPerTimeUnit);
        this.requestCounterMap = new ConcurrentHashMap<>();
        this.lastArrivalTimeMap = new ConcurrentHashMap<>();
        this.userPrioMap = new ConcurrentHashMap<>();
        this.fastQ = new LinkedBlockingQueue<>();
        this.slowQ = new LinkedBlockingQueue<>();
   //     REQUEST_COUNT_LIMIT = requestCountLimit;

    }

    //TODO: remove unused argument: User user
    private boolean areReqComingInTooFast(User user, Long userPrev, Long userCurrent) {
        long arrTimeDiff = userCurrent - userPrev;
//        System.out.println("UID: " + user.getUID() + " cnt: " + user.getCnt() + " CAT: " + userCurrent + ", PAT: " + userPrev + ", ATD: " + arrTimeDiff);
//        logger.info("Arrival time difference: " + arrTimeDiff);
       /* if (arrTimeDiff > 800_000L) {  //nanos!
            return false;
        }*/
        return arrTimeDiff < ARRIVL_TIME_DIFF_THRESH; //nanos
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
                if (prio == LOW_PRIO && (reqCount > /*2 * */PENALTY_LIMIT) && comingInTooFast) { //TODO: set back to != HIGH_PRIO later
                    userPrioMap.replace(uid, REJECTED);
                }
                /* now start enqueue (or reject) according to priority */
               if (userPrioMap.get(uid) == REJECTED) {
                    user.setPriority("2"); // 2 stands for REJECTED TODO: only for dev, remove later (replace with HTTP-Response:<<Too many request>>)
                }
                else if (userPrioMap.get(uid) == HIGH_PRIO) {
                    this.fastQ.put(user);
                    user.setPriority("0"); // 0 stands for fastQ TODO: only for dev, remove later
                    logger.info("Enqueued user to fastQ - UID: " + user.getUID() + " cnt: " + user.getCnt());
                }
                else {
                    this.slowQ.put(user);
                    user.setPriority("1"); // 1 stands for slowQ TODO: only for dev, remove later
                    logger.info("Enqueued user to slowQ - UID: " + user.getUID() + " cnt: " + user.getCnt());
                }
                this.requestCounterMap.replace(uid, reqCount); // update request counter
                lock.lock();
                justEnqd.signal();
                lock.unlock();

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
        if (this.slowQ.isEmpty() && this.fastQ.isEmpty()) {
            //TODO: block here and wait on a conditional variable
            lock.lock();
            justEnqd.await();
            lock.unlock();
        }
        if ((!this.slowQ.isEmpty() && dQcnt % swapQ == 0) || (!this.slowQ.isEmpty() && dQcnt % swapQ != 0 && this.fastQ.isEmpty())) {
            user = this.slowQ.poll(); // retrieve or wait/block for user who's first in slowQ
            logger.info("dequeued userRequest on slowQ:" + user.getCnt());
            dQcnt++;
            if (dQcnt == RESET_CNT) {
                dQcnt = 1;
            }
        }
        else if ((!this.fastQ.isEmpty() && dQcnt % swapQ != 0) || (!this.fastQ.isEmpty() && dQcnt % swapQ == 0 && this.slowQ.isEmpty())) {
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

    public static int getRequestCountLimit() {
        return REQUEST_COUNT_LIMIT;
    }

    public int getdQcnt() {
        return dQcnt;
    }
}
