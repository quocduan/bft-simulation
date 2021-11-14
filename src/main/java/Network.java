import java.util.List;
import java.util.Random;

abstract class Network {
  /** The speed of light in a vacuum, in meters per second. */
  /* toc do anh sang trong chan khong*/
  static double SPEED_OF_LIGHT = 299792458.0;

  /** The speed of light through a typical fiber optic cable, in meters per second. */
  // toc do cap quang
  static double SPEED_OF_FIBER = speedOfLight(1.4682); //1.4682: chiet suat cap quang

  private final List<Node> nodes;

  Network(List<Node> nodes) {
    this.nodes = nodes;
  }

  List<Node> getNodes() {
    return nodes;
  }

  Node getLeader(int index) {
    // Round robin.
    return nodes.get(index % nodes.size());
  }

  /**
   * The one-way latency, in seconds, taken to deliver a message from {@code source} to
   * {@code destination}.
   */
  abstract double getLatency(Node source, Node destination);

  /** The speed of light through a medium with a given index of refraction, in meters per second. */
  // Tốc độ ánh sáng qua môi trường có chiết suất cho trước
  private static double speedOfLight(double refractiveIndex) {
    return SPEED_OF_LIGHT / refractiveIndex;
  }
}

/**
 * A network in which all nodes are directly connected <-- chỗ này simulated bằng cách get latency giữa các node với nhau khi truyền (có latency --> assumed các node connected với nhau)
 * through a fiber optic cable, but there are
 * random delays up to 3x.
 */
class FullyConnectedNetwork extends Network {
  private final Random random;

  FullyConnectedNetwork(List<Node> nodes, Random random) {
    super(nodes);
    this.random = random;
  }

  double getLatency(Node source, Node destination) {
    // thoi gian truyen tai tot nhat giua 2 node
    double bestCaseLatency = source.getDistance(destination) / Network.SPEED_OF_FIBER;
    // random.nextDouble(); -->returns the next pseudorandom, uniformly distributed double value between 0.0 and 1.0 from this random number generator’s sequence.
    double multiplier = 1 + random.nextDouble();
    // mục đích là tạo ra độ trễ ngẫu nhiên theo hệ số nhân từ: 0.0 --> 1.0
    return multiplier * bestCaseLatency;
  }
}
