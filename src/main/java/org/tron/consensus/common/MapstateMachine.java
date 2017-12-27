package org.tron.consensus.common;

import io.atomix.catalyst.concurrent.Scheduled;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.Snapshottable;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.session.ServerSession;
import io.atomix.copycat.server.session.SessionListener;
import io.atomix.copycat.server.storage.snapshot.SnapshotReader;
import io.atomix.copycat.server.storage.snapshot.SnapshotWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class MapstateMachine extends StateMachine implements Snapshottable,SessionListener {
    private Map<Object, Object> map = new HashMap<>();
    private Set<ServerSession> sessions = new HashSet<>();
    private Set<ServerSession> listeners = new HashSet<>();

    public void listen(Commit<org.tron.consensus.common.Listen> commit) {
        listeners.add(commit.session());
        commit.close();
    }

    public Object put(Commit<org.tron.consensus.common.PutCommand> commit) {
        try {
            map.put(commit.operation().key(), commit.operation().value());
        } finally {
            commit.session();
            commit.close();
        }
        return null;
    }

    public Object get(Commit<org.tron.consensus.common.GetQuery> commit) {
        try {
            return map.get(commit.operation().key());
        } finally {
            commit.close();
        }
    }

    @Override
    public void snapshot(SnapshotWriter writer) {
        writer.writeObject(map);
    }

    @Override
    public void install(SnapshotReader reader) {
        map = reader.readObject();
    }

    /* Listening for Session State Changes */
    //  called when a new session is registered by a client
    @Override
    public void register(ServerSession session) {
        sessions.add(session);
    }

    // called when a session is unregistered by a client
    @Override
    public void unregister(ServerSession session) {

    }

    // called when a session is expired by the cluster
    @Override
    public void expire(ServerSession session) {

    }

    //  called after a session is either unregistered by a client or expired by the leader
    @Override
    public void close(ServerSession session) {
        sessions.remove(session);
    }
}