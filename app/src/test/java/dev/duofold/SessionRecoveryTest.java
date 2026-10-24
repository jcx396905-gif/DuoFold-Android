package dev.duofold;

import org.junit.Test;
import static org.junit.Assert.*;

public class SessionRecoveryTest {
    @Test public void screenOffUnlockAndDelayedBridge() {
        SessionRecovery state=new SessionRecovery(false);
        state.userStart(false);
        state.suspend();
        assertFalse(state.canResume(false,false,true,true));
        assertFalse(state.canResume(false,true,true,true));
        assertFalse(state.canResume(false,true,false,false));
        // A reconnect after unlock must still be able to consume the pending request.
        assertTrue(state.canResume(false,true,false,true));
        state.beginResume();
        assertFalse(state.canResume(true,true,false,true));
        state.recovered();
        assertFalse(state.pending());
        assertTrue(state.requested());
    }

    @Test public void repeatedOffAndInterruptDoNotEraseRequest() {
        SessionRecovery state=new SessionRecovery(false);
        state.userStart(false);
        state.suspend();state.suspend();state.suspend();
        assertTrue(state.canResume(false,true,false,true));
    }

    @Test public void stopWhileWakeIsQueuedCancelsIt() {
        SessionRecovery state=new SessionRecovery(true);
        state.stop();
        assertFalse(state.canResume(false,true,false,true));
        state.suspend();
        assertFalse(state.requested());
        assertFalse(state.pending());
    }

    @Test public void trialNeverResumesOrPersists() {
        SessionRecovery state=new SessionRecovery(true);
        state.userStart(true);state.suspend();
        assertFalse(state.requested());
        assertFalse(state.canResume(false,true,false,true));
    }

    @Test public void rebuildRestoresOnlyExplicitGlobalIntent() {
        SessionRecovery original=new SessionRecovery(false);
        original.userStart(false);
        SessionRecovery rebound=new SessionRecovery(original.requested());
        assertTrue(rebound.canResume(false,true,false,true));
        original.stop();
        assertFalse(new SessionRecovery(original.requested()).pending());
    }

    @Test public void startupRetriesAreBoundedAndFirstFrameCompletesRecovery() {
        SessionRecovery state=new SessionRecovery(true);
        for(int i=0;i<3;i++) {
            assertTrue(state.canResume(false,true,false,true));
            state.beginResume();state.suspend();
        }
        assertFalse(state.canRetry());
        assertFalse(state.canResume(false,true,false,true));
        state.stop();
        state.userStart(false);state.suspend();state.beginResume();state.recovered();
        assertEquals(0,state.attempts());
        state.suspend();
        assertTrue(state.canResume(false,true,false,true));
    }

    @Test public void normalCaptureFailureDoesNotAcquireRetryPermission() {
        SessionRecovery state=new SessionRecovery(false);
        state.userStart(false);state.recovered();
        assertFalse(state.canRetry());
    }
}
