import thera.Thera

object Main1 {

  def main(args: Array[String]): Unit = {
    val template =
      """
---
system:
  name: Solar System
  centralBody: Sun
  planets:
    -  name: "Mercury", mass: "3.30 * 10^23" }
    - { name: "Mars", mass: " 6.42 * 10^23" }
    - { name: "Venus", mass: "4.87 * 10^24" }
    - { name: "Earth", mass: "5.97 * 10^24" }
    - { name: "Uranus", mass: " 8.68 * 10^25" }
    - { name: "Neptune", mass: "1.02 * 10^26" }
    - { name: "Saturn", mass: " 5.68 * 10^26" }
    - { name: "Jupiter", mass: "1.90 * 10^27" }
---
Hello! We are located at the ${system.name}!
The central body here is ${system.centralBody}.
The planets and their masses are as follows:
${foreach: ${system.planets}, ${planet => \
  - ${planet.name} - ${planet.mass}
}}
"""

//    println(Thera(getClass.getResource("template1")).mkString)
    println(Thera(template).mkString)

  }
}
