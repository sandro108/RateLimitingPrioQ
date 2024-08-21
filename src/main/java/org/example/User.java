package org.example;

import java.time.LocalTime;
import java.util.Objects;

public class User {
    //this UID must most probably be constituted by the user's IP address
    // and the port number of the socket connection. So in principle the socket address of the http connection.
    private int UID;
    private Long arrivalTime;
    private int cnt;
    private String priority;

    public User(int UID, int cnt) {
        this.arrivalTime = LocalTime.now().toNanoOfDay(); // this is used as default metric for PrioQ
        this.UID = UID;  // only needed for mapping user to request counter!
        this.priority = "F"; // TODO: magic number
        this.cnt = cnt;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return getUID() == user.getUID() && Objects.equals(getArrivalTime(), user.getArrivalTime());
    }
    @Override
    public int hashCode() {
        return Objects.hash(getUID(), getArrivalTime());
    }

    //TODO: make it csv compatible!!
    @Override
    public synchronized String toString() {
        return "User{" +
                "UID=" + UID +
                ", arrTime=" + arrivalTime +
                ", prioQ=" + priority +
                ", cnt= " + cnt +
                '}';
    }

    public synchronized int getUID() {
        return UID;
    }

    public synchronized String getPriority() {
        return priority;
    }

    public synchronized void setPriority(String priority) {
        this.priority = priority;
    }

    public synchronized Long getArrivalTime() {
        return arrivalTime;
    }

    public synchronized int getCnt() {
        return cnt;
    }


}
