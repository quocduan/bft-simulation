abstract class Event implements Comparable<Event> {
  /** The time of the event, in seconds. */
  private final double time;

  /** The subject of the event. */
  private final Node subject;

  Event(double time, Node subject) {
    this.time = time;
    this.subject = subject;
  }

  double getTime() {
    return time;
  }

  Node getSubject() {
    return subject;
  }

  @Override public int compareTo(Event that) {
    double delta = this.time - that.time;
    if (delta == 0 && !this.equals(that)) {
      // We shouldn't return 0, since the messages aren't equal. identityHashCode gives us an
        // arbitrary but consistent ordering, although this isn't 100% safe since it assumes no
      // collisions (which are rare).
      return System.identityHashCode(this) - System.identityHashCode(that);
    } else {
      // returns the signum function of the argument; zero if the argument is zero, 1.0f if the argument is greater than zero, -1.0f if the argument is less than zero.
      return (int) Math.signum(delta); // 0, -1.0, 1.0
    }
  }
}

// tại sao đặt class khác giống hệt class cha: đơn giản là khác loại mà thôi.
// dùng để check isinstanceof chẳng hạn
class TimerEvent extends Event {
  TimerEvent(double time, Node subject) {
    super(time, subject);
  }
}

// có thêm phần message (ngoài time và subject)
class MessageEvent extends Event {
  private final Message message;

  MessageEvent(double time, Node subject, Message message) {
    super(time, subject);
    this.message = message;
  }

  Message getMessage() {
    return message;
  }
}
