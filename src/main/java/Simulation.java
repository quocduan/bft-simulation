import java.util.TreeSet;

class Simulation {
  public static int countProposals = 0;
  private final Network network;
  private final TreeSet<Event> eventsByTime = new TreeSet<>();
  private double id=-1;

  Simulation(Network network) {
    this.network = network;
  }

  Simulation(Network network, double id) {
    this.network = network;
    this.id=id;
  }

  private double getId(){
    return this.id;
  }

// time nay la gi?
  // khi khi onStart và beginProposal thì time = 0
  void broadcast(Node source, Message message, double time) {
    for (Node destination : network.getNodes()) { // all nodes
      double latency = network.getLatency(source, destination); // <--- gia lap
      double arrivalTime = time + latency; // thoi diem
      eventsByTime.add(new MessageEvent(arrivalTime, destination, message));
    }
  }

  Network getNetwork() {
    return network;
  }

  Node getLeader(int index) {
    return network.getLeader(index);
  }

  void scheduleEvent(Event event) {
    eventsByTime.add(event);
  }

  /**
   * Run until all events have been processed, including any newly added events which may be added
   * while running.
   *
   * @param timeLimit the maximum amount of time before the simulation halts
   * @return whether the simulation completed within the time limit
   */
  boolean run(double timeLimit) { // 4s
    System.out.println("Start Simulation: "+ this.getId());
    int i = 0;
    for (Node node : network.getNodes()) {
      i++;
//      if (i>3) {
//        break;
//      }
//      node.onStart(this);
//      Node n = network.getNodes().get(0);
      System.out.println("onStart Node: "+ node.getNodeIndex());
      node.onStart(this);
    }


    // mỗi loại có một simulator riêng --> quản lý events riêng
    while (!eventsByTime.isEmpty()) {
      Event event = eventsByTime.pollFirst();
      if (event.getTime() > timeLimit) {
        System.out.println("WARNING: Simulation timed out");
        return false;
      }

      Node subject = event.getSubject();
      if (event instanceof TimerEvent) { //
        subject.onTimerEvent((TimerEvent) event, this);
      } else if (event instanceof MessageEvent) { ///
        subject.onMessageEvent((MessageEvent) event, this);
      } else {
        throw new AssertionError("Unexpected event: " + event);
      }
    }
    System.out.println("End Simulation: " + this.getId());

    return true;
  }
}