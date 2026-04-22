namespace EventNotifier;

public class FaultEvent : ManagementEvent
{
    public const int CRITICAL = 1;
    public const int MODERATE = 2;
    public const int LOW = 3;
    public int severity;
    public string source;
}

public class StatusEvent : ManagementEvent
{
    public string status;
    public string source;
}
