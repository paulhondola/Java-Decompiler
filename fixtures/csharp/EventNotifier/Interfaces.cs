namespace EventNotifier;

public interface Event { }

public interface ManagementEvent : Event { }

public interface Filter
{
    void Apply();
}

public interface Subscriber
{
    void Inform();
}
