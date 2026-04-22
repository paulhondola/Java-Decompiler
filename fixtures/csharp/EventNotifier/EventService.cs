namespace EventNotifier;

public class EventService
{
    private Type eventClass;
    protected List<Subscription> subscriptions;
    private EventService singleton;

    public static EventService Instance() => null;

    public void Publish() { }

    public void Subscribe() { }

    public void Unsubscribe() { }
}
