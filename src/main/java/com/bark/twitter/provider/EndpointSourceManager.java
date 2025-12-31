package com.bark.twitter.provider;

import com.bark.twitter.infra.PushoverClient;
import com.bark.twitter.provider.SourceHealthMonitor.Endpoint;
import com.bark.twitter.provider.SourceHealthMonitor.Source;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages source selection per endpoint with automatic fallback and recovery.
 *
 * States per endpoint:
 * - NORMAL: Using primary source only
 * - FALLBACK: Using fallback source, shadowing requests to primary for health monitoring
 * - RECOVERING: Primary is healthy for some time, preparing to switch back
 */
@Component
public class EndpointSourceManager {

    private static final long RECOVERY_PERIOD_MS = 60_000; // 1 minute healthy before switching back

    private final SourceHealthMonitor healthMonitor;
    private final PushoverClient pushoverClient;
    private final Map<Endpoint, EndpointState> endpointStates = new ConcurrentHashMap<>();

    public EndpointSourceManager(SourceHealthMonitor healthMonitor, PushoverClient pushoverClient) {
        this.healthMonitor = healthMonitor;
        this.pushoverClient = pushoverClient;
    }

    public enum State {
        NORMAL,     // Using primary source only
        FALLBACK,   // Using fallback source, shadowing to primary
        RECOVERING  // Fallback active, but primary is healthy - waiting to switch back
    }

    private static class EndpointState {
        final AtomicReference<Source> primarySource;
        final AtomicReference<State> state = new AtomicReference<>(State.NORMAL);
        volatile long recoveryStartTime = 0;

        EndpointState(Source primary) {
            this.primarySource = new AtomicReference<>(primary);
        }
    }

    /**
     * Configures the primary source for an endpoint.
     */
    public void configurePrimarySource(Endpoint endpoint, Source source) {
        endpointStates.put(endpoint, new EndpointState(source));
        System.out.println("[" + System.currentTimeMillis() + "][" + endpoint + "][SOURCE_MANAGER] Configured primary source: " + source);
    }

    /**
     * Gets the current active source for an endpoint (considering fallback state).
     */
    public Source getActiveSource(Endpoint endpoint) {
        EndpointState state = getOrCreateState(endpoint);
        if (state.state.get() == State.NORMAL) {
            return state.primarySource.get();
        }
        return getFallbackSource(state.primarySource.get());
    }

    /**
     * Gets the primary (configured) source for an endpoint.
     */
    public Source getPrimarySource(Endpoint endpoint) {
        return getOrCreateState(endpoint).primarySource.get();
    }

    /**
     * Returns true if we should shadow requests to the primary source (for health monitoring).
     */
    public boolean shouldShadow(Endpoint endpoint) {
        EndpointState state = getOrCreateState(endpoint);
        State currentState = state.state.get();
        return currentState == State.FALLBACK || currentState == State.RECOVERING;
    }

    /**
     * Gets the current state for an endpoint.
     */
    public State getState(Endpoint endpoint) {
        return getOrCreateState(endpoint).state.get();
    }

    /**
     * Checks health and manages state transitions.
     * Called periodically and can also be called after recording metrics.
     */
    @Scheduled(fixedRate = 5000) // Check every 5 seconds
    public void checkHealthAndTransition() {
        for (Endpoint endpoint : Endpoint.values()) {
            checkEndpointHealth(endpoint);
        }
    }

    private void checkEndpointHealth(Endpoint endpoint) {
        EndpointState endpointState = endpointStates.get(endpoint);
        if (endpointState == null) {
            return;
        }

        Source primary = endpointState.primarySource.get();
        State currentState = endpointState.state.get();

        switch (currentState) {
            case NORMAL -> {
                // Check if primary has issues -> transition to FALLBACK
                if (healthMonitor.hasIssues(primary, endpoint)) {
                    if (endpointState.state.compareAndSet(State.NORMAL, State.FALLBACK)) {
                        String message = "[" + endpoint + "] Primary " + primary + " has issues. " +
                                healthMonitor.getHealthSummary(primary, endpoint);
                        System.out.println("[" + System.currentTimeMillis() + "][" + endpoint + "][SOURCE_MANAGER] FALLBACK triggered! " + message);

                        // Send high-priority Pushover notification
                        pushoverClient.sendHighPriority(
                                "Twitter Relay FALLBACK",
                                message
                        );
                    }
                }
            }
            case FALLBACK -> {
                // Check if primary is healthy -> transition to RECOVERING
                if (healthMonitor.isHealthy(primary, endpoint)) {
                    if (endpointState.state.compareAndSet(State.FALLBACK, State.RECOVERING)) {
                        endpointState.recoveryStartTime = System.currentTimeMillis();
                        System.out.println("[" + System.currentTimeMillis() + "][" + endpoint + "][SOURCE_MANAGER] RECOVERING started. " +
                                "Primary " + primary + " is healthy. " +
                                healthMonitor.getHealthSummary(primary, endpoint));
                    }
                }
            }
            case RECOVERING -> {
                // Check if primary is still healthy
                if (!healthMonitor.isHealthy(primary, endpoint)) {
                    // Primary degraded again, go back to FALLBACK
                    if (endpointState.state.compareAndSet(State.RECOVERING, State.FALLBACK)) {
                        endpointState.recoveryStartTime = 0;
                        System.out.println("[" + System.currentTimeMillis() + "][" + endpoint + "][SOURCE_MANAGER] Recovery aborted! " +
                                "Primary " + primary + " degraded again. " +
                                healthMonitor.getHealthSummary(primary, endpoint));
                    }
                } else if (System.currentTimeMillis() - endpointState.recoveryStartTime >= RECOVERY_PERIOD_MS) {
                    // Primary healthy for full recovery period -> switch back to NORMAL
                    if (endpointState.state.compareAndSet(State.RECOVERING, State.NORMAL)) {
                        endpointState.recoveryStartTime = 0;
                        System.out.println("[" + System.currentTimeMillis() + "][" + endpoint + "][SOURCE_MANAGER] Switched back to NORMAL. " +
                                "Primary " + primary + " healthy for 1+ minute. " +
                                healthMonitor.getHealthSummary(primary, endpoint));
                    }
                }
            }
        }
    }

    /**
     * Force a health check for a specific endpoint (call after recording metrics).
     */
    public void triggerHealthCheck(Endpoint endpoint) {
        checkEndpointHealth(endpoint);
    }

    private EndpointState getOrCreateState(Endpoint endpoint) {
        return endpointStates.computeIfAbsent(endpoint, e -> new EndpointState(Source.SYNOPTIC));
    }

    private Source getFallbackSource(Source primary) {
        return primary == Source.SYNOPTIC ? Source.TWITTERAPI : Source.SYNOPTIC;
    }

    /**
     * Returns status summary for all endpoints.
     */
    public String getStatusSummary() {
        StringBuilder sb = new StringBuilder();
        for (Endpoint endpoint : Endpoint.values()) {
            EndpointState state = endpointStates.get(endpoint);
            if (state != null) {
                sb.append(String.format("[%s] state=%s, primary=%s, active=%s%n",
                        endpoint, state.state.get(), state.primarySource.get(), getActiveSource(endpoint)));
            }
        }
        return sb.toString();
    }
}
