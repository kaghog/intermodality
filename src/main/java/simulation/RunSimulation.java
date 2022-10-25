package simulation;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.*;
//import intermodal.SwissIntermodalModule;
import intermodal.SwissIntermodalModule;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import simulation.utils.AdjustPopulation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {

		SimulationParameter simulationParams = new SimulationParameter();

		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowOptions("intermodal-search-factor", "min-access-dist", "person-filter-type") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), new SwissRailRaptorConfigGroup(), new EqasimConfigGroup(), new DiscreteModeChoiceConfigGroup());
		cmd.applyConfiguration(config);

		//set intermodal configuration params from command line options
		simulationParams.setIntermodalParams(cmd);

		//Set bike run options
		simulationParams.setBikeParams(cmd);

		Scenario scenario = ScenarioUtils.createScenario(config);

		SwitzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		
		//add household attributes to scenario such as bike availability
		SwitzerlandConfigurator.adjustScenario(scenario);


		// Adjust for person filter attributes in the SRR configuration for bikes
		SwissRailRaptorConfigGroup srrConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
		for (SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet srrParams: srrConfig.getIntermodalAccessEgressParameterSets()){
			if (srrParams.getMode().equals("bike")){
				if (simulationParams.getPersonFilterType().equals("none")){
					//ensuring that there are no person filter attribute set in the config
					srrParams.setPersonFilterAttribute(null);
					srrParams.setPersonFilterValue(null);
				} else {
					AdjustPopulation.setIntermodalPersonFilterAttribute(scenario, simulationParams.getPersonFilterType()); //bikeOwnership & bikeOwnershipPTSubscription
					srrParams.setPersonFilterAttribute("bikeIntermodalAvailable");
					srrParams.setPersonFilterValue("true");
				}
			}
		}
		//set bike to be allowed on non-motorways and non-trunk network links? toDo confirm for network mode
		//important to put it after loading scenario or it would not really show an effect

		//set bike and walk on car roads since those are connected properly
		for (Link link : scenario.getNetwork().getLinks().values()) {
			
			if (link.getAllowedModes().contains("car")) {
				//Add walk or bike to all links
				Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
				allowedModes.add(TransportMode.bike);
				link.setAllowedModes(allowedModes);
			}

		}
		
		//ToDo network needs to be filtered properly for bike and then cleaned
		/*for (Link link : scenario.getNetwork().getLinks().values()) {
			*//*if (//link.getAttributes().getAttribute("osm:way:highway").equals("motorway") |
					//link.getAttributes().getAttribute("osm:way:highway").equals("trunk") |
				//not an artificial pt link
					link.getAttributes().getAttribute("osm:way:access")==null ? true : link.getAttributes().getAttribute("osm:way:access").equals("no")
			) {
				continue;
			}
			if (link.getAttributes().getAttribute("osm:way:access")==null ? true : link.getAttributes().getAttribute("osm:way:access").equals("no")) {
				continue;
			}*//*
			//Add bike to links whose osm attributes are neither motorway or trunk
			Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
			allowedModes.add(TransportMode.bike);
			link.setAllowedModes(allowedModes);

		}*/



		Controler controller = new Controler(scenario);
		controller.addOverridingModule(new SwissRailRaptorModule());
		controller.addOverridingModule(new DiscreteModeChoiceModule());

		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SwissIntermodalModule(cmd));
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(SimulationParameter.class).toInstance(simulationParams); }
		});

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(RaptorStopFinder.class).to(SwissRaptorStopFinder.class);
			}
		});
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() { bind(RaptorIntermodalAccessEgress.class).to(SwissRaptorIntermodalAccessEgress.class);
			}});


		controller.run();
	}
}
