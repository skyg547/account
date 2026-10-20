package com.ho.account.masterdata.core.application.service;

/** Verified ingress identity carried through synchronous transactional use cases. */
public final class MasterDataActorContext {
    private static final ThreadLocal<Actor> CURRENT = new ThreadLocal<>();

    private MasterDataActorContext() { }

    public static Scope enter(String serviceActor, String businessActor) {
        return enter(serviceActor, businessActor, false);
    }

    public static Scope enterApprovedChange(String serviceActor, String businessActor) {
        return enter(serviceActor, businessActor, true);
    }

    private static Scope enter(String serviceActor, String businessActor, boolean approvedChange) {
        Actor previous = CURRENT.get();
        CURRENT.set(new Actor(serviceActor, businessActor, approvedChange));
        return () -> {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        };
    }

    public static Actor current() { return CURRENT.get(); }

    public static String auditUser() {
        Actor actor = CURRENT.get();
        if (actor == null) return "SYSTEM";
        return actor.serviceActor() == null ? actor.businessActor()
                : actor.serviceActor() + ":" + actor.businessActor();
    }

    public record Actor(String serviceActor, String businessActor, boolean approvedChange) { }
    public interface Scope extends AutoCloseable { @Override void close(); }
}
