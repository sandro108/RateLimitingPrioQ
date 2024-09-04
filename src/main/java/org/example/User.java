package org.example;

import java.time.LocalTime;
import java.util.Objects;

public class User {

    //TODO:this UID must most probably be constituted by the user's IP address
    // and the port number of the socket. So in principle the socket address of the tcp connection.
    private int UID;
    private Long arrivalTime;
    private int cnt;
    private String priority;  /* this is a field reuse, should be called 'qType' or something*/
    private Long dQTime;

    /*
    from System.nanoTime() api documentation:
    "@returns the current value of the running Java Virtual Machine's high-resolution time source, in nanoseconds"
    but beware:
    "The values returned by this method become meaningful only when the difference between two such values,
    obtained within the same instance of a Java virtual machine, is computed."
    */
    public User(int UID, int cnt) {
        this.arrivalTime = System.nanoTime();//LocalTime.now().toNanoOfDay();
        this.dQTime = 0L;//System.nanoTime();
        this.UID = UID;  // only needed for mapping user to request counter!
        this.priority = "0"; // 0 stands for fastQ TODO: magic number (only for development)
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
        return UID + "," + arrivalTime + "," + dQTime + "," + priority + "," + cnt;
    }
/*
    @Override
    public synchronized String toString() {
        return "User{" +
                "UID=" + UID +
                ", arrTime=" + arrivalTime +
                ", arrTime=" + dQTime +
                ", prioQ=" + priority +
                ", cnt= " + cnt +
                '}';
    }
*/

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

    public Long getDQTime() {
        return dQTime;
    }

    public void setdQTime(Long dQTime) {
        this.dQTime = dQTime;
    }
}
