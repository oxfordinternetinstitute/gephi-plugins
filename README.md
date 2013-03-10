This repository contains a gephi plugin, SigmaExporter, to export a network from Gephi to a Sigma.js template. This allows the network to be explored interactively, searched, etc.

This repository has code to export a network in json format (data.json), display a dialog window to gather settings and write these to config.json. It then places these two files (data.json and config.json) into an existing template held as a zip file in this project. That template/zip file comes the network directory in the following repository  
https://github.com/oxfordinternetinstitute/InteractiveVis/tree/master/network  
If anyone wishes to contribute changes to the HTML/JavaScript, please branch that repository.

This project was started by [Scott A. Hale](http://www.scotthale.net/) and [Joshua Melville](http://www.oii.ox.ac.uk/people/?id=273) at the [Oxford Internet Institute](http://www.oii.ox.ac.uk/), University of Oxford. Further background information on the project is available on [the project blog](http://blogs.oii.ox.ac.uk/vis/)

The plugin is listed in [the Gephi marketplace here](https://marketplace.gephi.org/plugin/sigmajs-exporter/). There is also a plugin to simply export a network into JSON format also contained within this repository and [listed separately in the Gephi marketplace](https://marketplace.gephi.org/plugin/json-exporter/).
