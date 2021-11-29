# The MATSim Open Kelheim Scenario for Matsim Advanced course (as an example)

[![Build Status](https://github.com/matsim-scenarios/matsim-kelheim/actions/workflows/build.yaml/badge.svg)](https://github.com/matsim-scenarios/matsim-kelheim/actions/workflows/build.yaml)
![license](https://img.shields.io/github/license/matsim-scenarios/matsim-kehlheim.svg)
![JDK](https://img.shields.io/badge/JDK-11+-green.svg)

![Kelheim MATSim network and agents](scenarios/visualization-kehlheim.png "Kelheim MATSim network and agents")


### About this project

This repository provides an open MATSim transport model for Kelheim, provided by the [Transport Systems Planning and Transport Telematics group](https://www.vsp.tu-berlin.de) of [Technische Universität Berlin](http://www.tu-berlin.de).

<a rel="TU Berlin" href="https://www.vsp.tu-berlin.de"><img src="https://svn.vsp.tu-berlin.de/repos/public-svn/ueber_uns/logo/TUB_Logo.png" width="15%" height="15%"/></a>

This scenario contains a 25pct sample of Kelheim and its surrounding area; road capacities are accordingly reduced. The scenario is calibrated taking into consideration the traffic counts, modal split and mode-specific trip distance distributions.

### Licenses

The **MATSim program code** in this repository is distributed under the terms of the [GNU General Public License as published by the Free Software Foundation (version 2)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html). The MATSim program code are files that reside in the `src` directory hierarchy and typically end with `*.java`.

The **MATSim input files, output files, analysis data and visualizations** are licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/4.0/">Creative Commons Attribution 4.0 International License</a>.
<a rel="license" href="http://creativecommons.org/licenses/by/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by/4.0/80x15.png" /></a><br /> MATSim input files are those that are used as input to run MATSim. They often, but not always, have a header pointing to matsim.org. They typically reside in the `scenarios` directory hierarchy. MATSim output files, analysis data, and visualizations are files generated by MATSim runs, or by postprocessing.  They typically reside in a directory hierarchy starting with `output`.

**Other data files**, in particular in `original-input-data`, have their own individual licenses that need to be individually clarified with the copyright holders.

### Note

Handling of large files within git is not without problems (git lfs files are not included in the zip download; we have to pay; ...).  In consequence, large files, both on the input and on the output side, reside at https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/kehlheim .

----
### Run the MATSim Düsseldorf scenario

The Kelheim scenario has a command line interface providing the following options:

![Kelheim Scenario CLI](scenarios/cli.png "Kelheim Scenario CLI")

It can be used by using either of these methods:

##### ... using an IDE, e.g. Eclipse, IntelliJ - Alternative 1: use cloned/downloaded matsim-kehlheim repository
(Requires either cloning or downloading the repository.)

1. Set up the project in your IDE.
1. Make sure the project is configured as maven project.
1. Run the JAVA class `src/main/java/org/matsim/run/RunKelheimScenario.java`.
1. "Open" the output directory.  You can drag files into VIA as was already done above.
1. Edit the config file or adjust the run class. Re-run MATSim.

##### ... using a runnable jar file
(Requires either cloning or downloading the repository and java)

1. Open the cmd and go to your project directory
2. Build the scenario using `mvnw package`
3. There should be a file directly in the `matsim-kehlheim` directory with name approximately as `matsim-kehlheim-1.0.jar`.
4. Run this file from the command line using `java -jar matsim-kehlheim-1.0.jar --help` to see all possible options.
  1. For example, one can disable lanes or run the 25pct scenario using the available options
5. Start this scenario using the default config by running `java -jar matsim-kehlheim-1.0.jar`
6. "Open" the output directory.  You can drag files into VIA as was already done above.
