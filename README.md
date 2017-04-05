# Ocelot

Ocelot is a development platform aimed at prototyping distributed algorithms
and as a support for teaching the design of such algorithms. The platform is written in
Scala and allows to write distributed algorithms in a very concise yet
easily-readable way.

Ocelot is inspired by ScalaNeko; a project developed at [JAIST](www.jaist.ac.jp) by Xavier Défago and used
for his lectures until 2015. ScalaNeko was itself based on the Neko toolkit [[1,2]](#Refs); a project originally developed
at [EPFL](www.epfl.ch) by Péter Urbán, Xavier Défago, and several other contributors in
the laboratory of Prof. André Schiper (now retired).
The Neko toolkit was written in Java and allowed, *without changing its
code*, to execute a distributed program either on a real network or in a
simulation. The purpose was to support the lifecycle of a distributed
algorithm from design to performance evaluation, under the premise that
a change in writing style between simulation and execution would be an
undesirable factor affecting performance measurements.

While Ocelot does not retain yet the ability to execute on a real network
(i.e., it supports simulation-only), its purpose is currently exclusively aimed at
teaching. With this in mind, great efforts where directed at simplifying
the way distributed algorithms would be expressed in that model.

This version of Ocelot is at a very experimental stage and it is not recommended
for normal use. There are many bugs that will be addressed over time, and no efforts will be
made to retain backward compatibility. Use at your own risks!
The documentation generated through scaladoc provide some information on how to use it and its
syntax, but it is definitely incomplete and even partly stale.

Although the repository is made public, we do not consider it to be easily usable without explicit instructions.
Anyone is free to install and use Ocelot in its current form, but this is at your own risk and
absolutely no support will be offered about it at this stage.

Ocelot will be used in a lecture on Distributed Algorithms at
[Tokyo Institute of Technology](www.titech.ac.jp) and we intend to gradually provide a complete
 documentation and full support for it.

### <a name="Refs"></a> References

1. P. Urbán, X. Défago, and A. Schiper.
   [Neko: A single environment to simulate and prototype distributed algorithms](http://www.iis.sinica.edu.tw/JISE/2002/200211_07.html).
   _Journal of Information Science and Engineering_, 18(6):981-997, November 2002.

2. P. Urbán, X. Défago, and A. Schiper.
   [Neko: A single environment to simulate and prototype distributed algorithms](http://dx.doi.org/10.1109/ICOIN.2001.905471).
   In _Proc. 15th IEEE Intl. Conf. on Information Networking (ICOIN)_, pp. 503-511, Beppu City, Japan, January 2001.
