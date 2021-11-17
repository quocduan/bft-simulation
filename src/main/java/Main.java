import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class Main {
  private static final int RANDOM_SEED = 12345;
  private static final double TIME_LIMIT = 3; //4s
  private static final int SAMPLES = 1000;

  public static void main(String[] args) {
    // Print the first row which contains column names.
    System.out.println("initial_timeout, tendermint, algorand, this_work");

    double tendermintBestLatency = Double.MAX_VALUE, tendermintBestTimeout = 0;
    double algorandBestLatency = Double.MAX_VALUE, algorandBestTimeout = 0;
    double mirBestLatency = Double.MAX_VALUE, mirBestTimeout = 0;

    for (double initialTimeout = 0.01; initialTimeout <= 0.01; initialTimeout += 0.01) { // 40 lan
//    for (double initialTimeout = 3; initialTimeout <= 3; initialTimeout += 0.01) { // 40 lan
//    for (double initialTimeout = 1; initialTimeout <= 1; initialTimeout += 0.01) { // 40 lan
//    for (double initialTimeout = 0.00001; initialTimeout <= 0.00001; initialTimeout += 0.01) { // 40 lan
//    for (double initialTimeout = 0.1; initialTimeout <= 0.1; initialTimeout += 0.01) { // 40 lan
      DoubleSummaryStatistics tendermintOverallStats = new DoubleSummaryStatistics(),
          algorandOverallStats = new DoubleSummaryStatistics(),
          mirOverallStats = new DoubleSummaryStatistics();



      System.out.println("Start run initialTimeout="+initialTimeout);
//      for (int i = 0; i < SAMPLES; ++i) { // 1000 --> chạy 1000 lần cho một initialTimout để lấy số liệu thống kê trung bình chính xác nhất.
        Optional<DoubleSummaryStatistics> tendermintStats =
//            runTendermint(initialTimeout, 3, 1); // 100
            runTendermint(initialTimeout, 3, 1); // 100
      System.out.println("End run initialTimeout="+initialTimeout);
      System.out.println("Total Proposals: "+ Simulation.countProposals);
      System.out.println("Total countBeginProposal: "+ CorrectTendermintNode.countBeginProposal);
      System.out.println("Total countFailedNode: "+ FailedNode.countFailedNode);
      System.out.println("max Cycle "+ (CorrectTendermintNode.maxCycle+1));
      System.out.println("Max Events Size "+ Simulation.maxEventsSize);





//        Optional<DoubleSummaryStatistics> algorandStats =
//            runAlgorand(initialTimeout, 90, 10);
//
//
//        Optional<DoubleSummaryStatistics> mirStats =
//            runMir(initialTimeout, 90, 10);

        tendermintStats.ifPresent(tendermintOverallStats::combine);
//        algorandStats.ifPresent(algorandOverallStats::combine);
//        mirStats.ifPresent(mirOverallStats::combine);
//      }

      if (tendermintOverallStats.getCount() > 0 &&
          tendermintOverallStats.getAverage() < tendermintBestLatency) {
        tendermintBestLatency = tendermintOverallStats.getAverage();
        tendermintBestTimeout = initialTimeout;
      }
      if (algorandOverallStats.getCount() > 0 &&
          algorandOverallStats.getAverage() < algorandBestLatency) {
        algorandBestLatency = algorandOverallStats.getAverage();
        algorandBestTimeout = initialTimeout;
      }
      if (mirOverallStats.getCount() > 0 &&
          mirOverallStats.getAverage() < mirBestLatency) {
        mirBestLatency = mirOverallStats.getAverage();
        mirBestTimeout = initialTimeout;
      }

      System.out.printf("%.2f, %s, %s, %s\n",
          initialTimeout,
          tendermintOverallStats.getCount() > 0 ? tendermintOverallStats.getAverage() : "",
          algorandOverallStats.getCount() > 0 ? algorandOverallStats.getAverage() : "",
          mirOverallStats.getCount() > 0 ? mirOverallStats.getAverage() : "");
    }

    System.out.println();
    System.out.printf("Tendermint best with timeout %.2f: %.4f\n",
        tendermintBestTimeout, tendermintBestLatency);
//    System.out.printf("Algorand best with timeout %.2f: %.4f\n",
//        algorandBestTimeout, algorandBestLatency);
//    System.out.printf("Mir best with timeout %.2f: %.4f\n",
//        mirBestTimeout, mirBestLatency);
//    double secondBestLatency = Math.min(tendermintBestLatency, algorandBestLatency);
//    System.out.printf("Mir speedup: %.4f\n",
//        (secondBestLatency - mirBestLatency) / secondBestLatency);
  }

  private static Optional<DoubleSummaryStatistics> runTendermint(
      double initialTimeout, int correctNodeCount, int failedNodeCount) {
    Random random = new Random();
    List<Node> nodes = new ArrayList<>();
    // tạo correct nodes
    for (int i = 0; i < correctNodeCount; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random); // 0 --> 89
      nodes.add(new CorrectTendermintNode(position, initialTimeout, i));
    }

    // tạo failed nodes
    for (int i = 0; i < failedNodeCount; ++i) { // 90 --> 99
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new FailedNode(position, correctNodeCount+i));
    }
    // trộn lại với nhau thành tập nodes ngẫu nhiên
     Collections.shuffle(nodes, random); // LIST <-- 0,1,2,4 (42, 81, 29, 13)

    // gia lap network, cac nodes connect voi nhau
    Network network = new FullyConnectedNetwork(nodes, random);

    Simulation simulation = new Simulation(network, initialTimeout);
    if (!simulation.run(TIME_LIMIT)) { // nguong tren
      return Optional.empty();
    }
    List<String> proposedNodeIndices = new ArrayList<>();
    for(Node node: CorrectTendermintNode.listProposedNode){
      proposedNodeIndices.add(Integer.toString(nodes.indexOf(node)));

    }


    System.out.println("First 10 nodes in list");

//    for(int i=0;i<=10;i++){
    for(int i=0;i<=3;i++){
      System.out.print(nodes.get(i).getClass().getSimpleName()+ ",");
    }
    System.out.println("");

    System.out.print("Proposed Nodes: ");
    for (Node node : CorrectTendermintNode.listProposedNode) {
      System.out.print(node.getNodeIndex() + ", ");

    }
    System.out.println("");
    System.out.println("Index in node array: " + String.join(", ", proposedNodeIndices));

//    System.out.println(",".joi);

    List<Node> correctNodes = nodes.stream()
        .filter(n -> n instanceof CorrectTendermintNode)
        .collect(Collectors.toList());
    if (!correctNodes.stream().allMatch(Node::hasTerminated)) {
      System.out.println("WARNING: Not all Tendermint nodes terminated.");
      return Optional.empty();
    }

    return Optional.of(correctNodes.stream()
        .mapToDouble(Node::getTerminationTime)
        .summaryStatistics());
  }

  private static Optional<DoubleSummaryStatistics> runAlgorand(
      double initialTimeout, int correctNodeCount, int failedNodeCout) {
    Random random = new Random();
    List<Node> nodes = new ArrayList<>();
    for (int i = 0; i < correctNodeCount; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new CorrectAlgorandNode(position, initialTimeout));
    }
    for (int i = 0; i < failedNodeCout; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new FailedNode(position));
    }
    Collections.shuffle(nodes, random);

    Network network = new FullyConnectedNetwork(nodes, random);
    Simulation simulation = new Simulation(network);
    if (!simulation.run(TIME_LIMIT)) {
      return Optional.empty();
    }

    List<Node> correctNodes = nodes.stream()
        .filter(n -> n instanceof CorrectAlgorandNode)
        .collect(Collectors.toList());
    if (!correctNodes.stream().allMatch(Node::hasTerminated)) {
      System.out.println("WARNING: Not all Algorand nodes terminated.");
      return Optional.empty();
    }

    //System.out.println("Algorand times: " + correctNodes.stream().mapToDouble(Node::getTerminationTime).sorted().boxed().collect(Collectors.toList()));
    return Optional.of(nodes.stream()
        .mapToDouble(Node::getTerminationTime)
        .summaryStatistics());
  }

  private static Optional<DoubleSummaryStatistics> runMir(
      double initialTimeout, int correctNodeCount, int failedNodeCount) {
    Random random = new Random();
    List<Node> nodes = new ArrayList<>();
    for (int i = 0; i < correctNodeCount; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new CorrectMirNode(position, initialTimeout));
    }
    for (int i = 0; i < failedNodeCount; ++i) {
      EarthPosition position = EarthPosition.randomPosition(random);
      nodes.add(new FailedNode(position));
    }
    Collections.shuffle(nodes, random);

    Network network = new FullyConnectedNetwork(nodes, random);
    Simulation simulation = new Simulation(network);
    if (!simulation.run(TIME_LIMIT)) {
      return Optional.empty();
    }

    List<Node> correctNodes = nodes.stream()
        .filter(n -> n instanceof CorrectMirNode)
        .collect(Collectors.toList());
    if (!correctNodes.stream().allMatch(Node::hasTerminated)) {
      System.out.println("WARNING: Not all Mir nodes terminated.");
      return Optional.empty();
    }

    //System.out.println("Mir times: " + correctNodes.stream().mapToDouble(Node::getTerminationTime).sorted().boxed().collect(Collectors.toList()));
    return Optional.of(nodes.stream()
        .mapToDouble(Node::getTerminationTime)
        .summaryStatistics());
  }

  private static String statisticsToCompactString(DoubleSummaryStatistics statistics) {
    return String.format("min=%.2f, max=%.2f, average=%.2f",
        statistics.getMin(), statistics.getMax(), statistics.getAverage());
  }
}
