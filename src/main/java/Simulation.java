import java.util.TreeSet;

class Simulation {
  public static int countProposals = 0;
  public static int maxEventsSize = 0;
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
      System.out.println("TIME:broadcast: time: "+ time + ", latency: "+ latency + ",arrivalTime: " + arrivalTime + ", from cycle: "+ message.getCycle() );
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
      i++; // 1

      // node 0 (2), cycle 0
      // node 1 (3), cycle 0 --> failed node
      // node 2 (1), cycle 0  ---> khong phai leader --> vao message queue cho vote
      // node 3 (0), cycle 0  ---> khong phai leader --> vao message queue cho vote
//      if (i>3) {
//        break;
//      }
//      node.onStart(this);
//      Node n = network.getNodes().get(0);
      System.out.println("onStart Node: "+ node.getNodeIndex());
      node.onStart(this);
    }

    // 1 broadcast msg add vao pool == dang lan truyen tren mang // proposal message --> luu trữ, prevotemessage --> prevote
    // 1 broadcast msg lay ra khoi pool == message do da den noi ( node đích lưu lại block đó vào bộ nhớ của nó)
    // event msg: khi duoc add vao pool == queue

    // proposal ? cycle?
//----------------------------------------------------------
    // mỗi loại có một simulator riêng --> quản lý events riêng
    while (!eventsByTime.isEmpty()) { // 65.000 --> 1 message
      System.out.println("*** eventsByTime size: "+ eventsByTime.size());
      if(maxEventsSize<eventsByTime.size()){
        maxEventsSize=eventsByTime.size();
      }
      // message có time nhỏ nhất
       Event event = eventsByTime.pollFirst();// time --> event.getTime()

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