namespace EventNotifier;

public class Subscription
{
    public Type eventType;
    public Filter filter;
    public Subscriber subscriber;
}
