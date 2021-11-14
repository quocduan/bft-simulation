import java.util.*;

class CorrectTendermintNode extends Node {
  public static int maxCycle=0;
  public static int countBeginProposal=0;
  public static List<CorrectTendermintNode> listProposedNode =  new ArrayList<>();

  private int cycle = 0; // tổng cộng có 4 cycles
  private Map<Integer, CycleState> cycleStates = new HashMap<>();
  private ProtocolState protocolState;
  private double timeout;
  private double nextTimer;

  CorrectTendermintNode(EarthPosition position, double initialTimeout, int nodeIndex) {
    super(position, nodeIndex);
    this.timeout = initialTimeout;

  }



  @Override public void onStart(Simulation simulation) {
    beginProposal(simulation, 0); // Proposal: any, message, block, ...  need to be voted
  }

  @Override public void onTimerEvent(TimerEvent timerEvent, Simulation simulation) {
    if (hasTerminated()) {
      return;
    }

    double time = timerEvent.getTime(); // time + timeout = 0 + initialTimeout
    if (time != nextTimer) {
      // It's a stale timer; we must have made the relevant state transition based on observed
      // messages rather than a timer. Ignore it.
      return;
    }

    switch (protocolState) {
      case PROPOSAL:
        beginPreVote(simulation, time);
        break;
      case PRE_VOTE:
        beginPreCommit(simulation, time);
        break;
      case PRE_COMMIT:
        ++cycle;
        // Exponential backoff.
        timeout *= 2;
        beginProposal(simulation, time);
        break;
      default:
        throw new AssertionError("Unexpected protocol state");
    }
  }

  @Override public void onMessageEvent(MessageEvent messageEvent, Simulation simulation) {
    System.out.println("----------------------------- ");
    System.out.println("onMessageEvent on node: " + this.getNodeIndex() + ", from node: "+messageEvent.getMessage().getNodeIndex());
    System.out.println("cycle: " + this.cycle);

    if (hasTerminated()) {
      System.out.println("Node has been terminated ");

      return;
    }

    Message message = messageEvent.getMessage();
    double time = messageEvent.getTime();
    cycleStates.putIfAbsent(message.getCycle(), new CycleState());
    CycleState cycleState = cycleStates.get(message.getCycle());
    System.out.println("Message type: "+ message.getClass().getSimpleName());

    if (message instanceof ProposalMessage) {
      cycleState.proposals.add(message.getProposal());
    } else if (message instanceof PreVoteMessage) {
      cycleState.preVoteCounts.merge(message.getProposal(), 1, Integer::sum);
    } else if (message instanceof PreCommitMessage) {
      cycleState.preCommitCounts.merge(message.getProposal(), 1, Integer::sum);
      Set<Proposal> committedProposals = cycleState.getCommittedProposals(simulation);
      if (!committedProposals.isEmpty()) {
        Proposal committedProposal = committedProposals.iterator().next();
        // 1 node sẽ bị terminated khi nhận đủ 1 polka committedProposal
        // nó sẽ không run bất kỳ messages event hay timerevent nào nữa
        if (committedProposal != null) {
          System.out.println("committedProposal: Terminate node "+this.getNodeIndex());
          terminate(committedProposal, time);
        }
      }
    } else {
      throw new AssertionError("Unexpected message: " + message);
    }
    System.out.println("----------------------------- ");

  }

    private void beginProposal(Simulation simulation, double time) { // time lien quan den so 4 // thang hien tai la thang nao?
    countBeginProposal++;
      System.out.println("----------------------------- " );
    System.out.println("beginProposal: " + this.getNodeIndex());
    System.out.println("cycle: " + this.cycle);

    protocolState = ProtocolState.PROPOSAL;
    if(cycle>maxCycle){
      maxCycle=cycle;
    }
    if (equals(simulation.getLeader(cycle))) {
      System.out.println("is leader & broadcast: " + this.getNodeIndex());

      if(listProposedNode.size()>=4){
        System.out.println("listProposedNode.size()>=4");
      }
      listProposedNode.add(this);
      Proposal proposal = new Proposal();
      Message message = new ProposalMessage(cycle, proposal, this.getNodeIndex()); // pre-prepare message + block
      Simulation.countProposals++;
      simulation.broadcast(this, message, time); //--> add message event
    }
    System.out.println("resetTimeout " + this.getNodeIndex());

    resetTimeout(simulation, time); // --> TimerEvent
    System.out.println("----------------------------- " );

  }

  private void beginPreVote(Simulation simulation, double time) {
    System.out.println("----------------------------- " );
    System.out.println("beginPreVote: " + this.getNodeIndex());
    System.out.println("cycle: " + this.cycle);

    protocolState = ProtocolState.PRE_VOTE;
    Proposal p = getProposalToPreVote(simulation);
    System.out.println("getProposalToPreVote: "+ p);
    Message message = new PreVoteMessage(cycle, p, this.getNodeIndex());
    simulation.broadcast(this, message, time);
    resetTimeout(simulation, time);
    System.out.println("broadcast & setTimeout" );

    System.out.println("----------------------------- " );

  }

  private Proposal getProposalToPreVote(Simulation simulation) {
    // Find the latest proposal which had 2/3 pre-votes, if any. If there is one, then either that's
    // the proposal we're locked on, or we were locked on an older proposal, in which case that
    // proposal unlocks us. Either way, we're able to vote for that proposal.

    // giả sử có 2 prevoteProposals thì cái còn lại sẽ được vote khi nào?
    // chỗ này là ưu tiên vote cho các proposals thuộc cycles trước.
    for (int prevCycle = cycle - 1; prevCycle >= 0; --prevCycle) {
      Set<Proposal> preVotedProposals = getCycleState(prevCycle).getPreVotedProposals(simulation);// >=2
      if(preVotedProposals.size()>=1){
        System.out.println("has proposal for pre-voting from previous cycle"); // test thì không xảy ra trường hợp này (>=2 proposals) !!!
      }
      for (Proposal preVotedProposal : preVotedProposals) {
        if (preVotedProposal != null) {
          return preVotedProposal;
        }
      }
    }

    // If we got here, then no proposal has received 2/3 pre-votes, so we never would have
    // pre-committed any proposal. Thus, we're not locked so we're free to vote for whatever
    // proposal we've received, if any.
    // lấy proposal thuộc cycle này.
    Set<Proposal> currentProposals = getCurrentCycleState().proposals;
    if (!currentProposals.isEmpty()) {
      System.out.println("currentProposals is not empty");
      return currentProposals.iterator().next();
    } else {
      return null;
    }
  }

  private void beginPreCommit(Simulation simulation, double time) {
    System.out.println("----------------------------- " );
    System.out.println("beginPreCommit: " + this.getNodeIndex());
    System.out.println("cycle: " + this.cycle);

    protocolState = ProtocolState.PRE_COMMIT;
    Set<Proposal> preVotedProposals = getCurrentCycleState().getPreVotedProposals(simulation);
    Message message;
    if (preVotedProposals.isEmpty()) {
      message = new PreCommitMessage(cycle, null, this.getNodeIndex());
      System.out.println("beginPreCommit: no proposal" );

    } else {
      Proposal proposal = preVotedProposals.iterator().next();
      if(proposal!=null){
//        System.out.println("Non-Null proposal");

      }else{
//        System.out.println("beginPreCommit: null proposal" );

      }
      System.out.println("beginPreCommit proposal: " + proposal);


      message = new PreCommitMessage(cycle, proposal, this.getNodeIndex());
    }
    System.out.println("broadcast & resetTimeout" );

    simulation.broadcast(this, message, time);
    resetTimeout(simulation, time);
    System.out.println("----------------------------- " );

  }

  private void resetTimeout(Simulation simulation, double time) {
    nextTimer = time + timeout;
    System.out.println("TIME:resetTimeout: time: "+ time + ", timeout: "+ timeout + ", nextTimer: " + nextTimer+ ", cycle: "+ this.cycle);
    simulation.scheduleEvent(new TimerEvent(nextTimer, this)); // re nhanh --> listener lang nghe event --> chay event
  }

  private CycleState getCurrentCycleState() {
    return getCycleState(cycle);
  }

  private CycleState getCycleState(int c) {
    cycleStates.putIfAbsent(c, new CycleState());
    return cycleStates.get(c);
  }


  // 2f + 1
  private int quorumSize(Simulation simulation) {
    int nodes = simulation.getNetwork().getNodes().size();
    return nodes * 2 / 3 + 1;
  }

  private class CycleState { // round?
    final Set<Proposal> proposals = new HashSet<>(); ///
    final Map<Proposal, Integer> preVoteCounts = new HashMap<>();
    final Map<Proposal, Integer> preCommitCounts = new HashMap<>();

    // get proposals mà có >= 2f+1 votes
    Set<Proposal> getPreVotedProposals(Simulation simulation) {
      return Util.keysWithMinCount(preVoteCounts, quorumSize(simulation));
    }

    // get proposals mà có >= 2f+1 commits
    Set<Proposal> getCommittedProposals(Simulation simulation) {
      return Util.keysWithMinCount(preCommitCounts, quorumSize(simulation));
    }
  }

  private enum ProtocolState {
    PROPOSAL, PRE_VOTE, PRE_COMMIT
  }
}
