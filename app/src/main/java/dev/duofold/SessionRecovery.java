package dev.duofold;

/** User intent survives suspension; only a successful frame consumes a pending resume. */
final class SessionRecovery {
    private static final int MAX_ATTEMPTS=3;
    private boolean requested,pending;
    private int attempts;

    SessionRecovery(boolean savedRequest) {requested=savedRequest;pending=savedRequest;}
    boolean requested() {return requested;}
    boolean pending() {return pending;}
    int attempts() {return attempts;}
    void userStart(boolean trial) {requested=!trial;pending=false;attempts=0;}
    void suspend() {pending=requested;}
    boolean canResume(boolean active,boolean interactive,boolean locked,boolean bridgeReady) {
        return pending && !active && interactive && !locked && bridgeReady && attempts<MAX_ATTEMPTS;
    }
    void beginResume() {attempts++;}
    void recovered() {pending=false;attempts=0;}
    boolean canRetry() {return pending && attempts<MAX_ATTEMPTS;}
    void stop() {requested=false;pending=false;attempts=0;}
}
